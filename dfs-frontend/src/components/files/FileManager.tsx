import React, { useState, useEffect } from 'react';
import { Grid, List, Upload, Plus } from 'lucide-react';
import { directoryApi, fileApi, searchApi } from '../../services/api';
import { FileGrid } from './FileGrid';
import { FileList } from './FileList';
import { FileUpload } from './FileUpload';
import { SearchBar } from './SearchBar';
import { BreadcrumbNav } from './BreadcrumbNav';
import { CreateFolderModal } from './CreateFolderModal';
import { FilePreview } from './FilePreview';
import { ContextMenu } from './ContextMenu';
import { useFileOperations } from '../../hooks/useFileOperations';

export interface FileItem {
    id: string;
    name: string;
    type: 'file' | 'directory';
    size: number;
    lastModified: string;
    path: string;
    mimeType?: string;
    isDirectory: boolean;
}

export const FileManager: React.FC = () => {
    const [files, setFiles] = useState<FileItem[]>([]);
    const [currentPath, setCurrentPath] = useState('');
    const [viewMode, setViewMode] = useState<'grid' | 'list'>('grid');
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [showUpload, setShowUpload] = useState(false);
    const [showCreateFolder, setShowCreateFolder] = useState(false);
    const [selectedFiles, setSelectedFiles] = useState<Set<string>>(new Set());
    const [previewFile, setPreviewFile] = useState<FileItem | null>(null);
    const [contextMenu, setContextMenu] = useState<{
        x: number;
        y: number;
        file: FileItem;
    } | null>(null);

    const {
        renameFile,
        loading: operationLoading
    } = useFileOperations();

    useEffect(() => {
        loadFiles();
    }, [currentPath]);

    const loadFiles = async () => {
        setLoading(true);
        setError(null);
        try {
            const response = currentPath
                ? await directoryApi.listDirectory(currentPath)
                : await directoryApi.listRoot();

            if (response.data.success) {
                setFiles(response.data.data || []);
            } else {
                setError(response.data.error || 'Failed to load files');
            }
        } catch (err: any) {
            setError(err.response?.data?.error || 'Failed to load files');
        } finally {
            setLoading(false);
        }
    };

    const handleSearch = async (query: string) => {
        if (!query.trim()) {
            loadFiles();
            return;
        }

        setLoading(true);
        try {
            const response = await searchApi.searchFiles({
                query,
                path: currentPath || undefined
            });
            if (response.data.success) {
                setFiles(response.data.data || []);
            }
        } catch (err: any) {
            setError(err.response?.data?.error || 'Search failed');
        } finally {
            setLoading(false);
        }
    };

    const handleFileClick = (file: FileItem) => {
        if (file.isDirectory) {
            const newPath = currentPath ? `${currentPath}/${file.name}` : file.name;
            setCurrentPath(newPath);
            setSelectedFiles(new Set());
        } else {
            setPreviewFile(file);
        }
    };

    const handleFileSelect = (fileId: string, selected: boolean) => {
        const newSelected = new Set(selectedFiles);
        if (selected) {
            newSelected.add(fileId);
        } else {
            newSelected.delete(fileId);
        }
        setSelectedFiles(newSelected);
    };

    const handleSelectAll = () => {
        if (selectedFiles.size === files.length) {
            setSelectedFiles(new Set());
        } else {
            setSelectedFiles(new Set(files.map(f => f.id)));
        }
    };

    const handleContextMenu = (e: React.MouseEvent, file: FileItem) => {
        e.preventDefault();
        setContextMenu({
            x: e.clientX,
            y: e.clientY,
            file
        });
    };

    const handleUploadComplete = () => {
        setShowUpload(false);
        loadFiles();
    };

    const handleCreateFolder = () => {
        setShowCreateFolder(false);
        loadFiles();
    };

    const handleDelete = async (fileIds: string[]) => {
        const filesToDelete = files.filter(f => fileIds.includes(f.id));

        for (const file of filesToDelete) {
            try {
                if (file.isDirectory) {
                    await directoryApi.deleteDirectory(file.path);
                } else {
                    await fileApi.deleteFile(file.path);
                }
            } catch (err) {
                console.error(`Failed to delete ${file.name}:`, err);
            }
        }

        setSelectedFiles(new Set());
        loadFiles();
    };

    const handleRename = async (file: FileItem, newName: string) => {
        try {
            const newPath = file.path.replace(file.name, newName);
            await renameFile(file.path, newPath, file.isDirectory);
            loadFiles();
        } catch (err) {
            console.error('Failed to rename file:', err);
        }
        setContextMenu(null);
    };

    const handleDownload = async (file: FileItem) => {
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
        setContextMenu(null);
    };

    const breadcrumbPath = currentPath.split('/').filter(Boolean);

    return (
        <div className="h-full flex flex-col">
            {/* Header */}
            <div className="flex items-center justify-between mb-6">
                <h1 className="text-2xl font-bold">My Files</h1>
                <div className="flex items-center gap-2">
                    <button
                        onClick={() => setViewMode('grid')}
                        className={`p-2 rounded-lg ${viewMode === 'grid' ? 'bg-primary-100 text-primary-600' : 'text-gray-500 hover:bg-gray-100'}`}
                    >
                        <Grid size={20} />
                    </button>
                    <button
                        onClick={() => setViewMode('list')}
                        className={`p-2 rounded-lg ${viewMode === 'list' ? 'bg-primary-100 text-primary-600' : 'text-gray-500 hover:bg-gray-100'}`}
                    >
                        <List size={20} />
                    </button>
                </div>
            </div>

            {/* Toolbar */}
            <div className="flex items-center gap-4 mb-6">
                <SearchBar onSearch={handleSearch} placeholder="Search files..." />
                <button
                    onClick={() => setShowUpload(true)}
                    className="btn-primary flex items-center gap-2"
                >
                    <Upload size={16} />
                    Upload
                </button>
                <button
                    onClick={() => setShowCreateFolder(true)}
                    className="btn-secondary flex items-center gap-2"
                >
                    <Plus size={16} />
                    New Folder
                </button>
                {selectedFiles.size > 0 && (
                    <button
                        onClick={() => handleDelete(Array.from(selectedFiles))}
                        className="btn-danger flex items-center gap-2"
                        disabled={operationLoading}
                    >
                        Delete ({selectedFiles.size})
                    </button>
                )}
            </div>

            {/* Breadcrumb */}
            <BreadcrumbNav
                path={breadcrumbPath}
                onNavigate={setCurrentPath}
            />

            {/* File Display */}
            <div className="flex-1 min-h-0">
                {loading ? (
                    <div className="flex items-center justify-center h-64">
                        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
                    </div>
                ) : error ? (
                    <div className="flex items-center justify-center h-64 text-red-500">
                        {error}
                    </div>
                ) : viewMode === 'grid' ? (
                    <FileGrid
                        files={files}
                        selectedFiles={selectedFiles}
                        onFileClick={handleFileClick}
                        onFileSelect={handleFileSelect}
                        onSelectAll={handleSelectAll}
                        onContextMenu={handleContextMenu}
                    />
                ) : (
                    <FileList
                        files={files}
                        selectedFiles={selectedFiles}
                        onFileClick={handleFileClick}
                        onFileSelect={handleFileSelect}
                        onSelectAll={handleSelectAll}
                        onContextMenu={handleContextMenu}
                    />
                )}
            </div>

            {/* Modals */}
            {showUpload && (
                <FileUpload
                    currentPath={currentPath}
                    onClose={() => setShowUpload(false)}
                    onUploadComplete={handleUploadComplete}
                />
            )}

            {showCreateFolder && (
                <CreateFolderModal
                    currentPath={currentPath}
                    onClose={() => setShowCreateFolder(false)}
                    onSuccess={handleCreateFolder}
                />
            )}

            {previewFile && (
                <FilePreview
                    file={previewFile}
                    onClose={() => setPreviewFile(null)}
                />
            )}

            {contextMenu && (
                <ContextMenu
                    x={contextMenu.x}
                    y={contextMenu.y}
                    file={contextMenu.file}
                    onClose={() => setContextMenu(null)}
                    onRename={handleRename}
                    onDelete={(file: FileItem) => handleDelete([file.id])}
                    onDownload={handleDownload}
                />
            )}
        </div>
    );
};