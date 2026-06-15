package com.smartfreezer.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "frost_reading")
public class FrostReading {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "zone_type", nullable = false)
    private ZoneType zoneType;

    @Column(name = "frost_thickness", nullable = false)
    private Double frostThickness;

    @Column(name = "evaporator_temp")
    private Double evaporatorTemp;

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
    public Double getFrostThickness() { return frostThickness; }
    public void setFrostThickness(Double frostThickness) { this.frostThickness = frostThickness; }
    public Double getEvaporatorTemp() { return evaporatorTemp; }
    public void setEvaporatorTemp(Double evaporatorTemp) { this.evaporatorTemp = evaporatorTemp; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
