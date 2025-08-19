import React, { useState, useEffect } from 'react';
import { Keyboard, X } from 'lucide-react';

interface Shortcut {
    keys: string[];
    description: string;
    category: string;
}

const shortcuts: Shortcut[] = [
    { keys: ['Ctrl', 'C'], description: 'Copy selected files', category: 'File Operations' },
    { keys: ['Ctrl', 'X'], description: 'Cut selected files', category: 'File Operations' },
    { keys: ['Ctrl', 'V'], description: 'Paste files', category: 'File Operations' },
    { keys: ['Ctrl', 'A'], description: 'Select all files', category: 'Selection' },
    { keys: ['Delete'], description: 'Delete selected files', category: 'File Operations' },
    { keys: ['Escape'], description: 'Clear selection', category: 'Selection' },
    { keys: ['F2'], description: 'Rename file', category: 'File Operations' },
    { keys: ['Space'], description: 'Preview file', category: 'Navigation' },
    { keys: ['Enter'], description: 'Open file/folder', category: 'Navigation' },
    { keys: ['Ctrl', 'F'], description: 'Search files', category: 'Search' },
    { keys: ['Ctrl', 'Shift', 'N'], description: 'New folder', category: 'File Operations' },
    { keys: ['Ctrl', 'U'], description: 'Upload files', category: 'File Operations' },
];

export const KeyboardShortcuts: React.FC = () => {
    const [isVisible, setIsVisible] = useState(false);

    useEffect(() => {
        const handleKeyDown = (e: KeyboardEvent) => {
            if (e.key === '?' && (e.ctrlKey || e.metaKey)) {
                e.preventDefault();
                setIsVisible(true);
            } else if (e.key === 'Escape' && isVisible) {
                setIsVisible(false);
            }
        };

        document.addEventListener('keydown', handleKeyDown);
        return () => document.removeEventListener('keydown', handleKeyDown);
    }, [isVisible]);

    const groupedShortcuts = shortcuts.reduce((groups, shortcut) => {
        if (!groups[shortcut.category]) {
            groups[shortcut.category] = [];
        }
        groups[shortcut.category].push(shortcut);
        return groups;
    }, {} as Record<string, Shortcut[]>);

    const formatKeys = (keys: string[]) => {
        return keys.map((key, index) => (
            <React.Fragment key={key}>
                {index > 0 && <span className="mx-1 text-gray-400">+</span>}
                <kbd className="px-2 py-1 text-xs font-semibold text-gray-800 bg-gray-100 border border-gray-200 rounded-lg">
                    {key}
                </kbd>
            </React.Fragment>
        ));
    };

    if (!isVisible) {
        return (
            <button
                onClick={() => setIsVisible(true)}
                className="fixed bottom-4 right-4 p-3 bg-gray-900 text-white rounded-full shadow-lg hover:bg-gray-800 transition-colors z-40"
                title="Keyboard shortcuts (Ctrl+?)"
            >
                <Keyboard size={20} />
            </button>
        );
    }

    return (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
            <div className="bg-white dark:bg-gray-800 rounded-lg shadow-xl max-w-2xl w-full max-h-[80vh] overflow-y-auto">
                <div className="flex items-center justify-between p-6 border-b border-gray-200 dark:border-gray-700">
                    <div className="flex items-center gap-3">
                        <Keyboard size={24} className="text-primary-600" />
                        <h2 className="text-xl font-semibold text-gray-900 dark:text-gray-100">
                            Keyboard Shortcuts
                        </h2>
                    </div>
                    <button
                        onClick={() => setIsVisible(false)}
                        className="p-2 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-full transition-colors"
                    >
                        <X size={20} />
                    </button>
                </div>

                <div className="p-6 space-y-6">
                    {Object.entries(groupedShortcuts).map(([category, shortcuts]) => (
                        <div key={category}>
                            <h3 className="text-lg font-medium text-gray-900 dark:text-gray-100 mb-3">
                                {category}
                            </h3>
                            <div className="space-y-2">
                                {shortcuts.map((shortcut, index) => (
                                    <div
                                        key={index}
                                        className="flex items-center justify-between py-2 px-3 bg-gray-50 dark:bg-gray-700 rounded-lg"
                                    >
                                        <span className="text-sm text-gray-700 dark:text-gray-300">
                                            {shortcut.description}
                                        </span>
                                        <div className="flex items-center">
                                            {formatKeys(shortcut.keys)}
                                        </div>
                                    </div>
                                ))}
                            </div>
                        </div>
                    ))}
                </div>

                <div className="p-6 border-t border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-900">
                    <p className="text-sm text-gray-600 dark:text-gray-400 text-center">
                        Press <kbd className="px-2 py-1 text-xs bg-gray-200 dark:bg-gray-700 rounded">Ctrl</kbd> +
                        <kbd className="px-2 py-1 text-xs bg-gray-200 dark:bg-gray-700 rounded ml-1">?</kbd> to
                        toggle this help panel
                    </p>
                </div>
            </div>
        </div>
    );
};