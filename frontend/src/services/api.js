import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    console.error('API Error:', error);
    return Promise.reject(error);
  }
);

export const temperatureApi = {
  getAllZones: () => api.get('/temperature/zones'),
  getZoneStatus: (zone) => api.get(`/temperature/zones/${zone}`),
  getTemperatureHistory: (zone, hours = 1) =>
    api.get(`/temperature/history/${zone}?hours=${hours}`),
  adjustTemperature: (data) => api.post('/temperature/adjust', data),
};

export const linkageApi = {
  getStatus: () => api.get('/linkage/status'),
  simulateFanChange: (fanSpeed) =>
    api.post('/linkage/simulate-fan', { fanSpeed }),
};

export const logsApi = {
  getTemperatureControlLogs: (zone) =>
    api.get(`/logs/temperature-control${zone ? `?zone=${zone}` : ''}`),
  getLinkageLogs: () => api.get('/logs/linkage'),
};

export const defrostApi = {
  getStatus: () => api.get('/defrost/status'),
  getBusinessStatus: () => api.get('/defrost/business'),
  setBusinessOpen: (operator) =>
    api.post('/defrost/business/open', { operator }),
  setBusinessClosed: (operator) =>
    api.post('/defrost/business/close', { operator }),
  getActiveAlerts: () => api.get('/defrost/alerts'),
  acknowledgeAlert: (id, operator) =>
    api.post(`/defrost/alerts/${id}/acknowledge`, { operator }),
  resolveAlert: (id, operator) =>
    api.post(`/defrost/alerts/${id}/resolve`, { operator }),
  executeDelayed: () => api.post('/defrost/execute-delayed'),
};

export default api;
