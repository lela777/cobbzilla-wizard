package org.cobbzilla.wizard.filters;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor @AllArgsConstructor
public class ApiRateLimit {
        @Getter @Setter int limit;
        @Getter @Setter long interval;
        @Getter @Setter long block;
}
