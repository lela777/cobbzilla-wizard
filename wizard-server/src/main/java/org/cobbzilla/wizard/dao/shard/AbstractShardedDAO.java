package org.cobbzilla.wizard.dao.shard;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.collection.SingletonList;
import org.cobbzilla.util.collection.mappy.MappyList;
import org.cobbzilla.util.reflect.ReflectionUtil;
import org.cobbzilla.wizard.dao.DAO;
import org.cobbzilla.wizard.dao.SearchResults;
import org.cobbzilla.wizard.dao.shard.task.*;
import org.cobbzilla.wizard.model.Identifiable;
import org.cobbzilla.wizard.model.ResultPage;
import org.cobbzilla.wizard.model.shard.ShardIO;
import org.cobbzilla.wizard.model.shard.ShardMap;
import org.cobbzilla.wizard.model.shard.ShardRange;
import org.cobbzilla.wizard.model.shard.Shardable;
import org.cobbzilla.wizard.server.ApplicationContextConfig;
import org.cobbzilla.wizard.server.CustomBeanResolver;
import org.cobbzilla.wizard.server.RestServer;
import org.cobbzilla.wizard.server.config.DatabaseConfiguration;
import org.cobbzilla.wizard.server.config.HasDatabaseConfiguration;
import org.cobbzilla.wizard.server.config.RestServerConfiguration;
import org.cobbzilla.wizard.server.config.ShardSetConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.Valid;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.cobbzilla.util.daemon.ZillaRuntime.*;
import static org.cobbzilla.util.reflect.ReflectionUtil.*;
import static org.cobbzilla.util.security.ShaUtil.sha256_hex;
import static org.cobbzilla.wizard.util.Await.awaitAndCollect;
import static org.cobbzilla.wizard.util.Await.awaitFirst;
import static org.cobbzilla.wizard.util.SpringUtil.autowire;

@Transactional @Slf4j
public abstract class AbstractShardedDAO<E extends Shardable, D extends SingleShardDAO<E>> implements DAO<E> {

    public static final int MAX_QUERY_RESULTS = 200;

    @Autowired private HasDatabaseConfiguration configuration;
    @Autowired private RestServer server;

    @Getter private final Class<E> entityClass;
    private final Class<D> singleShardDaoClass;
    private final String hashOn;

    private final Map<ShardMap, D> daos = new ConcurrentHashMap<>();

    private static final long DAO_MAP_CLEAN_INTERVAL = TimeUnit.DAYS.toMillis(1);
    private final AtomicLong daosLastCleaned = new AtomicLong(0);
    private void cleanDaoMap() { new DaoMapCleaner<>(daos, getShardDAO()).start(); }

    public static final long DEFAULT_SHARD_QUERY_TIMEOUT = TimeUnit.SECONDS.toMillis(30);
    protected long getShardQueryTimeout (String method) { return DEFAULT_SHARD_QUERY_TIMEOUT; }

    public static final int DEFAULT_MAX_QUERY_THREADS = 100;
    protected int getMaxQueryThreads () { return DEFAULT_MAX_QUERY_THREADS; }

    private final BlockingQueue<Runnable> queryWorkerQueue = new LinkedBlockingQueue<>();
    private final ThreadPoolExecutor queryWorkerPool = new ThreadPoolExecutor(getMaxQueryThreads()/2, getMaxQueryThreads(), 10, TimeUnit.MINUTES, queryWorkerQueue);

    protected ApplicationContext getApplicationContext(DatabaseConfiguration database) {

        final HasDatabaseConfiguration singleShardConfig = instantiate(configuration.getClass());
        copy(singleShardConfig, configuration);
        singleShardConfig.setDatabase(database);
        singleShardConfig.getDatabase().getHibernate().setHbm2ddlAuto("validate");

        final ApplicationContextConfig ctxConfig = new ApplicationContextConfig()
                .setConfig((RestServerConfiguration) singleShardConfig)
                .setResolvers(new CustomBeanResolver[] {new CustomShardedDAOResolver(server.getApplicationContext())})
                .setSpringContextPath(getSpringShardContextPath());
        final ApplicationContext applicationContext = server.buildSpringApplicationContext(ctxConfig);
        ((RestServerConfiguration) singleShardConfig).setApplicationContext(applicationContext);

        return applicationContext;
    }

    protected String getSpringShardContextPath() { return "spring-shard.xml"; }

    protected abstract ShardSetConfiguration getShardConfiguration();
    protected abstract DatabaseConfiguration getMasterDbConfiguration();
    protected abstract ShardMapDAO getShardDAO();

    public AbstractShardedDAO() {
        this.entityClass = getFirstTypeParam(getClass(), Identifiable.class);
        this.hashOn = instantiate(this.entityClass).getHashToShardField();
        this.singleShardDaoClass = initShardDaoClass();
    }
    protected Class<D> initShardDaoClass() { return getFirstTypeParam(getClass(), SingleShardDAO.class); }

    @Getter(lazy=true) private final ShardMap defaultShardMap = initDefaultShardMap();
    private ShardMap initDefaultShardMap () {
        log.warn("no shards defined, using master DB only: "+getShardConfiguration().getName());
        return new ShardMap()
                .setShardSet(getShardConfiguration().getName())
                .setRange(new ShardRange(0, Integer.MAX_VALUE))
                .setUrl(getMasterDbConfiguration().getUrl())
                .setAllowRead(true)
                .setAllowWrite(true)
                .setDefaultShard(true);
    }

    protected List<D> getDAOs(Serializable id) { return getDAOs(id, ShardIO.read); }
    protected List<D> getDAOs(Serializable id, ShardIO shardIO) {
        List<ShardMap> shardMaps = getShardDAO().findByEntityAndLogicalShard(getShardConfiguration().getName(), getLogicalShard(id), shardIO);
        if (shardMaps.isEmpty()) shardMaps = new SingletonList<>(getDefaultShardMap());
        final List<D> found = toDAOs(shardMaps);
        return found;
    }

    protected List<D> getDAOs(ShardIO shardIO) {
        List<ShardMap> shards;
        switch (shardIO) {
            case read: shards = getReadShards(); break;
            case write: shards = getWriteShards(); break;
            default: return die("getDAOs: invalid shardIO: "+shardIO);
        }
        if (shards.isEmpty()) shards = new SingletonList<>(getDefaultShardMap());
        return toDAOs(shards);
    }

    protected List<D> getDAOs(E entity) { return getDAOs(entity, ShardIO.read); }
    protected List<D> getDAOs(E entity, ShardIO shardIO) {
        final Object value = getIdToHash(entity);
        if (value == null) die("getDAOs: value of hashOn field ("+hashOn+") was null");
        return getDAOs(value.toString(), shardIO);
    }

    protected int getLogicalShard(Serializable id) {
        final String hash = sha256_hex(id.toString()).substring(0, 7);
        Long val;
        try {
            val = Long.valueOf(hash, 16);
        } catch (NumberFormatException e) {
            log.warn("wtf");
            return 0;
        }
        return (int) (Math.abs(val) % getShardConfiguration().getLogicalShards());
    }

    private List<D> toDAOs(Collection<ShardMap> shardMaps) {
        final List<D> list = new ArrayList<>();
        for (ShardMap map : shardMaps) list.add(toDAO(map));
        return list;
    }

    private D toDAO(ShardMap map) {
        if (now() - daosLastCleaned.get() > DAO_MAP_CLEAN_INTERVAL) cleanDaoMap();
        D dao = daos.get(map);
        if (dao == null) {
            synchronized (daos) {
                dao = daos.get(map);
                if (dao == null) {
                    // Wire-up the DAO's database and hibernateTemplate to point to the shard DB
                    final DatabaseConfiguration database = getMasterDbConfiguration().getShardDatabaseConfiguration(map);
                    final ApplicationContext ctx = getApplicationContext(database);
                    dao = autowire(ctx, instantiate(singleShardDaoClass));
                    dao.initialize();
                    daos.put(map, dao);
                }
            }
        }
        return dao;
    }

    protected D getDAO(Serializable id) { return getDAO(id, ShardIO.read); }
    protected D getDAO(Serializable id, ShardIO shardIO) { return pickRandom(getDAOs(id, shardIO)); }

    protected D getDAO(E entity) { return getDAO(entity, ShardIO.read); }
    protected D getDAO(E entity, ShardIO shardIO) {
        final Object value = getIdToHash(entity);
        if (value == null) die("hashOn field "+hashOn+" was null");
        return getDAO(value.toString(), shardIO);
    }

    protected Object getIdToHash(E entity) { return ReflectionUtil.get(entity, hashOn); }

    @Transactional(readOnly=true)
    public List<D> getNonOverlappingDAOs() {
        List<ShardMap> shards = getReadShards();
        if (shards.isEmpty()) {
            shards = new SingletonList<>(getDefaultShardMap());
        } else {
            final MappyList<ShardRange, ShardMap> nonOverlapping = new MappyList<>();
            for (ShardMap shard : shards) {
                nonOverlapping.put(shard.getRange(), shard);
            }
            shards = new ArrayList<>();
            for (ShardRange range : nonOverlapping.keySet()) {
                shards.add(pickRandom(nonOverlapping.getAll(range)));
            }
        }
        return toDAOs(shards);
    }

    public List<ShardMap> getReadShards() { return getShardDAO().findReadShards(getShardConfiguration().getName()); }
    public List<ShardMap> getWriteShards() { return getShardDAO().findWriteShards(getShardConfiguration().getName()); }

    @Transactional(readOnly=true)
    @Override public SearchResults<E> search(ResultPage resultPage) { return notSupported(); }

    @Transactional(readOnly=true)
    @Override public SearchResults<E> search(ResultPage resultPage, String entityType) { return notSupported(); }

    @Transactional(readOnly=true)
    @Override public E get(Serializable id) { return getDAO((String) id).get(id); }

    @Transactional(readOnly=true)
    @Override public List<E> findAll() { return notSupported(); }

    @Transactional(readOnly=true)
    @Override public E findByUuid(final String uuid) {
        if (hashOn.equals("uuid")) return get(uuid);
        return findByUniqueField("uuid", uuid);
    }

    @Transactional(readOnly=true)
    @Override public E findByUniqueField(String field, Object value) {
        if (hashOn.equals(field)) return getDAO((String) value).get((String) value);
        // have to search all shards for it
        return queryShardsUnique(new ShardFindFirstByFieldTask.Factory(field, value), "findByUniqueField");
    }

    @Transactional(readOnly=true)
    public E findByUniqueFields(String f1, Object v1, String f2, Object v2) {
        D dao = null;
        if (hashOn.equals(f1)) {
            dao = getDAO((String) v1);
        } else if (hashOn.equals(f2)) {
            dao = getDAO((String) v2);
        }
        if (dao != null) return dao.findByUniqueFields(f1, v1, f2, v2);

        // have to search all shards for it
        return queryShardsUnique(new ShardFindFirstBy2FieldsTask.Factory(f1, v1, f2, v2), "findByUniqueFields");
    }

    @Transactional(readOnly=true)
    public E findByUniqueFields(String f1, Object v1, String f2, Object v2, String f3, Object v3) {
        D dao = null;
        if (hashOn.equals(f1)) {
            dao = getDAO((String) v1);
        } else if (hashOn.equals(f2)) {
            dao = getDAO((String) v2);
        } else if (hashOn.equals(f3)) {
            dao = getDAO((String) v3);
        }
        if (dao != null) return dao.findByUniqueFields(f1, v1, f2, v2, f3, v3);

        // have to search all shards for it
        return queryShardsUnique(new ShardFindFirstBy3FieldsTask.Factory(f1, v1, f2, v2, f3, v3), "findByUniqueFields");
    }

    @Transactional(readOnly=true)
    @Override public List<E> findByField(String field, Object value) {
        if (hashOn.equals(field)) {
            return getDAO((String) value).findByField(field, value);
        }

        // have to search all shards for it
        return queryShardsList(new ShardFindByFieldTask.Factory(field, value), "findByField");
    }

    @Transactional(readOnly=true)
    public List<E> findByFields(String f1, Object v1, String f2, Object v2) {
        if (hashOn.equals(f1)) {
            return getDAO((String) v1).findByFields(f1, v1, f2, v2);
        }

        // have to search all shards for it
        return queryShardsList(new ShardFindBy2FieldsTask.Factory(f1, v1, f2, v2), "findByFields");
    }

    @Transactional(readOnly=true)
    public List<E> findByFields(String f1, Object v1, String f2, Object v2, String f3, Object v3) {
        if (hashOn.equals(f1)) {
            return getDAO((String) v1).findByFields(f1, v1, f2, v2, f3, v3);
        }

        // have to search all shards for it
        return queryShardsList(new ShardFindBy2FieldsTask.Factory(f1, v1, f2, v2), "findByFields");
    }

    protected E queryShardsUnique(ShardTaskFactory<E, D, E> factory, String ctx) {
        try {
            // Start iterator tasks on all DAOs
            final List<Future<E>> futures = new ArrayList<>();
            for (D dao : getNonOverlappingDAOs()) {
                futures.add(queryWorkerPool.submit(factory.newTask(dao)));
            }

            // Wait for all iterators to finish (or for enough to finish that the rest get cancelled)
            return awaitFirst(futures, getShardQueryTimeout(ctx));

        } finally {
            factory.cancelTasks();
        }
    }

    protected List<E> queryShardsList(ShardTaskFactory<E, D, List<E>> factory, String ctx) {
        try {
            // Start iterator tasks on all DAOs
            final List<Future<List<E>>> futures = new ArrayList<>();
            for (D dao : getNonOverlappingDAOs()) {
                futures.add(queryWorkerPool.submit(factory.newTask(dao)));
            }

            // Wait for all iterators to finish (or for enough to finish that the rest get cancelled)
            return awaitAndCollect(futures, MAX_QUERY_RESULTS, getShardQueryTimeout(ctx));

        } finally {
            for (ShardTask task : factory.getTasks()) task.cancel();
        }
    }

    public <R> List<R> search(ShardSearch search) {
        long timeout = search.hasTimeout() ? search.getTimeout() : getShardQueryTimeout("search");
        if (search.hasHash()) {
            final D dao = getDAO(search.getHash());
            return dao.search(search);
        } else {
            final ShardTaskFactory factory = new ShardSearchTask.Factory(search);
            try {
                // Start iterator tasks on all DAOs
                final List<Future<List<R>>> futures = new ArrayList<>();
                for (D dao : getNonOverlappingDAOs()) {
                    futures.add(queryWorkerPool.submit(factory.newTask(dao)));
                }

                // Wait for all iterators to finish (or for enough to finish that the rest get cancelled)
                return search.sort(awaitAndCollect(futures, MAX_QUERY_RESULTS, timeout));

            } finally {
                for (Object task : factory.getTasks()) ((ShardTask) task).cancel();
            }
        }
    }

    @Transactional(readOnly=true)
    @Override public boolean exists(String uuid) { return get(uuid) != null; }

    @Override public Object preCreate(@Valid E entity) { return null; }

    @Override public E create(@Valid E entity) {
        entity.beforeCreate();
        E rval = null;
        Object ctx = null;
        for (D dao : getDAOs(entity, ShardIO.write)) {
            if (ctx == null) {
                ctx = preCreate(entity);
            }
            E created = dao.create(entity);
            if (rval == null) {
                rval = created;
                postCreate(entity, ctx);
            }
        }
        return rval;
    }

    @Override public E createOrUpdate(@Valid E entity) {
        return getDAO(entity).createOrUpdate(entity);
    }

    @Override public E postCreate(E entity, Object context) { return null; }

    @Override public Object preUpdate(@Valid E entity) { return null; }

    @Override public E update(@Valid E entity) {
        E rval = null;
        Object ctx = null;
        for (D dao : getDAOs(entity, ShardIO.write)) {
            if (ctx == null) {
                ctx = preUpdate(entity);
            }
            E updated = dao.update(entity);
            if (rval == null) {
                rval = updated;
                postUpdate(entity, ctx);
            }
        }
        return rval;
    }

    @Override public E postUpdate(E entity, Object context) { return null; }

    @Override public void delete(String uuid) {
        for (D dao : getDAOs(uuid, ShardIO.read)) {
            dao.delete(uuid);
        }
        for (D dao : getDAOs(uuid, ShardIO.write)) {
            dao.delete(uuid);
        }
    }

}
