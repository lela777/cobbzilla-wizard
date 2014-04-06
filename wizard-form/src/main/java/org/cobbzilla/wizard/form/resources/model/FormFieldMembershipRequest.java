package org.cobbzilla.wizard.form.resources.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@NoArgsConstructor
public class FormFieldMembershipRequest {

    @Getter @Setter @NotNull private String fieldUuid;
    @Getter @Setter private String placement;

}
