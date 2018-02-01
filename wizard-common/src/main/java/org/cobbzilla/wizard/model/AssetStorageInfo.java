package org.cobbzilla.wizard.model;

public interface AssetStorageInfo extends Identifiable {

    String getAsset();
    void setAsset(String info);
    String getRelatedEntity();
    void setRelatedEntity(String uuid);

}
