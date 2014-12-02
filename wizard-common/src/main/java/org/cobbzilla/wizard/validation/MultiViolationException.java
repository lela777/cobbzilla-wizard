package org.cobbzilla.wizard.validation;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor @AllArgsConstructor
public class MultiViolationException extends RuntimeException {

    @Getter @Setter private List<ConstraintViolationBean> violations = new ArrayList<>();

}
