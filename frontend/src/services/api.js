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

export default api;
