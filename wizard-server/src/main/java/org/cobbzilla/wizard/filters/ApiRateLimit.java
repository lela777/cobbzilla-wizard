package org.cobbzilla.wizard.filters;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import static org.cobbzilla.util.time.TimeUtil.parseDuration;

@NoArgsConstructor @AllArgsConstructor
public class ApiRateLimit {

    @Getter @Setter int limit;
    @Getter @Setter String interval;
    @Getter @Setter String block;

    public long getIntervalDuration () { return parseDuration(interval); }
    public long getBlockDuration () { return parseDuration(block); }

}
