package org.cobbzilla.wizard.dao.shard;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.collection.mappy.MappyList;
import org.cobbzilla.util.collection.SingletonList;
import org.cobbzilla.util.reflect.ReflectionUtil;
import org.cobbzilla.wizard.dao.DAO;
import org.cobbzilla.wizard.dao.SearchResults;
import org.cobbzilla.wizard.model.Identifiable;
import org.cobbzilla.wizard.model.ResultPage;
import org.cobbzilla.wizard.model.shard.ShardIO;
import org.cobbzilla.wizard.model.shard.ShardMap;
import org.cobbzilla.wizard.model.shard.ShardRange;
import org.cobbzilla.wizard.server.config.DatabaseConfiguration;
import org.cobbzilla.wizard.server.config.ShardSetConfiguration;
import org.cobbzilla.wizard.spring.config.rdbms.RdbmsConfigCommon;
import org.cobbzilla.wizard.spring.config.rdbms.RdbmsConfigSimple;
import org.springframework.context.ApplicationContext;

import javax.validation.Valid;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.cobbzilla.util.daemon.ZillaRuntime.*;
import static org.cobbzilla.util.reflect.ReflectionUtil.getFirstTypeParam;
import static org.cobbzilla.util.reflect.ReflectionUtil.instantiate;
import static org.cobbzilla.util.security.ShaUtil.sha256_hex;
import static org.cobbzilla.wizard.util.SpringUtil.autowire;

@Slf4j
public abstract class AbstractShardedDAO<E extends Identifiable, D extends SingleShardDAO<E>> implements DAO<E> {

    @Getter @Setter private ApplicationContext applicationContext;
    @Getter @Setter private DatabaseConfiguration masterConfiguration;
    @Getter @Setter private ShardSetConfiguration shardConfiguration;
    @Getter @Setter private ShardMapDAO shardMapDAO;

    private final Class<E> entityClass;
    private final Class<D> singleShardDaoClass;

    private final Map<ShardMap, D> daos = new ConcurrentHashMap<>();

    public AbstractShardedDAO() {
        this.entityClass = getFirstTypeParam(getClass(), Identifiable.class);
        this.singleShardDaoClass = initShardDaoClass();
    }

    protected Class initShardDaoClass() { return getFirstTypeParam(getClass(), SingleShardDAO.class); }

    @Getter(lazy=true) private final ShardMap defaultShardMap = initDefaultShardMap();
    private ShardMap initDefaultShardMap () {
        log.warn("no shards defined, using master DB only: "+entityClass.getName());
        return new ShardMap()
                .setShardSet(entityClass.getName())
                .setRange(new ShardRange(0, Integer.MAX_VALUE))
                .setUrl(masterConfiguration.getUrl())
                .setAllowRead(true)
                .setAllowWrite(true);
    }

    protected List<D> getDAOs(Serializable id) { return getDAOs(id, ShardIO.read); }
    protected List<D> getDAOs(Serializable id, ShardIO shardIO) {
        List<ShardMap> shardMaps = shardMapDAO.findByEntityAndLogicalShard(getShardConfiguration().getName(), getLogicalShard(id), shardIO);
        if (shardMaps.isEmpty()) shardMaps = new SingletonList<>(getDefaultShardMap());
        final List<D> found = toDAOs(shardMaps);
        return found;
    }

    protected List<D> getDAOs(E entity) { return getDAOs(entity, ShardIO.read); }
    protected List<D> getDAOs(E entity, ShardIO shardIO) {
        final Object value = getIdToHash(entity);
        if (value == null) die("hashOn field "+shardConfiguration.getHashOn()+" was null");
        return getDAOs(value.toString(), shardIO);
    }

    protected int getLogicalShard(Serializable id) {
        final String hash = sha256_hex(id.toString()).substring(0, 8);
        return Math.abs(Integer.parseInt(hash, 16)) % shardConfiguration.getLogicalShards();
    }

    private List<D> toDAOs(Collection<ShardMap> shardMaps) {
        final List<D> list = new ArrayList<>();
        for (ShardMap map : shardMaps) list.add(toDAO(map));
        return list;
    }

    private D toDAO(ShardMap map) {
        D dao = daos.get(map);
        if (dao == null) {
            synchronized (daos) {
                dao = daos.get(map);
                if (dao == null) {
                    // Jack-up the DAO's database and hibernateTemplate to point to the shard DB
                    dao = autowire(applicationContext, instantiate(singleShardDaoClass));
                    dao.setDatabase(masterConfiguration.getShardDatabaseConfiguration(map));
                    final RdbmsConfigCommon rdbms = new RdbmsConfigSimple(dao.getDatabase());
                    dao.setHibernateTemplate(rdbms.hibernateTemplate(rdbms.sessionFactory().getObject()));
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
        if (value == null) die("hashOn field "+shardConfiguration.getHashOn()+" was null");
        return getDAO(value.toString(), shardIO);
    }

    protected Object getIdToHash(E entity) { return ReflectionUtil.get(entity, shardConfiguration.getHashOn()); }

    public List<D> getNonOverlappingDAOs() {
        List<ShardMap> shards = shardMapDAO.findReadShards(entityClass.getName());
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

    @Override public SearchResults<E> search(ResultPage resultPage) { return notSupported(); }

    @Override public SearchResults<E> search(ResultPage resultPage, String entityType) { return notSupported(); }

    @Override public E get(Serializable id) { return getDAO((String) id).get(id); }

    @Override public List<E> findAll() { return notSupported(); }

    @Override public E findByUuid(String uuid) { return get(uuid); }

    @Override public E findByUniqueField(String field, Object value) {
        if (shardConfiguration.getHashOn().equals(field)) return getDAO((String) value).get((String) value);
        return notSupported();
    }

    public E findByUniqueFields(String f1, Object v1, String f2, Object v2) {
        D dao = null;
        if (shardConfiguration.getHashOn().equals(f1)) {
            dao = getDAO((String) v1);
        } else if (shardConfiguration.getHashOn().equals(f2)) {
            dao = getDAO((String) v2);
        }
        if (dao == null) return notSupported();
        return dao.findByUniqueFields(f1, v1, f2, v2);
    }

    public E findByUniqueFields(String f1, Object v1, String f2, Object v2, String f3, Object v3) {
        D dao = null;
        if (shardConfiguration.getHashOn().equals(f1)) {
            dao = getDAO((String) v1);
        } else if (shardConfiguration.getHashOn().equals(f2)) {
            dao = getDAO((String) v2);
        } else if (shardConfiguration.getHashOn().equals(f3)) {
            dao = getDAO((String) v3);
        }
        if (dao == null) return notSupported();
        return dao.findByUniqueFields(f1, v1, f2, v2);
    }

    @Override public List<E> findByField(String field, Object value) {
        if (shardConfiguration.getHashOn().equals(field)) {
            return getDAO((String) value).findByField(field, value);
        }
        return notSupported();
    }

    @Override public boolean exists(String uuid) { return get(uuid) != null; }

    @Override public Object preCreate(@Valid E entity) { return null; }

    @Override public E create(@Valid E entity) {
        E rval = null;
        for (D dao : getDAOs(entity, ShardIO.write)) {
            E created = dao.create(entity);
            if (rval == null) rval = created;
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
        for (D dao : getDAOs(entity, ShardIO.write)) {
            E updated = dao.update(entity);
            if (rval == null) rval = updated;
        }
        return rval;
    }

    @Override public E postUpdate(@Valid E entity, Object context) { return null; }

    @Override public void delete(String uuid) {
        for (D dao : getDAOs(uuid, ShardIO.read)) {
            dao.delete(uuid);
        }
        for (D dao : getDAOs(uuid, ShardIO.write)) {
            dao.delete(uuid);
        }
    }
}
