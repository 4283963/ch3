package com.smartfreezer.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "linkage_log")
public class LinkageLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trigger_zone", nullable = false)
    private String triggerZone;

    @Column(name = "trigger_event", nullable = false)
    private String triggerEvent;

    @Column(name = "trigger_value", nullable = false)
    private Double triggerValue;

    @Column(name = "target_zone", nullable = false)
    private String targetZone;

    @Column(name = "target_action", nullable = false)
    private String targetAction;

    @Column(name = "old_value", nullable = false)
    private Double oldValue;

    @Column(name = "new_value", nullable = false)
    private Double newValue;

    @Column(name = "linkage_ratio", nullable = false)
    private Double linkageRatio;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTriggerZone() { return triggerZone; }
    public void setTriggerZone(String triggerZone) { this.triggerZone = triggerZone; }
    public String getTriggerEvent() { return triggerEvent; }
    public void setTriggerEvent(String triggerEvent) { this.triggerEvent = triggerEvent; }
    public Double getTriggerValue() { return triggerValue; }
    public void setTriggerValue(Double triggerValue) { this.triggerValue = triggerValue; }
    public String getTargetZone() { return targetZone; }
    public void setTargetZone(String targetZone) { this.targetZone = targetZone; }
    public String getTargetAction() { return targetAction; }
    public void setTargetAction(String targetAction) { this.targetAction = targetAction; }
    public Double getOldValue() { return oldValue; }
    public void setOldValue(Double oldValue) { this.oldValue = oldValue; }
    public Double getNewValue() { return newValue; }
    public void setNewValue(Double newValue) { this.newValue = newValue; }
    public Double getLinkageRatio() { return linkageRatio; }
    public void setLinkageRatio(Double linkageRatio) { this.linkageRatio = linkageRatio; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
