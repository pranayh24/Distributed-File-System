import { useState } from 'react';
import { bulkOperationsService, type BulkOperationResult, type BulkOperationProgress } from '../services/bulkOperationService';
import type { FileItem } from '../components/files/types';

export const useBulkOperations = () => {
    const [loading, setLoading] = useState(false);
    const [progress, setProgress] = useState<BulkOperationProgress | null>(null);
    const [results, setResults] = useState<BulkOperationResult[]>([]);

    const deleteMultiple = async (files: FileItem[]): Promise<BulkOperationResult[]> => {
        setLoading(true);
        setProgress(null);
        setResults([]);

        try {
            const results = await bulkOperationsService.deleteMultiple(files, setProgress);
            setResults(results);
            return results;
        } finally {
            setLoading(false);
            setProgress(null);
        }
    };

    const downloadMultiple = async (files: FileItem[]): Promise<void> => {
        const fileItems = files.filter(f => !f.isDirectory);

        if (fileItems.length === 0) {
            throw new Error('No files selected for download');
        }

        setLoading(true);
        setProgress(null);

        try {
            await bulkOperationsService.downloadMultiple(fileItems, setProgress);
        } finally {
            setLoading(false);
            setProgress(null);
        }
    };

    const getSelectionSummary = async (files: FileItem[]) => {
        const totalSize = await bulkOperationsService.calculateTotalSize(files);
        const groups = bulkOperationsService.groupFilesByType(files);

        return {
            totalCount: files.length,
            totalSize,
            groups,
            directories: groups.directories?.length || 0,
            files: files.length - (groups.directories?.length || 0)
        };
    };

    return {
        loading,
        progress,
        results,
        deleteMultiple,
        downloadMultiple,
        getSelectionSummary
    };
};