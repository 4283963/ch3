package com.smartfreezer.repository;

import com.smartfreezer.entity.DeviceStatus;
import com.smartfreezer.entity.ZoneType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeviceStatusRepository extends JpaRepository<DeviceStatus, Long> {
    Optional<DeviceStatus> findTopByZoneTypeOrderByCreatedAtDesc(ZoneType zoneType);
}
