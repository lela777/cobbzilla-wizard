package org.cobbzilla.wizard.cache.memcached;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.cobbzilla.util.daemon.ZillaRuntime;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

@NoArgsConstructor @AllArgsConstructor
public class MemcachedConfiguration {

    @Getter @Setter private String host = "127.0.0.1";
    @Getter @Setter private int port = 11211;
    @Getter @Setter private String key;

    public MemcachedConfiguration (String key) { this.key = key; }

    public boolean hasKey() { return !empty(key); }

}
