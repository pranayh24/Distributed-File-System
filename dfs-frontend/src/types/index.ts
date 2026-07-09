export interface User {
    userId: string;
    username: string;
    email: string;
    createdAt: string;
    lastLoginAt: string | null;
    active: boolean;
    userDirectory: string;
    quotaLimit: number;
    currentUsage: number;
}

export interface ApiResponse<T> {
    success: boolean;
    message: string;
    data?: T;
    error?: string;
    timestamp: string;
}

export interface AuthResult {
    token: string;
    user: User;
}

export interface LoginRequest {
    username: string;
    password: string;
}

export interface RegisterRequest {
    username: string;
    email: string;
    password: string;
}

export interface NodeInfo {
    nodeId: string;
    address: string;
    port: number;
    healthy: boolean;
    hostedFiles: number;        // replica count on this node
    hostedFilesList?: string[]; // the actual file paths
    availableDiskSpace: number;
    lastHeartbeat?: number;
}

export interface FilePlacement {
    filePath: string;
    fileName: string;
    size: number;
    replicationFactor: number;
    currentReplicas: number;
    nodeIds: string[];
    healthyNodeIds: string[];
    underReplicated: boolean;
    createdAt: string;
}

export type DfsEventType =
    | 'UPLOAD_STARTED'
    | 'ENCRYPTED'
    | 'REPLICA_STORED'
    | 'UPLOAD_COMMITTED'
    | 'NODE_KILLED'
    | 'NODE_REVIVED'
    | 'HEAL_STARTED'
    | 'REPLICA_RESTORED'
    | 'HEAL_COMPLETED'
    | 'FILE_DELETED';

export interface DfsEvent {
    type: DfsEventType;
    timestamp: number;
    message: string;
    fileName?: string | null;
    nodeId?: string | null;
    sourceNodeId?: string | null;
    replicaIndex?: number | null;
    replicas?: number | null;
    replicationFactor?: number | null;
    size?: number | null;
    /** client-only unique id for list rendering */
    _id?: string;
}
