import React, {useContext, useEffect, useReducer, createContext} from "react";
import type {LoginRequest, RegisterRequest, User} from "../types";
import {AuthService} from "../services/authService.ts";

interface AuthState {
    user: User | null;
    isAuthenticated: boolean;
    isLoading: boolean;
    error: string | null;
}

interface AuthContextType extends AuthState {
    login: (credentials : LoginRequest) => Promise<void>;
    register: (userData: RegisterRequest) => Promise<void>;
    logout: () => Promise<void>;
    clearError: () => void;
}

type AuthAction =
    | { type: 'AUTH_START' }
    | { type: 'AUTH_SUCCESS'; payload: User}
    | { type: 'AUTH_ERROR'; payload: string}
    | { type: 'AUTH_LOGOUT'}
    | { type: 'CLEAR_ERROR'}
    | { type: 'SET_USER'; payload: User };

const initialState: AuthState = {
    user: null,
    isAuthenticated: false,
    isLoading: false,
    error: null,
};

const authReducer = (state: AuthState, action: AuthAction): AuthState => {
    switch (action.type) {
        case 'AUTH_START':
            return {
                ...state,
                isLoading: true,
                error: null,
            };
        case 'AUTH_SUCCESS':
            return {
                ...state,
                user: action.payload,
                isAuthenticated: true,
                isLoading: false,
                error: null,
            };
        case 'AUTH_ERROR' :
            return {
                ...state,
                user: null,
                isAuthenticated: false,
                isLoading: false,
                error: action.payload,
            };
        case 'AUTH_LOGOUT':
            return {
                ...state,
                user: null,
                isAuthenticated: false,
                isLoading: false,
                error: null,
            };
        case 'SET_USER':
            return {
                ...state,
                user: action.payload,
                isAuthenticated: true,
            };
        case 'CLEAR_ERROR':
            return {
                ...state,
                error: null,
            };
        default:
            return state;
    }
};

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
    const [state, dispatch] = useReducer(authReducer, initialState);

    useEffect(() => {
        const user = AuthService.getCurrentUser();
        const sessionId = AuthService.getSessionId();

        if(user && sessionId) {
            dispatch({ type: 'SET_USER', payload: user })
        }
    }, []);

    const login = async (credentials: LoginRequest) => {
        try {
            dispatch({ type: 'AUTH_START' });
            const { user } = await AuthService.login(credentials);
            dispatch({ type: 'AUTH_SUCCESS', payload: user });
        } catch (error : any) {
            dispatch({ type: 'AUTH_ERROR', payload: error.message });
            throw error;
        }
    };

    const register = async(userData: RegisterRequest) => {
        try {
            dispatch({ type: 'AUTH_START' });
            const { user } = await AuthService.register(userData);
            dispatch({ type: 'AUTH_SUCCESS', payload: user });
        } catch (error : any) {
            dispatch({ type: 'AUTH_ERROR', payload: error.message });
            throw error;
        }
    };

    const logout = async() => {
        try {
            await AuthService.logout();
            dispatch({ type: 'AUTH_LOGOUT' });
        } catch (error) {
            console.error("Logout error:", error);
            dispatch({ type: 'AUTH_LOGOUT' });
        }
    };

    const clearError = () => {
        dispatch({ type: 'CLEAR_ERROR' });
    };

    const value: AuthContextType = {
        ...state,
        login,
        register,
        logout,
        clearError,
    };

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export const useAuth = () => {
    const context = useContext(AuthContext);
    if(context == undefined) {
        throw new Error('useAuth must be used within an AuthProvider');
    }
    return context;
}