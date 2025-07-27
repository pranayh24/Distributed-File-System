import React from 'react';
import type { FileItem } from './types';
import { FileIcon } from './FileIcon';
import { formatFileSize, formatDate } from '../../utils/formatters';

interface FileGridProps {
    files: FileItem[];
    selectedFiles: Set<string>;
    onFileClick: (file: FileItem) => void;
    onFileSelect: (fileId: string, selected: boolean) => void;
    onSelectAll: () => void;
    onContextMenu: (e: React.MouseEvent, file: FileItem) => void;
}

export const FileGrid: React.FC<FileGridProps> = ({
    files,
    selectedFiles,
    onFileClick,
    onFileSelect,
    onSelectAll,
    onContextMenu,
}) => {
    const handleFileClick = (file: FileItem, e: React.MouseEvent) => {
        if (e.ctrlKey || e.metaKey) {
            // Multi-select with Ctrl/Cmd
            onFileSelect(file.id, !selectedFiles.has(file.id));
        } else {
            // Single click - open file/folder
            onFileClick(file);
        }
    };

    return (
        <div className="h-full overflow-auto">
            {/* Select All Header */}
            <div className="sticky top-0 bg-white/90 backdrop-blur-sm border-b p-4 mb-4">
                <label className="flex items-center gap-2 cursor-pointer">
                    <input
                        type="checkbox"
                        checked={files.length > 0 && selectedFiles.size === files.length}
                        onChange={onSelectAll}
                        className="rounded"
                    />
                    <span className="text-sm text-gray-600">
                        Select all ({files.length} items)
                    </span>
                </label>
            </div>

            {/* Grid */}
            <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 xl:grid-cols-8 gap-4 p-4">
                {files.map((file) => (
                    <div
                        key={file.id}
                        className={`
                            group relative p-3 rounded-lg border-2 cursor-pointer
                            transition-all duration-200 hover:shadow-md
                            ${selectedFiles.has(file.id)
                                ? 'border-primary-300 bg-primary-50'
                                : 'border-gray-200 bg-white hover:border-gray-300'
                            }
                        `}
                        onClick={(e) => handleFileClick(file, e)}
                        onContextMenu={(e) => onContextMenu(e, file)}
                    >
                        {/* Selection Checkbox */}
                        <div className="absolute top-2 left-2 opacity-0 group-hover:opacity-100 transition-opacity">
                            <input
                                type="checkbox"
                                checked={selectedFiles.has(file.id)}
                                onChange={(e) => {
                                    e.stopPropagation();
                                    onFileSelect(file.id, e.target.checked);
                                }}
                                className="rounded"
                            />
                        </div>

                        {/* File Icon */}
                        <div className="flex flex-col items-center">
                            <div className="mb-2">
                                <FileIcon
                                    file={file}
                                    size={48}
                                    className="text-gray-600"
                                />
                            </div>

                            {/* File Name */}
                            <div className="text-center w-full">
                                <p className="text-sm font-medium text-gray-900 truncate">
                                    {file.name}
                                </p>
                                <p className="text-xs text-gray-500 mt-1">
                                    {file.isDirectory ? 'Folder' : formatFileSize(file.size)}
                                </p>
                                <p className="text-xs text-gray-400">
                                    {formatDate(file.lastModified)}
                                </p>
                            </div>
                        </div>

                        {/* Selection Indicator */}
                        {selectedFiles.has(file.id) && (
                            <div className="absolute inset-0 rounded-lg border-2 border-primary-500 bg-primary-100/20"></div>
                        )}
                    </div>
                ))}
            </div>

            {/* Empty State */}
            {files.length === 0 && (
                <div className="flex flex-col items-center justify-center h-64 text-gray-500">
                    <div className="text-4xl mb-4">📁</div>
                    <p className="text-lg font-medium">No files found</p>
                    <p className="text-sm">This folder is empty</p>
                </div>
            )}
        </div>
    );
};