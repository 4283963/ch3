package com.smartfreezer.repository;

import com.smartfreezer.entity.DefrostAlert;
import com.smartfreezer.entity.DefrostAlert.AlertStatus;
import com.smartfreezer.entity.ZoneType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DefrostAlertRepository extends JpaRepository<DefrostAlert, Long> {
    List<DefrostAlert> findByAlertStatusOrderByCreatedAtDesc(AlertStatus status);

    List<DefrostAlert> findByAlertStatusInOrderByCreatedAtDesc(List<AlertStatus> statuses);

    Optional<DefrostAlert> findTopByZoneTypeAndAlertStatusInOrderByCreatedAtDesc(
            ZoneType zoneType, List<AlertStatus> statuses);

    List<DefrostAlert> findByAlertStatusAndDelayedUntilBeforeOrderByDelayedUntilAsc(
            AlertStatus status, LocalDateTime time);
}
