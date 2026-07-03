package com.order.producer.config;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.jms.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;
@Configuration
public class ProducerMQConfig {
    @Bean public ObjectMapper objectMapper() {
        ObjectMapper m = new ObjectMapper(); m.registerModule(new JavaTimeModule()); return m;
    }
    @Bean public MessageConverter jacksonJmsMessageConverter(ObjectMapper om) {
        MappingJackson2MessageConverter c = new MappingJackson2MessageConverter();
        c.setObjectMapper(om); c.setTargetType(MessageType.TEXT); c.setTypeIdPropertyName("_type"); return c;
    }
    @Bean public JmsTemplate jmsTemplate(ConnectionFactory cf, MessageConverter mc) {
        JmsTemplate t = new JmsTemplate(cf); t.setMessageConverter(mc);
        t.setDeliveryPersistent(true); t.setExplicitQosEnabled(true); return t;
    }
}
