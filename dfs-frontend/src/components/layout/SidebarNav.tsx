import React from 'react';
import { Link, useLocation } from 'react-router-dom';
import { HardDrive, File, BarChart2, Trash2, Settings } from 'lucide-react';

const navItems = [
    { href: '/files', icon: File, label: 'My Files' },
    { href: '/dashboard', icon: BarChart2, label: 'Dashboard' },
    { href: '/trash', icon: Trash2, label: 'Trash' },
    { href: '/account', icon: Settings, label: 'Account' },
];

export const SidebarNav: React.FC = () => {
    const location = useLocation();

    return (
        <aside className="w-64 flex-shrink-0 bg-white dark:bg-gray-900 border-r border-gray-200 dark:border-gray-800 flex flex-col">
            <div className="h-16 flex items-center px-6 border-b border-gray-200 dark:border-gray-800">
                <Link to="/" className="flex items-center gap-3">
                    <HardDrive className="h-8 w-8 text-primary-600" />
                    <span className="text-xl font-bold text-gray-900 dark:text-white">DFS</span>
                </Link>
            </div>
            <nav className="flex-1 px-4 py-6 space-y-2">
                {navItems.map((item) => (
                    <Link
                        key={item.label}
                        to={item.href}
                        className={`flex items-center gap-3 px-4 py-2.5 rounded-lg text-sm font-medium transition-colors ${
                            location.pathname.startsWith(item.href)
                                ? 'bg-primary-500 text-white shadow-md'
                                : 'text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-800'
                        }`}
                    >
                        <item.icon className="h-5 w-5" />
                        <span>{item.label}</span>
                    </Link>
                ))}
            </nav>
            <div className="p-4 border-t border-gray-200 dark:border-gray-800">
                {/* Can add storage quota indicator here later */}
            </div>
        </aside>
    );
};
