package org.cobbzilla.wizard.dao.shard.task;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public class ShardResultCollectorBase<T> implements ShardResultCollector<T> {

    @Getter private List<T> results = new ArrayList<>();
    @Getter private int maxResults;

    @Override public void addResult(Object thing) { results.add((T) thing); }

    @Override public int size() { return results.size(); }

}
