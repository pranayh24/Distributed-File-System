import api from './api';
import type { ApiResponse, FilePlacement, NodeInfo } from '../types';

export const systemService = {
    async getNodes(): Promise<NodeInfo[]> {
        const { data } = await api.get<ApiResponse<NodeInfo[]>>('/system/nodes');
        return data.data ?? [];
    },

    async getPlacement(): Promise<FilePlacement[]> {
        const { data } = await api.get<ApiResponse<FilePlacement[]>>('/system/files/placement');
        return data.data ?? [];
    },

    async killNode(nodeId: string): Promise<void> {
        await api.post(`/system/nodes/${nodeId}/kill`);
    },

    async reviveNode(nodeId: string): Promise<void> {
        await api.post(`/system/nodes/${nodeId}/revive`);
    },
};
