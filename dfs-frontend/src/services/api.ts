import axios from 'axios';
import {ApiResponse} from "../types";

const API_BASE_URL = "http://localhost:8080/api";

export const api = axios.create({
    baseURL: API_BASE_URL,
    timeout: 30000,
    headers: {
        'content-Type': 'application/json'
    },
});

// Request interceptor to add session token
api.interceptors.request.use((config) => {
    const sessionId = localStorage.getItem('sessionId');
    if(sessionId) {
        config.headers['X-Session_ID'] = sessionId;
    }
    return config;
});

api.interceptors.response.use(
    (response) => response,
    (error) => {
        if(error.response?.status === 401) {
            localStorage.removeItem('sessionId');
            window.location.href = '/login';
        }
        return Promise.reject(error);
    }
);

export default api;

