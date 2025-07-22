import React, { useState } from 'react';
import { HardDrive, User, LogOut, Settings } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { formatFileSize } from '../../utils/formatters';

interface LayoutProps {
    children: React.ReactNode;
}

export const Layout: React.FC<LayoutProps> = ({ children }) => {
    const { user, logout } = useAuth();
    const [showUserMenu, setShowUserMenu] = useState(false);

    const handleLogout = async () => {
        await logout();
        setShowUserMenu(false);
    };

    return (
        <div className="min-h-screen bg-gray-50">
            <header className="bg-white shadow-sm border-b">
                <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
                    <div className="flex justify-between items-center h-16">
                        <div className="flex items-center space-x-2">
                            <HardDrive className="h-8 w-8 text-primary-600" />
                            <h1 className="text-xl font-semibold text-gray-900">
                                Distributed File System
                            </h1>
                        </div>

                        <div className="flex items-center space-x-4">
                            <nav className="flex space-x-4">
                                <button className="text-gray-600 hover:text-gray-900 px-3 py-2 rounded-md text-sm font-medium">
                                    Files
                                </button>
                                <button className="text-gray-600 hover:text-gray-900 px-3 py-2 rounded-md text-sm font-medium">
                                    Dashboard
                                </button>
                            </nav>

                            {/* User Menu */}
                            <div className="relative">
                                <button
                                    onClick={() => setShowUserMenu(!showUserMenu)}
                                    className="flex items-center space-x-2 text-gray-600 hover:text-gray-900 px-3 py-2 rounded-md text-sm font-medium"
                                >
                                    <User className="h-5 w-5" />
                                    <span>{user?.username}</span>
                                </button>

                                {showUserMenu && (
                                    <div className="absolute right-0 mt-2 w-64 bg-white rounded-md shadow-lg py-1 z-50 border">
                                        <div className="px-4 py-2 border-b">
                                            <p className="text-sm font-medium text-gray-900">{user?.username}</p>
                                            <p className="text-sm text-gray-500">{user?.email}</p>
                                            <p className="text-xs text-gray-400 mt-1">
                                                Storage: {formatFileSize(user?.currentUsage || 0)} / {formatFileSize(user?.quotaLimit || 0)}
                                            </p>
                                        </div>

                                        <button
                                            onClick={() => setShowUserMenu(false)}
                                            className="flex items-center space-x-2 w-full text-left px-4 py-2 text-sm text-gray-700 hover:bg-gray-100"
                                        >
                                            <Settings className="h-4 w-4" />
                                            <span>Settings</span>
                                        </button>

                                        <button
                                            onClick={handleLogout}
                                            className="flex items-center space-x-2 w-full text-left px-4 py-2 text-sm text-gray-700 hover:bg-gray-100"
                                        >
                                            <LogOut className="h-4 w-4" />
                                            <span>Sign out</span>
                                        </button>
                                    </div>
                                )}
                            </div>
                        </div>
                    </div>
                </div>
            </header>

            <main className="max-w-7xl mx-auto py-6 px-4 sm:px-6 lg:px-8">
                {children}
            </main>
        </div>
    );
};