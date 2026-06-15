package com.smartfreezer.controller;

import com.smartfreezer.entity.LinkageLog;
import com.smartfreezer.entity.TemperatureControlLog;
import com.smartfreezer.repository.LinkageLogRepository;
import com.smartfreezer.service.TemperatureControlService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/logs")
@CrossOrigin(origins = "*")
public class LogController {
    private static final Logger log = LoggerFactory.getLogger(LogController.class);

    private final TemperatureControlService temperatureControlService;
    private final LinkageLogRepository linkageLogRepository;

    public LogController(TemperatureControlService temperatureControlService,
                         LinkageLogRepository linkageLogRepository) {
        this.temperatureControlService = temperatureControlService;
        this.linkageLogRepository = linkageLogRepository;
    }

    @GetMapping("/temperature-control")
    public ResponseEntity<Map<String, Object>> getTemperatureControlLogs(
            @RequestParam(required = false) String zone) {
        try {
            List<TemperatureControlLog> logs = temperatureControlService.getControlLogs(zone);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", logs);
            response.put("count", logs.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取温控日志失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @GetMapping("/linkage")
    public ResponseEntity<Map<String, Object>> getLinkageLogs() {
        try {
            List<LinkageLog> logs = linkageLogRepository.findAllByOrderByCreatedAtDesc();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", logs);
            response.put("count", logs.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取联动日志失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
}
