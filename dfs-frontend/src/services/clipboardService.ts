export interface ClipboardItem {
    id: string;
    name: string;
    path: string;
    isDirectory: boolean;
    operation: 'copy' | 'cut';
}

class ClipboardService {
    private clipboard: ClipboardItem[] = [];
    private listeners: ((items: ClipboardItem[]) => void)[] = [];

    copy(items: ClipboardItem[]) {
        this.clipboard = items.map(item => ({ ...item, operation: 'copy' }));
        this.notifyListeners();
    }

    cut(items: ClipboardItem[]) {
        this.clipboard = items.map(item => ({ ...item, operation: 'cut' }));
        this.notifyListeners();
    }

    getClipboard(): ClipboardItem[] {
        return [...this.clipboard];
    }

    clear() {
        this.clipboard = [];
        this.notifyListeners();
    }

    isEmpty(): boolean {
        return this.clipboard.length === 0;
    }

    hasOperation(operation: 'copy' | 'cut'): boolean {
        return this.clipboard.some(item => item.operation === operation);
    }

    subscribe(callback: (items: ClipboardItem[]) => void) {
        this.listeners.push(callback);
        return () => {
            this.listeners = this.listeners.filter(listener => listener !== callback);
        };
    }

    private notifyListeners() {
        this.listeners.forEach(listener => listener(this.clipboard));
    }
}

export const clipboardService = new ClipboardService();