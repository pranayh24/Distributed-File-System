import { NavLink } from 'react-router-dom';
import { HomeIcon, FolderIcon, TrashIcon, UserCircleIcon } from '@heroicons/react/24/outline';
import clsx from 'clsx';

const nav = [
    { name: "Dashboard", icon: HomeIcon, key: "dashboard", path: "/dashboard" },
    { name: "My Files", icon: FolderIcon, key: "files", path: "/files" },
    { name: "Trash", icon: TrashIcon, key: "trash", path: "/trash" },
    { name: "Account", icon: UserCircleIcon, key: "account", path: "/account" }
];

export function SidebarNav() {
    return (
        <nav className="flex flex-col gap-1 pt-6 glassy h-full">
            {nav.map(item => (
                <NavLink
                    key={item.key}
                    to={item.path}
                    className={({ isActive }) =>
                        clsx(
                            "flex items-center gap-3 px-4 py-2 rounded-lg transition-all group",
                            isActive
                                ? "bg-primary-500/10 text-primary-700 font-bold"
                                : "hover:bg-primary-100 hover:text-primary-600"
                        )
                    }
                >
                    <item.icon className={clsx("h-5 w-5", "text-gray-400 group-hover:text-primary-600")} />
                    {item.name}
                </NavLink>
            ))}
        </nav>
    );
}