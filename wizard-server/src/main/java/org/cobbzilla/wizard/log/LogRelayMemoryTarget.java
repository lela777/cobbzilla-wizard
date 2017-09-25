package org.cobbzilla.wizard.log;

import org.cobbzilla.util.string.StringUtil;
import org.cobbzilla.wizard.server.config.RestServerConfiguration;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.cobbzilla.util.daemon.ZillaRuntime.now;

public abstract class LogRelayMemoryTarget implements LogRelayAppenderTarget {

    private String[] lines;
    private final AtomicInteger index = new AtomicInteger(0);
    private final AtomicInteger indexAtLastGetLines = new AtomicInteger(-1);
    private final AtomicLong lastGetLinesCall = new AtomicLong(-1);
    private final AtomicReference<String[]> lastLines = new AtomicReference<>(null);

    public abstract RestServerConfiguration getConfiguration ();

    @PostConstruct public void initConfiguration () { LogRelayAppender.setConfig(getConfiguration()); }

    @Override public void init(Map<String, String> params) {
        Integer max = StringUtil.safeParseInt(params.get("maxLines"));
        max = (max == null) ? 100 : max;
        lines = new String[max];
    }

    @Override public void relay(String line) {
        synchronized (index) {
            lines[index.getAndIncrement() % lines.length] = String.format("%8d: %s", index.get(), line);
        }
    }

    // cache results for 2 seconds
    public static final long CACHE_TIME = SECONDS.toMillis(2);

    public String[] getLines () {
        final String[] copy;
        synchronized (index) {
            // only perform this calculation once per CACHE_TIME
            final long now = now();
            if (now - lastGetLinesCall.get() < CACHE_TIME && lastLines.get() != null) {
                return lastLines.get();
            }
            lastGetLinesCall.set(now);

            int i = index.get();

            // if nothing new has been written, return old value
            if (indexAtLastGetLines.get() == i) return lastLines.get();
            indexAtLastGetLines.set(i--);

            // if nothing at all has been written, return nothing
            if (i == 0) return StringUtil.EMPTY_ARRAY;

            // copy depends on whether or not we have wrapped

            // allocate size. if we have not wrapped, size is smaller than lines.length
            copy = new String[i > lines.length ? lines.length : i];
            if (i < lines.length) {
                // we have not wrapped. simply copy what we have written so far
                System.arraycopy(lines, 0, copy, 0, i);
            } else {
                // we have wrapped. copy lines from just after index to end, then copy from 0 to index (if any)
                i %= lines.length;
                System.arraycopy(lines, i+1, copy, 0, lines.length-i-1);
                if (lines.length - i > 0) System.arraycopy(lines, 0, copy, lines.length-i-1, i+1);
            }
            lastLines.set(copy);
        }
        return copy;
    }
}
