import api from './api';

// Utility function to normalize file paths for download
const normalizePathForDownload = (filePath: string): string => {
    if (!filePath) return '';

    console.log('Original path:', filePath); // Debug logging

    // Convert backslashes to forward slashes
    let normalizedPath = filePath.replace(/\\/g, '/');
    console.log('After backslash conversion:', normalizedPath); // Debug logging

    // Remove leading slashes first
    normalizedPath = normalizedPath.replace(/^\/+/, '');
    console.log('After removing leading slashes:', normalizedPath); // Debug logging

    console.log('Does it start with users/? ', normalizedPath.startsWith('users/')); // Debug logging

    // Handle the case where path starts with "users/"
    if (normalizedPath.startsWith('users/')) {
        const parts = normalizedPath.split('/');
        console.log('Path parts:', parts); // Debug logging

        if (parts.length >= 3) {
            // Remove "users" and username, keep the rest
            // For "users/prh/filename.pdf" -> "filename.pdf"
            // For "users/prh/documents/filename.pdf" -> "documents/filename.pdf"
            normalizedPath = parts.slice(2).join('/');
        }
    }


    console.log('Final normalized path:', normalizedPath); // Debug logging

    return normalizedPath;
};

export const fileApi = {
    uploadFile: async (formData: FormData) => {
        return api.post('/files/upload', formData, {
            headers: {
                'Content-Type': 'multipart/form-data',
            },
        });
    },

    downloadFile: async (path: string) => {
        const normalizedPath = normalizePathForDownload(path);
        console.log('Making download request with path:', normalizedPath);

        try {
            // Use axios but with proper configuration
            const response = await api.get(`/files/download/${encodeURIComponent(normalizedPath)}`, {
                responseType: 'blob',
                headers: {
                    'Accept': 'application/octet-stream'
                }
            });

            // Create blob URL and trigger download
            const blob = new Blob([response.data], {
                type: response.headers['content-type'] || 'application/octet-stream'
            });

            const url = window.URL.createObjectURL(blob);
            const link = document.createElement('a');
            link.href = url;

            // Extract filename from Content-Disposition header properly
            let filename = 'download'; // fallback

            const contentDisposition = response.headers['content-disposition'];
            console.log('Content-Disposition header:', contentDisposition);

            if (contentDisposition) {
                // Try multiple patterns to extract filename
                const patterns = [
                    /filename[^;=\n]*=\s*"([^"]+)"/i,  // filename="example.pdf"
                    /filename[^;=\n]*=\s*([^;\n\s]+)/i, // filename=example.pdf
                    /filename\*?=[^']*''([^;\n]+)/i     // filename*=UTF-8''example.pdf
                ];

                for (const pattern of patterns) {
                    const match = contentDisposition.match(pattern);
                    if (match && match[1]) {
                        filename = decodeURIComponent(match[1].trim());
                        console.log('Extracted filename from header:', filename);
                        break;
                    }
                }
            }

            // If no filename from header, try to get from original path
            if (filename === 'download') {
                // Get the original filename from the path parameter (before normalization)
                const originalFilename = path.split(/[/\\]/).pop();
                if (originalFilename && originalFilename.includes('.')) {
                    filename = originalFilename;
                    console.log('Using original filename from path:', filename);
                } else {
                    // Last resort: use normalized path
                    filename = normalizedPath.split('/').pop() || 'download';
                    console.log('Using normalized path filename:', filename);
                }
            }

            // Ensure filename has an extension if possible
            if (!filename.includes('.') && normalizedPath.includes('.')) {
                const extension = normalizedPath.split('.').pop();
                if (extension && extension.length <= 4) { // reasonable extension length
                    filename += '.' + extension;
                    console.log('Added extension to filename:', filename);
                }
            }

            console.log('Final download filename:', filename);

            link.download = filename;
            link.style.display = 'none';

            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);

            // Cleanup
            window.URL.revokeObjectURL(url);

            return response;
        } catch (error) {
            console.error('Download failed:', error);
            throw error;
        }
    },

    deleteFile: async (path: string) => {
        return api.delete(`/files/${encodeURIComponent(path)}`);
    },

    getFileInfo: async (path: string) => {
        return api.get(`/files/info/${encodeURIComponent(path)}`);
    },
};