import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api';

// Create axios instance with default config
const api = axios.create({
    baseURL: API_BASE_URL,
    headers: {
        'Content-Type': 'application/json',
    },
    withCredentials: true, // This is important for cookies
});

// Request interceptor to add session ID
api.interceptors.request.use(
    (config) => {
        const sessionId = localStorage.getItem('sessionId');
        if (sessionId) {
            // Use the correct header name that matches your backend
            config.headers['X-Session-ID'] = sessionId;
        }
        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

// Response interceptor to handle authentication errors
api.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error.response?.status === 401) {
            // Clear session on 401
            localStorage.removeItem('sessionId');
            localStorage.removeItem('user');
            // Redirect to login if not already there
            if (window.location.pathname !== '/login') {
                window.location.href = '/login';
            }
        }
        return Promise.reject(error);
    }
);

// Auth API endpoints
export const authAPI = {
    login: (credentials: { username: string; password: string }) =>
        api.post('/user/login', credentials),

    register: (userData: { username: string; email: string; password: string }) =>
        api.post('/user/register', userData),

    getCurrentUser: () =>
        api.get('/user/current-user'),

    logout: () =>
        api.post('/user/logout'),

    getProfile: () =>
        api.get('/user/profile'),

    changePassword: (passwords: { oldPassword: string; newPassword: string }) =>
        api.post('/user/change-password', passwords),

    getStorageInfo: () =>
        api.get('/user/storage-info'),
};

// Directory API endpoints
export const directoryAPI = {
    listRoot: () =>
        api.get('/directories/'),

    listDirectory: (path: string) =>
        api.get(`/directories/${encodeURIComponent(path)}`),

    createDirectory: (path: string) =>
        api.post('/directories', { path }),

    deleteDirectory: (path: string) =>
        api.delete(`/directories/${encodeURIComponent(path)}`),

    moveDirectory: (sourcePath: string, destinationPath: string) =>
        api.put('/directories/move', { sourcePath, destinationPath }),
};

// File API endpoints
export const fileAPI = {
    uploadFile: (formData: FormData) =>
        api.post('/files/upload', formData, {
            headers: { 'Content-Type': 'multipart/form-data' }
        }),

    downloadFile: (path: string) =>
        api.get(`/files/download/${encodeURIComponent(path)}`, {
            responseType: 'blob'
        }),

    getFileInfo: (path: string) =>
        api.get(`/files/info/${encodeURIComponent(path)}`),

    deleteFile: (path: string) =>
        api.delete(`/files/${encodeURIComponent(path)}`),
};

// Search API endpoints
export const searchAPI = {
    searchFiles: (params: any) =>
        api.get('/search/files', { params }),

    getSearchSuggestions: (query: string) =>
        api.get('/search/suggestions', { params: { query } }),

    getRecentFiles: (page = 0, size = 20) =>
        api.get('/search/recent', { params: { page, size } }),

    getPopularFiles: (page = 0, size = 20) =>
        api.get('/search/popular', { params: { page, size } }),

    searchByTag: (tag: string, page = 0, size = 20) =>
        api.get(`/search/tags/${encodeURIComponent(tag)}`, { params: { page, size } }),
};

// System API endpoints
export const systemAPI = {
    getSystemHealth: () =>
        api.get('/system/health'),

    getSystemMetrics: () =>
        api.get('/system/metrics'),

    getAllNodes: () =>
        api.get('/system/nodes'),

    getHealthyNodes: () =>
        api.get('/system/nodes/healthy'),

    getNodeDetails: (nodeId: string) =>
        api.get(`/system/nodes/${nodeId}`),

    triggerNodeHealthCheck: (nodeId: string) =>
        api.post(`/system/nodes/${nodeId}/health-check`),

    triggerSystemHealthCheck: () =>
        api.post('/system/health-check'),

    recoverNode: (nodeId: string) =>
        api.post(`/system/nodes/${nodeId}/recover`),

    getFileDistribution: () =>
        api.get('/system/files/distribution'),

    getStorageSummary: () =>
        api.get('/system/storage/summary'),
};

export { api };
export default api;