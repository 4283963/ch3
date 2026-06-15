package com.smartfreezer.service;

import com.smartfreezer.entity.*;
import com.smartfreezer.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class DataBatchService {
    private static final Logger log = LoggerFactory.getLogger(DataBatchService.class);

    private static final int BATCH_SIZE = 100;
    private static final int QUEUE_CAPACITY = 10000;

    private final TemperatureReadingRepository temperatureReadingRepository;
    private final FrostReadingRepository frostReadingRepository;
    private final DeviceStatusRepository deviceStatusRepository;
    private final LinkageLogRepository linkageLogRepository;
    private final TemperatureControlLogRepository temperatureControlLogRepository;

    private final BlockingQueue<TemperatureReading> tempReadingQueue =
            new LinkedBlockingQueue<>(QUEUE_CAPACITY);
    private final BlockingQueue<FrostReading> frostReadingQueue =
            new LinkedBlockingQueue<>(QUEUE_CAPACITY);
    private final BlockingQueue<DeviceStatus> deviceStatusQueue =
            new LinkedBlockingQueue<>(QUEUE_CAPACITY);

    private final AtomicInteger tempQueueSize = new AtomicInteger(0);
    private final AtomicInteger frostQueueSize = new AtomicInteger(0);
    private final AtomicInteger deviceQueueSize = new AtomicInteger(0);

    public DataBatchService(TemperatureReadingRepository temperatureReadingRepository,
                            FrostReadingRepository frostReadingRepository,
                            DeviceStatusRepository deviceStatusRepository,
                            LinkageLogRepository linkageLogRepository,
                            TemperatureControlLogRepository temperatureControlLogRepository) {
        this.temperatureReadingRepository = temperatureReadingRepository;
        this.frostReadingRepository = frostReadingRepository;
        this.deviceStatusRepository = deviceStatusRepository;
        this.linkageLogRepository = linkageLogRepository;
        this.temperatureControlLogRepository = temperatureControlLogRepository;
    }

    public void queueTemperatureReading(TemperatureReading reading) {
        if (tempReadingQueue.offer(reading)) {
            tempQueueSize.incrementAndGet();
        } else {
            log.warn("温度数据队列已满，丢弃数据 - 温区: {}", reading.getZoneType());
        }
    }

    public void queueFrostReading(FrostReading reading) {
        if (frostReadingQueue.offer(reading)) {
            frostQueueSize.incrementAndGet();
        } else {
            log.warn("结霜数据队列已满，丢弃数据 - 温区: {}", reading.getZoneType());
        }
    }

    public void queueDeviceStatus(DeviceStatus status) {
        if (deviceStatusQueue.offer(status)) {
            deviceQueueSize.incrementAndGet();
        } else {
            log.warn("设备状态队列已满，丢弃数据");
        }
    }

    @Async
    public void saveTemperatureControlLogAsync(TemperatureControlLog logEntity) {
        try {
            temperatureControlLogRepository.save(logEntity);
        } catch (Exception e) {
            log.error("异步保存温控日志失败", e);
        }
    }

    @Async
    public void saveLinkageLogAsync(LinkageLog logEntity) {
        try {
            linkageLogRepository.save(logEntity);
        } catch (Exception e) {
            log.error("异步保存联动日志失败", e);
        }
    }

    @Scheduled(fixedDelay = 500)
    @Transactional
    public void flushTemperatureBatch() {
        int currentSize = tempQueueSize.get();
        if (currentSize == 0) {
            return;
        }

        List<TemperatureReading> batch = new ArrayList<>(Math.min(BATCH_SIZE, currentSize));
        try {
            for (int i = 0; i < BATCH_SIZE; i++) {
                TemperatureReading reading = tempReadingQueue.poll(0, TimeUnit.MILLISECONDS);
                if (reading == null) {
                    break;
                }
                batch.add(reading);
                tempQueueSize.decrementAndGet();
            }

            if (!batch.isEmpty()) {
                temperatureReadingRepository.saveAll(batch);
                log.debug("批量保存温度读数: {} 条", batch.size());
            }
        } catch (Exception e) {
            log.error("批量保存温度读数失败, 批次大小: {}", batch.size(), e);
        }
    }

    @Scheduled(fixedDelay = 500)
    @Transactional
    public void flushFrostBatch() {
        int currentSize = frostQueueSize.get();
        if (currentSize == 0) {
            return;
        }

        List<FrostReading> batch = new ArrayList<>(Math.min(BATCH_SIZE, currentSize));
        try {
            for (int i = 0; i < BATCH_SIZE; i++) {
                FrostReading reading = frostReadingQueue.poll(0, TimeUnit.MILLISECONDS);
                if (reading == null) {
                    break;
                }
                batch.add(reading);
                frostQueueSize.decrementAndGet();
            }

            if (!batch.isEmpty()) {
                frostReadingRepository.saveAll(batch);
                log.debug("批量保存结霜读数: {} 条", batch.size());
            }
        } catch (Exception e) {
            log.error("批量保存结霜读数失败, 批次大小: {}", batch.size(), e);
        }
    }

    @Scheduled(fixedDelay = 500)
    @Transactional
    public void flushDeviceStatusBatch() {
        int currentSize = deviceQueueSize.get();
        if (currentSize == 0) {
            return;
        }

        List<DeviceStatus> batch = new ArrayList<>(Math.min(BATCH_SIZE, currentSize));
        try {
            for (int i = 0; i < BATCH_SIZE; i++) {
                DeviceStatus status = deviceStatusQueue.poll(0, TimeUnit.MILLISECONDS);
                if (status == null) {
                    break;
                }
                batch.add(status);
                deviceQueueSize.decrementAndGet();
            }

            if (!batch.isEmpty()) {
                deviceStatusRepository.saveAll(batch);
                log.debug("批量保存设备状态: {} 条", batch.size());
            }
        } catch (Exception e) {
            log.error("批量保存设备状态失败, 批次大小: {}", batch.size(), e);
        }
    }

    @Transactional(readOnly = true)
    public int[] getQueueStats() {
        return new int[]{
                tempQueueSize.get(),
                frostQueueSize.get(),
                deviceQueueSize.get()
        };
    }
}
