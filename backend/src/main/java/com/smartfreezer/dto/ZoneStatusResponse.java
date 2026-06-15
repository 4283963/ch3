package com.smartfreezer.dto;

import java.time.LocalDateTime;

public class ZoneStatusResponse {
    private String zone;
    private String zoneName;
    private Double currentTemp;
    private Double targetTemp;
    private Double minTemp;
    private Double maxTemp;
    private Double frostThickness;
    private Integer fanSpeed;
    private Integer defrostPower;
    private LocalDateTime lastUpdate;

    public String getZone() { return zone; }
    public void setZone(String zone) { this.zone = zone; }
    public String getZoneName() { return zoneName; }
    public void setZoneName(String zoneName) { this.zoneName = zoneName; }
    public Double getCurrentTemp() { return currentTemp; }
    public void setCurrentTemp(Double currentTemp) { this.currentTemp = currentTemp; }
    public Double getTargetTemp() { return targetTemp; }
    public void setTargetTemp(Double targetTemp) { this.targetTemp = targetTemp; }
    public Double getMinTemp() { return minTemp; }
    public void setMinTemp(Double minTemp) { this.minTemp = minTemp; }
    public Double getMaxTemp() { return maxTemp; }
    public void setMaxTemp(Double maxTemp) { this.maxTemp = maxTemp; }
    public Double getFrostThickness() { return frostThickness; }
    public void setFrostThickness(Double frostThickness) { this.frostThickness = frostThickness; }
    public Integer getFanSpeed() { return fanSpeed; }
    public void setFanSpeed(Integer fanSpeed) { this.fanSpeed = fanSpeed; }
    public Integer getDefrostPower() { return defrostPower; }
    public void setDefrostPower(Integer defrostPower) { this.defrostPower = defrostPower; }
    public LocalDateTime getLastUpdate() { return lastUpdate; }
    public void setLastUpdate(LocalDateTime lastUpdate) { this.lastUpdate = lastUpdate; }
}
