import React from 'react';
import { ChevronRight, Home } from 'lucide-react';

interface BreadcrumbNavProps {
    path: string[];
    onNavigate: (path: string) => void;
}

export const BreadcrumbNav: React.FC<BreadcrumbNavProps> = ({
    path,
    onNavigate,
}) => {
    const handleNavigate = (index: number) => {
        if (index === -1) {
            // Navigate to root
            onNavigate('');
        } else {
            // Navigate to specific path level
            const newPath = path.slice(0, index + 1).join('/');
            onNavigate(newPath);
        }
    };

    return (
        <nav className="flex items-center gap-2 mb-4 text-sm">
            {/* Home/Root */}
            <button
                onClick={() => handleNavigate(-1)}
                className="flex items-center gap-1 px-2 py-1 text-gray-600 hover:text-primary-600 hover:bg-gray-100 rounded transition-colors"
            >
                <Home size={16} />
                <span>Home</span>
            </button>

            {/* Path segments */}
            {path.map((segment, index) => (
                <React.Fragment key={index}>
                    <ChevronRight size={16} className="text-gray-400" />
                    <button
                        onClick={() => handleNavigate(index)}
                        className={`px-2 py-1 rounded transition-colors ${
                            index === path.length - 1
                                ? 'text-gray-900 font-medium'
                                : 'text-gray-600 hover:text-primary-600 hover:bg-gray-100'
                        }`}
                    >
                        {segment}
                    </button>
                </React.Fragment>
            ))}
        </nav>
    );
};