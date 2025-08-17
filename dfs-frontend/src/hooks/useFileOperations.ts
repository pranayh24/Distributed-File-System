import { useState } from 'react';
import { directoryApi } from '../services/directoryApi';
import { fileApi } from '../services/fileApi';
import toast from 'react-hot-toast';

export const useFileOperations = () => {
    const [loading, setLoading] = useState(false);

    const deleteFile = async (path: string, isDirectory: boolean = false) => {
        setLoading(true);
        try {
            if (isDirectory) {
                await directoryApi.deleteDirectory(path);
            } else {
                await fileApi.deleteFile(path);
            }
            toast.success('File deleted successfully');
            return true;
        } catch (err: any) {
            const errorMessage = err.response?.data?.error || 'Delete operation failed';
            toast.error(errorMessage);
            throw new Error(errorMessage);
        } finally {
            setLoading(false);
        }
    };

    const renameFile = async (oldPath: string, newPath: string, isDirectory: boolean = false) => {
        setLoading(true);
        try {
            await directoryApi.moveDirectory(oldPath, newPath);
            toast.success('Renamed successfully');
            return true;
        } catch (err: any) {
            const errorMessage = err.response?.data?.error || 'Rename operation failed';
            toast.error(errorMessage);
            throw new Error(errorMessage);
        } finally {
            setLoading(false);
        }
    };

    return {
        deleteFile,
        renameFile,
        loading
    };
};