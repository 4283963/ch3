package com.smartfreezer.dto;

import jakarta.validation.constraints.NotNull;

public class TemperatureAdjustRequest {
    @NotNull(message = "温区不能为空")
    private String zone;

    @NotNull(message = "目标温度不能为空")
    private Double targetTemp;

    private String operator;

    private String reason;

    public String getZone() { return zone; }
    public void setZone(String zone) { this.zone = zone; }
    public Double getTargetTemp() { return targetTemp; }
    public void setTargetTemp(Double targetTemp) { this.targetTemp = targetTemp; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
