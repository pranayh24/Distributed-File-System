import { SidebarNav } from './SidebarNav';
import { Topbar } from './Topbar';

export function DashboardShell({ children }: { children: React.ReactNode }) {
    return (
        <div className="flex h-screen overflow-hidden">
            <aside className="w-64 hidden md:block h-full shadow-lg">
                <SidebarNav />
            </aside>
            <div className="flex-1 flex flex-col bg-gradient-to-br from-primary-50 to-primary-100 dark:from-gray-950 dark:to-slate-900">
                <Topbar />
                <main className="flex-1 overflow-y-auto p-8">
                    {children}
                </main>
            </div>
        </div>
    );
}