package com.smartfreezer.service;

import com.smartfreezer.config.FreezerConfig;
import com.smartfreezer.entity.*;
import com.smartfreezer.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class TemperatureLinkageService {
    private static final Logger log = LoggerFactory.getLogger(TemperatureLinkageService.class);

    private final FreezerConfig freezerConfig;
    private final LinkageLogRepository linkageLogRepository;
    private final DeviceStatusRepository deviceStatusRepository;
    private final MqttService mqttService;

    private final AtomicInteger currentFanSpeed = new AtomicInteger(50);
    private final AtomicInteger currentDefrostPower = new AtomicInteger(80);

    public TemperatureLinkageService(FreezerConfig freezerConfig,
                                     LinkageLogRepository linkageLogRepository,
                                     DeviceStatusRepository deviceStatusRepository,
                                     MqttService mqttService) {
        this.freezerConfig = freezerConfig;
        this.linkageLogRepository = linkageLogRepository;
        this.deviceStatusRepository = deviceStatusRepository;
        this.mqttService = mqttService;
    }

    public void handleFanSpeedChange(int newFanSpeed) {
        int oldFanSpeed = currentFanSpeed.get();
        if (newFanSpeed == oldFanSpeed) {
            log.debug("风机转速无变化，跳过联动处理");
            return;
        }

        log.info("检测到中层熟食区风机转速变化: {} -> {}", oldFanSpeed, newFanSpeed);

        if (newFanSpeed > oldFanSpeed) {
            adjustDefrostPowerForFanIncrease(oldFanSpeed, newFanSpeed);
        } else if (newFanSpeed < oldFanSpeed) {
            adjustDefrostPowerForFanDecrease(oldFanSpeed, newFanSpeed);
        }

        currentFanSpeed.set(newFanSpeed);
    }

    private void adjustDefrostPowerForFanIncrease(int oldFanSpeed, int newFanSpeed) {
        double ratio = freezerConfig.getLinkage().getFanPowerToDefrostRatio();
        int minDefrostPower = freezerConfig.getLinkage().getMinDefrostPower();

        int fanSpeedDelta = newFanSpeed - oldFanSpeed;
        int powerDelta = (int) Math.round(fanSpeedDelta * ratio);
        int oldPower = currentDefrostPower.get();
        int newPower = oldPower - powerDelta;

        newPower = Math.max(minDefrostPower, newPower);

        if (newPower != oldPower) {
            log.info("风机风量增大，按比例下调上层除霜功率: {} -> {} (比例: {}, 最小功率: {})",
                    oldPower, newPower, ratio, minDefrostPower);

            boolean published = mqttService.publishDefrostPowerSet(newPower);
            if (published) {
                currentDefrostPower.set(newPower);
                saveDeviceStatus(newFanSpeed, newPower);
            }

            LinkageLog linkageLog = new LinkageLog();
            linkageLog.setTriggerZone("MIDDLE");
            linkageLog.setTriggerEvent("FAN_SPEED_INCREASE");
            linkageLog.setTriggerValue((double) newFanSpeed);
            linkageLog.setTargetZone("UPPER");
            linkageLog.setTargetAction("DEFROST_POWER_DECREASE");
            linkageLog.setOldValue((double) oldPower);
            linkageLog.setNewValue((double) newPower);
            linkageLog.setLinkageRatio(ratio);
            linkageLogRepository.save(linkageLog);

            log.info("联动逻辑执行完成: 风机转速↑ -> 除霜功率↓");
        } else {
            log.debug("除霜功率已达到最小值 {}，无需继续下调", minDefrostPower);
        }
    }

    private void adjustDefrostPowerForFanDecrease(int oldFanSpeed, int newFanSpeed) {
        double ratio = freezerConfig.getLinkage().getFanPowerToDefrostRatio();

        int fanSpeedDelta = oldFanSpeed - newFanSpeed;
        int powerDelta = (int) Math.round(fanSpeedDelta * ratio);
        int oldPower = currentDefrostPower.get();
        int newPower = Math.min(100, oldPower + powerDelta);

        if (newPower != oldPower) {
            log.info("风机风量减小，适当恢复上层除霜功率: {} -> {}", oldPower, newPower);

            boolean published = mqttService.publishDefrostPowerSet(newPower);
            if (published) {
                currentDefrostPower.set(newPower);
                saveDeviceStatus(newFanSpeed, newPower);
            }

            LinkageLog linkageLog = new LinkageLog();
            linkageLog.setTriggerZone("MIDDLE");
            linkageLog.setTriggerEvent("FAN_SPEED_DECREASE");
            linkageLog.setTriggerValue((double) newFanSpeed);
            linkageLog.setTargetZone("UPPER");
            linkageLog.setTargetAction("DEFROST_POWER_INCREASE");
            linkageLog.setOldValue((double) oldPower);
            linkageLog.setNewValue((double) newPower);
            linkageLog.setLinkageRatio(ratio);
            linkageLogRepository.save(linkageLog);

            log.info("联动逻辑执行完成: 风机转速↓ -> 除霜功率↑");
        }
    }

    private void saveDeviceStatus(int fanSpeed, int defrostPower) {
        DeviceStatus upperStatus = new DeviceStatus();
        upperStatus.setZoneType(ZoneType.UPPER);
        upperStatus.setDefrostPower(defrostPower);
        deviceStatusRepository.save(upperStatus);

        DeviceStatus middleStatus = new DeviceStatus();
        middleStatus.setZoneType(ZoneType.MIDDLE);
        middleStatus.setFanSpeed(fanSpeed);
        middleStatus.setDefrostPower(defrostPower);
        deviceStatusRepository.save(middleStatus);
    }

    public int getCurrentFanSpeed() {
        return currentFanSpeed.get();
    }

    public int getCurrentDefrostPower() {
        return currentDefrostPower.get();
    }

    public int simulateFanSpeedChange(int newFanSpeed) {
        newFanSpeed = Math.max(0, Math.min(100, newFanSpeed));
        handleFanSpeedChange(newFanSpeed);
        return currentDefrostPower.get();
    }

    public Integer getDefrostPowerForZone(ZoneType zoneType) {
        Optional<DeviceStatus> status = deviceStatusRepository
                .findTopByZoneTypeOrderByCreatedAtDesc(zoneType);
        return status.map(DeviceStatus::getDefrostPower).orElse(null);
    }

    public Integer getFanSpeedForZone(ZoneType zoneType) {
        Optional<DeviceStatus> status = deviceStatusRepository
                .findTopByZoneTypeOrderByCreatedAtDesc(zoneType);
        return status.map(DeviceStatus::getFanSpeed).orElse(null);
    }
}
