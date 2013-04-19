package org.cobbzilla.wizard.spring.config.mq;

import org.cobbzilla.util.mq.MqClient;
import org.cobbzilla.util.mq.MqClientFactory;
import org.cobbzilla.wizard.server.config.HasMqConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MqConfig {

    @SuppressWarnings("SpringJavaAutowiringInspection")
    @Autowired private HasMqConfiguration configuration;

    @Bean MqClientFactory mqClientFactory () {
        return new MqClientFactory(configuration.getMq().getClientClass(), configuration.getMq().getProperties());
    }

}
