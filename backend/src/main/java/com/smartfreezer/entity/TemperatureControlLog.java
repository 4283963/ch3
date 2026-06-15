package com.smartfreezer.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "temperature_control_log")
public class TemperatureControlLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "zone_type", nullable = false)
    private ZoneType zoneType;

    @Column(name = "old_target_temp", nullable = false)
    private Double oldTargetTemp;

    @Column(name = "new_target_temp", nullable = false)
    private Double newTargetTemp;

    @Column(name = "operator", nullable = false)
    private String operator;

    @Column(name = "reason")
    private String reason;

    @Column(name = "mqtt_delivered", nullable = false)
    private Boolean mqttDelivered;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ZoneType getZoneType() { return zoneType; }
    public void setZoneType(ZoneType zoneType) { this.zoneType = zoneType; }
    public Double getOldTargetTemp() { return oldTargetTemp; }
    public void setOldTargetTemp(Double oldTargetTemp) { this.oldTargetTemp = oldTargetTemp; }
    public Double getNewTargetTemp() { return newTargetTemp; }
    public void setNewTargetTemp(Double newTargetTemp) { this.newTargetTemp = newTargetTemp; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Boolean getMqttDelivered() { return mqttDelivered; }
    public void setMqttDelivered(Boolean mqttDelivered) { this.mqttDelivered = mqttDelivered; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
