package com.smartfreezer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "freezer")
public class FreezerConfig {
    private Map<String, ZoneConfig> zones = new HashMap<>();
    private LinkageConfig linkage = new LinkageConfig();
    private DefrostConfig defrost = new DefrostConfig();

    public static class ZoneConfig {
        private String name;
        private Double minTemp;
        private Double maxTemp;
        private Double defaultTargetTemp;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Double getMinTemp() { return minTemp; }
        public void setMinTemp(Double minTemp) { this.minTemp = minTemp; }
        public Double getMaxTemp() { return maxTemp; }
        public void setMaxTemp(Double maxTemp) { this.maxTemp = maxTemp; }
        public Double getDefaultTargetTemp() { return defaultTargetTemp; }
        public void setDefaultTargetTemp(Double defaultTargetTemp) { this.defaultTargetTemp = defaultTargetTemp; }
    }

    public static class LinkageConfig {
        private Double fanPowerToDefrostRatio;
        private Integer minDefrostPower;

        public Double getFanPowerToDefrostRatio() { return fanPowerToDefrostRatio; }
        public void setFanPowerToDefrostRatio(Double fanPowerToDefrostRatio) { this.fanPowerToDefrostRatio = fanPowerToDefrostRatio; }
        public Integer getMinDefrostPower() { return minDefrostPower; }
        public void setMinDefrostPower(Integer minDefrostPower) { this.minDefrostPower = minDefrostPower; }
    }

    public static class DefrostConfig {
        private Double normalThreshold = 3.0;
        private Double safetyLimit = 6.0;
        private Integer delayedHour = 23;
        private Integer delayedMinute = 0;

        public Double getNormalThreshold() { return normalThreshold; }
        public void setNormalThreshold(Double normalThreshold) { this.normalThreshold = normalThreshold; }
        public Double getSafetyLimit() { return safetyLimit; }
        public void setSafetyLimit(Double safetyLimit) { this.safetyLimit = safetyLimit; }
        public Integer getDelayedHour() { return delayedHour; }
        public void setDelayedHour(Integer delayedHour) { this.delayedHour = delayedHour; }
        public Integer getDelayedMinute() { return delayedMinute; }
        public void setDelayedMinute(Integer delayedMinute) { this.delayedMinute = delayedMinute; }
    }

    public Map<String, ZoneConfig> getZones() { return zones; }
    public void setZones(Map<String, ZoneConfig> zones) { this.zones = zones; }
    public LinkageConfig getLinkage() { return linkage; }
    public void setLinkage(LinkageConfig linkage) { this.linkage = linkage; }
    public DefrostConfig getDefrost() { return defrost; }
    public void setDefrost(DefrostConfig defrost) { this.defrost = defrost; }
}
