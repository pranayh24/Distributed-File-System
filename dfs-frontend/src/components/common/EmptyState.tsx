import React from 'react';

interface EmptyStateProps {
    icon?: React.ReactNode;
    title: string;
    description?: string;
    action?: {
        label: string;
        onClick: () => void;
        variant?: 'primary' | 'secondary';
    };
    children?: React.ReactNode;
}

export const EmptyState: React.FC<EmptyStateProps> = ({
                                                          icon,
                                                          title,
                                                          description,
                                                          action,
                                                          children
                                                      }) => {
    return (
        <div className="flex flex-col items-center justify-center h-64 p-8 text-center">
            {icon && (
                <div className="text-gray-300 dark:text-gray-600 mb-4 animate-bounce-gentle">
                    {icon}
                </div>
            )}

            <h3 className="text-xl font-semibold text-gray-900 dark:text-gray-100 mb-2">
                {title}
            </h3>

            {description && (
                <p className="text-gray-600 dark:text-gray-400 mb-6 max-w-md">
                    {description}
                </p>
            )}

            {action && (
                <button
                    onClick={action.onClick}
                    className={action.variant === 'secondary' ? 'btn-secondary' : 'btn-primary'}
                >
                    {action.label}
                </button>
            )}

            {children}
        </div>
    );
};