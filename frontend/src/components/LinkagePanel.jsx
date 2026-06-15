import React from 'react';
import { Card, Slider, Button, Space, Progress, Row, Col } from 'antd';
import {
  ThunderboltOutlined,
  ReloadOutlined,
  ArrowRightOutlined,
  ExperimentOutlined,
} from '@ant-design/icons';

const LinkagePanel = ({ linkageStatus, onFanChange, loading }) => {
  const {
    currentFanSpeed = 50,
    currentDefrostPower = 80,
    linkageRatio = 0.3,
    minDefrostPower = 20,
  } = linkageStatus || {};

  const handleSliderChange = (value) => {
    onFanChange(value);
  };

  const marks = {
    0: '0%',
    25: '25%',
    50: '50%',
    75: '75%',
    100: '100%',
  };

  return (
    <div className="linkage-panel">
      <div className="linkage-title">
        <ExperimentOutlined className="linkage-icon" />
        温度联动控制 - 风机与除霜功率联动
      </div>

      <div className="gauge-container">
        <div className="gauge-item">
          <div className="stats-icon" style={{ color: '#52c41a' }}>
            <ThunderboltOutlined />
          </div>
          <div className="gauge-value">{currentFanSpeed}%</div>
          <div className="gauge-label">中层熟食区</div>
          <div className="gauge-unit">风机转速</div>
        </div>

        <div className="linkage-arrow">
          <ArrowRightOutlined />
        </div>

        <div className="gauge-item">
          <div className="stats-icon" style={{ color: '#1890ff' }}>
            <ReloadOutlined />
          </div>
          <div className="gauge-value">{currentDefrostPower}%</div>
          <div className="gauge-label">上层酸奶区</div>
          <div className="gauge-unit">除霜功率</div>
        </div>
      </div>

      <Card
        style={{
          marginTop: 24,
          background: 'rgba(255, 255, 255, 0.6)',
          border: 'none',
        }}
        size="small"
      >
        <div style={{ marginBottom: 16 }}>
          <div
            style={{
              display: 'flex',
              justifyContent: 'space-between',
              marginBottom: 8,
              fontSize: 14,
              color: '#595959',
            }}
          >
            <span>模拟风机转速调整</span>
            <span>
              联动比例: <strong>{(linkageRatio * 100).toFixed(0)}%</strong> |
              最小除霜功率: <strong>{minDefrostPower}%</strong>
            </span>
          </div>
          <Slider
            min={0}
            max={100}
            value={currentFanSpeed}
            marks={marks}
            onChange={handleSliderChange}
            disabled={loading}
            tooltip={{
              formatter: (value) => `风机转速: ${value}%`,
            }}
          />
        </div>

        <Row gutter={16}>
          <Col span={12}>
            <div style={{ marginBottom: 8, fontSize: 12, color: '#8c8c8c' }}>
              风机转速
            </div>
            <Progress
              percent={currentFanSpeed}
              strokeColor="#52c41a"
              format={(percent) => `${percent}%`}
            />
          </Col>
          <Col span={12}>
            <div style={{ marginBottom: 8, fontSize: 12, color: '#8c8c8c' }}>
              除霜功率
            </div>
            <Progress
              percent={currentDefrostPower}
              strokeColor="#1890ff"
              format={(percent) => `${percent}%`}
            />
          </Col>
        </Row>

        <div
          style={{
            marginTop: 16,
            padding: 12,
            background: '#e6f7ff',
            borderRadius: 8,
            fontSize: 13,
            color: '#1890ff',
          }}
        >
          <strong>联动逻辑说明:</strong> 当中层熟食区风机转速增大时，系统自动按
          {(linkageRatio * 100).toFixed(0)}%
          的比例下调上层酸奶区的电热除霜丝输出功率，防止串温导致酸奶变质。
        </div>
      </Card>

      <div style={{ marginTop: 16, textAlign: 'center' }}>
        <Space>
          <Button
            size="small"
            onClick={() => handleSliderChange(30)}
            disabled={loading}
          >
            低风量 (30%)
          </Button>
          <Button
            size="small"
            type="primary"
            onClick={() => handleSliderChange(80)}
            disabled={loading}
          >
            高风量 (80%)
          </Button>
          <Button
            size="small"
            onClick={() => handleSliderChange(100)}
            disabled={loading}
            danger
          >
            最大风量 (100%)
          </Button>
        </Space>
      </div>
    </div>
  );
};

export default LinkagePanel;
