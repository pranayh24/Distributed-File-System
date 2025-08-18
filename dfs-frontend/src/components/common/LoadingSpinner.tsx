import React from 'react';

interface LoadingSpinnerProps {
    size?: 'sm' | 'md' | 'lg' | 'xl';
    variant?: 'primary' | 'secondary' | 'white';
    className?: string;
}

export const LoadingSpinner: React.FC<LoadingSpinnerProps> = ({
                                                                  size = 'md',
                                                                  variant = 'primary',
                                                                  className = ''
                                                              }) => {
    const sizeClasses = {
        sm: 'w-4 h-4',
        md: 'w-6 h-6',
        lg: 'w-8 h-8',
        xl: 'w-12 h-12'
    };

    const variantClasses = {
        primary: 'border-primary-600 border-t-transparent',
        secondary: 'border-gray-400 border-t-transparent',
        white: 'border-white border-t-transparent'
    };

    return (
        <div
            className={`
                animate-spin rounded-full border-2
                ${sizeClasses[size]}
                ${variantClasses[variant]}
                ${className}
            `}
        />
    );
};