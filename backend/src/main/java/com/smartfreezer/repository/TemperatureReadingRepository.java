package com.smartfreezer.repository;

import com.smartfreezer.entity.TemperatureReading;
import com.smartfreezer.entity.ZoneType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TemperatureReadingRepository extends JpaRepository<TemperatureReading, Long> {
    Optional<TemperatureReading> findTopByZoneTypeOrderByCreatedAtDesc(ZoneType zoneType);

    List<TemperatureReading> findByZoneTypeAndCreatedAtAfterOrderByCreatedAtDesc(
            ZoneType zoneType, LocalDateTime startTime);

    @Query("SELECT tr FROM TemperatureReading tr WHERE tr.zoneType = :zoneType " +
           "ORDER BY tr.createdAt DESC LIMIT :limit")
    List<TemperatureReading> findLatestByZoneType(@Param("zoneType") ZoneType zoneType,
                                                  @Param("limit") int limit);
}
