import React, { useState, useRef, useCallback } from 'react';
import { Upload, X, File, CheckCircle, AlertCircle } from 'lucide-react';
import { fileApi } from '../../services/fileApi';

interface FileUploadProps {
    currentPath: string;
    onClose: () => void;
    onUploadComplete: () => void;
}

interface UploadFile {
    id: string;
    file: File;
    progress: number;
    status: 'pending' | 'uploading' | 'success' | 'error';
    error?: string;
}

export const FileUpload: React.FC<FileUploadProps> = ({
    currentPath,
    onClose,
    onUploadComplete,
}) => {
    const [uploadFiles, setUploadFiles] = useState<UploadFile[]>([]);
    const [isDragOver, setIsDragOver] = useState(false);
    const fileInputRef = useRef<HTMLInputElement>(null);

    const addFiles = useCallback((files: FileList) => {
        const newFiles: UploadFile[] = Array.from(files).map((file) => ({
            id: Math.random().toString(36).substr(2, 9),
            file,
            progress: 0,
            status: 'pending'
        }));
        setUploadFiles(prev => [...prev, ...newFiles]);
    }, []);

    const uploadFile = async (uploadFile: UploadFile) => {
        setUploadFiles(prev => prev.map(f =>
            f.id === uploadFile.id ? { ...f, status: 'uploading' } : f
        ));

        try {
            const formData = new FormData();
            formData.append('file', uploadFile.file);
            if (currentPath) {
                formData.append('path', currentPath);
            }

            await fileApi.uploadFile(formData);

            setUploadFiles(prev => prev.map(f =>
                f.id === uploadFile.id ? { ...f, status: 'success', progress: 100 } : f
            ));
        } catch (error: any) {
            setUploadFiles(prev => prev.map(f =>
                f.id === uploadFile.id ? {
                    ...f,
                    status: 'error',
                    error: error.response?.data?.error || 'Upload failed'
                } : f
            ));
        }
    };

    const handleUploadAll = async () => {
        const pendingFiles = uploadFiles.filter(f => f.status === 'pending');

        for (const file of pendingFiles) {
            await uploadFile(file);
        }

        // Check if all uploads are complete
        const allComplete = uploadFiles.every(f => f.status === 'success' || f.status === 'error');
        if (allComplete) {
            setTimeout(() => {
                onUploadComplete();
            }, 1000);
        }
    };

    const removeFile = (id: string) => {
        setUploadFiles(prev => prev.filter(f => f.id !== id));
    };

    const handleDragOver = (e: React.DragEvent) => {
        e.preventDefault();
        setIsDragOver(true);
    };

    const handleDragLeave = (e: React.DragEvent) => {
        e.preventDefault();
        setIsDragOver(false);
    };

    const handleDrop = (e: React.DragEvent) => {
        e.preventDefault();
        setIsDragOver(false);
        const files = e.dataTransfer.files;
        if (files.length > 0) {
            addFiles(files);
        }
    };

    const handleFileSelect = () => {
        fileInputRef.current?.click();
    };

    const formatFileSize = (bytes: number) => {
        if (bytes === 0) return '0 Bytes';
        const k = 1024;
        const sizes = ['Bytes', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    };

    return (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
            <div className="bg-white rounded-lg shadow-xl w-full max-w-2xl mx-4 max-h-[80vh] flex flex-col">
                {/* Header */}
                <div className="flex items-center justify-between p-6 border-b">
                    <h2 className="text-xl font-semibold">Upload Files</h2>
                    <button
                        onClick={onClose}
                        className="p-2 hover:bg-gray-100 rounded-full transition-colors"
                    >
                        <X size={20} />
                    </button>
                </div>

                {/* Upload Area */}
                <div className="p-6">
                    <div
                        className={`
                            border-2 border-dashed rounded-lg p-8 text-center transition-colors
                            ${isDragOver ? 'border-primary-400 bg-primary-50' : 'border-gray-300'}
                        `}
                        onDragOver={handleDragOver}
                        onDragLeave={handleDragLeave}
                        onDrop={handleDrop}
                    >
                        <Upload size={48} className="mx-auto mb-4 text-gray-400" />
                        <p className="text-lg font-medium mb-2">
                            Drop files here or click to browse
                        </p>
                        <p className="text-gray-500 mb-4">
                            Upload to: {currentPath || 'Root'}
                        </p>
                        <button
                            onClick={handleFileSelect}
                            className="btn-primary"
                        >
                            Select Files
                        </button>
                        <input
                            ref={fileInputRef}
                            type="file"
                            multiple
                            onChange={(e) => e.target.files && addFiles(e.target.files)}
                            className="hidden"
                        />
                    </div>
                </div>

                {/* File List */}
                {uploadFiles.length > 0 && (
                    <div className="flex-1 min-h-0 px-6">
                        <div className="max-h-64 overflow-y-auto border rounded-lg">
                            {uploadFiles.map((uploadFile) => (
                                <div key={uploadFile.id} className="flex items-center gap-3 p-3 border-b last:border-b-0">
                                    <File size={20} className="text-gray-400" />
                                    <div className="flex-1 min-w-0">
                                        <p className="text-sm font-medium truncate">
                                            {uploadFile.file.name}
                                        </p>
                                        <p className="text-xs text-gray-500">
                                            {formatFileSize(uploadFile.file.size)}
                                        </p>
                                        {uploadFile.error && (
                                            <p className="text-xs text-red-500 mt-1">
                                                {uploadFile.error}
                                            </p>
                                        )}
                                    </div>
                                    <div className="flex items-center gap-2">
                                        {uploadFile.status === 'success' && (
                                            <CheckCircle size={16} className="text-green-500" />
                                        )}
                                        {uploadFile.status === 'error' && (
                                            <AlertCircle size={16} className="text-red-500" />
                                        )}
                                        {uploadFile.status === 'uploading' && (
                                            <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-primary-600"></div>
                                        )}
                                        <button
                                            onClick={() => removeFile(uploadFile.id)}
                                            className="p-1 hover:bg-gray-100 rounded"
                                            disabled={uploadFile.status === 'uploading'}
                                        >
                                            <X size={16} />
                                        </button>
                                    </div>
                                </div>
                            ))}
                        </div>
                    </div>
                )}

                {/* Actions */}
                <div className="flex items-center justify-end gap-3 p-6 border-t">
                    <button
                        onClick={onClose}
                        className="btn-secondary"
                    >
                        Cancel
                    </button>
                    {uploadFiles.length > 0 && (
                        <button
                            onClick={handleUploadAll}
                            className="btn-primary"
                            disabled={uploadFiles.every(f => f.status !== 'pending')}
                        >
                            Upload {uploadFiles.filter(f => f.status === 'pending').length} Files
                        </button>
                    )}
                </div>
            </div>
        </div>
    );
};