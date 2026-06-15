package com.smartfreezer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartfreezer.config.MqttConfig;
import com.smartfreezer.entity.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class MqttService {
    private static final Logger log = LoggerFactory.getLogger(MqttService.class);

    private final MqttConfig mqttConfig;
    private final MessageChannel mqttOutputChannel;
    private final ObjectMapper objectMapper;
    private final DataBatchService dataBatchService;
    private final TemperatureLinkageService temperatureLinkageService;
    private final SmartDefrostService smartDefrostService;

    public MqttService(MqttConfig mqttConfig,
                       MessageChannel mqttOutputChannel,
                       ObjectMapper objectMapper,
                       DataBatchService dataBatchService,
                       TemperatureLinkageService temperatureLinkageService,
                       SmartDefrostService smartDefrostService) {
        this.mqttConfig = mqttConfig;
        this.mqttOutputChannel = mqttOutputChannel;
        this.objectMapper = objectMapper;
        this.dataBatchService = dataBatchService;
        this.temperatureLinkageService = temperatureLinkageService;
        this.smartDefrostService = smartDefrostService;
    }

    @ServiceActivator(inputChannel = "mqttInputChannel")
    public void handleIncomingMessage(Message<String> message) {
        String topic = message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC, String.class);
        String payload = message.getPayload();

        try {
            if (topic != null) {
                if (topic.contains("temperature/report")) {
                    handleTemperatureReport(topic, payload);
                } else if (topic.contains("frost/report")) {
                    handleFrostReport(topic, payload);
                } else if (topic.contains("fan/status")) {
                    handleFanStatus(payload);
                }
            }
        } catch (Exception e) {
            log.error("处理MQTT消息失败 - Topic: {}", topic, e);
        }
    }

    private void handleTemperatureReport(String topic, String payload) throws Exception {
        Map<String, Object> data = objectMapper.readValue(payload, Map.class);
        String zoneStr = extractZoneFromTopic(topic);
        ZoneType zoneType = ZoneType.valueOf(zoneStr.toUpperCase());

        TemperatureReading reading = new TemperatureReading();
        reading.setZoneType(zoneType);
        reading.setCurrentTemp(((Number) data.get("currentTemp")).doubleValue());
        reading.setTargetTemp(((Number) data.get("targetTemp")).doubleValue());

        dataBatchService.queueTemperatureReading(reading);

        log.debug("温度数据已入队 - 温区: {}, 当前温度: {}, 目标温度: {}",
                zoneType.getDisplayName(), reading.getCurrentTemp(), reading.getTargetTemp());
    }

    private void handleFrostReport(String topic, String payload) throws Exception {
        Map<String, Object> data = objectMapper.readValue(payload, Map.class);
        String zoneStr = extractZoneFromTopic(topic);
        ZoneType zoneType = ZoneType.valueOf(zoneStr.toUpperCase());

        FrostReading reading = new FrostReading();
        reading.setZoneType(zoneType);
        reading.setFrostThickness(((Number) data.get("frostThickness")).doubleValue());
        if (data.containsKey("evaporatorTemp")) {
            reading.setEvaporatorTemp(((Number) data.get("evaporatorTemp")).doubleValue());
        }

        dataBatchService.queueFrostReading(reading);

        smartDefrostService.evaluateFrostReading(zoneType, reading.getFrostThickness());

        log.debug("结霜数据已入队并评估 - 温区: {}, 结霜厚度: {}",
                zoneType.getDisplayName(), reading.getFrostThickness());
    }

    private void handleFanStatus(String payload) throws Exception {
        Map<String, Object> data = objectMapper.readValue(payload, Map.class);
        Integer fanSpeed = ((Number) data.get("fanSpeed")).intValue();

        DeviceStatus status = new DeviceStatus();
        status.setZoneType(ZoneType.MIDDLE);
        status.setFanSpeed(fanSpeed);
        if (data.containsKey("defrostPower")) {
            status.setDefrostPower(((Number) data.get("defrostPower")).intValue());
        }
        if (data.containsKey("compressorStatus")) {
            status.setCompressorStatus((Boolean) data.get("compressorStatus"));
        }

        dataBatchService.queueDeviceStatus(status);

        log.debug("风机状态已入队 - 风机转速: {}", fanSpeed);

        temperatureLinkageService.handleFanSpeedChange(fanSpeed);
    }

    private String extractZoneFromTopic(String topic) {
        String[] parts = topic.split("/");
        if (parts.length >= 2) {
            return parts[1];
        }
        throw new IllegalArgumentException("无法从Topic中提取温区: " + topic);
    }

    public boolean publishTemperatureSet(String zone, double targetTemp) {
        try {
            String topic = mqttConfig.getTopics().getTemperatureSet().replace("{zone}", zone);
            Map<String, Object> payload = Map.of(
                    "targetTemp", targetTemp,
                    "timestamp", System.currentTimeMillis()
            );
            return publish(topic, objectMapper.writeValueAsString(payload));
        } catch (Exception e) {
            log.error("发送温度设置指令失败", e);
            return false;
        }
    }

    public boolean publishDefrostPowerSet(int power) {
        try {
            String topic = mqttConfig.getTopics().getDefrostPowerSet();
            Map<String, Object> payload = Map.of(
                    "defrostPower", power,
                    "timestamp", System.currentTimeMillis()
            );
            return publish(topic, objectMapper.writeValueAsString(payload));
        } catch (Exception e) {
            log.error("发送除霜功率设置指令失败", e);
            return false;
        }
    }

    private boolean publish(String topic, String payload) {
        try {
            Message<String> message = MessageBuilder.withPayload(payload)
                    .setHeader(MqttHeaders.TOPIC, topic)
                    .setHeader(MqttHeaders.QOS, 1)
                    .build();
            boolean sent = mqttOutputChannel.send(message, 5000);
            if (sent) {
                log.info("MQTT消息发送成功 - Topic: {}, Payload: {}", topic, payload);
            } else {
                log.warn("MQTT消息发送超时 - Topic: {}", topic);
            }
            return sent;
        } catch (Exception e) {
            log.error("MQTT消息发送失败 - Topic: {}", topic, e);
            return false;
        }
    }
}
