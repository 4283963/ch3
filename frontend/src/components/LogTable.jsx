import React from 'react';
import { Table, Tag, Space } from 'antd';
import dayjs from 'dayjs';

const zoneTypeMap = {
  UPPER: '上层酸奶区',
  MIDDLE: '中层熟食区',
  LOWER: '下层鲜肉区',
};

export const TemperatureControlLogTable = ({ data, loading }) => {
  const columns = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      width: 80,
    },
    {
      title: '温区',
      dataIndex: 'zoneType',
      key: 'zoneType',
      width: 120,
      render: (text) => (
        <Tag color={text === 'LOWER' ? 'orange' : text === 'MIDDLE' ? 'green' : 'blue'}>
          {zoneTypeMap[text] || text}
        </Tag>
      ),
    },
    {
      title: '原目标温度',
      dataIndex: 'oldTargetTemp',
      key: 'oldTargetTemp',
      width: 120,
      render: (val) => `${val?.toFixed(1)}°C`,
    },
    {
      title: '新目标温度',
      dataIndex: 'newTargetTemp',
      key: 'newTargetTemp',
      width: 120,
      render: (val, record) => {
        const diff = record.newTargetTemp - record.oldTargetTemp;
        return (
          <Space>
            <span>{val?.toFixed(1)}°C</span>
            <Tag color={diff > 0 ? 'red' : diff < 0 ? 'blue' : 'default'}>
              {diff > 0 ? '↑' : diff < 0 ? '↓' : '→'} {Math.abs(diff).toFixed(1)}°C
            </Tag>
          </Space>
        );
      },
    },
    {
      title: '操作员',
      dataIndex: 'operator',
      key: 'operator',
      width: 100,
    },
    {
      title: '调整原因',
      dataIndex: 'reason',
      key: 'reason',
      ellipsis: true,
    },
    {
      title: 'MQTT下发',
      dataIndex: 'mqttDelivered',
      key: 'mqttDelivered',
      width: 100,
      render: (val) => (
        <Tag color={val ? 'success' : 'warning'}>
          {val ? '成功' : '失败'}
        </Tag>
      ),
    },
    {
      title: '操作时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 180,
      render: (val) => dayjs(val).format('YYYY-MM-DD HH:mm:ss'),
    },
  ];

  return (
    <div className="data-table">
      <Table
        columns={columns}
        dataSource={data}
        rowKey="id"
        loading={loading}
        pagination={{
          pageSize: 10,
          showSizeChanger: true,
          showQuickJumper: true,
          showTotal: (total) => `共 ${total} 条记录`,
        }}
        scroll={{ x: 900 }}
      />
    </div>
  );
};

export const LinkageLogTable = ({ data, loading }) => {
  const columns = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      width: 80,
    },
    {
      title: '触发区',
      dataIndex: 'triggerZone',
      key: 'triggerZone',
      width: 100,
      render: (text) => (
        <Tag color="green">{zoneTypeMap[text] || text}</Tag>
      ),
    },
    {
      title: '触发事件',
      dataIndex: 'triggerEvent',
      key: 'triggerEvent',
      width: 160,
      render: (text) => {
        const eventMap = {
          FAN_SPEED_INCREASE: '风机转速↑',
          FAN_SPEED_DECREASE: '风机转速↓',
        };
        return eventMap[text] || text;
      },
    },
    {
      title: '触发值',
      dataIndex: 'triggerValue',
      key: 'triggerValue',
      width: 100,
      render: (val) => `${val?.toFixed(0)}%`,
    },
    {
      title: '目标区',
      dataIndex: 'targetZone',
      key: 'targetZone',
      width: 100,
      render: (text) => (
        <Tag color="blue">{zoneTypeMap[text] || text}</Tag>
      ),
    },
    {
      title: '目标动作',
      dataIndex: 'targetAction',
      key: 'targetAction',
      width: 160,
      render: (text) => {
        const actionMap = {
          DEFROST_POWER_DECREASE: '除霜功率↓',
          DEFROST_POWER_INCREASE: '除霜功率↑',
        };
        return actionMap[text] || text;
      },
    },
    {
      title: '原值',
      dataIndex: 'oldValue',
      key: 'oldValue',
      width: 80,
      render: (val) => `${val?.toFixed(0)}%`,
    },
    {
      title: '新值',
      dataIndex: 'newValue',
      key: 'newValue',
      width: 80,
      render: (val) => `${val?.toFixed(0)}%`,
    },
    {
      title: '联动比例',
      dataIndex: 'linkageRatio',
      key: 'linkageRatio',
      width: 100,
      render: (val) => `${(val * 100).toFixed(0)}%`,
    },
    {
      title: '时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 180,
      render: (val) => dayjs(val).format('YYYY-MM-DD HH:mm:ss'),
    },
  ];

  return (
    <div className="data-table">
      <Table
        columns={columns}
        dataSource={data}
        rowKey="id"
        loading={loading}
        pagination={{
          pageSize: 10,
          showSizeChanger: true,
          showQuickJumper: true,
          showTotal: (total) => `共 ${total} 条记录`,
        }}
        scroll={{ x: 1100 }}
      />
    </div>
  );
};
