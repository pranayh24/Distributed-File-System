import { fileApi } from './fileApi';
import { directoryApi } from './directoryApi';
import type { FileItem } from '../components/files/types';

export interface BulkOperationResult {
    success: boolean;
    item: FileItem;
    error?: string;
}

export interface BulkOperationProgress {
    total: number;
    completed: number;
    failed: number;
    current?: string;
}

class BulkOperationsService {
    async deleteMultiple(
        files: FileItem[],
        onProgress?: (progress: BulkOperationProgress) => void
    ): Promise<BulkOperationResult[]> {
        const results: BulkOperationResult[] = [];
        let completed = 0;
        let failed = 0;

        for (const file of files) {
            try {
                onProgress?.({
                    total: files.length,
                    completed,
                    failed,
                    current: file.name
                });

                if (file.isDirectory) {
                    await directoryApi.deleteDirectory(file.path);
                } else {
                    await fileApi.deleteFile(file.path);
                }

                results.push({ success: true, item: file });
                completed++;
            } catch (error: any) {
                results.push({
                    success: false,
                    item: file,
                    error: error.response?.data?.error || error.message
                });
                failed++;
            }

            onProgress?.({
                total: files.length,
                completed,
                failed
            });
        }

        return results;
    }

    async downloadMultiple(
        files: FileItem[],
        onProgress?: (progress: BulkOperationProgress) => void
    ): Promise<void> {
        const fileItems = files.filter(f => !f.isDirectory);
        let completed = 0;

        for (const file of fileItems) {
            try {
                onProgress?.({
                    total: fileItems.length,
                    completed,
                    failed: 0,
                    current: file.name
                });

                const response = await fileApi.downloadFile(file.path);

                // Create download
                const blob = new Blob([response.data], {
                    type: response.headers['content-type'] || 'application/octet-stream'
                });

                const url = window.URL.createObjectURL(blob);
                const link = document.createElement('a');
                link.href = url;
                link.download = file.name;
                link.style.display = 'none';

                document.body.appendChild(link);
                link.click();
                document.body.removeChild(link);

                window.URL.revokeObjectURL(url);
                completed++;

                // Small delay to prevent overwhelming the browser
                await new Promise(resolve => setTimeout(resolve, 100));

            } catch (error) {
                console.error(`Failed to download ${file.name}:`, error);
            }

            onProgress?.({
                total: fileItems.length,
                completed,
                failed: 0
            });
        }
    }

    async calculateTotalSize(files: FileItem[]): Promise<number> {
        return files.reduce((total, file) => total + (file.size || 0), 0);
    }

    groupFilesByType(files: FileItem[]): Record<string, FileItem[]> {
        const groups: Record<string, FileItem[]> = {
            directories: [],
            documents: [],
            images: [],
            videos: [],
            audio: [],
            archives: [],
            others: []
        };

        files.forEach(file => {
            if (file.isDirectory) {
                groups.directories.push(file);
            } else {
                const extension = file.name.split('.').pop()?.toLowerCase() || '';

                if (['pdf', 'doc', 'docx', 'txt', 'rtf', 'odt'].includes(extension)) {
                    groups.documents.push(file);
                } else if (['jpg', 'jpeg', 'png', 'gif', 'bmp', 'svg', 'webp'].includes(extension)) {
                    groups.images.push(file);
                } else if (['mp4', 'avi', 'mov', 'wmv', 'flv', 'webm', 'mkv'].includes(extension)) {
                    groups.videos.push(file);
                } else if (['mp3', 'wav', 'flac', 'aac', 'ogg', 'wma'].includes(extension)) {
                    groups.audio.push(file);
                } else if (['zip', 'rar', '7z', 'tar', 'gz', 'bz2'].includes(extension)) {
                    groups.archives.push(file);
                } else {
                    groups.others.push(file);
                }
            }
        });

        // Remove empty groups
        Object.keys(groups).forEach(key => {
            if (groups[key].length === 0) {
                delete groups[key];
            }
        });

        return groups;
    }
}

export const bulkOperationsService = new BulkOperationsService();