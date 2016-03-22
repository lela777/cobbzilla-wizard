package org.cobbzilla.wizard.dao.shard;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.cobbzilla.util.system.Sleep;

import java.util.concurrent.TimeUnit;

@AllArgsConstructor @Slf4j
class DAOInitializer extends Thread {

    public static final int MAX_INIT_DAO_ATTEMPTS = 5;

    private AbstractShardedDAO shardedDAO;

    @Override public void run() {
        int attempt = 1;
        final String shardSetName = shardedDAO.getMasterDbConfiguration().getShardSetName(shardedDAO.getEntityClass());
        final String prefix = "initAllDAOs(" + shardedDAO.getEntityClass().getSimpleName() + ")";
        while (attempt <= MAX_INIT_DAO_ATTEMPTS) {
            try {
                // don't all hit the CPU at the same time, building ApplicationContexts is expensive,
                // even as lightweight as the shard context aims to be.
                Sleep.sleep(RandomUtils.nextLong(TimeUnit.SECONDS.toMillis(10), TimeUnit.SECONDS.toMillis(60)));
                shardedDAO.toDAOs(shardedDAO.getShardDAO().findByShardSet(shardSetName));
                log.warn(prefix + " SUCCEEDED ON attempt " + attempt);
                return;

            } catch (Exception e) {
                log.warn(prefix + " (attempt " + attempt + "): error initializing: " + e, e);
            } finally {
                attempt++;
            }
            Sleep.sleep(TimeUnit.SECONDS.toMillis(4 * attempt));
        }
        if (attempt >= MAX_INIT_DAO_ATTEMPTS) log.error(prefix + ": too many errors, giving up");
    }
}
