import React, { useState, useEffect, useCallback } from 'react';
import { Layout, Row, Col, Tabs, Button, message, Space, Spin, Alert } from 'antd';
import {
  HistoryOutlined,
  DashboardOutlined,
  BarChartOutlined,
} from '@ant-design/icons';
import dayjs from 'dayjs';
import ZoneCard from './components/ZoneCard';
import LinkagePanel from './components/LinkagePanel';
import BusinessStatusPanel from './components/BusinessStatusPanel';
import DefrostAlertPanel from './components/DefrostAlertPanel';
import TemperatureAdjustModal from './components/TemperatureAdjustModal';
import {
  TemperatureControlLogTable,
  LinkageLogTable,
} from './components/LogTable';
import TemperatureChart from './components/TemperatureChart';
import { temperatureApi, linkageApi, logsApi, defrostApi } from './services/api';

const { Header, Content } = Layout;
const { TabPane } = Tabs;

function App() {
  const [zones, setZones] = useState({
    upper: null,
    middle: null,
    lower: null,
  });
  const [linkageStatus, setLinkageStatus] = useState(null);
  const [controlLogs, setControlLogs] = useState([]);
  const [linkageLogs, setLinkageLogs] = useState([]);
  const [historyData, setHistoryData] = useState({
    upper: [],
    middle: [],
    lower: [],
  });
  const [defrostStatus, setDefrostStatus] = useState({
    businessOpen: true,
    normalThreshold: 3.0,
    safetyLimit: 6.0,
    delayedTime: '23:00',
    activeAlerts: [],
  });

  const [loading, setLoading] = useState({
    zones: true,
    linkage: true,
    defrost: true,
    logs: false,
  });
  const [adjustModalVisible, setAdjustModalVisible] = useState(false);
  const [selectedZone, setSelectedZone] = useState(null);
  const [adjustLoading, setAdjustLoading] = useState(false);
  const [fanChangeLoading, setFanChangeLoading] = useState(false);
  const [businessToggleLoading, setBusinessToggleLoading] = useState(false);
  const [currentTime, setCurrentTime] = useState(dayjs().format('YYYY-MM-DD HH:mm:ss'));
  const [error, setError] = useState(null);

  useEffect(() => {
    const timer = setInterval(() => {
      setCurrentTime(dayjs().format('YYYY-MM-DD HH:mm:ss'));
    }, 1000);
    return () => clearInterval(timer);
  }, []);

  const fetchDefrostStatus = useCallback(async () => {
    try {
      const response = await defrostApi.getStatus();
      if (response.data?.success) {
        setDefrostStatus(response.data.data);
      }
      const alertsResp = await defrostApi.getActiveAlerts();
      if (alertsResp.data?.success) {
        setDefrostStatus((prev) => ({
          ...prev,
          activeAlerts: alertsResp.data.data || [],
        }));
      }
    } catch (err) {
      console.error('获取除霜状态失败:', err);
    } finally {
      setLoading((prev) => ({ ...prev, defrost: false }));
    }
  }, []);

  const fetchAllData = useCallback(async () => {
    try {
      setLoading((prev) => ({ ...prev, zones: true }));
      const zonesResponse = await temperatureApi.getAllZones();
      if (zonesResponse.data?.success) {
        const zoneList = zonesResponse.data.data;
        const zoneMap = {};
        zoneList.forEach((z) => {
          zoneMap[z.zone] = z;
        });
        setZones(zoneMap);
      }

      const linkageResponse = await linkageApi.getStatus();
      if (linkageResponse.data?.success) {
        setLinkageStatus(linkageResponse.data.data);
      }

      const historyPromises = ['upper', 'middle', 'lower'].map((zone) =>
        temperatureApi.getTemperatureHistory(zone, 1)
      );
      const historyResponses = await Promise.all(historyPromises);
      const historyMap = {};
      historyResponses.forEach((resp, idx) => {
        if (resp.data?.success) {
          historyMap[['upper', 'middle', 'lower'][idx]] = resp.data.data;
        }
      });
      setHistoryData(historyMap);

      await fetchDefrostStatus();
    } catch (err) {
      console.error('获取数据失败:', err);
      setError('无法连接到后端服务，请检查后端是否启动');
    } finally {
      setLoading((prev) => ({ ...prev, zones: false, linkage: false }));
    }
  }, [fetchDefrostStatus]);

  const fetchLogs = useCallback(async () => {
    try {
      setLoading((prev) => ({ ...prev, logs: true }));
      const [controlResp, linkageResp] = await Promise.all([
        logsApi.getTemperatureControlLogs(),
        logsApi.getLinkageLogs(),
      ]);
      if (controlResp.data?.success) {
        setControlLogs(controlResp.data.data);
      }
      if (linkageResp.data?.success) {
        setLinkageLogs(linkageResp.data.data);
      }
    } catch (err) {
      console.error('获取日志失败:', err);
    } finally {
      setLoading((prev) => ({ ...prev, logs: false }));
    }
  }, []);

  useEffect(() => {
    fetchAllData();
    fetchLogs();

    const interval = setInterval(() => {
      fetchAllData();
    }, 10000);

    return () => clearInterval(interval);
  }, [fetchAllData, fetchLogs]);

  const handleAdjustClick = (zoneData) => {
    setSelectedZone(zoneData);
    setAdjustModalVisible(true);
  };

  const handleAdjustConfirm = async (values) => {
    setAdjustLoading(true);
    try {
      const response = await temperatureApi.adjustTemperature(values);
      if (response.data?.success) {
        message.success(
          `已将${response.data.zoneName}目标温度调整为 ${response.data.newTarget}°C`
        );
        setAdjustModalVisible(false);
        fetchAllData();
        setTimeout(fetchLogs, 500);
      } else {
        message.error(response.data?.message || '调整失败');
      }
    } catch (err) {
        message.error(err.response?.data?.message || '调整失败');
      } finally {
      setAdjustLoading(false);
    }
  };

  const handleFanChange = async (fanSpeed) => {
    setFanChangeLoading(true);
    try {
      const response = await linkageApi.simulateFanChange(fanSpeed);
      if (response.data?.success) {
        message.success(
          `风机转速已调整为 ${response.data.newFanSpeed}%，除霜功率联动调整为 ${response.data.newDefrostPower}%`
        );
        setLinkageStatus({
          ...linkageStatus,
          currentFanSpeed: response.data.newFanSpeed,
          currentDefrostPower: response.data.newDefrostPower,
        });
        fetchAllData();
        setTimeout(fetchLogs, 500);
      }
    } catch (err) {
      message.error('风机调整失败');
    } finally {
      setFanChangeLoading(false);
    }
  };

  const handleBusinessToggle = async (open) => {
    setBusinessToggleLoading(true);
    try {
      const response = open
        ? await defrostApi.setBusinessOpen('调度员')
        : await defrostApi.setBusinessClosed('调度员');
      if (response.data?.success) {
        message.success(response.data.message);
        fetchDefrostStatus();
        fetchAllData();
      } else {
        message.error(response.data?.message || '状态切换失败');
      }
    } catch (err) {
      message.error('状态切换失败');
    } finally {
      setBusinessToggleLoading(false);
    }
  };

  const handleExecuteDelayed = async () => {
    try {
      const response = await defrostApi.executeDelayed();
      if (response.data?.success) {
        message.success('已触发延时除霜');
        fetchDefrostStatus();
      }
    } catch (err) {
      message.error('触发失败');
    }
  };

  const handleAcknowledgeAlert = async (id) => {
    try {
      const response = await defrostApi.acknowledgeAlert(id, '调度员');
      if (response.data?.success) {
        message.success('告警已确认');
        fetchDefrostStatus();
      }
    } catch (err) {
      message.error('操作失败');
    }
  };

  const handleResolveAlert = async (id) => {
    try {
      const response = await defrostApi.resolveAlert(id, '调度员');
      if (response.data?.success) {
        message.success('已标记为已清霜');
        fetchDefrostStatus();
        fetchAllData();
      }
    } catch (err) {
      message.error('操作失败');
    }
  };

  const refreshData = () => {
    setError(null);
    fetchAllData();
    fetchLogs();
  };

  return (
    <Layout className="app-container">
      <Header className="app-header">
        <div className="logo">
          <BarChartOutlined className="logo-icon" />
          <span>智能风幕柜运维看板</span>
        </div>
        <div className="header-info">
          <Space size="large">
            <span>
              <DashboardOutlined /> 实时监控系统
            </span>
            <span>{currentTime}</span>
          </Space>
          </div>
      </Header>

      <Content className="main-content">
        {error && (
          <Alert
            message="连接错误"
            description={
              <div>
                <p>{error}</p>
                <Button type="primary" size="small" onClick={refreshData}>
                  重试
                </Button>
              </div>
              }
            type="error"
            showIcon
            closable
            style={{ marginBottom: 16 }}
          />
        )}

        <BusinessStatusPanel
          isOpen={defrostStatus.businessOpen}
          delayedTime={defrostStatus.delayedTime}
          loading={businessToggleLoading}
          onToggleOpen={handleBusinessToggle}
          onExecuteDelayed={handleExecuteDelayed}
        />

        <DefrostAlertPanel
          alerts={defrostStatus.activeAlerts}
          normalThreshold={defrostStatus.normalThreshold}
          safetyLimit={defrostStatus.safetyLimit}
          onAcknowledge={handleAcknowledgeAlert}
          onResolve={handleResolveAlert}
        />

        <LinkagePanel
          linkageStatus={linkageStatus}
          onFanChange={handleFanChange}
          loading={fanChangeLoading}
        />

        <Spin spinning={loading.zones} tip="加载中...">
          <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
            <Col xs={24} sm={24} md={8}>
              <ZoneCard
                zone="upper"
                data={zones.upper}
                onAdjust={handleAdjustClick}
                defrostConfig={defrostStatus}
              />
            </Col>
            <Col xs={24} sm={24} md={8}>
              <ZoneCard
                zone="middle"
                data={zones.middle}
                onAdjust={handleAdjustClick}
                defrostConfig={defrostStatus}
              />
            </Col>
            <Col xs={24} sm={24} md={8}>
              <ZoneCard
                zone="lower"
                data={zones.lower}
                onAdjust={handleAdjustClick}
                defrostConfig={defrostStatus}
              />
            </Col>
          </Row>
          </Spin>

        <Tabs
          defaultActiveKey="charts"
          size="large"
          items={[
            {
              key: 'charts',
              label: (
                <span>
                  <BarChartOutlined /> 温度趋势
                </span>
              ),
              children: (
                <Row gutter={[16, 16]}>
                  <Col xs={24} md={8}>
                    <TemperatureChart
                      data={historyData.upper}
                      title="上层酸奶区 - 温度趋势"
                    />
                  </Col>
                  <Col xs={24} md={8}>
                    <TemperatureChart
                      data={historyData.middle}
                      title="中层熟食区 - 温度趋势"
                    />
                  </Col>
                  <Col xs={24} md={8}>
                    <TemperatureChart
                      data={historyData.lower}
                      title="下层鲜肉区 - 温度趋势"
                    />
                  </Col>
                </Row>
              ),
            },
            {
              key: 'logs',
              label: (
                <span>
                  <HistoryOutlined /> 操作日志
                </span>
              ),
              children: (
                <Tabs defaultActiveKey="control" type="card">
                  <TabPane tab="温度调整日志" key="control">
                    <TemperatureControlLogTable
                      data={controlLogs}
                      loading={loading.logs}
                    />
                  </TabPane>
                  <TabPane tab="联动控制日志" key="linkage">
                    <LinkageLogTable
                      data={linkageLogs}
                      loading={loading.logs}
                    />
                  </TabPane>
                </Tabs>
              ),
            },
          ]}
        />

        <TemperatureAdjustModal
          visible={adjustModalVisible}
          zoneData={selectedZone}
          onConfirm={handleAdjustConfirm}
          onCancel={() => setAdjustModalVisible(false)}
          loading={adjustLoading}
        />
      </Content>
    </Layout>
  );
}

export default App;
