import React, { createContext, useContext, useReducer, useEffect } from 'react';
import { CheckCircle, XCircle, AlertCircle, Info, X } from 'lucide-react';

interface Notification {
    id: string;
    type: 'success' | 'error' | 'warning' | 'info';
    title: string;
    message?: string;
    duration?: number;
    action?: {
        label: string;
        onClick: () => void;
    };
}

interface NotificationState {
    notifications: Notification[];
}

type NotificationAction =
    | { type: 'ADD_NOTIFICATION'; payload: Notification }
    | { type: 'REMOVE_NOTIFICATION'; payload: string }
    | { type: 'CLEAR_ALL' };

const NotificationContext = createContext<{
    state: NotificationState;
    addNotification: (notification: Omit<Notification, 'id'>) => void;
    removeNotification: (id: string) => void;
    clearAll: () => void;
} | null>(null);

const notificationReducer = (state: NotificationState, action: NotificationAction): NotificationState => {
    switch (action.type) {
        case 'ADD_NOTIFICATION':
            return {
                ...state,
                notifications: [...state.notifications, action.payload]
            };
        case 'REMOVE_NOTIFICATION':
            return {
                ...state,
                notifications: state.notifications.filter(n => n.id !== action.payload)
            };
        case 'CLEAR_ALL':
            return {
                ...state,
                notifications: []
            };
        default:
            return state;
    }
};

export const NotificationProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
    const [state, dispatch] = useReducer(notificationReducer, { notifications: [] });

    const addNotification = (notification: Omit<Notification, 'id'>) => {
        const id = Math.random().toString(36).substring(2, 9);
        const newNotification: Notification = {
            ...notification,
            id,
            duration: notification.duration || 5000
        };

        dispatch({ type: 'ADD_NOTIFICATION', payload: newNotification });

        // Auto remove after duration
        if (newNotification.duration > 0) {
            setTimeout(() => {
                dispatch({ type: 'REMOVE_NOTIFICATION', payload: id });
            }, newNotification.duration);
        }
    };

    const removeNotification = (id: string) => {
        dispatch({ type: 'REMOVE_NOTIFICATION', payload: id });
    };

    const clearAll = () => {
        dispatch({ type: 'CLEAR_ALL' });
    };

    return (
        <NotificationContext.Provider value={{
            state,
            addNotification,
            removeNotification,
            clearAll
        }}>
            {children}
            <NotificationContainer />
        </NotificationContext.Provider>
    );
};

const NotificationContainer: React.FC = () => {
    const context = useContext(NotificationContext);
    if (!context) return null;

    const { state, removeNotification } = context;

    return (
        <div className="fixed top-4 right-4 z-50 space-y-2 w-96 max-w-sm">
            {state.notifications.map(notification => (
                <NotificationItem
                    key={notification.id}
                    notification={notification}
                    onRemove={removeNotification}
                />
            ))}
        </div>
    );
};

const NotificationItem: React.FC<{
    notification: Notification;
    onRemove: (id: string) => void;
}> = ({ notification, onRemove }) => {
    const icons = {
        success: CheckCircle,
        error: XCircle,
        warning: AlertCircle,
        info: Info
    };

    const colors = {
        success: 'bg-green-50 border-green-200 text-green-800',
        error: 'bg-red-50 border-red-200 text-red-800',
        warning: 'bg-yellow-50 border-yellow-200 text-yellow-800',
        info: 'bg-blue-50 border-blue-200 text-blue-800'
    };

    const iconColors = {
        success: 'text-green-500',
        error: 'text-red-500',
        warning: 'text-yellow-500',
        info: 'text-blue-500'
    };

    const Icon = icons[notification.type];

    return (
        <div className={`
            animate-slideIn border rounded-lg p-4 shadow-lg backdrop-blur-sm
            ${colors[notification.type]}
        `}>
            <div className="flex items-start">
                <Icon size={20} className={`mt-0.5 mr-3 ${iconColors[notification.type]}`} />
                <div className="flex-1 min-w-0">
                    <p className="font-medium text-sm">{notification.title}</p>
                    {notification.message && (
                        <p className="mt-1 text-sm opacity-90">{notification.message}</p>
                    )}
                    {notification.action && (
                        <button
                            onClick={notification.action.onClick}
                            className="mt-2 text-sm font-medium underline hover:no-underline"
                        >
                            {notification.action.label}
                        </button>
                    )}
                </div>
                <button
                    onClick={() => onRemove(notification.id)}
                    className="ml-2 p-1 rounded-full hover:bg-white/20 transition-colors"
                >
                    <X size={16} />
                </button>
            </div>
        </div>
    );
};

export const useNotifications = () => {
    const context = useContext(NotificationContext);
    if (!context) {
        throw new Error('useNotifications must be used within a NotificationProvider');
    }
    return context;
};