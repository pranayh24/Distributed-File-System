import React, { useState, useEffect, useRef, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { User, LogOut, Settings, Search, Bell, X } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';

interface TopbarProps {
    onSearch?: (query: string) => void;
}

export const Topbar: React.FC<TopbarProps> = ({ onSearch }) => {
    const { user, logout } = useAuth();
    const [showUserMenu, setShowUserMenu] = useState(false);
    const [searchQuery, setSearchQuery] = useState('');
    const [isFocused, setIsFocused] = useState(false);
    const searchRef = useRef<HTMLInputElement>(null);
    const debounceRef = useRef<NodeJS.Timeout>();

    // Debounced search function
    const debouncedSearch = useCallback((query: string) => {
        if (debounceRef.current) {
            clearTimeout(debounceRef.current);
        }

        debounceRef.current = setTimeout(() => {
            if (onSearch) {
                onSearch(query);
            }
        }, 500);
    }, [onSearch]);

    // Handle search query changes
    useEffect(() => {
        debouncedSearch(searchQuery);

        return () => {
            if (debounceRef.current) {
                clearTimeout(debounceRef.current);
            }
        };
    }, [searchQuery, debouncedSearch]);

    const handleSearchChange = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
        setSearchQuery(e.target.value);
    }, []);

    const handleClearSearch = useCallback(() => {
        setSearchQuery('');
        if (debounceRef.current) {
            clearTimeout(debounceRef.current);
        }
        if (onSearch) {
            onSearch('');
        }
    }, [onSearch]);

    const handleLogout = async () => {
        await logout();
        setShowUserMenu(false);
    };

    return (
        <header className="h-16 flex-shrink-0 flex items-center justify-between px-6 bg-white dark:bg-gray-900 border-b border-gray-200 dark:border-gray-800">
            {/* Search bar */}
            <div className="flex-1 max-w-md">
                <div className={`
                    relative flex items-center border rounded-lg transition-all duration-200
                    ${isFocused ? 'border-primary-300 ring-2 ring-primary-100' : 'border-gray-300 dark:border-gray-700'}
                `}>
                    <Search className="absolute left-3 h-5 w-5 text-gray-400" />
                    <input
                        ref={searchRef}
                        type="search"
                        value={searchQuery}
                        onChange={handleSearchChange}
                        onFocus={() => setIsFocused(true)}
                        onBlur={() => setIsFocused(false)}
                        placeholder="Search files, folders..."
                        className="w-full pl-10 pr-10 py-2 border-0 rounded-lg bg-gray-50 dark:bg-gray-800 text-gray-900 dark:text-gray-100 focus:ring-0 focus:outline-none"
                    />
                    {searchQuery && (
                        <button
                            onClick={handleClearSearch}
                            className="absolute right-3 p-1 text-gray-400 hover:text-gray-600 transition-colors"
                            type="button"
                        >
                            <X className="h-4 w-4" />
                        </button>
                    )}
                </div>
            </div>

            {/* User menu */}
            <div className="flex items-center gap-4">
                <button className="p-2 rounded-full hover:bg-gray-100 dark:hover:bg-gray-800">
                    <Bell className="h-5 w-5 text-gray-600 dark:text-gray-400" />
                </button>

                <div className="relative">
                    <button
                        onClick={() => setShowUserMenu(!showUserMenu)}
                        className="flex items-center gap-2"
                    >
                        <div className="w-8 h-8 rounded-full bg-primary-500 text-white flex items-center justify-center font-bold">
                            {user?.username.charAt(0).toUpperCase()}
                        </div>
                        <span className="hidden md:inline text-sm font-medium text-gray-700 dark:text-gray-300">{user?.username}</span>
                    </button>

                    {showUserMenu && (
                        <div className="absolute right-0 mt-2 w-56 bg-white dark:bg-gray-800 rounded-lg shadow-lg border dark:border-gray-700 py-1 z-10">
                            <div className="px-4 py-2 border-b dark:border-gray-700">
                                <p className="text-sm font-semibold text-gray-900 dark:text-white">{user?.username}</p>
                                <p className="text-xs text-gray-500 dark:text-gray-400 truncate">{user?.email}</p>
                            </div>
                            <Link
                                to="/account"
                                className="flex items-center gap-3 px-4 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700"
                                onClick={() => setShowUserMenu(false)}
                            >
                                <Settings className="h-4 w-4" />
                                <span>Account</span>
                            </Link>
                            <button
                                onClick={handleLogout}
                                className="w-full text-left flex items-center gap-3 px-4 py-2 text-sm text-red-600 hover:bg-red-50 dark:hover:bg-red-900/20"
                            >
                                <LogOut className="h-4 w-4" />
                                <span>Logout</span>
                            </button>
                        </div>
                    )}
                </div>
            </div>
        </header>
    );
};
