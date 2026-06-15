import React from 'react';
import { Card, Progress, Button, Tag } from 'antd';
import {
  ThunderboltOutlined,
  SettingOutlined,
  SnowflakeOutlined,
} from '@ant-design/icons';
import dayjs from 'dayjs';

const zoneConfig = {
  upper: {
    icon: <SnowflakeOutlined style={{ color: '#1890ff', fontSize: 24 }} />,
    color: '#1890ff',
  },
  middle: {
    icon: <ThunderboltOutlined style={{ color: '#52c41a', fontSize: 24 }} />,
    color: '#52c41a',
  },
  lower: {
    icon: <SettingOutlined style={{ color: '#fa8c16', fontSize: 24 }} />,
    color: '#fa8c16',
  },
};

const ZoneCard = ({ zone, data, onAdjust, defrostConfig }) => {
  const normalThreshold = defrostConfig?.normalThreshold ?? 3.0;
  const safetyLimit = defrostConfig?.safetyLimit ?? 6.0;
  const config = zoneConfig[zone] || zoneConfig.upper;

  const getStatus = () => {
    if (!data || data.currentTemp === undefined) return 'loading';
    const diff = Math.abs(data.currentTemp - data.targetTemp);
    if (diff <= 1) return 'normal';
    if (diff <= 2) return 'warning';
    return 'alert';
  };

  const status = getStatus();
  const statusConfig = {
    normal: { text: '正常', className: 'status-normal' },
    warning: { text: '预警', className: 'status-warning' },
    alert: { text: '告警', className: 'status-alert' },
    loading: { text: '加载中', className: 'status-warning' },
  };

  const getFrostStatus = (thickness, normalThreshold = 3.0, safetyLimit = 6.0) => {
    if (thickness < normalThreshold) return 'success';
    if (thickness < safetyLimit) return 'active';
    return 'exception';
  };

  const getFrostWarningTag = (thickness, normalThreshold = 3.0, safetyLimit = 6.0) => {
    if (thickness >= safetyLimit) {
      return (
        <Tag color="red" style={{ marginTop: 8, fontWeight: 'bold', animation: 'blink 1s infinite' }}>
          ⚠️ 超安全极限 - 需立即手动清霜
        </Tag>
      );
    }
    if (thickness >= normalThreshold) {
      return (
        <Tag color="orange" style={{ marginTop: 8 }}>
          ⏰ 已达阈值 - 打烊后自动除霜
        </Tag>
      );
    }
    return null;
  };

  if (!data) {
    return (
      <Card className={`zone-card ${zone}`} loading>
        <div style={{ height: 200 }} />
      </Card>
    );
  }

  return (
    <Card
      className={`zone-card ${zone}`}
      title={
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          {config.icon}
          <span style={{ fontSize: 16, fontWeight: 600 }}>{data.zoneName}</span>
          <Tag className={`status-indicator ${statusConfig[status].className}`}>
            {statusConfig[status].text}
          </Tag>
        </div>
      }
      extra={
        <Button
          type="primary"
          size="small"
          icon={<SettingOutlined />}
          onClick={() => onAdjust(data)}
        >
          调温
        </Button>
      }
    >
      <div className="temp-display">
        <div>
          <span className="temp-value">
            {data.currentTemp !== undefined ? data.currentTemp.toFixed(1) : '--'}
          </span>
          <span className="temp-unit">°C</span>
        </div>
        <div className="temp-label">当前温度</div>
        <div className="temp-target">
          目标温度: {data.targetTemp?.toFixed(1)}°C
        </div>
        <div className="temp-range">
          范围: {data.minTemp?.toFixed(1)}°C ~ {data.maxTemp?.toFixed(1)}°C
        </div>
      </div>

      {data.frostThickness !== undefined && (
        <div className="frost-bar">
          <div style={{ marginBottom: 8, fontSize: 12, color: '#8c8c8c' }}>
            蒸发器结霜厚度: {data.frostThickness.toFixed(1)} mm
          </div>
          <Progress
            percent={Math.min(100, (data.frostThickness / safetyLimit) * 100)}
            status={getFrostStatus(data.frostThickness, normalThreshold, safetyLimit)}
            showInfo={false}
            strokeColor={{
              '0%': '#52c41a',
              '50%': '#faad14',
              '100%': '#f5222d',
            }}
          />
          <div style={{ fontSize: 11, color: '#8c8c8c', marginTop: 4 }}>
            阈值: {normalThreshold}mm / 安全极限: {safetyLimit}mm
          </div>
          {getFrostWarningTag(data.frostThickness, normalThreshold, safetyLimit)}
        </div>
      )}

      <div className="device-status">
        {data.fanSpeed !== undefined && (
          <div className="device-item">
            <div className="device-value">{data.fanSpeed}%</div>
            <div className="device-label">风机转速</div>
          </div>
        )}
        {data.defrostPower !== undefined && (
          <div className="device-item">
            <div className="device-value">{data.defrostPower}%</div>
            <div className="device-label">除霜功率</div>
          </div>
        )}
      </div>

      {data.lastUpdate && (
        <div
          style={{
            marginTop: 16,
            paddingTop: 16,
            borderTop: '1px solid #f0f0f0',
            fontSize: 12,
            color: '#8c8c8c',
            textAlign: 'center',
          }}
        >
          最后更新: {dayjs(data.lastUpdate).format('YYYY-MM-DD HH:mm:ss')}
        </div>
      )}
    </Card>
  );
};

export default ZoneCard;
