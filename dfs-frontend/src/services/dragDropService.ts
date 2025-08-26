export interface DragItem {
    id: string;
    name: string;
    path: string;
    isDirectory: boolean;
    type: 'file' | 'directory';
}

export interface DropZone {
    id: string;
    path: string;
    accepts: ('file' | 'directory' | 'external')[];
}

export class DragDropService {
    private dragData: DragItem | null = null;
    private draggedFiles: FileList | null = null;
    private dropZones: Map<string, DropZone> = new Map();

    // Internal drag/drop (moving files within the app)
    setDragData(item: DragItem) {
        this.dragData = item;
    }

    getDragData(): DragItem | null {
        return this.dragData;
    }

    clearDragData() {
        this.dragData = null;
        this.draggedFiles = null;
    }

    // External drag/drop (dragging files from OS)
    setDraggedFiles(files: FileList) {
        this.draggedFiles = files;
    }

    getDraggedFiles(): FileList | null {
        return this.draggedFiles;
    }

    // Drop zone management
    registerDropZone(zone: DropZone) {
        this.dropZones.set(zone.id, zone);
    }

    unregisterDropZone(zoneId: string) {
        this.dropZones.delete(zoneId);
    }

    getDropZone(zoneId: string): DropZone | undefined {
        return this.dropZones.get(zoneId);
    }

    // Validation
    canDrop(sourceItem: DragItem, targetPath: string): boolean {
        // Can't drop item into itself
        if (sourceItem.path === targetPath) {
            return false;
        }

        // Can't drop parent into child
        if (targetPath.startsWith(sourceItem.path + '/')) {
            return false;
        }

        return true;
    }

    // File type detection
    getFileTypeFromExtension(filename: string): string {
        const extension = filename.split('.').pop()?.toLowerCase() || '';

        const imageExtensions = ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'svg', 'webp'];
        const videoExtensions = ['mp4', 'avi', 'mov', 'wmv', 'flv', 'webm', 'mkv'];
        const audioExtensions = ['mp3', 'wav', 'flac', 'aac', 'ogg', 'wma'];
        const documentExtensions = ['pdf', 'doc', 'docx', 'txt', 'rtf', 'odt'];
        const archiveExtensions = ['zip', 'rar', '7z', 'tar', 'gz', 'bz2'];

        if (imageExtensions.includes(extension)) return 'image';
        if (videoExtensions.includes(extension)) return 'video';
        if (audioExtensions.includes(extension)) return 'audio';
        if (documentExtensions.includes(extension)) return 'document';
        if (archiveExtensions.includes(extension)) return 'archive';

        return 'file';
    }

    // Helper to process dropped files
    processDroppedFiles(files: FileList): Array<{
        file: File;
        name: string;
        size: number;
        type: string;
        category: string;
    }> {
        return Array.from(files).map(file => ({
            file,
            name: file.name,
            size: file.size,
            type: file.type || 'application/octet-stream',
            category: this.getFileTypeFromExtension(file.name)
        }));
    }
}

export const dragDropService = new DragDropService();