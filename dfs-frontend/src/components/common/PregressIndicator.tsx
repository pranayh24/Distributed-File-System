import React from 'react';

interface ProgressIndicatorProps {
    value: number; // 0-100
    size?: 'sm' | 'md' | 'lg';
    variant?: 'linear' | 'circular';
    color?: 'primary' | 'success' | 'warning' | 'error';
    showLabel?: boolean;
    label?: string;
    animated?: boolean;
    className?: string;
}

export const ProgressIndicator: React.FC<ProgressIndicatorProps> = ({
                                                                        value,
                                                                        size = 'md',
                                                                        variant = 'linear',
                                                                        color = 'primary',
                                                                        showLabel = false,
                                                                        label,
                                                                        animated = true,
                                                                        className = ''
                                                                    }) => {
    const normalizedValue = Math.min(100, Math.max(0, value));

    const colorClasses = {
        primary: 'text-primary-600 bg-primary-600',
        success: 'text-green-600 bg-green-600',
        warning: 'text-yellow-600 bg-yellow-600',
        error: 'text-red-600 bg-red-600'
    };

    const sizeClasses = {
        sm: variant === 'linear' ? 'h-1' : 'w-8 h-8',
        md: variant === 'linear' ? 'h-2' : 'w-12 h-12',
        lg: variant === 'linear' ? 'h-3' : 'w-16 h-16'
    };

    if (variant === 'circular') {
        const radius = size === 'sm' ? 14 : size === 'md' ? 20 : 28;
        const circumference = 2 * Math.PI * radius;
        const strokeDashoffset = circumference - (normalizedValue / 100) * circumference;

        return (
            <div className={`relative ${sizeClasses[size]} ${className}`}>
                <svg className="w-full h-full transform -rotate-90" viewBox="0 0 48 48">
                    <circle
                        cx="24"
                        cy="24"
                        r={radius}
                        stroke="currentColor"
                        strokeWidth="3"
                        fill="none"
                        className="text-gray-200 dark:text-gray-700"
                    />
                    <circle
                        cx="24"
                        cy="24"
                        r={radius}
                        stroke="currentColor"
                        strokeWidth="3"
                        fill="none"
                        strokeDasharray={circumference}
                        strokeDashoffset={strokeDashoffset}
                        strokeLinecap="round"
                        className={`${colorClasses[color]} ${animated ? 'transition-all duration-500 ease-out' : ''}`}
                    />
                </svg>
                {showLabel && (
                    <div className="absolute inset-0 flex items-center justify-center">
                        <span className={`text-xs font-medium ${colorClasses[color]}`}>
                            {label || `${Math.round(normalizedValue)}%`}
                        </span>
                    </div>
                )}
            </div>
        );
    }

    return (
        <div className={`w-full ${className}`}>
            {showLabel && (
                <div className="flex justify-between mb-1">
                    <span className="text-sm font-medium text-gray-700 dark:text-gray-300">
                        {label}
                    </span>
                    <span className="text-sm text-gray-500 dark:text-gray-400">
                        {Math.round(normalizedValue)}%
                    </span>
                </div>
            )}
            <div className={`w-full bg-gray-200 dark:bg-gray-700 rounded-full ${sizeClasses[size]}`}>
                <div
                    className={`${colorClasses[color]} ${sizeClasses[size]} rounded-full ${
                        animated ? 'transition-all duration-500 ease-out' : ''
                    }`}
                    style={{ width: `${normalizedValue}%` }}
                />
            </div>
        </div>
    );
};