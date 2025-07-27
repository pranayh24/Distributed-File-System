import api from './api';

interface SearchParams {
    query: string;
    path?: string;
    fileType?: string;
    minSize?: number;
    maxSize?: number;
}

export const searchApi = {
    searchFiles: async (params: SearchParams) => {
        return api.get('/search', { params });
    },
};