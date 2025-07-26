import { BellIcon } from '@heroicons/react/24/outline';
import { useAuth } from '../../context/AuthContext';

export function Topbar() {
    const { user } = useAuth();
    return (
        <header className="flex items-center justify-between px-8 h-16 bg-white/80 dark:bg-gray-900/70 glassy shadow">
            <h1 className="text-xl font-bold text-primary-700 tracking-tight">DFS</h1>
            <div className="flex items-center gap-4">
                <button className="p-2 rounded-full hover:bg-primary-100">
                    <BellIcon className="h-6 w-6 text-primary-600" />
                </button>
                <span className="flex items-center gap-2 bg-primary-50 px-3 py-1 rounded-full">
          <span className="h-8 w-8 bg-primary-600 text-white rounded-full flex items-center justify-center font-bold text-lg uppercase">{user?.username?.[0] || "U"}</span>
          <span className="text-sm font-medium">{user?.username}</span>
        </span>
            </div>
        </header>
    );
}