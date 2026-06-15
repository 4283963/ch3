package com.smartfreezer.service;

import com.smartfreezer.config.FreezerConfig;
import com.smartfreezer.config.FreezerConfig.DefrostConfig;
import com.smartfreezer.entity.*;
import com.smartfreezer.entity.DefrostAlert.AlertLevel;
import com.smartfreezer.entity.DefrostAlert.AlertStatus;
import com.smartfreezer.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SmartDefrostService {
    private static final Logger log = LoggerFactory.getLogger(SmartDefrostService.class);

    private final FreezerConfig freezerConfig;
    private final FrostReadingRepository frostReadingRepository;
    private final BusinessStatusRepository businessStatusRepository;
    private final DefrostAlertRepository defrostAlertRepository;
    private final MqttService mqttService;
    private final DataBatchService dataBatchService;

    private volatile boolean currentBusinessOpen = true;
    private final Map<ZoneType, DefrostAlert> activeAlerts = new ConcurrentHashMap<>();

    public SmartDefrostService(FreezerConfig freezerConfig,
                               FrostReadingRepository frostReadingRepository,
                               BusinessStatusRepository businessStatusRepository,
                               DefrostAlertRepository defrostAlertRepository,
                               MqttService mqttService,
                               DataBatchService dataBatchService) {
        this.freezerConfig = freezerConfig;
        this.frostReadingRepository = frostReadingRepository;
        this.businessStatusRepository = businessStatusRepository;
        this.defrostAlertRepository = defrostAlertRepository;
        this.mqttService = mqttService;
        this.dataBatchService = dataBatchService;
    }

    @Transactional(readOnly = true)
    public boolean isBusinessOpen() {
        return businessStatusRepository.findTopByOrderByCreatedAtDesc()
                .map(BusinessStatus::getIsOpen)
                .orElse(true);
    }

    @Transactional
    public BusinessStatus setBusinessStatus(boolean isOpen, String operator) {
        BusinessStatus status = new BusinessStatus(isOpen, operator);
        BusinessStatus saved = businessStatusRepository.save(status);
        currentBusinessOpen = isOpen;

        log.info("营业状态变更: {} -> {}, 操作员: {}",
                !isOpen ? "营业中" : "已打烊",
                isOpen ? "营业中" : "已打烊",
                operator);

        if (!isOpen) {
            executeDelayedDefrosts();
        }

        return saved;
    }

    public void evaluateFrostReading(ZoneType zoneType, double frostThickness) {
        DefrostConfig config = freezerConfig.getDefrost();
        boolean open = isBusinessOpen();

        if (frostThickness >= config.getSafetyLimit()) {
            handleSafetyLimitExceeded(zoneType, frostThickness);
        } else if (frostThickness >= config.getNormalThreshold()) {
            if (open && !isDelayedTimeNow()) {
                handleDelayedDefrost(zoneType, frostThickness);
            } else {
                triggerDefrostNow(zoneType, frostThickness);
            }
        } else {
            clearNormalAlert(zoneType);
        }
    }

    private boolean isDelayedTimeNow() {
        DefrostConfig config = freezerConfig.getDefrost();
        LocalTime now = LocalTime.now();
        LocalTime delayTime = LocalTime.of(config.getDelayedHour(), config.getDelayedMinute());
        return !now.isBefore(delayTime);
    }

    private LocalDateTime getNextDelayedTime() {
        DefrostConfig config = freezerConfig.getDefrost();
        LocalDateTime now = LocalDateTime.now();
        LocalTime delayTime = LocalTime.of(config.getDelayedHour(), config.getDelayedMinute());
        LocalDateTime todayDelay = LocalDateTime.of(now.toLocalDate(), delayTime);

        if (now.isBefore(todayDelay)) {
            return todayDelay;
        }
        return todayDelay.plusDays(1);
    }

    @Transactional
    protected void handleSafetyLimitExceeded(ZoneType zoneType, double frostThickness) {
        DefrostAlert existing = activeAlerts.get(zoneType);
        if (existing != null && existing.getAlertLevel() == AlertLevel.SAFETY_LIMIT
                && existing.getAlertStatus() == AlertStatus.ACTIVE) {
            return;
        }

        DefrostAlert alert = new DefrostAlert();
        alert.setZoneType(zoneType);
        alert.setFrostThickness(frostThickness);
        alert.setAlertLevel(AlertLevel.SAFETY_LIMIT);
        alert.setAlertStatus(AlertStatus.ACTIVE);
        alert.setMessage(String.format("⚠️ %s结霜厚度 %.1f mm 已超过安全极限 %.1f mm！请店员立即手动清霜！",
                zoneType.getDisplayName(), frostThickness, freezerConfig.getDefrost().getSafetyLimit()));

        DefrostAlert saved = defrostAlertRepository.save(alert);
        activeAlerts.put(zoneType, saved);

        log.warn("【安全警报】{}结霜厚度 {:.1f}mm 超过安全极限，需要手动清霜！",
                zoneType.getDisplayName(), frostThickness);
    }

    @Transactional
    protected void handleDelayedDefrost(ZoneType zoneType, double frostThickness) {
        DefrostAlert existing = activeAlerts.get(zoneType);
        if (existing != null && existing.getAlertStatus() != AlertStatus.RESOLVED) {
            return;
        }

        LocalDateTime delayedUntil = getNextDelayedTime();

        DefrostAlert alert = new DefrostAlert();
        alert.setZoneType(zoneType);
        alert.setFrostThickness(frostThickness);
        alert.setAlertLevel(AlertLevel.DELAYED);
        alert.setAlertStatus(AlertStatus.ACTIVE);
        alert.setDelayedUntil(delayedUntil);
        alert.setMessage(String.format("%s结霜 %.1f mm 达到阈值，已延时至 %s 打烊后自动除霜",
                zoneType.getDisplayName(), frostThickness, delayedUntil.toString()));

        DefrostAlert saved = defrostAlertRepository.save(alert);
        activeAlerts.put(zoneType, saved);

        log.info("{}结霜 {:.1f}mm，营业中不除霜，已延时至 {}",
                zoneType.getDisplayName(), frostThickness, delayedUntil);
    }

    @Transactional
    protected void triggerDefrostNow(ZoneType zoneType, double frostThickness) {
        DefrostAlert alert = new DefrostAlert();
        alert.setZoneType(zoneType);
        alert.setFrostThickness(frostThickness);
        alert.setAlertLevel(AlertLevel.NORMAL);
        alert.setAlertStatus(AlertStatus.ACTIVE);
        alert.setMessage(String.format("%s结霜 %.1f mm，已触发自动除霜",
                zoneType.getDisplayName(), frostThickness));

        defrostAlertRepository.save(alert);

        mqttService.publishDefrostPowerSet(100);
        log.info("已触发{}自动除霜，结霜厚度 {:.1f}mm", zoneType.getDisplayName(), frostThickness);

        resolveAlert(zoneType);
    }

    @Transactional
    protected void clearNormalAlert(ZoneType zoneType) {
        DefrostAlert existing = activeAlerts.get(zoneType);
        if (existing != null && existing.getAlertLevel() == AlertLevel.DELAYED) {
            activeAlerts.remove(zoneType);
        }
    }

    @Transactional
    public void executeDelayedDefrosts() {
        List<DefrostAlert> pending = defrostAlertRepository
                .findByAlertStatusAndDelayedUntilBeforeOrderByDelayedUntilAsc(
                        AlertStatus.ACTIVE, LocalDateTime.now().plusMinutes(1));

        for (DefrostAlert alert : pending) {
            if (alert.getAlertLevel() == AlertLevel.DELAYED) {
                log.info("执行延时除霜 - 温区: {}, 结霜厚度: {}",
                        alert.getZoneType().getDisplayName(), alert.getFrostThickness());
                mqttService.publishDefrostPowerSet(100);
                resolveAlert(alert.getZoneType());
            }
        }
    }

    @Scheduled(cron = "0 * * * * ?")
    @Transactional
    public void checkDelayedDefrosts() {
        if (!isBusinessOpen()) {
            executeDelayedDefrosts();
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        DefrostConfig config = freezerConfig.getDefrost();
        LocalTime delayTime = LocalTime.of(config.getDelayedHour(), config.getDelayedMinute());

        if (!now.toLocalTime().isBefore(delayTime)) {
            executeDelayedDefrosts();
        }
    }

    @Transactional(readOnly = true)
    public List<DefrostAlert> getActiveAlerts() {
        return defrostAlertRepository.findByAlertStatusInOrderByCreatedAtDesc(
                List.of(AlertStatus.ACTIVE, AlertStatus.ACKNOWLEDGED));
    }

    @Transactional
    public DefrostAlert acknowledgeAlert(Long alertId, String operator) {
        Optional<DefrostAlert> opt = defrostAlertRepository.findById(alertId);
        if (opt.isEmpty()) {
            return null;
        }
        DefrostAlert alert = opt.get();
        alert.setAlertStatus(AlertStatus.ACKNOWLEDGED);
        alert.setAcknowledgedBy(operator);
        alert.setAcknowledgedAt(LocalDateTime.now());
        return defrostAlertRepository.save(alert);
    }

    @Transactional
    public DefrostAlert resolveAlert(Long alertId, String operator) {
        Optional<DefrostAlert> opt = defrostAlertRepository.findById(alertId);
        if (opt.isEmpty()) {
            return null;
        }
        DefrostAlert alert = opt.get();
        alert.setAlertStatus(AlertStatus.RESOLVED);
        alert.setResolvedAt(LocalDateTime.now());
        activeAlerts.remove(alert.getZoneType());
        return defrostAlertRepository.save(alert);
    }

    private void resolveAlert(ZoneType zoneType) {
        DefrostAlert existing = activeAlerts.get(zoneType);
        if (existing != null) {
            existing.setAlertStatus(AlertStatus.RESOLVED);
            existing.setResolvedAt(LocalDateTime.now());
            defrostAlertRepository.save(existing);
            activeAlerts.remove(zoneType);
        }
    }

    public Map<String, Object> getDefrostStatusSummary() {
        Map<String, Object> summary = new HashMap<>();
        DefrostConfig config = freezerConfig.getDefrost();

        summary.put("businessOpen", isBusinessOpen());
        summary.put("normalThreshold", config.getNormalThreshold());
        summary.put("safetyLimit", config.getSafetyLimit());
        summary.put("delayedTime", String.format("%02d:%02d",
                config.getDelayedHour(), config.getDelayedMinute()));
        summary.put("activeAlerts", getActiveAlerts());

        for (ZoneType zone : ZoneType.values()) {
            frostReadingRepository.findTopByZoneTypeOrderByCreatedAtDesc(zone)
                    .ifPresent(reading ->
                            summary.put("frost_" + zone.name().toLowerCase(), reading.getFrostThickness()));
        }

        return summary;
    }
}
