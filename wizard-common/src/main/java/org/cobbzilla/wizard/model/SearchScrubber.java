package org.cobbzilla.wizard.model;

import java.util.List;

public interface SearchScrubber<IN, OUT> {

    public List<OUT> scrub(List<IN> results);

}
