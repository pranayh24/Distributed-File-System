import React from 'react';
import {
    Folder,
    File,
    FileText,
    FileImage,
    FileVideo,
    FileAudio,
    Archive,
    Code,
    FileSpreadsheet,
    FileBarChart
} from 'lucide-react';
import type { FileItem } from './types';

interface FileIconProps {
    file: FileItem;
    size?: number;
    className?: string;
}

export const FileIcon: React.FC<FileIconProps> = ({
    file,
    size = 24,
    className = ''
}) => {
    const getFileIcon = () => {
        if (file.isDirectory) {
            return <Folder size={size} className={`text-blue-500 ${className}`} />;
        }

        const extension = file.name.split('.').pop()?.toLowerCase();
        const mimeType = file.mimeType?.toLowerCase();

        // Document files
        if (extension && ['txt', 'md', 'rtf'].includes(extension)) {
            return <FileText size={size} className={`text-gray-600 ${className}`} />;
        }

        // Code files
        if (extension && ['js', 'ts', 'jsx', 'tsx', 'html', 'css', 'py', 'java', 'cpp', 'c', 'php', 'rb', 'go', 'rs'].includes(extension)) {
            return <Code size={size} className={`text-green-600 ${className}`} />;
        }

        // Image files
        if (mimeType?.startsWith('image/') || (extension && ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'svg', 'webp'].includes(extension))) {
            return <FileImage size={size} className={`text-purple-600 ${className}`} />;
        }

        // Video files
        if (mimeType?.startsWith('video/') || (extension && ['mp4', 'avi', 'mov', 'wmv', 'flv', 'webm', 'mkv'].includes(extension))) {
            return <FileVideo size={size} className={`text-red-600 ${className}`} />;
        }

        // Audio files
        if (mimeType?.startsWith('audio/') || (extension && ['mp3', 'wav', 'flac', 'aac', 'ogg', 'wma'].includes(extension))) {
            return <FileAudio size={size} className={`text-orange-600 ${className}`} />;
        }

        // Archive files
        if (extension && ['zip', 'rar', '7z', 'tar', 'gz', 'bz2', 'xz'].includes(extension)) {
            return <Archive size={size} className={`text-yellow-600 ${className}`} />;
        }

        // Spreadsheet files
        if (extension && ['xlsx', 'xls', 'csv', 'ods'].includes(extension)) {
            return <FileSpreadsheet size={size} className={`text-green-700 ${className}`} />;
        }

        // PDF and document files
        if (extension && ['pdf', 'doc', 'docx', 'odt', 'ppt', 'pptx'].includes(extension)) {
            return <FileBarChart size={size} className={`text-red-700 ${className}`} />;
        }

        // Default file icon
        return <File size={size} className={`text-gray-500 ${className}`} />;
    };

    return getFileIcon();
};