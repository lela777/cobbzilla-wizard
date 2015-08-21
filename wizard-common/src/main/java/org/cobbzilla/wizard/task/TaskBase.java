package org.cobbzilla.wizard.task;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.wizard.validation.ConstraintViolationBean;
import org.cobbzilla.wizard.validation.MultiViolationException;

import java.util.List;

import static org.cobbzilla.util.reflect.ReflectionUtil.getFirstTypeParam;
import static org.cobbzilla.util.reflect.ReflectionUtil.instantiate;

@Accessors(chain=true)
public abstract class TaskBase<R extends TaskResult> implements ITask<R> {

    @Getter @Setter protected TaskId taskId;
    @Getter @Setter protected R result = newTaskResult();

    protected R newTaskResult() { return (R) instantiate(getFirstTypeParam(getClass())); }

    @Override public void init() {
        taskId = new TaskId();
        taskId.initUUID();
    }

    @Override public void description(String messageKey, String target) { result.description(messageKey, target); }

    @Override public void addEvent(String messageKey) { result.add(new TaskEvent(this, messageKey)); }

    @Override public void error(String messageKey, Exception e) { result.error(new TaskEvent(this, messageKey), e); }

    @Override public void error(String messageKey, String message) { error(messageKey, new Exception(message)); }

    @Override public void error(String messageKey, List<ConstraintViolationBean> errors) {
        error(messageKey, new MultiViolationException(errors));
    }

}
