import type { ReactNode } from 'react';
import { LogOut } from 'lucide-react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export function Layout({ children, connected }: { children: ReactNode; connected: boolean }) {
    const { user, logout } = useAuth();

    return (
        <div className="relative z-[1] mx-auto max-w-[1240px] px-4 sm:px-7 pb-16 pt-5">
            <header className="mb-5 flex flex-wrap items-center gap-4">
                <Link to="/" className="flex items-center gap-3">
                    <div className="logo-mark" aria-hidden="true" />
                    <div>
                        <h1 className="m-0 text-base font-semibold tracking-tight">
                            DFS<span className="text-cyan">·</span>LIVE
                        </h1>
                        <div className="font-mono text-[10.5px] tracking-[0.12em] text-ink-faint">
                            DISTRIBUTED&nbsp;FILE&nbsp;SYSTEM&nbsp;·&nbsp;CONTROL
                        </div>
                    </div>
                </Link>

                <div className="ml-auto flex items-center gap-3">
                    <span
                        className={`inline-flex items-center gap-2 rounded-full border px-3 py-[6px] font-mono text-[11px] tracking-[0.08em] ${
                            connected
                                ? 'border-good/30 bg-good/[0.08] text-good'
                                : 'border-bad/30 bg-bad/[0.08] text-bad'
                        }`}
                    >
                        <span
                            className={`h-[7px] w-[7px] rounded-full ${connected ? 'animate-beat bg-good' : 'bg-bad'}`}
                        />
                        {connected ? 'STREAM LIVE' : 'RECONNECTING'}
                    </span>

                    {user && (
                        <div className="flex items-center gap-3">
                            <span className="font-mono text-xs text-ink-dim">{user.username}</span>
                            <button
                                onClick={logout}
                                className="btn !px-3 !py-[7px]"
                                title="Log out"
                            >
                                <LogOut size={14} />
                            </button>
                        </div>
                    )}
                </div>
            </header>

            {children}
        </div>
    );
}
