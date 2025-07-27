import React, { useState } from 'react';
import { X, Folder } from 'lucide-react';
import { directoryApi } from '../../services/directoryApi';

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

        // Validate folder name
        const invalidChars = /[<>:"/\\|?*]/;
        if (invalidChars.test(folderName)) {
            setError('Folder name contains invalid characters');
            return;
        }

        setLoading(true);
        setError(null);

        try {
            const fullPath = currentPath ? `${currentPath}/${folderName.trim()}` : folderName.trim();
            console.log('Creating directory at path:', fullPath);

            const response = await directoryApi.createDirectory(fullPath);
            console.log('Directory creation response:', response);

            // Check if the response indicates success
            if (response.data && (response.data.success === true || response.status === 200 || response.status === 201)) {
                console.log('Directory created successfully');
                onCreateComplete();
            } else {
                // Handle cases where backend returns success but with different format
                const errorMessage = response.data?.error || response.data?.message || 'Failed to create folder';
                console.error('Directory creation failed:', errorMessage);
                setError(errorMessage);
            }
        } catch (err: any) {
            console.error('Directory creation error:', err);

            // Better error handling for different response codes
            if (err.response?.status === 409) {
                setError('A folder with this name already exists');
            } else if (err.response?.status === 403) {
                setError('You do not have permission to create folders here');
            } else if (err.response?.status === 401) {
                setError('Authentication required. Please log in again.');
            } else {
                const errorMessage = err.response?.data?.error ||
                                   err.response?.data?.message ||
                                   err.message ||
                                   'Failed to create folder';
                setError(errorMessage);
            }
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