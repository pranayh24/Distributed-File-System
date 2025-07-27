import { useState } from 'react';
import { fileApi, directoryApi } from '../services/api';

export const useFileOperations = () => {
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const deleteFile = async (path: string, isDirectory: boolean = false) => {
        setLoading(true);
        setError(null);

        try {
            if (isDirectory) {
                await directoryApi.deleteDirectory(path);
            } else {
                await fileApi.deleteFile(path);
            }
            return true;
        } catch (err: any) {
            const errorMessage = err.response?.data?.error || 'Delete operation failed';
            setError(errorMessage);
            throw new Error(errorMessage);
        } finally {
            setLoading(false);
        }
    };

    const renameFile = async (oldPath: string, newPath: string, isDirectory: boolean = false) => {
        setLoading(true);
        setError(null);

        try {
            if (isDirectory) {
                await directoryApi.moveDirectory(oldPath, newPath);
            } else {
                // For files, we'll use the move functionality to rename
                await directoryApi.moveDirectory(oldPath, newPath);
            }
            return true;
        } catch (err: any) {
            const errorMessage = err.response?.data?.error || 'Rename operation failed';
            setError(errorMessage);
            throw new Error(errorMessage);
        } finally {
            setLoading(false);
        }
    };

    const moveFile = async (sourcePath: string, destinationPath: string, isDirectory: boolean = false) => {
        setLoading(true);
        setError(null);

        try {
            if (isDirectory) {
                await directoryApi.moveDirectory(sourcePath, destinationPath);
            } else {
                // Implement file move functionality
                await directoryApi.moveDirectory(sourcePath, destinationPath);
            }
            return true;
        } catch (err: any) {
            const errorMessage = err.response?.data?.error || 'Move operation failed';
            setError(errorMessage);
            throw new Error(errorMessage);
        } finally {
            setLoading(false);
        }
    };

    const copyFile = async (sourcePath: string, destinationPath: string) => {
        setLoading(true);
        setError(null);

        try {
            // This would need to be implemented on the backend
            const response = await fetch('/api/files/copy', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ sourcePath, destinationPath })
            });

            if (!response.ok) {
                throw new Error('Copy failed');
            }
            return true;
        } catch (err: any) {
            const errorMessage = err.message || 'Copy operation failed';
            setError(errorMessage);
            throw new Error(errorMessage);
        } finally {
            setLoading(false);
        }
    };

    return {
        deleteFile,
        renameFile,
        moveFile,
        copyFile,
        loading,
        error
    };
};