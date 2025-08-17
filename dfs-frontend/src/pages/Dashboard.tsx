import { DashboardShell } from '../components/layout/DashboardShell';
import { useAuth } from '../context/AuthContext';
import { useEffect, useState } from 'react';
import { api } from '../services/api';
import { formatFileSize } from '../utils/formatters';
import { HardDrive, File, Clock, Server } from 'lucide-react';

export default function DashboardPage() {
    const { user } = useAuth();
    const [recentFiles, setRecentFiles] = useState<any[]>([]);
    const [systemHealth, setSystemHealth] = useState<any>(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        async function fetchData() {
            setLoading(true);
            try {
                const [filesRes, healthRes] = await Promise.all([
                    api.get('/search/recent?size=5'), // Using the recent files endpoint
                    api.get('/system/health')
                ]);
                setRecentFiles(filesRes.data.data?.files || []);
                setSystemHealth(healthRes.data.data || {});
            } catch (e) {
                console.error("Failed to fetch dashboard data:", e);
            } finally {
                setLoading(false);
            }
        }
        fetchData();
    }, []);

    const percentUsed = user ? Math.round((user.currentUsage / user.quotaLimit) * 100) : 0;

    return (
        <DashboardShell>
            <h1 className="text-3xl font-bold mb-6 text-gray-900 dark:text-white">Welcome, {user?.username}!</h1>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
                {/* Storage Usage */}
                <div className="bg-white dark:bg-gray-800 p-6 rounded-xl shadow-lg border border-gray-200 dark:border-gray-700">
                    <div className="flex items-center mb-4">
                        <div className="p-3 rounded-full bg-primary-100 dark:bg-primary-900/50">
                            <HardDrive className="h-6 w-6 text-primary-600 dark:text-primary-400" />
                        </div>
                        <h3 className="ml-4 text-lg font-semibold text-gray-900 dark:text-white">Storage</h3>
                    </div>
                    <div className="w-full bg-gray-200 rounded-full h-2.5 dark:bg-gray-700 mb-2">
                        <div className="bg-primary-600 h-2.5 rounded-full" style={{ width: `${percentUsed}%` }}></div>
                    </div>
                    <p className="text-sm text-gray-600 dark:text-gray-300">{formatFileSize(user?.currentUsage || 0)} of {formatFileSize(user?.quotaLimit || 0)} used</p>
                </div>

                {/* System Health */}
                <div className="bg-white dark:bg-gray-800 p-6 rounded-xl shadow-lg border border-gray-200 dark:border-gray-700">
                    <div className="flex items-center mb-4">
                        <div className="p-3 rounded-full bg-green-100 dark:bg-green-900/50">
                            <Server className="h-6 w-6 text-green-600 dark:text-green-400" />
                        </div>
                        <h3 className="ml-4 text-lg font-semibold text-gray-900 dark:text-white">Cluster Health</h3>
                    </div>
                    {loading ? <p>Loading...</p> :
                        <>
                            <p className="text-3xl font-bold text-gray-900 dark:text-white">{systemHealth?.clusterHealth?.healthyNodes ?? "?"}/{systemHealth?.clusterHealth?.totalNodes ?? "?"}</p>
                            <p className="text-sm text-gray-600 dark:text-gray-300">nodes are healthy</p>
                        </>
                    }
                </div>
            </div>

            <div className="bg-white dark:bg-gray-800 p-6 rounded-xl shadow-lg border border-gray-200 dark:border-gray-700">
                <h3 className="text-lg font-semibold mb-4 text-gray-900 dark:text-white">Recent Files</h3>
                <ul>
                    {loading ? <p>Loading...</p> : recentFiles.length === 0 ? <li className="text-gray-400">No recent files.</li> :
                        recentFiles.map((file, idx) => (
                            <li key={idx} className="flex items-center justify-between py-2 border-b last:border-b-0 border-gray-200 dark:border-gray-700">
                                <div className="flex items-center">
                                    <File className="h-5 w-5 mr-3 text-gray-400" />
                                    <span className="truncate font-medium">{file.fileName}</span>
                                </div>
                                <span className="text-sm text-gray-500">{formatFileSize(file.fileSize)}</span>
                            </li>
                        ))}
                </ul>
            </div>
        </DashboardShell>
    );
}