package org.cobbzilla.wizard.dao.shard;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.system.Sleep;

import java.util.concurrent.TimeUnit;

@AllArgsConstructor @Slf4j
class DAOInitializer extends Thread {

    public static final int MAX_INIT_DAO_ATTEMPTS = 5;

    private AbstractShardedDAO shardedDAO;

    @Override public void run() {
        int attempt = 1;
        final String prefix = "initAllDAOs(" + shardedDAO.getEntityClass().getSimpleName() + ")";
        String shardSetName = null;
        while (attempt <= MAX_INIT_DAO_ATTEMPTS) {
            try {
                boolean ok = false;
                while (!ok) {
                    try {
                        if (shardSetName == null) shardSetName = shardedDAO.getMasterDbConfiguration().getShardSetName(shardedDAO.getEntityClass());
                        ok = !shardSetName.isEmpty();
                    } catch (Exception ignored) {}
                    Sleep.sleep(200);
                }
                shardedDAO.toDAOs(shardedDAO.getShardDAO().findByShardSet(shardSetName));
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
