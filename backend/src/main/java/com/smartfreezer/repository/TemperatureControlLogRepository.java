package com.smartfreezer.repository;

import com.smartfreezer.entity.TemperatureControlLog;
import com.smartfreezer.entity.ZoneType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TemperatureControlLogRepository extends JpaRepository<TemperatureControlLog, Long> {
    List<TemperatureControlLog> findByZoneTypeOrderByCreatedAtDesc(ZoneType zoneType);

    List<TemperatureControlLog> findByCreatedAtAfterOrderByCreatedAtDesc(LocalDateTime startTime);

    List<TemperatureControlLog> findAllByOrderByCreatedAtDesc();
}
