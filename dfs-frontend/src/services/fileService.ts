import api from './api';
import type { ApiResponse } from '../types';

export const fileService = {
    async upload(file: File, targetDirectory = '/', replicationFactor = 3): Promise<ApiResponse<any>> {
        const form = new FormData();
        form.append('file', file);
        form.append('targetDirectory', targetDirectory);
        form.append('replicationFactor', String(replicationFactor));
        const { data } = await api.post<ApiResponse<any>>('/files/upload', form, {
            headers: { 'Content-Type': 'multipart/form-data' },
            timeout: 120000,
        });
        return data;
    },

    async download(path: string): Promise<void> {
        const res = await api.get(`/files/download/${encodeURIComponent(path)}`, { responseType: 'blob' });
        const url = URL.createObjectURL(new Blob([res.data]));
        const a = document.createElement('a');
        a.href = url;
        a.download = path.split('/').pop() || 'download';
        document.body.appendChild(a);
        a.click();
        a.remove();
        URL.revokeObjectURL(url);
    },

    async remove(path: string): Promise<boolean> {
        const { data } = await api.delete<ApiResponse<any>>(`/files/${encodeURIComponent(path)}`);
        return data.success;
    },
};
