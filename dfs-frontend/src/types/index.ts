export interface User {
    userId: string;
    username: string;
    email: string;
    createdAt: string;
    lastLoginAt: string;
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

export interface FileMetadata {
    name: string;
    path: string;
    isDirectory: boolean;
    size: number;
    uploadTime?: string;
    lastModified: string;
    checksum?: string;
    replicationFactor?: number;
    currentReplicas?: number;
    contentType?: string;
}

export interface FileUploadRequest {
    file: File;
    targetDirectory?: string;
    replicationFactor?: number;
    comment?: string;
    createVersion?: boolean;
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