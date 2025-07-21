import { api } from "./api";
import type {ApiResponse, LoginRequest, RegisterRequest, User} from "../types";

export class AuthService {

    static async login(credentials : LoginRequest): Promise<{ user : User; sessionId: string}> {
        try {
            const response = await api.post<ApiResponse<{ user: User; sessionId: string }>>(
                '/user/login',
                credentials
            );

            if(response.data.success && response.data.data) {
                const { user, sessionId } = response.data.data;
                localStorage.setItem('sessionId', sessionId);
                localStorage.setItem('user', JSON.stringify(user));
                return { user, sessionId };
            }

            throw new Error(response.data.error || 'Login failed');
        } catch (error : any) {
            throw new Error(error.response?.data?.error || error.message || 'Login failed');
        }
    }

    static async register( userData: RegisterRequest): Promise<{ user : User; sessionId: string }> {
        try {
            const response = await api.post<ApiResponse<{ user : User; sessionId: string}>>(
                '/user/register',
                userData
            );

            if(response.data.success && response.data.data) {
                const {user, sessionId } = response.data.data;
                localStorage.setItem('sessionId', sessionId);
                localStorage.setItem('user', JSON.stringify(user));
                return { user, sessionId };
            }

            throw new Error(response.data.error || 'Registration failed');
        } catch (error : any) {
            throw new Error(error.response?.data?.error || error.message || 'Registration failed');
        }
    }

    static async logout(): Promise<void> {
        try {
            await api.post<void>('/user/logout');
        } catch (error) {
            console.error('Logout error', error);
        } finally {
            localStorage.removeItem('sessionId');
            localStorage.removeItem('user');
        }
    }

    static getCurrentUser() : User | null {
        const userStr = localStorage.getItem('USER');
        if(userStr) {
            try {
                return JSON.parse(userStr);
            } catch (error) {
                console.log("Error parsing user data:",error);
                localStorage.removeItem('user');
            }
        }
        return null;
    }

    static getSessionId() : string | null {
        return localStorage.getItem('sessionId');
    }

    static isAuthenticated() : boolean {
        return !!(this.getSessionId() && this.getCurrentUser());
    }
}