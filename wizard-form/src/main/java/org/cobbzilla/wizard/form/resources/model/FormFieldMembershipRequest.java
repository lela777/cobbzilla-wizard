package org.cobbzilla.wizard.form.resources.model;

import com.sun.istack.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
public class FormFieldMembershipRequest {

    @Getter @Setter @NotNull private String fieldUuid;
    @Getter @Setter private String placement;

}
