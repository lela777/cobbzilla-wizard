package org.cobbzilla.wizard.model.entityconfig;

public interface VerifyLogAware<T> {

    T beforeDiff (T thing, Object context);

}
