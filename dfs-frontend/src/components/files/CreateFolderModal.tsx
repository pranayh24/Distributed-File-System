import React, { useState } from 'react';
import { X, Folder } from 'lucide-react';
import { directoryApi } from '../../services/api';

interface CreateFolderModalProps {
    currentPath: string;
    onClose: () => void;
    onCreateComplete: () => void;
}

export const CreateFolderModal: React.FC<CreateFolderModalProps> = ({
    currentPath,
    onClose,
    onCreateComplete,
}) => {
    const [folderName, setFolderName] = useState('');
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();

        if (!folderName.trim()) {
            setError('Folder name is required');
            return;
        }

        setLoading(true);
        setError(null);

        try {
            const fullPath = currentPath ? `${currentPath}/${folderName.trim()}` : folderName.trim();
            await directoryApi.createDirectory(fullPath);
            onCreateComplete();
        } catch (err: any) {
            setError(err.response?.data?.error || 'Failed to create folder');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
            <div className="bg-white rounded-lg shadow-xl w-full max-w-md mx-4">
                {/* Header */}
                <div className="flex items-center justify-between p-6 border-b">
                    <div className="flex items-center gap-3">
                        <Folder size={24} className="text-blue-500" />
                        <h2 className="text-xl font-semibold">Create New Folder</h2>
                    </div>
                    <button
                        onClick={onClose}
                        className="p-2 hover:bg-gray-100 rounded-full transition-colors"
                    >
                        <X size={20} />
                    </button>
                </div>

                {/* Form */}
                <form onSubmit={handleSubmit} className="p-6">
                    <div className="mb-4">
                        <label htmlFor="folderName" className="block text-sm font-medium text-gray-700 mb-2">
                            Folder Name
                        </label>
                        <input
                            id="folderName"
                            type="text"
                            value={folderName}
                            onChange={(e) => setFolderName(e.target.value)}
                            placeholder="Enter folder name"
                            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
                            autoFocus
                        />
                        {error && (
                            <p className="mt-2 text-sm text-red-600">{error}</p>
                        )}
                    </div>

                    <div className="mb-4 p-3 bg-gray-50 rounded-lg">
                        <p className="text-sm text-gray-600">
                            <span className="font-medium">Location:</span> {currentPath || 'Root'}
                        </p>
                        {folderName && (
                            <p className="text-sm text-gray-600 mt-1">
                                <span className="font-medium">Full path:</span> {currentPath ? `${currentPath}/${folderName}` : folderName}
                            </p>
                        )}
                    </div>

                    {/* Actions */}
                    <div className="flex items-center justify-end gap-3">
                        <button
                            type="button"
                            onClick={onClose}
                            className="btn-secondary"
                            disabled={loading}
                        >
                            Cancel
                        </button>
                        <button
                            type="submit"
                            className="btn-primary"
                            disabled={loading || !folderName.trim()}
                        >
                            {loading ? (
                                <div className="flex items-center gap-2">
                                    <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white"></div>
                                    Creating...
                                </div>
                            ) : (
                                'Create Folder'
                            )}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
};