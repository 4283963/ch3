import React from 'react';
import { Button, Tag, Space, Tooltip, Popconfirm } from 'antd';
import {
  ShopOutlined,
  ShopFilled,
  MoonFilled,
  ClockCircleOutlined,
} from '@ant-design/icons';

function BusinessStatusPanel({
  isOpen,
  delayedTime,
  loading,
  onToggleOpen,
  onExecuteDelayed,
}) {
  return (
    <div className="business-status-panel">
      <div className="business-status-left">
        <Space size="middle">
          <div className="business-status-indicator">
            {isOpen ? (
              <ShopFilled className="business-icon open" />
            ) : (
              <MoonFilled className="business-icon closed" />
            )}
            <Space direction="vertical" size={0}>
              <Tag
                color={isOpen ? 'green' : 'default'}
                className="business-status-tag"
              >
                {isOpen ? '● 营业中' : '● 已打烊'}
              </Tag>
              <span className="business-delayed-info">
                <ClockCircleOutlined /> 自动除霜时间：每日 {delayedTime}
              </span>
            </Space>
          </div>
        </Space>
      </div>

      <div className="business-status-right">
        <Space>
          {isOpen ? (
            <Popconfirm
              title="确认切换到打烊状态？"
              description="切换到打烊状态后将立即执行所有延时除霜任务"
              onConfirm={() => onToggleOpen(false)}
              okText="确认打烊"
              cancelText="取消"
              okButtonProps={{ danger: true }}
            >
              <Button
                type="default"
                icon={<MoonFilled />}
                loading={loading}
                size="large"
              >
                切换打烊
              </Button>
            </Popconfirm>
          ) : (
            <Popconfirm
              title="确认开始营业？"
              description="营业期间达到除霜阈值将自动延时到打烊后执行"
              onConfirm={() => onToggleOpen(true)}
              okText="开始营业"
              cancelText="取消"
            >
              <Button
                type="primary"
                icon={<ShopOutlined />}
                loading={loading}
                size="large"
              >
                开始营业
              </Button>
            </Popconfirm>
          )}

          {isOpen && (
            <Tooltip title="立即执行所有延时除霜任务（手动触发）">
              <Button
                icon={<ClockCircleOutlined />}
                onClick={onExecuteDelayed}
                size="large"
              >
                手动执行延时除霜
              </Button>
            </Tooltip>
          )}
        </Space>
      </div>
    </div>
  );
}

export default BusinessStatusPanel;
