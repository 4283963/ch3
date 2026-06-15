import React from 'react';
import { Alert, Button, Space, Tag, Tooltip } from 'antd';
import {
  WarningFilled,
  ExclamationCircleFilled,
  CheckCircleFilled,
  ClockCircleFilled,
  ThunderboltFilled,
} from '@ant-design/icons';
import dayjs from 'dayjs';

function DefrostAlertPanel({ alerts, normalThreshold, safetyLimit, onAcknowledge, onResolve }) {
  if (!alerts || alerts.length === 0) {
    return null;
  }

  const safetyAlerts = alerts.filter((a) => a.alertLevel === 'SAFETY_LIMIT' && a.alertStatus === 'ACTIVE');
  const delayedAlerts = alerts.filter((a) => a.alertLevel === 'DELAYED');

  if (safetyAlerts.length === 0 && delayedAlerts.length === 0) {
    return null;
  }

  const zoneNameMap = {
    UPPER: '上层酸奶区',
    MIDDLE: '中层熟食区',
    LOWER: '下层鲜肉区',
  };

  return (
    <div className="defrost-alert-panel">
      {safetyAlerts.length > 0 && (
        <div className="safety-alert-banner">
          <div className="safety-alert-flash">
            <ThunderboltFilled className="safety-alert-icon" />
            <div className="safety-alert-content">
              <div className="safety-alert-title">
                ⚠️ 紧急安全警报 - 需要立即手动清霜！
              </div>
              <div className="safety-alert-list">
                {safetyAlerts.map((alert) => (
                  <div key={alert.id} className="safety-alert-item">
                    <Tag color="red" style={{ fontWeight: 'bold' }}>
                      {zoneNameMap[alert.zoneType]}
                    </Tag>
                    <span className="safety-alert-frost">
                      结霜: <strong>{alert.frostThickness}</strong> mm
                      <span className="safety-limit-hint">
                        (安全极限: {safetyLimit} mm)
                      </span>
                    </span>
                    <Space>
                      <Tooltip title="标记为已查看">
                        <Button
                          size="small"
                          icon={<CheckCircleFilled />}
                          onClick={() => onAcknowledge(alert.id)}
                        >
                          已查看
                        </Button>
                      </Tooltip>
                      <Tooltip title="标记为已完成清霜">
                        <Button
                          size="small"
                          type="primary"
                          danger
                          onClick={() => onResolve(alert.id)}
                        >
                          已清霜
                        </Button>
                      </Tooltip>
                    </Space>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      )}

      {delayedAlerts.length > 0 && (
        <Alert
          type="warning"
          showIcon
          icon={<ClockCircleFilled />}
          className="delayed-defrost-alert"
          message={
            <div>
              <span style={{ fontWeight: 'bold' }}>
                营业中自动延时除霜（共 {delayedAlerts.length} 项）
              </span>
            </div>
          }
          description={
            <div>
              {delayedAlerts.map((alert) => (
                <div key={alert.id} className="delayed-alert-item">
                  <Tag color="orange">{zoneNameMap[alert.zoneType]}</Tag>
                  <span>
                    结霜 {alert.frostThickness} mm (阈值: {normalThreshold} mm)
                    {alert.delayedUntil && (
                      <span className="delayed-until">
                        → 计划执行: <strong>{dayjs(alert.delayedUntil).format('MM-DD HH:mm')}</strong>
                      </span>
                    )}
                  </span>
                </div>
              ))}
              <div className="delayed-alert-hint">
                <ExclamationCircleFilled /> 打烊后将自动触发除霜，无需人工干预
              </div>
            </div>
          }
        />
      )}
    </div>
  );
}

export default DefrostAlertPanel;
