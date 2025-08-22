import React, { useState, useEffect } from 'react';
import {
    Download,
    Trash2,
    Copy,
    Scissors,
    Archive,
    X,
    FileText,
    Image,
    Video,
    Music,
    Folder,
    File
} from 'lucide-react';
import { formatFileSize } from '../../utils/formatters';
import { useBulkOperations } from '../../hooks/useBulkOperations';
import type { FileItem } from './types';

interface BulkOperationsPanelProps {
    selectedFiles: FileItem[];
    onCopy: () => void;
    onCut: () => void;
    onDelete: (fileIds: string[]) => void;
    onClearSelection: () => void;
}

interface SelectionSummary {
    totalCount: number;
    totalSize: number;
    directories: number;
    files: number;
    groups: Record<string, FileItem[]>;
}

export const BulkOperationsPanel: React.FC<BulkOperationsPanelProps> = ({
                                                                            selectedFiles,
                                                                            onCopy,
                                                                            onCut,
                                                                            onDelete,
                                                                            onClearSelection
                                                                        }) => {
    const [summary, setSummary] = useState<SelectionSummary | null>(null);
    const [showDetails, setShowDetails] = useState(false);
    const { loading, progress, downloadMultiple, getSelectionSummary } = useBulkOperations();

    useEffect(() => {
        const loadSummary = async () => {
            if (selectedFiles.length > 0) {
                const summaryData = await getSelectionSummary(selectedFiles);
                setSummary(summaryData);
            } else {
                setSummary(null);
            }
        };

        loadSummary();
    }, [selectedFiles, getSelectionSummary]);

    const handleDownload = async () => {
        try {
            await downloadMultiple(selectedFiles);
        } catch (error: any) {
            console.error('Bulk download failed:', error);
            alert(`Download failed: ${error.message}`);
        }
    };

    const handleDelete = () => {
        if (confirm(`Are you sure you want to delete ${selectedFiles.length} item${selectedFiles.length > 1 ? 's' : ''}?`)) {
            onDelete(selectedFiles.map(f => f.id));
        }
    };

    if (selectedFiles.length === 0) return null;

    const getTypeIcon = (type: string) => {
        switch (type) {
            case 'directories': return <Folder size={16} className="text-blue-500" />;
            case 'documents': return <FileText size={16} className="text-red-500" />;
            case 'images': return <Image size={16} className="text-purple-500" />;
            case 'videos': return <Video size={16} className="text-orange-500" />;
            case 'audio': return <Music size={16} className="text-green-500" />;
            case 'archives': return <Archive size={16} className="text-yellow-600" />;
            default: return <File size={16} className="text-gray-500" />;
        }
    };

    const getTypeLabel = (type: string) => {
        return type.charAt(0).toUpperCase() + type.slice(1);
    };

    return (
        <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 mb-4">
            {/* Progress Bar */}
    {loading && progress && (
        <div className="mb-4">
        <div className="flex items-center justify-between text-sm text-blue-700 mb-2">
            <span>
                {progress.current ? `Processing: ${progress.current}` : 'Processing...'}
            </span>
            <span>
            {progress.completed} / {progress.total}
        {progress.failed > 0 && ` (${progress.failed} failed)`}
        </span>
        </div>
        <div className="w-full bg-blue-200 rounded-full h-2">
    <div
        className="bg-blue-600 h-2 rounded-full transition-all duration-300"
        style={{ width: `${(progress.completed / progress.total) * 100}%` }}
        />
        </div>
        </div>
    )}

    <div className="flex items-center justify-between">
    <div className="flex items-center gap-4">
        {/* Selection Summary */}
        <div className="flex items-center gap-2">
    <span className="text-sm font-medium text-blue-700">
        {selectedFiles.length} item{selectedFiles.length > 1 ? 's' : ''} selected
    </span>

    {summary && (
        <>
            <span className="text-blue-400">•</span>
    <span className="text-sm text-blue-600">
        {formatFileSize(summary.totalSize)}
        </span>

        {summary.directories > 0 && summary.files > 0 && (
            <>
                <span className="text-blue-400">•</span>
        <span className="text-sm text-blue-600">
            {summary.files} file{summary.files > 1 ? 's' : ''}, {summary.directories} folder{summary.directories > 1 ? 's' : ''}
            </span>
            </>
        )}

        <button
            onClick={() => setShowDetails(!showDetails)}
        className="text-sm text-blue-600 hover:text-blue-800 underline"
            >
            {showDetails ? 'Hide details' : 'Show details'}
            </button>
            </>
    )}
    </div>
    </div>

    <div className="flex items-center gap-2">
        {/* Action Buttons */}
        <button
    onClick={onCopy}
    className="flex items-center gap-1 px-3 py-1 text-sm text-blue-700 hover:bg-blue-100 rounded transition-colors"
    title="Copy (Ctrl+C)"
    disabled={loading}
    >
    <Copy size={14} />
    Copy
    </button>

    <button
    onClick={onCut}
    className="flex items-center gap-1 px-3 py-1 text-sm text-blue-700 hover:bg-blue-100 rounded transition-colors"
    title="Cut (Ctrl+X)"
    disabled={loading}
    >
    <Scissors size={14} />
    Cut
    </button>

    <button
    onClick={handleDownload}
    className="flex items-center gap-1 px-3 py-1 text-sm text-blue-700 hover:bg-blue-100 rounded transition-colors"
    disabled={loading || selectedFiles.every(f => f.isDirectory)}
>
    <Download size={14} />
    Download
    </button>

    <div className="w-px h-4 bg-blue-300" />

    <button
        onClick={handleDelete}
    className="flex items-center gap-1 px-3 py-1 text-sm text-red-700 hover:bg-red-100 rounded transition-colors"
    disabled={loading}
    >
    <Trash2 size={14} />
    Delete
    </button>

    <button
    onClick={onClearSelection}
    className="p-1 text-blue-700 hover:bg-blue-100 rounded transition-colors"
    title="Clear selection"
    >
    <X size={16} />
    </button>
    </div>
    </div>

    {/* Detailed Breakdown */}
    {showDetails && summary && Object.keys(summary.groups).length > 0 && (
        <div className="mt-4 pt-4 border-t border-blue-200">
        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-3">
            {Object.entries(summary.groups).map(([type, items]) => (
                    <div key={type} className="flex items-center gap-2 p-2 bg-white rounded border">
                    {getTypeIcon(type)}
                    <div className="min-w-0 flex-1">
                <div className="text-sm font-medium text-gray-900 truncate">
                    {getTypeLabel(type)}
                    </div>
                    <div className="text-xs text-gray-500">
                    {items.length} item{items.length > 1 ? 's' : ''}
                    </div>
                    </div>
                    </div>
    ))}
        </div>
        </div>
    )}
    </div>
);
};