package com.smartfreezer.repository;

import com.smartfreezer.entity.LinkageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LinkageLogRepository extends JpaRepository<LinkageLog, Long> {
    List<LinkageLog> findByCreatedAtAfterOrderByCreatedAtDesc(LocalDateTime startTime);

    List<LinkageLog> findAllByOrderByCreatedAtDesc();
}
