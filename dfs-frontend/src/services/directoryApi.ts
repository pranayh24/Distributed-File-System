import api from './api';

export const directoryApi = {
    listRoot: async () => {
        return api.get('/directories/');
    },

    listDirectory: async (path: string) => {
        return api.get(`/directories/${encodeURIComponent(path)}`);
    },

    createDirectory: async (path: string) => {
        return api.post('/directories', { path });
    },

    deleteDirectory: async (path: string) => {
        return api.delete(`/directories/${encodeURIComponent(path)}`);
    },

    moveDirectory: async (sourcePath: string, destinationPath: string) => {
        return api.put('/directories/move', {
            sourcePath,
            destinationPath,
        });
    },
};