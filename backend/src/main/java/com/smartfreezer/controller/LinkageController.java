package com.smartfreezer.controller;

import com.smartfreezer.service.TemperatureControlService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/linkage")
@CrossOrigin(origins = "*")
public class LinkageController {
    private static final Logger log = LoggerFactory.getLogger(LinkageController.class);

    private final TemperatureControlService temperatureControlService;

    public LinkageController(TemperatureControlService temperatureControlService) {
        this.temperatureControlService = temperatureControlService;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getLinkageStatus() {
        try {
            Map<String, Object> status = temperatureControlService.getLinkageStatus();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", status);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取联动状态失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @PostMapping("/simulate-fan")
    public ResponseEntity<Map<String, Object>> simulateFanChange(
            @RequestBody Map<String, Integer> request) {
        try {
            Integer fanSpeed = request.get("fanSpeed");
            if (fanSpeed == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "缺少fanSpeed参数");
                return ResponseEntity.badRequest().body(error);
            }

            Map<String, Object> result = temperatureControlService.simulateFanChange(fanSpeed);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("模拟风机变化失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
}
