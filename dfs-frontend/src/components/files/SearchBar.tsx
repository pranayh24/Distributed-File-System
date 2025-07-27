import React, { useState, useEffect, useRef } from 'react';
import { Search, X } from 'lucide-react';

interface SearchBarProps {
    onSearch: (query: string) => void;
    placeholder?: string;
    className?: string;
}

export const SearchBar: React.FC<SearchBarProps> = ({
    onSearch,
    placeholder = "Search files...",
    className = ""
}) => {
    const [query, setQuery] = useState('');
    const [isFocused, setIsFocused] = useState(false);
    const searchRef = useRef<HTMLInputElement>(null);

    // Debounce search to avoid too many API calls
    useEffect(() => {
        const timeoutId = setTimeout(() => {
            onSearch(query);
        }, 300);

        return () => clearTimeout(timeoutId);
    }, [query, onSearch]);

    const handleClear = () => {
        setQuery('');
        onSearch('');
    };

    return (
        <div className={`relative flex-1 max-w-md ${className}`}>
            <div className={`
                relative flex items-center border rounded-lg transition-all duration-200
                ${isFocused ? 'border-primary-300 ring-2 ring-primary-100' : 'border-gray-300'}
            `}>
                <Search
                    size={20}
                    className="absolute left-3 text-gray-400"
                />
                <input
                    ref={searchRef}
                    type="text"
                    value={query}
                    onChange={(e) => setQuery(e.target.value)}
                    onFocus={() => setIsFocused(true)}
                    onBlur={() => setIsFocused(false)}
                    placeholder={placeholder}
                    className="w-full pl-10 pr-10 py-2 border-0 rounded-lg focus:ring-0 focus:outline-none"
                />
                {query && (
                    <button
                        onClick={handleClear}
                        className="absolute right-3 p-1 text-gray-400 hover:text-gray-600 transition-colors"
                    >
                        <X size={16} />
                    </button>
                )}
            </div>
        </div>
    );
};