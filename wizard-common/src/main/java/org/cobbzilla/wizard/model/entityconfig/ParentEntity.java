package org.cobbzilla.wizard.model.entityconfig;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

public interface ParentEntity {

    Map<String, JsonNode[]> getChildren();
    void setChildren(Map<String, JsonNode[]> children);

    boolean hasChildren();
}
