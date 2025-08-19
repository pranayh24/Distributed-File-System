import React from 'react';
import {
    Folder,
    FolderOpen,
    File,
    FileText,
    FileImage,
    FileVideo,
    FileAudio,
    Archive,
    Code,
    FileSpreadsheet,
    FileBarChart,
    Database,
    Zap
} from 'lucide-react';
import type { FileItem } from './types';

interface EnhancedFileIconProps {
    file: FileItem;
    size?: number;
    className?: string;
    variant?: 'default' | 'colored' | 'outlined';
    isOpen?: boolean;
}

export const EnhancedFileIcon: React.FC<EnhancedFileIconProps> = ({
                                                                      file,
                                                                      size = 24,
                                                                      className = '',
                                                                      variant = 'colored',
                                                                      isOpen = false
                                                                  }) => {
    const getFileIcon = () => {
        if (file.isDirectory) {
            const FolderIcon = isOpen ? FolderOpen : Folder;
            return (
                <div className={`relative ${getIconColor('folder')}`}>
                    <FolderIcon size={size} className={className} />
                    {variant === 'colored' && (
                        <div className="absolute inset-0 bg-gradient-to-br from-blue-400 to-blue-600 opacity-20 rounded" />
                    )}
                </div>
            );
        }

        const extension = file.name.split('.').pop()?.toLowerCase();
        const mimeType = file.mimeType?.toLowerCase();

        // Document files
        if (extension && ['txt', 'md', 'rtf'].includes(extension)) {
            return <FileText size={size} className={`${getIconColor('text')} ${className}`} />;
        }

        // Code files
        if (extension && ['js', 'ts', 'jsx', 'tsx', 'html', 'css', 'py', 'java', 'cpp', 'c', 'php', 'rb', 'go', 'rs'].includes(extension)) {
            return (
                <div className="relative">
                    <Code size={size} className={`${getIconColor('code')} ${className}`} />
                    {variant === 'colored' && (
                        <div className="absolute top-0 right-0 w-2 h-2 bg-green-500 rounded-full" />
                    )}
                </div>
            );
        }

        // Image files
        if (mimeType?.startsWith('image/') || (extension && ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'svg', 'webp'].includes(extension))) {
            return (
                <div className="relative">
                    <FileImage size={size} className={`${getIconColor('image')} ${className}`} />
                    {variant === 'colored' && (
                        <div className="absolute inset-0 bg-gradient-to-br from-purple-400 to-pink-500 opacity-10 rounded" />
                    )}
                </div>
            );
        }

        // Video files
        if (mimeType?.startsWith('video/') || (extension && ['mp4', 'avi', 'mov', 'wmv', 'flv', 'webm', 'mkv'].includes(extension))) {
            return (
                <div className="relative">
                    <FileVideo size={size} className={`${getIconColor('video')} ${className}`} />
                    {variant === 'colored' && (
                        <div className="absolute bottom-0 left-0 w-3 h-1 bg-red-500 rounded-full" />
                    )}
                </div>
            );
        }

        // Audio files
        if (mimeType?.startsWith('audio/') || (extension && ['mp3', 'wav', 'flac', 'aac', 'ogg', 'wma'].includes(extension))) {
            return (
                <div className="relative">
                    <FileAudio size={size} className={`${getIconColor('audio')} ${className}`} />
                    {variant === 'colored' && (
                        <div className="absolute top-1 right-1 flex space-x-0.5">
                            <div className="w-0.5 h-2 bg-orange-500 rounded-full" />
                            <div className="w-0.5 h-3 bg-orange-500 rounded-full" />
                            <div className="w-0.5 h-1 bg-orange-500 rounded-full" />
                        </div>
                    )}
                </div>
            );
        }

        // Archive files
        if (extension && ['zip', 'rar', '7z', 'tar', 'gz', 'bz2', 'xz'].includes(extension)) {
            return (
                <div className="relative">
                    <Archive size={size} className={`${getIconColor('archive')} ${className}`} />
                    {variant === 'colored' && (
                        <Zap size={size / 3} className="absolute bottom-0 right-0 text-yellow-500" />
                    )}
                </div>
            );
        }

        // Spreadsheet files
        if (extension && ['xlsx', 'xls', 'csv', 'ods'].includes(extension)) {
            return (
                <div className="relative">
                    <FileSpreadsheet size={size} className={`${getIconColor('spreadsheet')} ${className}`} />
                    {variant === 'colored' && (
                        <div className="absolute inset-0 grid grid-cols-2 gap-0.5 p-1 opacity-30">
                            <div className="bg-green-600 rounded-sm" />
                            <div className="bg-green-400 rounded-sm" />
                            <div className="bg-green-400 rounded-sm" />
                            <div className="bg-green-600 rounded-sm" />
                        </div>
                    )}
                </div>
            );
        }

        // Database files
        if (extension && ['db', 'sql', 'sqlite', 'mdb'].includes(extension)) {
            return <Database size={size} className={`${getIconColor('database')} ${className}`} />;
        }

        // PDF and document files
        if (extension && ['pdf', 'doc', 'docx', 'odt', 'ppt', 'pptx'].includes(extension)) {
            return (
                <div className="relative">
                    <FileBarChart size={size} className={`${getIconColor('document')} ${className}`} />
                    {extension === 'pdf' && variant === 'colored' && (
                        <div className="absolute bottom-0 right-0 text-xs font-bold text-red-600">
                            PDF
                        </div>
                    )}
                </div>
            );
        }

        // Default file icon
        return <File size={size} className={`${getIconColor('default')} ${className}`} />;
    };

    const getIconColor = (type: string) => {
        if (variant === 'outlined') {
            return 'text-gray-600 dark:text-gray-300 stroke-2';
        }

        const colors = {
            folder: 'text-blue-500',
            text: 'text-gray-600',
            code: 'text-green-600',
            image: 'text-purple-600',
            video: 'text-red-600',
            audio: 'text-orange-600',
            archive: 'text-yellow-600',
            spreadsheet: 'text-green-700',
            document: 'text-red-700',
            database: 'text-indigo-600',
            default: 'text-gray-500'
        };

        return colors[type as keyof typeof colors] || colors.default;
    };

    return getFileIcon();
};