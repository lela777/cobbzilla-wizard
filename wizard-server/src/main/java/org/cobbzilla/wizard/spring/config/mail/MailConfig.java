package org.cobbzilla.wizard.spring.config.mail;

import org.cobbzilla.mail.TemplatedMailConfiguration;
import org.cobbzilla.mail.client.TemplatedMailClient;
import org.cobbzilla.wizard.server.config.HasMailConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MailConfig {

    @SuppressWarnings("SpringJavaAutowiringInspection")
    @Autowired private HasMailConfiguration configuration;

    @Bean
    public TemplatedMailClient mailClient () throws Exception {
        TemplatedMailClient client = new TemplatedMailClient(configuration.getMail());
        client.init();
        return client;
    }
}
