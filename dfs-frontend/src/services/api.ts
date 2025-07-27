import axios from 'axios';

const API_BASE_URL = "http://localhost:8080/api";

export const api = axios.create({
    baseURL: API_BASE_URL,
    timeout: 30000,
    headers: {
        'Content-Type': 'application/json'
    },
    withCredentials: true // Enable cookies for cross-origin requests
});

// Request interceptor to add session token
api.interceptors.request.use((config) => {
    const sessionId = localStorage.getItem('sessionId');
    if (sessionId) {
        config.headers['X-Session_ID'] = sessionId;
        // Also try setting as a standard Authorization header as backup
        config.headers['Authorization'] = `Bearer ${sessionId}`;
    }
    return config;
}, (error) => {
    return Promise.reject(error);
});

api.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error.response?.status === 401) {
            // Clear invalid session
            localStorage.removeItem('sessionId');
            // Redirect to auth page instead of /login
            window.location.href = '/auth';
        }
        return Promise.reject(error);
    }
);

export default api;
