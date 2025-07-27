import React, { useState, useEffect, useCallback } from 'react';
import { Grid, List, Upload, Plus } from 'lucide-react';
import { directoryApi } from '../../services/directoryApi';
import { fileApi } from '../../services/fileApi';
import { searchApi } from '../../services/searchApi';
import { FileGrid } from './FileGrid';
import { FileList } from './FileList';
import { FileUpload } from './FileUpload';
import { SearchBar } from './SearchBar';
import { BreadcrumbNav } from './BreadcrumbNav';
import { CreateFolderModal } from './CreateFolderModal';
import { FilePreview } from './FilePreview';
import { ContextMenu } from './ContextMenu';
import { useFileOperations } from '../../hooks/useFileOperations';
import type { FileItem } from './types';

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
        // Check authentication state once on mount
        const sessionId = localStorage.getItem('sessionId');
        if (!sessionId) {
            setError('Please log in to access your files');
            setLoading(false);
            return;
        }
        loadFiles();
    }, [currentPath]); // Only depend on currentPath, not session state

    const loadFiles = async () => {
        setLoading(true);
        setError(null);
        try {
            const response = currentPath
                ? await directoryApi.listDirectory(currentPath)
                : await directoryApi.listRoot();

            if (response.data.success) {
                console.log('Raw backend response:', response.data.data); // Debug log

                // Process the files/directories data to ensure proper type detection
                const processedFiles = (response.data.data || []).map((item: any) => {
                    console.log('Processing item:', item); // Debug each item

                    // Handle timestamp fields from backend - FileMetaDataDto uses 'uploadTime' and 'lastModified'
                    let lastModified = item.lastModified || item.uploadTime || item.modifiedDate || item.createdDate || item.created || item.timestamp;

                    // Don't use current time as fallback - let it be empty if not provided
                    if (!lastModified) {
                        console.warn(`No timestamp found for file: ${item.name}`, item);
                        lastModified = null;
                    }

                    // Check if this is a directory based on multiple criteria
                    const isDirectory = item.isDirectory === true ||
                                       item.directory === true ||
                                       item.type === 'directory' ||
                                       (item.size === 0 && !item.contentType && !item.mimeType && item.name && !item.name.includes('.'));

                    const processedItem = {
                        id: item.name || Math.random().toString(36), // Use name as ID since backend doesn't provide ID
                        name: item.name,
                        type: isDirectory ? 'directory' : 'file',
                        size: item.size || 0,
                        lastModified: lastModified,
                        path: item.path || `${currentPath ? currentPath + '/' : ''}${item.name}`,
                        mimeType: item.contentType || item.mimeType,
                        isDirectory: isDirectory
                    };

                    console.log('Processed item:', processedItem); // Debug processed item
                    return processedItem;
                });

                console.log('Final processed files with timestamps:', processedFiles); // Debug log
                setFiles(processedFiles);
            } else {
                setError(response.data.error || 'Failed to load files');
            }
        } catch (err: any) {
            console.error('Load files error:', err);
            if (err.response?.status === 401) {
                setError('Session expired. Please log in again.');
                // Clear invalid session and redirect
                localStorage.removeItem('sessionId');
                window.location.href = '/auth';
            } else if (err.code === 'ERR_NETWORK' || err.message?.includes('Network Error')) {
                setError('Backend server connection failed. Please check if the server is running and CORS is configured properly.');
            } else {
                setError(err.response?.data?.error || 'Failed to load files');
            }
        } finally {
            setLoading(false);
        }
    };

    // Memoize the search handler to prevent SearchBar from re-rendering constantly
    const handleSearch = useCallback(async (query: string) => {
        if (!query.trim()) {
            loadFiles();
            return;
        }

        setLoading(true);
        setError(null);
        try {
            const response = await searchApi.searchFiles({
                query,
                path: currentPath || undefined
            });
            if (response.data.success) {
                setFiles(response.data.data || []);
            } else {
                setError(response.data.error || 'Search failed');
            }
        } catch (err: any) {
            console.error('Search error:', err);
            setError(err.response?.data?.error || 'Search failed');
        } finally {
            setLoading(false);
        }
    }, [currentPath]); // Only depend on currentPath

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
            setContextMenu(null); // Close context menu immediately

            // Show loading indicator
            setError(null);

            const response = await fileApi.downloadFile(file.path);

            // The backend returns a Resource with APPLICATION_OCTET_STREAM
            // We need to handle this as a blob
            const blob = new Blob([response.data], {
                type: response.headers['content-type'] || 'application/octet-stream'
            });

            // Create download URL
            const url = window.URL.createObjectURL(blob);
            const link = document.createElement('a');
            link.href = url;

            // Use the filename from Content-Disposition header if available, otherwise use file.name
            const contentDisposition = response.headers['content-disposition'];
            let filename = file.name;
            if (contentDisposition) {
                const filenameMatch = contentDisposition.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/);
                if (filenameMatch) {
                    filename = filenameMatch[1].replace(/['"]/g, '');
                }
            }

            link.setAttribute('download', filename);

            // Trigger download
            document.body.appendChild(link);
            link.click();

            // Cleanup
            document.body.removeChild(link);
            window.URL.revokeObjectURL(url);

            console.log(`Successfully downloaded: ${filename}`);

        } catch (err: any) {
            console.error('Failed to download file:', err);

            let errorMessage = `Failed to download ${file.name}`;
            if (err.response?.status === 404) {
                errorMessage = `File not found: ${file.name}`;
            } else if (err.response?.status === 403) {
                errorMessage = `Access denied for file: ${file.name}`;
            } else if (err.response?.status === 401) {
                errorMessage = 'Authentication required. Please log in again.';
            } else if (err.response?.data?.error) {
                errorMessage = err.response.data.error;
            } else if (err.message) {
                errorMessage += `: ${err.message}`;
            }

            setError(errorMessage);
        }
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