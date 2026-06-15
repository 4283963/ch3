package com.smartfreezer.controller;

import com.smartfreezer.dto.TemperatureAdjustRequest;
import com.smartfreezer.dto.ZoneStatusResponse;
import com.smartfreezer.entity.TemperatureReading;
import com.smartfreezer.service.TemperatureControlService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/temperature")
@CrossOrigin(origins = "*")
public class TemperatureController {
    private static final Logger log = LoggerFactory.getLogger(TemperatureController.class);

    private final TemperatureControlService temperatureControlService;

    public TemperatureController(TemperatureControlService temperatureControlService) {
        this.temperatureControlService = temperatureControlService;
    }

    @GetMapping("/zones")
    public ResponseEntity<Map<String, Object>> getAllZoneStatus() {
        try {
            List<ZoneStatusResponse> zones = temperatureControlService.getAllZoneStatus();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", zones);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取所有温区状态失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @GetMapping("/zones/{zone}")
    public ResponseEntity<Map<String, Object>> getZoneStatus(@PathVariable String zone) {
        try {
            ZoneStatusResponse status = temperatureControlService.getZoneStatus(
                    com.smartfreezer.entity.ZoneType.valueOf(zone.toUpperCase()));
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", status);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "无效的温区: " + zone);
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            log.error("获取温区状态失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @GetMapping("/history/{zone}")
    public ResponseEntity<Map<String, Object>> getTemperatureHistory(
            @PathVariable String zone,
            @RequestParam(defaultValue = "1") int hours) {
        try {
            List<TemperatureReading> history = temperatureControlService
                    .getTemperatureHistory(zone, hours);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", history);
            response.put("zone", zone);
            response.put("hours", hours);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "无效的温区: " + zone);
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            log.error("获取温度历史失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @PostMapping("/adjust")
    public ResponseEntity<Map<String, Object>> adjustTargetTemperature(
            @Valid @RequestBody TemperatureAdjustRequest request) {
        try {
            Map<String, Object> result = temperatureControlService
                    .adjustTargetTemperature(request);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            log.warn("温度调整参数错误: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            log.error("调整目标温度失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
}
