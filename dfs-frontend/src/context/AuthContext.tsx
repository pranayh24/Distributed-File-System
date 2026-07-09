import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';
import type { LoginRequest, RegisterRequest, User } from '../types';
import { authService } from '../services/authService';

interface AuthContextType {
    user: User | null;
    isAuthenticated: boolean;
    login: (credentials: LoginRequest) => Promise<void>;
    register: (body: RegisterRequest) => Promise<void>;
    logout: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
    const [user, setUser] = useState<User | null>(() => authService.getStoredUser());

    // If we have a token, refresh the user from the server (validates the token too).
    useEffect(() => {
        if (authService.getToken()) {
            authService.me().then((u) => {
                if (u) setUser(u);
            });
        }
    }, []);

    const login = async (credentials: LoginRequest) => {
        const result = await authService.login(credentials);
        setUser(result.user);
    };

    const register = async (body: RegisterRequest) => {
        const result = await authService.register(body);
        setUser(result.user);
    };

    const logout = () => {
        authService.logout();
        setUser(null);
    };

    return (
        <AuthContext.Provider value={{ user, isAuthenticated: !!user, login, register, logout }}>
            {children}
        </AuthContext.Provider>
    );
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth() {
    const ctx = useContext(AuthContext);
    if (!ctx) throw new Error('useAuth must be used within an AuthProvider');
    return ctx;
}
