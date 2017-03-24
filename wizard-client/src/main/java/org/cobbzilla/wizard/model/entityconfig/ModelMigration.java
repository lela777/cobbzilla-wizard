package org.cobbzilla.wizard.model.entityconfig;


import org.cobbzilla.wizard.client.ApiClientBase;

import java.io.File;
import java.util.Collection;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.daemon.ZillaRuntime.realNow;
import static org.cobbzilla.util.reflect.ReflectionUtil.arrayClass;

public class ModelMigration {

    public static <V extends ModelVersion> ModelMigrationResult migrate(ApiClientBase api,
                                                                        String entityConfigUrl,
                                                                        Class<V> modelVersionClass,
                                                                        String modelVersionEndpoint,
                                                                        File localMigrationsDir,
                                                                        ModelMigrationListener listener,
                                                                        String callerName) throws Exception {
        // find/sort local migrations
        final Collection<ModelVersion> localMigrations = ModelVersion.fromBaseDir(localMigrationsDir);

        // find remote migrations
        final Class<V[]> modelArrayClass = (Class<V[]>) arrayClass(modelVersionClass);
        final V[] remoteMigrations = api.get(modelVersionEndpoint, modelArrayClass);

        final ModelMigrationResult result = new ModelMigrationResult();
        result.setCurrentRemoteVersion(empty(remoteMigrations) ? -1 : remoteMigrations[remoteMigrations.length-1].getVersion());

        for (ModelVersion localMigration : localMigrations) {
            // is there a corresponding remote migration?
            final ModelVersion remoteMigration = findMigration(remoteMigrations, localMigration.getVersion());
            if (remoteMigration == null && localMigration.getVersion() > result.getCurrentRemoteVersion()) {
                if (listener != null) listener.beforeApplyMigration(localMigration);
                applyMigration(api, modelVersionEndpoint, entityConfigUrl, listener, localMigration, callerName);
                if (listener != null) listener.successfulMigration(localMigration);
                result.incrNumApplied();
                result.setLatestApplied(localMigration.getVersion());

            } else if (remoteMigration != null && !remoteMigration.getHash().equals(localMigration.getHash())) {
                die("remote migration ("+remoteMigration+") has different hash than local migration ("+localMigration+")");
            } else {
                if (listener != null) listener.alreadyAppliedMigration(localMigration);
                result.getAlreadyApplied().add(localMigration.getVersion());
            }
        }

        return result;
    }

    private static ModelVersion findMigration(ModelVersion[] migrations, int version) {
        for (ModelVersion v : migrations) if (v.getVersion() == version) return v;
        return null;
    }

    private static void applyMigration(ApiClientBase api,
                                       String modelVersionEndpoint,
                                       String entityConfigUrl,
                                       ModelSetupListener listener,
                                       ModelVersion migration,
                                       String callerName) throws Exception {
        api.put(modelVersionEndpoint, migration);
        final long start = realNow();
        ModelSetup.setupModel(api, entityConfigUrl, migration.getModels(), listener, callerName);
        migration.setExecutionTime(realNow() - start).setSuccess(true);
        api.put(modelVersionEndpoint, migration);
    }

}
