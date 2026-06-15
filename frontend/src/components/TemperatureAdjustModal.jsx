import React, { useState, useEffect } from 'react';
import { Modal, Form, InputNumber, Input, Select, message } from 'antd';

const { TextArea } = Input;
const { Option } = Select;

const zoneOptions = [
  { value: 'upper', label: '上层酸奶区', min: 2, max: 8 },
  { value: 'middle', label: '中层熟食区', min: 0, max: 6 },
  { value: 'lower', label: '下层鲜肉区', min: -2, max: 4 },
];

const TemperatureAdjustModal = ({ visible, zoneData, onConfirm, onCancel, loading }) => {
  const [form] = Form.useForm();
  const [selectedZone, setSelectedZone] = useState(null);

  useEffect(() => {
    if (zoneData && visible) {
      const zoneConfig = zoneOptions.find((z) => z.value === zoneData.zone);
      setSelectedZone(zoneConfig);
      form.setFieldsValue({
        zone: zoneData.zone,
        targetTemp: zoneData.targetTemp,
        operator: '调度员',
      });
    }
  }, [zoneData, visible, form]);

  const handleZoneChange = (value) => {
    const zoneConfig = zoneOptions.find((z) => z.value === value);
    setSelectedZone(zoneConfig);
    form.setFieldsValue({ targetTemp: zoneConfig ? (zoneConfig.min + zoneConfig.max) / 2 });
  };

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      onConfirm(values);
    } catch (error) {
      console.error('表单验证失败:', error);
    }
  };

  return (
    <Modal
      title={
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <span style={{ fontSize: 18, fontWeight: 600 }}>
            调整目标温度
          </span>
        </div>
      }
      open={visible}
      onOk={handleSubmit}
      onCancel={onCancel}
      confirmLoading={loading}
      okText="确认调整"
      cancelText="取消"
      width={500}
    >
      <Form form={form} layout="vertical" className="adjust-form">
        <Form.Item
          name="zone" label="选择温区" rules={[{ required: true, message: '请选择温区' }]}
        >
          <Select placeholder="请选择温区" onChange={handleZoneChange}>
            {zoneOptions.map((z) => (
              <Option key={z.value} value={z.value}>
                {z.label} (范围: {z.min}°C ~ {z.max}°C)
              </Option>
            ))}
          </Select>
        </Form.Item>

        {selectedZone && (
          <>
            <Form.Item
            name="targetTemp"
            label={
              <span>
                目标温度
                <span style={{ color: '#8c8c8c', fontSize: 12, marginLeft: 8 }}>
                  (范围: {selectedZone.min}°C ~ {selectedZone.max}°C)
                </span>
              </span>
            }
            rules={[
              { required: true, message: '请输入目标温度' },
              {
                validator: (_, value) => {
                if (value < selectedZone.min || value > selectedZone.max) {
                return Promise.reject(
                  new Error(
                    `温度必须在 ${selectedZone.min}°C ~ ${selectedZone.max}°C 范围内`
                  )
                );
                }
                return Promise.resolve();
              },
            },
            ]}
          >
            <InputNumber
              style={{ width: '100%' }}
              min={selectedZone.min}
              max={selectedZone.max}
              step={0.1}
              precision={1}
              placeholder="请输入目标温度"
              addonAfter="°C"
            />
          </Form.Item>

          <Form.Item
            name="operator"
            label="操作员"
            rules={[{ required: true, message: '请输入操作员姓名' }]}
          >
            <Input placeholder="请输入操作员姓名" />
          </Form.Item>

          <Form.Item name="reason" label="调整原因">
            <TextArea rows={3} placeholder="请输入调整原因（选填）" />
          </Form.Item>
          </>
        )}
      </Form>
    </Modal>
  );
};

export default TemperatureAdjustModal;
