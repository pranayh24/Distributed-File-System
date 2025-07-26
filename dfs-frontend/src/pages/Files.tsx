import { DashboardShell } from '../components/layout/DashboardShell';

export default function FilesPage() {
    // Implement file manager grid/list here.
    return (
        <DashboardShell>
            <h1 className="text-2xl font-bold mb-6">My Files</h1>
            <div className="bg-white/60 dark:bg-gray-800/60 rounded-xl shadow-lg p-8 min-h-[200px] flex items-center justify-center text-gray-400">
                File manager coming soon!
            </div>
        </DashboardShell>
    );
}