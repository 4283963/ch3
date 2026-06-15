package com.smartfreezer.service;

import com.smartfreezer.config.FreezerConfig;
import com.smartfreezer.config.MqttConfig;
import com.smartfreezer.entity.*;
import com.smartfreezer.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
@ConditionalOnProperty(prefix = "scheduling", name = "enabled", havingValue = "true")
public class ScheduledTaskService {
    private static final Logger log = LoggerFactory.getLogger(ScheduledTaskService.class);

    private final FreezerConfig freezerConfig;
    private final MqttConfig mqttConfig;
    private final MessageChannel mqttOutputChannel;
    private final TemperatureReadingRepository temperatureReadingRepository;
    private final FrostReadingRepository frostReadingRepository;
    private final DeviceStatusRepository deviceStatusRepository;
    private final Random random = new Random();

    private final Map<ZoneType, Double> currentTemps = new HashMap<>();
    private final Map<ZoneType, Double> targetTemps = new HashMap<>();

    public ScheduledTaskService(FreezerConfig freezerConfig,
                                MqttConfig mqttConfig,
                                MessageChannel mqttOutputChannel,
                                TemperatureReadingRepository temperatureReadingRepository,
                                FrostReadingRepository frostReadingRepository,
                                DeviceStatusRepository deviceStatusRepository) {
        this.freezerConfig = freezerConfig;
        this.mqttConfig = mqttConfig;
        this.mqttOutputChannel = mqttOutputChannel;
        this.temperatureReadingRepository = temperatureReadingRepository;
        this.frostReadingRepository = frostReadingRepository;
        this.deviceStatusRepository = deviceStatusRepository;

        currentTemps.put(ZoneType.UPPER, 4.0);
        currentTemps.put(ZoneType.MIDDLE, 3.0);
        currentTemps.put(ZoneType.LOWER, 0.0);

        targetTemps.put(ZoneType.UPPER, 4.0);
        targetTemps.put(ZoneType.MIDDLE, 3.0);
        targetTemps.put(ZoneType.LOWER, 0.0);
    }

    @Scheduled(fixedRateString = "${scheduling.temperature-read-interval:10000}")
    public void readTemperatures() {
        log.info("定时读取三层温区温度...");
        for (ZoneType zone : ZoneType.values()) {
            simulateAndPublishTemperature(zone);
        }
    }

    @Scheduled(fixedRateString = "${scheduling.frost-read-interval:30000}")
    public void readFrostThickness() {
        log.info("定时读取蒸发器结霜厚度...");
        for (ZoneType zone : ZoneType.values()) {
            simulateAndPublishFrost(zone);
        }
    }

    private void simulateAndPublishTemperature(ZoneType zone) {
        String zoneLower = zone.name().toLowerCase();
        FreezerConfig.ZoneConfig zoneConfig = freezerConfig.getZones().get(zoneLower);

        double baseTemp = targetTemps.getOrDefault(zone, zoneConfig.getDefaultTargetTemp());
        double variation = (random.nextDouble() - 0.5) * 2.0;
        double currentTemp = Math.round((baseTemp + variation) * 10.0) / 10.0;

        currentTemp = Math.max(zoneConfig.getMinTemp(),
                Math.min(zoneConfig.getMaxTemp(), currentTemp));

        TemperatureReading reading = new TemperatureReading();
        reading.setZoneType(zone);
        reading.setCurrentTemp(currentTemp);
        reading.setTargetTemp(baseTemp);
        temperatureReadingRepository.save(reading);

        publishTemperatureReport(zoneLower, currentTemp, baseTemp);
        log.debug("温度数据已发布 - 温区: {}, 当前: {}, 目标: {}",
                zone.getDisplayName(), currentTemp, baseTemp);
    }

    private void simulateAndPublishFrost(ZoneType zone) {
        String zoneLower = zone.name().toLowerCase();

        double frostThickness = Math.round((random.nextDouble() * 5.0) * 10.0) / 10.0;
        double evaporatorTemp = Math.round((random.nextDouble() * -10.0 - 5.0) * 10.0) / 10.0;

        FrostReading reading = new FrostReading();
        reading.setZoneType(zone);
        reading.setFrostThickness(frostThickness);
        reading.setEvaporatorTemp(evaporatorTemp);
        frostReadingRepository.save(reading);

        publishFrostReport(zoneLower, frostThickness, evaporatorTemp);
        log.debug("结霜数据已发布 - 温区: {}, 厚度: {}, 蒸发温度: {}",
                zone.getDisplayName(), frostThickness, evaporatorTemp);
    }

    private void publishTemperatureReport(String zone, double currentTemp, double targetTemp) {
        try {
            String topic = "freezer/" + zone + "/temperature/report";
            String payload = String.format(
                    "{\"zone\":\"%s\",\"currentTemp\":%.1f,\"targetTemp\":%.1f,\"timestamp\":%d}",
                    zone, currentTemp, targetTemp, System.currentTimeMillis()
            );
            publishMqtt(topic, payload);
        } catch (Exception e) {
            log.error("发布温度报告失败", e);
        }
    }

    private void publishFrostReport(String zone, double frostThickness, double evaporatorTemp) {
        try {
            String topic = "freezer/" + zone + "/frost/report";
            String payload = String.format(
                    "{\"zone\":\"%s\",\"frostThickness\":%.1f,\"evaporatorTemp\":%.1f,\"timestamp\":%d}",
                    zone, frostThickness, evaporatorTemp, System.currentTimeMillis()
            );
            publishMqtt(topic, payload);
        } catch (Exception e) {
            log.error("发布结霜报告失败", e);
        }
    }

    public void publishFanStatus(int fanSpeed, int defrostPower) {
        try {
            String topic = "freezer/middle/fan/status";
            String payload = String.format(
                    "{\"fanSpeed\":%d,\"defrostPower\":%d,\"compressorStatus\":true,\"timestamp\":%d}",
                    fanSpeed, defrostPower, System.currentTimeMillis()
            );
            publishMqtt(topic, payload);

            DeviceStatus status = new DeviceStatus();
            status.setZoneType(ZoneType.MIDDLE);
            status.setFanSpeed(fanSpeed);
            status.setDefrostPower(defrostPower);
            status.setCompressorStatus(true);
            deviceStatusRepository.save(status);
        } catch (Exception e) {
            log.error("发布风机状态失败", e);
        }
    }

    public void updateTargetTemp(ZoneType zone, double newTarget) {
        String zoneLower = zone.name().toLowerCase();
        FreezerConfig.ZoneConfig zoneConfig = freezerConfig.getZones().get(zoneLower);
        double clampedTarget = Math.max(zoneConfig.getMinTemp(),
                Math.min(zoneConfig.getMaxTemp(), newTarget));

        targetTemps.put(zone, clampedTarget);
    }

    public double getCurrentTargetTemp(ZoneType zone) {
        return targetTemps.getOrDefault(zone,
                freezerConfig.getZones().get(zone.name().toLowerCase()).getDefaultTargetTemp());
    }

    private void publishMqtt(String topic, String payload) {
        try {
            mqttOutputChannel.send(
                    MessageBuilder.withPayload(payload)
                            .setHeader(MqttHeaders.TOPIC, topic)
                            .setHeader(MqttHeaders.QOS, 1)
                            .build(),
                    3000
            );
        } catch (Exception e) {
            log.warn("MQTT发布失败 (模拟环境下可忽略): {}", e.getMessage());
        }
    }
}
