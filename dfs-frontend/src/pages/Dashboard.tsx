import { DashboardShell } from '../components/layout/DashboardShell';
import { useAuth } from '../context/AuthContext';
import { useEffect, useState } from 'react';
import { api } from '../services/api';
import { formatFileSize } from '../utils/formatters';

export default function DashboardPage() {
    const { user } = useAuth();
    const [recentFiles, setRecentFiles] = useState<any[]>([]);
    const [nodeHealth, setNodeHealth] = useState<any>({});
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        async function fetchData() {
            setLoading(true);
            try {
                // Fetch recent files
                const filesRes = await api.get('/directories/');
                setRecentFiles(filesRes.data.data?.slice(0, 5) || []);

                // Fetch node health
                const healthRes = await api.get('/system/health');
                setNodeHealth(healthRes.data.nodes || {});
            } catch (e) {
                // Handle error
            } finally {
                setLoading(false);
            }
        }
        fetchData();
    }, []);

    const percentUsed = user ? Math.round((user.currentUsage / user.quotaLimit) * 100) : 0;

    return (
        <DashboardShell>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-8 mb-8">
                <div className="glassy rounded-xl p-6 flex flex-col items-center shadow-lg">
                    <span className="text-5xl font-bold text-primary-600">{user?.username?.[0]?.toUpperCase() || "U"}</span>
                    <h2 className="mt-3 text-xl font-semibold">{user?.username}</h2>
                    <p className="text-gray-500">{user?.email}</p>
                </div>
                <div className="glassy rounded-xl p-6 flex flex-col items-center shadow-lg">
                    <div className="w-24 h-24 rounded-full bg-primary-100 flex items-center justify-center relative">
                        <svg className="w-full h-full absolute top-0 left-0" viewBox="0 0 100 100">
                            <circle cx="50" cy="50" r="45" stroke="#e5e7eb" strokeWidth="10" fill="none" />
                            <circle
                                cx="50" cy="50" r="45"
                                stroke="#437ef7"
                                strokeWidth="10"
                                fill="none"
                                strokeDasharray={`${percentUsed * 2.83} 283`}
                                strokeLinecap="round"
                                transform="rotate(-90 50 50)"
                            />
                        </svg>
                        <span className="text-2xl font-bold text-primary-600 z-10">{percentUsed}%</span>
                    </div>
                    <div className="mt-3 text-lg font-semibold">Storage Used</div>
                    <div className="text-gray-500">{formatFileSize(user?.currentUsage || 0)} / {formatFileSize(user?.quotaLimit || 0)}</div>
                </div>
                <div className="glassy rounded-xl p-6 flex flex-col items-center shadow-lg">
                    <div className="text-lg font-semibold mb-1">Cluster Health</div>
                    <div className="text-3xl font-bold text-green-600">{nodeHealth.healthyNodes ?? "?"} / {nodeHealth.totalNodes ?? "?"}</div>
                    <div className="text-xs text-gray-500">healthy nodes</div>
                </div>
            </div>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
                <div className="glassy rounded-xl p-6 shadow-lg">
                    <h3 className="text-lg font-semibold mb-4">Recent Files</h3>
                    <ul>
                        {recentFiles.length === 0 && <li className="text-gray-400">No files to show.</li>}
                        {recentFiles.map((file, idx) => (
                            <li key={idx} className="flex items-center justify-between py-2 border-b last:border-b-0">
                                <span className="truncate">{file.name}</span>
                                <span className="text-xs text-gray-500">{formatFileSize(file.size)}</span>
                            </li>
                        ))}
                    </ul>
                </div>
                <div className="glassy rounded-xl p-6 shadow-lg">
                    <h3 className="text-lg font-semibold mb-4">Recent Activity</h3>
                    <ul>
                        <li className="py-2 text-gray-400 text-sm">Feature coming soon: File uploads, downloads, deletions, etc.</li>
                    </ul>
                </div>
            </div>
            {loading && (
                <div className="fixed inset-0 bg-black/10 flex items-center justify-center z-50">
                    <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
                </div>
            )}
        </DashboardShell>
    );
}