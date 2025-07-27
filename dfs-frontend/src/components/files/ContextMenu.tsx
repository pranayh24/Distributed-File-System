import React, { useEffect, useRef, useState } from 'react';
import { Download, Edit, Trash2, Copy, Move, Info } from 'lucide-react';
import { FileItem } from './FileManager';

interface ContextMenuProps {
    x: number;
    y: number;
    file: FileItem;
    onClose: () => void;
    onRename: (file: FileItem, newName: string) => void;
    onDelete: (file: FileItem) => void;
    onDownload: (file: FileItem) => void;
}

export const ContextMenu: React.FC<ContextMenuProps> = ({
    x,
    y,
    file,
    onClose,
    onRename,
    onDelete,
    onDownload,
}) => {
    const menuRef = useRef<HTMLDivElement>(null);
    const [isRenaming, setIsRenaming] = useState(false);
    const [newName, setNewName] = useState(file.name);

    useEffect(() => {
        const handleClickOutside = (event: MouseEvent) => {
            if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
                onClose();
            }
        };

        const handleEscape = (event: KeyboardEvent) => {
            if (event.key === 'Escape') {
                onClose();
            }
        };

        document.addEventListener('mousedown', handleClickOutside);
        document.addEventListener('keydown', handleEscape);

        return () => {
            document.removeEventListener('mousedown', handleClickOutside);
            document.removeEventListener('keydown', handleEscape);
        };
    }, [onClose]);

    const handleRename = () => {
        if (newName.trim() && newName !== file.name) {
            onRename(file, newName.trim());
        }
        setIsRenaming(false);
        onClose();
    };

    const handleRenameKeyDown = (e: React.KeyboardEvent) => {
        if (e.key === 'Enter') {
            handleRename();
        } else if (e.key === 'Escape') {
            setIsRenaming(false);
            setNewName(file.name);
        }
    };

    const menuItems = [
        ...(file.isDirectory ? [] : [{
            icon: Download,
            label: 'Download',
            onClick: () => {
                onDownload(file);
                onClose();
            }
        }]),
        {
            icon: Edit,
            label: 'Rename',
            onClick: () => setIsRenaming(true)
        },
        {
            icon: Copy,
            label: 'Copy',
            onClick: () => {
                // TODO: Implement copy functionality
                onClose();
            }
        },
        {
            icon: Move,
            label: 'Move',
            onClick: () => {
                // TODO: Implement move functionality
                onClose();
            }
        },
        {
            icon: Info,
            label: 'Properties',
            onClick: () => {
                // TODO: Implement properties modal
                onClose();
            }
        },
        {
            icon: Trash2,
            label: 'Delete',
            onClick: () => {
                if (confirm(`Are you sure you want to delete "${file.name}"?`)) {
                    onDelete(file);
                }
                onClose();
            },
            className: 'text-red-600 hover:bg-red-50'
        }
    ];

    return (
        <div
            ref={menuRef}
            className="fixed bg-white border border-gray-200 rounded-lg shadow-lg py-2 z-50 min-w-48"
            style={{
                left: Math.min(x, window.innerWidth - 200),
                top: Math.min(y, window.innerHeight - 300)
            }}
        >
            {isRenaming ? (
                <div className="px-3 py-2">
                    <input
                        type="text"
                        value={newName}
                        onChange={(e) => setNewName(e.target.value)}
                        onKeyDown={handleRenameKeyDown}
                        onBlur={handleRename}
                        className="w-full px-2 py-1 text-sm border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-primary-500"
                        autoFocus
                    />
                </div>
            ) : (
                menuItems.map((item, index) => (
                    <button
                        key={index}
                        onClick={item.onClick}
                        className={`
                            w-full flex items-center gap-3 px-3 py-2 text-sm text-left
                            hover:bg-gray-100 transition-colors
                            ${item.className || 'text-gray-700'}
                        `}
                    >
                        <item.icon size={16} />
                        {item.label}
                    </button>
                ))
            )}
        </div>
    );
};