package org.cobbzilla.wizard.model.search;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.reflect.ReflectionUtil.instantiate;

@NoArgsConstructor @AllArgsConstructor @Accessors(chain=true)
public class SearchBound {

    @Getter @Setter private String name;
    @Getter @Setter private SearchBoundComparison comparison;

    @Getter @Setter private String[] params;
    public boolean hasParams () { return !empty(params); }

    @Getter @Setter @JsonIgnore private String processorClass;
    public boolean hasProcessor () { return !empty(processorClass); }

    @JsonIgnore public <T extends CustomSearchBoundProcessor> T getProcessor() { return instantiate(processorClass); }

}
