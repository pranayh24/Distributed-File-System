import React from 'react';
import { LoadingSpinner } from './LoadingSpinner';

interface LoadingStateProps {
    message?: string;
    description?: string;
    size?: 'sm' | 'md' | 'lg';
    fullHeight?: boolean;
}

export const LoadingState: React.FC<LoadingStateProps> = ({
                                                              message = 'Loading...',
                                                              description,
                                                              size = 'md',
                                                              fullHeight = false
                                                          }) => {
    const containerClass = fullHeight ? 'min-h-screen' : 'h-64';

    return (
        <div className={`${containerClass} flex items-center justify-center p-8`}>
            <div className="text-center">
                <LoadingSpinner size={size === 'sm' ? 'md' : size === 'md' ? 'lg' : 'xl'} />
                <div className="mt-4">
                    <h3 className="text-lg font-medium text-gray-900 dark:text-gray-100">
                        {message}
                    </h3>
                    {description && (
                        <p className="mt-2 text-sm text-gray-500 dark:text-gray-400">
                            {description}
                        </p>
                    )}
                </div>
            </div>
        </div>
    );
};