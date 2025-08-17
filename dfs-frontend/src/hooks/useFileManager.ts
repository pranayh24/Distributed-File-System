import { useState, useEffect, useCallback } from 'react';
import { directoryApi } from '../services/directoryApi';
import type { FileItem } from '../components/files/types';

export const useFileManager = (initialPath = '') => {
    const [files, setFiles] = useState<FileItem[]>([]);
    const [currentPath, setCurrentPath] = useState(initialPath);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    const loadFiles = useCallback(async (path: string) => {
        setLoading(true);
        setError(null);
        try {
            const response = path
                ? await directoryApi.listDirectory(path)
                : await directoryApi.listRoot();

            if (response.data.success) {
                const processedFiles = (response.data.data || []).map((item: any) => ({
                    id: item.name,
                    name: item.name,
                    type: item.isDirectory ? 'directory' : 'file',
                    size: item.size || 0,
                    lastModified: item.lastModified || new Date().toISOString(),
                    path: item.path,
                    mimeType: item.contentType,
                    isDirectory: item.isDirectory
                }));
                setFiles(processedFiles);
            } else {
                setError(response.data.error || 'Failed to load files');
            }
        } catch (err: any) {
            setError(err.response?.data?.error || 'Failed to load files');
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        loadFiles(currentPath);
    }, [currentPath, loadFiles]);

    return { files, currentPath, setCurrentPath, loading, error, reloadFiles: () => loadFiles(currentPath) };
};