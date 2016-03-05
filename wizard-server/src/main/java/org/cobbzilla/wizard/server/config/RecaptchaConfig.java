package org.cobbzilla.wizard.server.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor @AllArgsConstructor
public class RecaptchaConfig {

    @Getter @Setter private String publicKey;
    @Getter @Setter private String privateKey;

}
