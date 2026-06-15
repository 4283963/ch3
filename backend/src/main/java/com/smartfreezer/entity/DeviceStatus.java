package com.smartfreezer.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "device_status")
public class DeviceStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "zone_type", nullable = false)
    private ZoneType zoneType;

    @Column(name = "fan_speed")
    private Integer fanSpeed;

    @Column(name = "defrost_power")
    private Integer defrostPower;

    @Column(name = "compressor_status")
    private Boolean compressorStatus;

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
    public Integer getFanSpeed() { return fanSpeed; }
    public void setFanSpeed(Integer fanSpeed) { this.fanSpeed = fanSpeed; }
    public Integer getDefrostPower() { return defrostPower; }
    public void setDefrostPower(Integer defrostPower) { this.defrostPower = defrostPower; }
    public Boolean getCompressorStatus() { return compressorStatus; }
    public void setCompressorStatus(Boolean compressorStatus) { this.compressorStatus = compressorStatus; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
