import api from './api';
import type { ApiResponse, AuthResult, LoginRequest, RegisterRequest, User } from '../types';

function persist(result: AuthResult) {
    localStorage.setItem('token', result.token);
    localStorage.setItem('user', JSON.stringify(result.user));
}

function extractError(e: any, fallback: string): string {
    return e?.response?.data?.error || e?.response?.data?.message || e?.message || fallback;
}

export const authService = {
    async login(credentials: LoginRequest): Promise<AuthResult> {
        try {
            const { data } = await api.post<ApiResponse<AuthResult>>('/user/login', credentials);
            if (data.success && data.data) {
                persist(data.data);
                return data.data;
            }
            throw new Error(data.error || 'Login failed');
        } catch (e: any) {
            throw new Error(extractError(e, 'Login failed'));
        }
    },

    async register(body: RegisterRequest): Promise<AuthResult> {
        try {
            const { data } = await api.post<ApiResponse<AuthResult>>('/user/register', body);
            if (data.success && data.data) {
                persist(data.data);
                return data.data;
            }
            throw new Error(data.error || 'Registration failed');
        } catch (e: any) {
            throw new Error(extractError(e, 'Registration failed'));
        }
    },

    logout() {
        localStorage.removeItem('token');
        localStorage.removeItem('user');
    },

    getToken(): string | null {
        return localStorage.getItem('token');
    },

    getStoredUser(): User | null {
        try {
            const s = localStorage.getItem('user');
            return s ? (JSON.parse(s) as User) : null;
        } catch {
            return null;
        }
    },

    async me(): Promise<User | null> {
        try {
            const { data } = await api.get<ApiResponse<User>>('/user/me');
            return data.data ?? null;
        } catch {
            return null;
        }
    },
};
