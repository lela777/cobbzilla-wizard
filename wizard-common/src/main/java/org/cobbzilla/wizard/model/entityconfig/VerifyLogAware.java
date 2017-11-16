package org.cobbzilla.wizard.model.entityconfig;

import org.cobbzilla.wizard.model.Identifiable;

import java.util.Map;

public interface VerifyLogAware<T> {

    T beforeDiff (T thing, Map<String, Identifiable> context, Object resolver) throws Exception;

}
