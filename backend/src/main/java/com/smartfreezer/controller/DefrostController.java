package com.smartfreezer.controller;

import com.smartfreezer.entity.BusinessStatus;
import com.smartfreezer.entity.DefrostAlert;
import com.smartfreezer.service.SmartDefrostService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/defrost")
@CrossOrigin(origins = "*")
public class DefrostController {
    private static final Logger log = LoggerFactory.getLogger(DefrostController.class);

    private final SmartDefrostService smartDefrostService;

    public DefrostController(SmartDefrostService smartDefrostService) {
        this.smartDefrostService = smartDefrostService;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getDefrostStatus() {
        try {
            Map<String, Object> status = smartDefrostService.getDefrostStatusSummary();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", status);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取除霜状态失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @PostMapping("/business/open")
    public ResponseEntity<Map<String, Object>> setBusinessOpen(
            @RequestBody(required = false) Map<String, String> body) {
        try {
            String operator = body != null ? body.get("operator") : "system";
            BusinessStatus status = smartDefrostService.setBusinessStatus(true, operator);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "已切换到营业状态");
            response.put("data", status);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("设置营业状态失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @PostMapping("/business/close")
    public ResponseEntity<Map<String, Object>> setBusinessClosed(
            @RequestBody(required = false) Map<String, String> body) {
        try {
            String operator = body != null ? body.get("operator") : "system";
            BusinessStatus status = smartDefrostService.setBusinessStatus(false, operator);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "已切换到打烊状态，延时除霜已触发");
            response.put("data", status);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("设置打烊状态失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @GetMapping("/business")
    public ResponseEntity<Map<String, Object>> getBusinessStatus() {
        try {
            boolean isOpen = smartDefrostService.isBusinessOpen();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", Map.of("isOpen", isOpen));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取营业状态失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @GetMapping("/alerts")
    public ResponseEntity<Map<String, Object>> getActiveAlerts() {
        try {
            List<DefrostAlert> alerts = smartDefrostService.getActiveAlerts();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", alerts);
            response.put("count", alerts.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取活动告警失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @PostMapping("/alerts/{id}/acknowledge")
    public ResponseEntity<Map<String, Object>> acknowledgeAlert(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        try {
            String operator = body != null ? body.get("operator") : "system";
            DefrostAlert alert = smartDefrostService.acknowledgeAlert(id, operator);
            if (alert == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "告警不存在");
                return ResponseEntity.badRequest().body(error);
            }
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "告警已确认");
            response.put("data", alert);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("确认告警失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @PostMapping("/alerts/{id}/resolve")
    public ResponseEntity<Map<String, Object>> resolveAlert(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        try {
            String operator = body != null ? body.get("operator") : "system";
            DefrostAlert alert = smartDefrostService.resolveAlert(id, operator);
            if (alert == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "告警不存在");
                return ResponseEntity.badRequest().body(error);
            }
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "告警已解决");
            response.put("data", alert);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("解决告警失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @PostMapping("/execute-delayed")
    public ResponseEntity<Map<String, Object>> executeDelayedDefrosts() {
        try {
            smartDefrostService.executeDelayedDefrosts();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "已手动触发延时除霜检查");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("触发延时除霜失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
}
