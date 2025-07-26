import { DashboardShell } from '../components/layout/DashboardShell';
import { useAuth } from '../context/AuthContext';

export default function AccountPage() {
    const { user } = useAuth();
    return (
        <DashboardShell>
            <h1 className="text-2xl font-bold mb-6">Account</h1>
            <div className="bg-white/60 dark:bg-gray-800/60 rounded-xl shadow-lg p-8">
                <div className="mb-4 flex flex-col gap-2">
                    <div><span className="font-semibold">Username:</span> {user?.username}</div>
                    <div><span className="font-semibold">Email:</span> {user?.email}</div>
                    <div><span className="font-semibold">Directory:</span> {user?.userDirectory}</div>
                    <div><span className="font-semibold">Quota Limit:</span> {user?.quotaLimit}</div>
                    <div><span className="font-semibold">Current Usage:</span> {user?.currentUsage}</div>
                </div>
            </div>
        </DashboardShell>
    );
}