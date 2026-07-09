export function formatBytes(bytes?: number | null): string {
    if (bytes == null) return '—';
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    if (bytes < 1024 * 1024 * 1024) return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
    return `${(bytes / (1024 * 1024 * 1024)).toFixed(2)} GB`;
}

export function clockTime(ts: number): string {
    return new Date(ts).toLocaleTimeString('en-GB', { hour12: false });
}

/** short filename from a user-scoped path */
export function baseName(path: string): string {
    return path.split('/').pop() || path;
}
