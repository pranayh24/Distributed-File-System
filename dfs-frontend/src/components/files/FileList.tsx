import React from 'react';
import type { FileItem } from './types';
import { FileIcon } from './FileIcon';
import { formatFileSize, formatDate } from '../../utils/formatters';

interface FileListProps {
    files: FileItem[];
    selectedFiles: Set<string>;
    onFileClick: (file: FileItem) => void;
    onFileSelect: (fileId: string, selected: boolean) => void;
    onSelectAll: () => void;
    onContextMenu: (e: React.MouseEvent, file: FileItem) => void;
}

export const FileList: React.FC<FileListProps> = ({
    files,
    selectedFiles,
    onFileClick,
    onFileSelect,
    onSelectAll,
    onContextMenu,
}) => {
    const handleFileClick = (file: FileItem, e: React.MouseEvent) => {
        if (e.ctrlKey || e.metaKey) {
            onFileSelect(file.id, !selectedFiles.has(file.id));
        } else {
            onFileClick(file);
        }
    };

    return (
        <div className="h-full overflow-auto">
            {/* Header */}
            <div className="sticky top-0 bg-white/90 backdrop-blur-sm border-b">
                <div className="flex items-center px-4 py-3 text-sm font-medium text-gray-700">
                    <div className="w-8 mr-4">
                        <input
                            type="checkbox"
                            checked={files.length > 0 && selectedFiles.size === files.length}
                            onChange={onSelectAll}
                            className="rounded"
                        />
                    </div>
                    <div className="flex-1 grid grid-cols-4 gap-4">
                        <div>Name</div>
                        <div>Size</div>
                        <div>Type</div>
                        <div>Modified</div>
                    </div>
                </div>
            </div>

            {/* File List */}
            <div className="divide-y divide-gray-200">
                {files.map((file) => (
                    <div
                        key={file.id}
                        className={`
                            flex items-center px-4 py-3 cursor-pointer transition-colors
                            hover:bg-gray-50
                            ${selectedFiles.has(file.id) ? 'bg-primary-50' : ''}
                        `}
                        onClick={(e) => handleFileClick(file, e)}
                        onContextMenu={(e) => onContextMenu(e, file)}
                    >
                        {/* Selection Checkbox */}
                        <div className="w-8 mr-4">
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

                        {/* File Details */}
                        <div className="flex-1 grid grid-cols-4 gap-4 items-center">
                            {/* Name with Icon */}
                            <div className="flex items-center gap-3 min-w-0">
                                <FileIcon
                                    file={file}
                                    size={20}
                                    className="flex-shrink-0"
                                />
                                <span className="truncate font-medium text-gray-900">
                                    {file.name}
                                </span>
                            </div>

                            {/* Size */}
                            <div className="text-sm text-gray-500">
                                {file.isDirectory ? '-' : formatFileSize(file.size)}
                            </div>

                            {/* Type */}
                            <div className="text-sm text-gray-500">
                                {file.isDirectory ? 'Folder' : (file.mimeType || 'File')}
                            </div>

                            {/* Modified Date */}
                            <div className="text-sm text-gray-500">
                                {formatDate(file.lastModified)}
                            </div>
                        </div>
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