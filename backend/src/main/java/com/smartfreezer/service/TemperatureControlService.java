package com.smartfreezer.service;

import com.smartfreezer.config.FreezerConfig;
import com.smartfreezer.dto.TemperatureAdjustRequest;
import com.smartfreezer.dto.ZoneStatusResponse;
import com.smartfreezer.entity.*;
import com.smartfreezer.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class TemperatureControlService {
    private static final Logger log = LoggerFactory.getLogger(TemperatureControlService.class);

    private final FreezerConfig freezerConfig;
    private final TemperatureReadingRepository temperatureReadingRepository;
    private final FrostReadingRepository frostReadingRepository;
    private final TemperatureControlLogRepository controlLogRepository;
    private final MqttService mqttService;
    private final ScheduledTaskService scheduledTaskService;
    private final TemperatureLinkageService temperatureLinkageService;
    private final DataBatchService dataBatchService;

    public TemperatureControlService(FreezerConfig freezerConfig,
                                     TemperatureReadingRepository temperatureReadingRepository,
                                     FrostReadingRepository frostReadingRepository,
                                     TemperatureControlLogRepository controlLogRepository,
                                     MqttService mqttService,
                                     ScheduledTaskService scheduledTaskService,
                                     TemperatureLinkageService temperatureLinkageService,
                                     DataBatchService dataBatchService) {
        this.freezerConfig = freezerConfig;
        this.temperatureReadingRepository = temperatureReadingRepository;
        this.frostReadingRepository = frostReadingRepository;
        this.controlLogRepository = controlLogRepository;
        this.mqttService = mqttService;
        this.scheduledTaskService = scheduledTaskService;
        this.temperatureLinkageService = temperatureLinkageService;
        this.dataBatchService = dataBatchService;
    }

    @Transactional
    public Map<String, Object> adjustTargetTemperature(TemperatureAdjustRequest request) {
        ZoneType zoneType = ZoneType.valueOf(request.getZone().toUpperCase());
        String zoneLower = zoneType.name().toLowerCase();
        FreezerConfig.ZoneConfig zoneConfig = freezerConfig.getZones().get(zoneLower);

        if (zoneConfig == null) {
            throw new IllegalArgumentException("无效的温区: " + request.getZone());
        }

        double newTarget = request.getTargetTemp();
        if (newTarget < zoneConfig.getMinTemp() || newTarget > zoneConfig.getMaxTemp()) {
            throw new IllegalArgumentException(String.format(
                    "目标温度 %.1f°C 超出范围 [%.1f°C, %.1f°C]",
                    newTarget, zoneConfig.getMinTemp(), zoneConfig.getMaxTemp()));
        }

        double oldTarget = scheduledTaskService.getCurrentTargetTemp(zoneType);

        if (Math.abs(newTarget - oldTarget) < 0.1) {
            return Map.of(
                    "success", true,
                    "message", "目标温度无变化",
                    "zone", zoneLower,
                    "oldTarget", oldTarget,
                    "newTarget", newTarget
            );
        }

        log.info("调整{}目标温度: {}°C -> {}°C, 操作员: {}, 原因: {}",
                zoneType.getDisplayName(), oldTarget, newTarget,
                request.getOperator(), request.getReason());

        boolean mqttDelivered = mqttService.publishTemperatureSet(zoneLower, newTarget);

        scheduledTaskService.updateTargetTemp(zoneType, newTarget);

        TemperatureControlLog controlLog = new TemperatureControlLog();
        controlLog.setZoneType(zoneType);
        controlLog.setOldTargetTemp(oldTarget);
        controlLog.setNewTargetTemp(newTarget);
        controlLog.setOperator(request.getOperator() != null ? request.getOperator() : "system");
        controlLog.setReason(request.getReason());
        controlLog.setMqttDelivered(mqttDelivered);
        dataBatchService.saveTemperatureControlLogAsync(controlLog);

        log.info("温度调整日志已异步入队, MQTT下发状态: {}", mqttDelivered);

        return Map.of(
                "success", true,
                "message", "目标温度调整成功",
                "zone", zoneLower,
                "zoneName", zoneConfig.getName(),
                "oldTarget", oldTarget,
                "newTarget", newTarget,
                "mqttDelivered", mqttDelivered
        );
    }

    @Transactional(readOnly = true)
    public List<ZoneStatusResponse> getAllZoneStatus() {
        List<ZoneStatusResponse> statusList = new ArrayList<>();

        for (ZoneType zoneType : ZoneType.values()) {
            statusList.add(getZoneStatus(zoneType));
        }

        return statusList;
    }

    @Transactional(readOnly = true)
    public ZoneStatusResponse getZoneStatus(ZoneType zoneType) {
        String zoneLower = zoneType.name().toLowerCase();
        FreezerConfig.ZoneConfig zoneConfig = freezerConfig.getZones().get(zoneLower);

        ZoneStatusResponse response = new ZoneStatusResponse();
        response.setZone(zoneLower);
        response.setZoneName(zoneConfig.getName());
        response.setMinTemp(zoneConfig.getMinTemp());
        response.setMaxTemp(zoneConfig.getMaxTemp());

        temperatureReadingRepository.findTopByZoneTypeOrderByCreatedAtDesc(zoneType)
                .ifPresent(reading -> {
                    response.setCurrentTemp(reading.getCurrentTemp());
                    response.setTargetTemp(reading.getTargetTemp());
                    response.setLastUpdate(reading.getCreatedAt());
                });

        if (response.getTargetTemp() == null) {
            response.setTargetTemp(zoneConfig.getDefaultTargetTemp());
        }

        frostReadingRepository.findTopByZoneTypeOrderByCreatedAtDesc(zoneType)
                .ifPresent(reading -> response.setFrostThickness(reading.getFrostThickness()));

        response.setFanSpeed(temperatureLinkageService.getFanSpeedForZone(zoneType));
        response.setDefrostPower(temperatureLinkageService.getDefrostPowerForZone(zoneType));

        if (zoneType == ZoneType.MIDDLE && response.getFanSpeed() == null) {
            response.setFanSpeed(temperatureLinkageService.getCurrentFanSpeed());
        }
        if (zoneType == ZoneType.UPPER && response.getDefrostPower() == null) {
            response.setDefrostPower(temperatureLinkageService.getCurrentDefrostPower());
        }

        return response;
    }

    @Transactional(readOnly = true)
    public List<TemperatureReading> getTemperatureHistory(String zone, int hours) {
        ZoneType zoneType = ZoneType.valueOf(zone.toUpperCase());
        LocalDateTime startTime = LocalDateTime.now().minusHours(hours);
        return temperatureReadingRepository.findByZoneTypeAndCreatedAtAfterOrderByCreatedAtDesc(
                zoneType, startTime);
    }

    @Transactional(readOnly = true)
    public List<TemperatureControlLog> getControlLogs(String zone) {
        if (zone != null && !zone.isEmpty()) {
            ZoneType zoneType = ZoneType.valueOf(zone.toUpperCase());
            return controlLogRepository.findByZoneTypeOrderByCreatedAtDesc(zoneType);
        }
        return controlLogRepository.findAllByOrderByCreatedAtDesc();
    }

    public Map<String, Object> getLinkageStatus() {
        return Map.of(
                "currentFanSpeed", temperatureLinkageService.getCurrentFanSpeed(),
                "currentDefrostPower", temperatureLinkageService.getCurrentDefrostPower(),
                "linkageRatio", freezerConfig.getLinkage().getFanPowerToDefrostRatio(),
                "minDefrostPower", freezerConfig.getLinkage().getMinDefrostPower()
        );
    }

    public Map<String, Object> simulateFanChange(int fanSpeed) {
        int oldFanSpeed = temperatureLinkageService.getCurrentFanSpeed();
        int newDefrostPower = temperatureLinkageService.simulateFanSpeedChange(fanSpeed);

        scheduledTaskService.publishFanStatus(fanSpeed, newDefrostPower);

        return Map.of(
                "success", true,
                "oldFanSpeed", oldFanSpeed,
                "newFanSpeed", fanSpeed,
                "newDefrostPower", newDefrostPower,
                "linkageRatio", freezerConfig.getLinkage().getFanPowerToDefrostRatio()
        );
    }
}
