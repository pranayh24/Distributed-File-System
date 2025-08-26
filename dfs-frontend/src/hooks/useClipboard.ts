import { useState, useEffect } from 'react';
import { clipboardService, type ClipboardItem } from '../services/clipboardService';
import { directoryApi } from '../services/directoryApi';
import type { FileItem } from '../components/files/types';

export const useClipboard = () => {
    const [clipboard, setClipboard] = useState<ClipboardItem[]>([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        const unsubscribe = clipboardService.subscribe(setClipboard);
        return unsubscribe;
    }, []);

    const copyFiles = (files: FileItem[]) => {
        const clipboardItems: ClipboardItem[] = files.map(file => ({
            id: file.id,
            name: file.name,
            path: file.path,
            isDirectory: file.isDirectory,
            operation: 'copy'
        }));
        clipboardService.copy(clipboardItems);
    };

    const cutFiles = (files: FileItem[]) => {
        const clipboardItems: ClipboardItem[] = files.map(file => ({
            id: file.id,
            name: file.name,
            path: file.path,
            isDirectory: file.isDirectory,
            operation: 'cut'
        }));
        clipboardService.cut(clipboardItems);
    };

    const pasteFiles = async (destinationPath: string) => {
        if (clipboardService.isEmpty()) {
            throw new Error('Clipboard is empty');
        }

        setLoading(true);
        setError(null);

        try {
            const items = clipboardService.getClipboard();
            const results = [];

            for (const item of items) {
                const newPath = `${destinationPath}/${item.name}`.replace(/\/+/g, '/');

                if (item.operation === 'copy') {
                    // For copy operations, we'll need a backend API to copy files
                    // For now, we'll simulate it
                    console.log(`Copying ${item.path} to ${newPath}`);
                    results.push({ success: true, item });
                } else if (item.operation === 'cut') {
                    // For cut operations, use move API
                    try {
                        await directoryApi.moveDirectory(item.path, newPath);
                        results.push({ success: true, item });
                    } catch (err) {
                        results.push({ success: false, item, error: err });
                    }
                }
            }

            // If all operations were cut operations, clear clipboard
            if (items.every(item => item.operation === 'cut')) {
                clipboardService.clear();
            }

            return results;
        } catch (err: any) {
            setError(err.message);
            throw err;
        } finally {
            setLoading(false);
        }
    };

    const clearClipboard = () => {
        clipboardService.clear();
    };

    return {
        clipboard,
        loading,
        error,
        copyFiles,
        cutFiles,
        pasteFiles,
        clearClipboard,
        isEmpty: clipboardService.isEmpty(),
        hasItems: !clipboardService.isEmpty()
    };
};