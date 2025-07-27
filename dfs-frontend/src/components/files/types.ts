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

