package org.cobbzilla.wizard.log;

import org.cobbzilla.util.string.StringUtil;
import org.cobbzilla.wizard.server.config.RestServerConfiguration;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public abstract class LogRelayMemoryTarget implements LogRelayAppenderTarget {

    private String[] lines;
    private final AtomicInteger index = new AtomicInteger(0);
    private final AtomicInteger indexAtLastGetLines = new AtomicInteger(-1);
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

    public String[] getLines () {
        final String[] copy;
        synchronized (index) {
            if (indexAtLastGetLines.get() == index.get()) {
                return lastLines.get();
            }
            int i = index.get();
            indexAtLastGetLines.set(index.get());
            i--;
            if (i == 0) return StringUtil.EMPTY_ARRAY;
            copy = new String[i > lines.length ? lines.length : i];
            if (i < lines.length) {
                System.arraycopy(lines, 0, copy, 0, i);
            } else {
                i %= lines.length;
                // we have wrapped. copy lines from just after index to end, then from 0 to index
                System.arraycopy(lines, i+1, copy, 0, lines.length-i-1);
                if (lines.length - i > 0) System.arraycopy(lines, 0, copy, lines.length-i-1, i+1);
            }
            lastLines.set(copy);
        }
        return copy;
    }
}
