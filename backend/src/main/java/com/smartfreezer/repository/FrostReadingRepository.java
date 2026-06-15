package com.smartfreezer.repository;

import com.smartfreezer.entity.FrostReading;
import com.smartfreezer.entity.ZoneType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FrostReadingRepository extends JpaRepository<FrostReading, Long> {
    Optional<FrostReading> findTopByZoneTypeOrderByCreatedAtDesc(ZoneType zoneType);

    List<FrostReading> findByZoneTypeAndCreatedAtAfterOrderByCreatedAtDesc(
            ZoneType zoneType, LocalDateTime startTime);
}
