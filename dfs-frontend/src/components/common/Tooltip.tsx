import React, { useState, useRef, useEffect } from 'react';

interface TooltipProps {
    content: string | React.ReactNode;
    position?: 'top' | 'bottom' | 'left' | 'right';
    delay?: number;
    className?: string;
    children: React.ReactNode;
    disabled?: boolean;
}

export const Tooltip: React.FC<TooltipProps> = ({
                                                    content,
                                                    position = 'top',
                                                    delay = 500,
                                                    className = '',
                                                    children,
                                                    disabled = false
                                                }) => {
    const [isVisible, setIsVisible] = useState(false);
    const [actualPosition, setActualPosition] = useState(position);
    const timeoutRef = useRef<NodeJS.Timeout>();
    const tooltipRef = useRef<HTMLDivElement>(null);
    const triggerRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        return () => {
            if (timeoutRef.current) {
                clearTimeout(timeoutRef.current);
            }
        };
    }, []);

    const showTooltip = () => {
        if (disabled) return;

        timeoutRef.current = setTimeout(() => {
            setIsVisible(true);
            adjustPosition();
        }, delay);
    };

    const hideTooltip = () => {
        if (timeoutRef.current) {
            clearTimeout(timeoutRef.current);
        }
        setIsVisible(false);
    };

    const adjustPosition = () => {
        if (!tooltipRef.current || !triggerRef.current) return;

        const tooltipRect = tooltipRef.current.getBoundingClientRect();
        const triggerRect = triggerRef.current.getBoundingClientRect();
        const viewport = {
            width: window.innerWidth,
            height: window.innerHeight
        };

        let newPosition = position;

        // Check if tooltip would overflow viewport and adjust
        switch (position) {
            case 'top':
                if (triggerRect.top - tooltipRect.height < 10) {
                    newPosition = 'bottom';
                }
                break;
            case 'bottom':
                if (triggerRect.bottom + tooltipRect.height > viewport.height - 10) {
                    newPosition = 'top';
                }
                break;
            case 'left':
                if (triggerRect.left - tooltipRect.width < 10) {
                    newPosition = 'right';
                }
                break;
            case 'right':
                if (triggerRect.right + tooltipRect.width > viewport.width - 10) {
                    newPosition = 'left';
                }
                break;
        }

        setActualPosition(newPosition);
    };

    const getPositionClasses = () => {
        const baseClasses = 'absolute z-50 px-3 py-2 text-sm font-medium text-white bg-gray-900 rounded-lg shadow-sm';

        switch (actualPosition) {
            case 'top':
                return `${baseClasses} bottom-full left-1/2 transform -translate-x-1/2 mb-2`;
            case 'bottom':
                return `${baseClasses} top-full left-1/2 transform -translate-x-1/2 mt-2`;
            case 'left':
                return `${baseClasses} right-full top-1/2 transform -translate-y-1/2 mr-2`;
            case 'right':
                return `${baseClasses} left-full top-1/2 transform -translate-y-1/2 ml-2`;
            default:
                return baseClasses;
        }
    };

    const getArrowClasses = () => {
        const baseClasses = 'absolute w-2 h-2 bg-gray-900 transform rotate-45';

        switch (actualPosition) {
            case 'top':
                return `${baseClasses} top-full left-1/2 -translate-x-1/2 -mt-1`;
            case 'bottom':
                return `${baseClasses} bottom-full left-1/2 -translate-x-1/2 -mb-1`;
            case 'left':
                return `${baseClasses} left-full top-1/2 -translate-y-1/2 -ml-1`;
            case 'right':
                return `${baseClasses} right-full top-1/2 -translate-y-1/2 -mr-1`;
            default:
                return baseClasses;
        }
    };

    return (
        <div
            ref={triggerRef}
            className={`relative inline-block ${className}`}
            onMouseEnter={showTooltip}
            onMouseLeave={hideTooltip}
            onFocus={showTooltip}
            onBlur={hideTooltip}
        >
            {children}
            {isVisible && content && (
                <div
                    ref={tooltipRef}
                    className={`${getPositionClasses()} animate-fadeIn pointer-events-none whitespace-nowrap max-w-xs`}
                >
                    {content}
                    <div className={getArrowClasses()} />
                </div>
            )}
        </div>
    );
};