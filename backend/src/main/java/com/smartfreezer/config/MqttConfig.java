package com.smartfreezer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

@Configuration
@ConfigurationProperties(prefix = "mqtt")
public class MqttConfig {
    private String broker;
    private String clientId;
    private String username;
    private String password;
    private Topics topics = new Topics();

    public static class Topics {
        private String temperatureReport;
        private String frostReport;
        private String fanStatus;
        private String temperatureSet;
        private String defrostPowerSet;

        public String getTemperatureReport() { return temperatureReport; }
        public void setTemperatureReport(String temperatureReport) { this.temperatureReport = temperatureReport; }
        public String getFrostReport() { return frostReport; }
        public void setFrostReport(String frostReport) { this.frostReport = frostReport; }
        public String getFanStatus() { return fanStatus; }
        public void setFanStatus(String fanStatus) { this.fanStatus = fanStatus; }
        public String getTemperatureSet() { return temperatureSet; }
        public void setTemperatureSet(String temperatureSet) { this.temperatureSet = temperatureSet; }
        public String getDefrostPowerSet() { return defrostPowerSet; }
        public void setDefrostPowerSet(String defrostPowerSet) { this.defrostPowerSet = defrostPowerSet; }
    }

    public String getBroker() { return broker; }
    public void setBroker(String broker) { this.broker = broker; }
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public Topics getTopics() { return topics; }
    public void setTopics(Topics topics) { this.topics = topics; }

    @Bean
    public MqttPahoClientFactory mqttClientFactory() {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        org.eclipse.paho.client.mqttv3.MqttConnectOptions options =
                new org.eclipse.paho.client.mqttv3.MqttConnectOptions();
        options.setServerURIs(new String[]{broker});
        options.setUserName(username);
        options.setPassword(password.toCharArray());
        options.setCleanSession(true);
        options.setConnectionTimeout(10);
        options.setKeepAliveInterval(30);
        factory.setConnectionOptions(options);
        return factory;
    }

    @Bean
    public MessageChannel mqttInputChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel mqttOutputChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageProducer mqttInbound() {
        MqttPahoMessageDrivenChannelAdapter adapter =
                new MqttPahoMessageDrivenChannelAdapter(
                        clientId + "-inbound",
                        mqttClientFactory(),
                        topics.getTemperatureReport(),
                        topics.getFrostReport(),
                        topics.getFanStatus()
                );
        adapter.setCompletionTimeout(5000);
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(1);
        adapter.setOutputChannel(mqttInputChannel());
        return adapter;
    }

    @Bean
    @ServiceActivator(inputChannel = "mqttOutputChannel")
    public MessageHandler mqttOutbound() {
        MqttPahoMessageHandler messageHandler =
                new MqttPahoMessageHandler(clientId + "-outbound", mqttClientFactory());
        messageHandler.setAsync(true);
        messageHandler.setDefaultQos(1);
        return messageHandler;
    }
}
