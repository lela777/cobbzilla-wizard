package org.cobbzilla.wizard.validation;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

@NoArgsConstructor @AllArgsConstructor
public class ConstraintViolationList {

    @Getter @Setter private ConstraintViolationBean[] violations;

    public boolean hasError (String messageTemplate) {
        if (!empty(violations)) {
            for (ConstraintViolationBean violation : violations) {
                if (violation.getMessageTemplate().equals(messageTemplate)) return true;
            }
        }
        return false;
    }

}
