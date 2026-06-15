import React from 'react';
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts';
import dayjs from 'dayjs';

const TemperatureChart = ({ data, title }) => {
  const chartData = data
    ?.slice()
    .reverse()
    .map((item) => ({
      time: dayjs(item.createdAt).format('HH:mm:ss'),
      当前温度: item.currentTemp,
      目标温度: item.targetTemp,
    }));

  return (
    <div className="chart-container">
      <div className="section-title">{title}</div>
      {chartData && chartData.length > 0 ? (
        <ResponsiveContainer width="100%" height={300}>
          <LineChart data={chartData} margin={{ top: 10, right: 30, left: 0, bottom: 0 }}>
            <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
            <XAxis
              dataKey="time"
              tick={{ fontSize: 12 }}
              stroke="#8c8c8c"
            />
            <YAxis
              tick={{ fontSize: 12 }}
              stroke="#8c8c8c"
              unit="°C"
              domain={['auto', 'auto']}
            />
            <Tooltip
              contentStyle={{
                background: 'rgba(255, 255, 255, 0.95)',
                border: '1px solid #d9d9d9',
                borderRadius: '8px',
                boxShadow: '0 4px 12px rgba(0, 0, 0, 0.1)',
              }}
              formatter={(value) => [`${value.toFixed(1)}°C`]}
            />
            <Legend wrapperStyle={{ paddingTop: 20 }} />
            <Line
              type="monotone"
              dataKey="当前温度"
              stroke="#1890ff"
              strokeWidth={2}
              dot={{ r: 3 }}
              activeDot={{ r: 6 }}
            />
            <Line
              type="monotone"
              dataKey="目标温度"
              stroke="#fa8c16"
              strokeWidth={2}
              strokeDasharray="5 5"
              dot={false}
            />
          </LineChart>
        </ResponsiveContainer>
      ) : (
        <div style={{ textAlign: 'center', color: '#8c8c8c', padding: 40 }}>
          暂无数据
        </div>
      )}
    </div>
  );
};

export default TemperatureChart;
