import React from 'react';

interface SkeletonLoaderProps {
    variant?: 'text' | 'circular' | 'rectangular';
    width?: string | number;
    height?: string | number;
    className?: string;
}

export const SkeletonLoader: React.FC<SkeletonLoaderProps> = ({
                                                                  variant = 'text',
                                                                  width,
                                                                  height,
                                                                  className = ''
                                                              }) => {
    const getVariantClasses = () => {
        switch (variant) {
            case 'circular':
                return 'rounded-full';
            case 'rectangular':
                return 'rounded-md';
            case 'text':
            default:
                return 'rounded';
        }
    };

    const style: React.CSSProperties = {};
    if (width) style.width = typeof width === 'number' ? `${width}px` : width;
    if (height) style.height = typeof height === 'number' ? `${height}px` : height;

    return (
        <div
            className={`
                loading-shimmer
                ${getVariantClasses()}
                ${variant === 'text' ? 'h-4' : ''}
                ${className}
            `}
            style={style}
        />
    );
};

export const FileGridSkeleton: React.FC = () => (
    <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 xl:grid-cols-8 gap-4 p-4">
        {Array.from({ length: 16 }).map((_, index) => (
            <div key={index} className="p-3 border-2 border-gray-200 rounded-lg">
                <div className="flex flex-col items-center">
                    <SkeletonLoader variant="rectangular" width={48} height={48} className="mb-2" />
                    <SkeletonLoader width="80%" height={16} className="mb-1" />
                    <SkeletonLoader width="60%" height={12} />
                </div>
            </div>
        ))}
    </div>
);

export const FileListSkeleton: React.FC = () => (
    <div className="divide-y divide-gray-200">
        {Array.from({ length: 10 }).map((_, index) => (
            <div key={index} className="flex items-center px-4 py-3">
                <SkeletonLoader variant="circular" width={20} height={20} className="mr-4" />
                <div className="flex-1 grid grid-cols-4 gap-4">
                    <SkeletonLoader width="70%" height={16} />
                    <SkeletonLoader width="40%" height={16} />
                    <SkeletonLoader width="50%" height={16} />
                    <SkeletonLoader width="60%" height={16} />
                </div>
            </div>
        ))}
    </div>
);