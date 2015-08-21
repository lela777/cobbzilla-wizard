package org.cobbzilla.wizard.task;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.cobbzilla.wizard.model.IdentifiableBase;

import javax.persistence.MappedSuperclass;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

@NoArgsConstructor @MappedSuperclass
@Accessors(chain=true) @ToString
public class TaskEvent extends IdentifiableBase {

    @Getter @Setter protected String taskId;
    @Getter @Setter protected String messageKey;
    @Getter @Setter protected boolean success = false;
    @Getter @Setter protected String exception;
    public boolean hasException () { return !empty(exception); }

    public TaskEvent(TaskBase task, String messageKey) {
        this.taskId = task.getTaskId().getUuid();
        this.messageKey = messageKey;
    }

    public long getTimestamp () { return getCtime(); }
    public void setTimestamp (long time) { /*noop*/ }

}
