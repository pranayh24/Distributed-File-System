import React, { useState } from 'react';
import { SidebarNav } from './SidebarNav';
import { Topbar } from './Topbar';

interface LayoutProps {
    children: React.ReactNode;
    onSearch?: (query: string) => void;
}

export const Layout: React.FC<LayoutProps> = ({ children, onSearch }) => {
    return (
        <div className="flex h-screen bg-gray-50 dark:bg-gray-900">
            <SidebarNav />
            <div className="flex-1 flex flex-col overflow-hidden">
                <Topbar onSearch={onSearch} />
                <main className="flex-1 overflow-x-hidden overflow-y-auto bg-gray-100 dark:bg-gray-950 p-6">
                    {children}
                </main>
            </div>
        </div>
    );
};