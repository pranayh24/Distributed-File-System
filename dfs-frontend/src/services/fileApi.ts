import api from './api';

export const fileApi = {
    uploadFile: async (formData: FormData) => {
        return api.post('/files/upload', formData, {
            headers: {
                'Content-Type': 'multipart/form-data',
            },
        });
    },

    downloadFile: async (path: string) => {
        return api.get(`/files/download/${encodeURIComponent(path)}`, {
            responseType: 'blob',
        });
    },

    deleteFile: async (path: string) => {
        return api.delete(`/files/${encodeURIComponent(path)}`);
    },

    getFileInfo: async (path: string) => {
        return api.get(`/files/info/${encodeURIComponent(path)}`);
    },
};