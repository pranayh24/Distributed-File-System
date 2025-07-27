import React, { useState, useEffect } from 'react';
import { X, Download } from 'lucide-react';
import type { FileItem } from './types';
import { FileIcon } from './FileIcon';
import { fileApi } from '../../services/fileApi';
import { formatFileSize, formatDate } from '../../utils/formatters';

interface FilePreviewProps {
    file: FileItem;
    onClose: () => void;
}

export const FilePreview: React.FC<FilePreviewProps> = ({ file, onClose }) => {
    const [previewUrl, setPreviewUrl] = useState<string | null>(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        const loadPreview = async () => {
            // Since files are encrypted, disable preview functionality
            // to prevent unwanted downloads with random UUID names
            if (canPreview(file)) {
                setError('Preview not available - files are encrypted');
                return;
            }
        };

        loadPreview();

        return () => {
            if (previewUrl) {
                URL.revokeObjectURL(previewUrl);
            }
        };
    }, [file]);

    const canPreview = (file: FileItem) => {
        // Disable preview for encrypted files to prevent unwanted downloads
        return false;

        // Original logic kept for reference:
        // const extension = file.name.split('.').pop()?.toLowerCase();
        // const previewableExtensions = ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'svg', 'webp', 'pdf', 'txt', 'md'];
        // return previewableExtensions.includes(extension || '');
    };

    const isImage = (file: FileItem) => {
        const extension = file.name.split('.').pop()?.toLowerCase();
        return ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'svg', 'webp'].includes(extension || '');
    };

    const isText = (file: FileItem) => {
        const extension = file.name.split('.').pop()?.toLowerCase();
        return ['txt', 'md', 'json', 'xml', 'csv'].includes(extension || '');
    };

    const isPdf = (file: FileItem) => {
        const extension = file.name.split('.').pop()?.toLowerCase();
        return extension === 'pdf';
    };

    const handleDownload = async () => {
        try {
            const response = await fileApi.downloadFile(file.path);
            const url = window.URL.createObjectURL(new Blob([response.data]));
            const link = document.createElement('a');
            link.href = url;
            link.setAttribute('download', file.name);
            document.body.appendChild(link);
            link.click();
            link.remove();
            window.URL.revokeObjectURL(url);
        } catch (err) {
            console.error('Failed to download file:', err);
        }
    };

    const renderPreview = () => {
        if (loading) {
            return (
                <div className="flex items-center justify-center h-64">
                    <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
                </div>
            );
        }

        if (error) {
            return (
                <div className="flex flex-col items-center justify-center h-64 text-gray-500">
                    <FileIcon file={file} size={64} />
                    <p className="mt-4 text-sm">{error}</p>
                </div>
            );
        }

        if (!canPreview(file)) {
            return (
                <div className="flex flex-col items-center justify-center h-64 text-gray-500">
                    <FileIcon file={file} size={64} />
                    <p className="mt-4 text-sm">Preview not available for this file type</p>
                </div>
            );
        }

        if (isImage(file) && previewUrl) {
            return (
                <div className="flex items-center justify-center max-h-96 overflow-hidden">
                    <img
                        src={previewUrl}
                        alt={file.name}
                        className="max-w-full max-h-full object-contain"
                    />
                </div>
            );
        }

        if (isPdf(file) && previewUrl) {
            return (
                <iframe
                    src={previewUrl}
                    className="w-full h-96 border rounded"
                    title={file.name}
                />
            );
        }

        if (isText(file) && previewUrl) {
            return (
                <div className="max-h-96 overflow-auto">
                    <iframe
                        src={previewUrl}
                        className="w-full h-96 border rounded"
                        title={file.name}
                    />
                </div>
            );
        }

        return (
            <div className="flex flex-col items-center justify-center h-64 text-gray-500">
                <FileIcon file={file} size={64} />
                <p className="mt-4 text-sm">Preview loading...</p>
            </div>
        );
    };

    return (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
            <div className="bg-white rounded-lg shadow-xl w-full max-w-4xl mx-4 max-h-[90vh] flex flex-col">
                {/* Header */}
                <div className="flex items-center justify-between p-6 border-b">
                    <div className="flex items-center gap-3">
                        <FileIcon file={file} size={24} />
                        <div>
                            <h2 className="text-xl font-semibold truncate">{file.name}</h2>
                            <p className="text-sm text-gray-500">
                                {formatFileSize(file.size)} • {formatDate(file.lastModified)}
                            </p>
                        </div>
                    </div>
                    <div className="flex items-center gap-2">
                        <button
                            onClick={handleDownload}
                            className="p-2 hover:bg-gray-100 rounded-full transition-colors"
                            title="Download"
                        >
                            <Download size={20} />
                        </button>
                        <button
                            onClick={onClose}
                            className="p-2 hover:bg-gray-100 rounded-full transition-colors"
                        >
                            <X size={20} />
                        </button>
                    </div>
                </div>

                {/* Preview Content */}
                <div className="flex-1 p-6 min-h-0 overflow-auto">
                    {renderPreview()}
                </div>

                {/* File Info */}
                <div className="border-t p-6 bg-gray-50">
                    <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
                        <div>
                            <span className="font-medium text-gray-700">Size:</span>
                            <p className="text-gray-600">{formatFileSize(file.size)}</p>
                        </div>
                        <div>
                            <span className="font-medium text-gray-700">Type:</span>
                            <p className="text-gray-600">{file.mimeType || 'Unknown'}</p>
                        </div>
                        <div>
                            <span className="font-medium text-gray-700">Modified:</span>
                            <p className="text-gray-600">{formatDate(file.lastModified)}</p>
                        </div>
                        <div>
                            <span className="font-medium text-gray-700">Path:</span>
                            <p className="text-gray-600 truncate">{file.path}</p>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};