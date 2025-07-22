import React from 'react';
import { Layout } from '../components/layout/Layout';
import { useAuth } from '../context/AuthContext';
import { formatFileSize, formatDate } from '../utils/formatters';
import { User, HardDrive, Shield, Clock } from 'lucide-react';

export const Dashboard: React.FC = () => {
    const { user } = useAuth();

    const usagePercentage = user ? (user.currentUsage / user.quotaLimit) * 100 : 0;

    return (
        <Layout>
            <div className="space-y-6">
                {/* Welcome Section */}
                <div className="bg-white p-6 rounded-lg shadow-sm border">
                    <h2 className="text-2xl font-bold text-gray-900 mb-2">
                        Welcome back, {user?.username}!
                    </h2>
                    <p className="text-gray-600">
                        Manage your files and monitor your storage usage.
                    </p>
                </div>

                {/* Stats Grid */}
                <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                    <div className="bg-white p-6 rounded-lg shadow-sm border">
                        <div className="flex items-center">
                            <HardDrive className="h-8 w-8 text-primary-600" />
                            <div className="ml-4">
                                <p className="text-sm font-medium text-gray-600">Storage Used</p>
                                <p className="text-2xl font-bold text-gray-900">
                                    {formatFileSize(user?.currentUsage || 0)}
                                </p>
                            </div>
                        </div>
                    </div>

                    <div className="bg-white p-6 rounded-lg shadow-sm border">
                        <div className="flex items-center">
                            <Shield className="h-8 w-8 text-green-600" />
                            <div className="ml-4">
                                <p className="text-sm font-medium text-gray-600">Quota Limit</p>
                                <p className="text-2xl font-bold text-gray-900">
                                    {formatFileSize(user?.quotaLimit || 0)}
                                </p>
                            </div>
                        </div>
                    </div>

                    <div className="bg-white p-6 rounded-lg shadow-sm border">
                        <div className="flex items-center">
                            <Clock className="h-8 w-8 text-blue-600" />
                            <div className="ml-4">
                                <p className="text-sm font-medium text-gray-600">Last Login</p>
                                <p className="text-lg font-bold text-gray-900">
                                    {user?.lastLoginAt ? formatDate(user.lastLoginAt) : 'First time'}
                                </p>
                            </div>
                        </div>
                    </div>
                </div>

                {/* Storage Usage */}
                <div className="bg-white p-6 rounded-lg shadow-sm border">
                    <h3 className="text-lg font-semibold text-gray-900 mb-4">Storage Usage</h3>
                    <div className="space-y-2">
                        <div className="flex justify-between text-sm">
                            <span>Used: {formatFileSize(user?.currentUsage || 0)}</span>
                            <span>Available: {formatFileSize((user?.quotaLimit || 0) - (user?.currentUsage || 0))}</span>
                        </div>
                        <div className="w-full bg-gray-200 rounded-full h-2">
                            <div
                                className={`h-2 rounded-full ${
                                    usagePercentage > 90 ? 'bg-red-600' :
                                        usagePercentage > 70 ? 'bg-yellow-600' : 'bg-green-600'
                                }`}
                                style={{ width: `${Math.min(usagePercentage, 100)}%` }}
                            ></div>
                        </div>
                        <p className="text-xs text-gray-500">
                            {usagePercentage.toFixed(1)}% of quota used
                        </p>
                    </div>
                </div>

                {/* Quick Actions */}
                <div className="bg-white p-6 rounded-lg shadow-sm border">
                    <h3 className="text-lg font-semibold text-gray-900 mb-4">Quick Actions</h3>
                    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
                        <button className="btn-primary w-full">
                            Upload Files
                        </button>
                        <button className="btn-secondary w-full">
                            Browse Files
                        </button>
                        <button className="btn-secondary w-full">
                            Create Folder
                        </button>
                        <button className="btn-secondary w-full">
                            View Activity
                        </button>
                    </div>
                </div>
            </div>
        </Layout>
    );
};