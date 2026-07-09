import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export function Login() {
    const [mode, setMode] = useState<'login' | 'register'>('login');
    const [username, setUsername] = useState('');
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState<string | null>(null);
    const [busy, setBusy] = useState(false);

    const { login, register } = useAuth();
    const navigate = useNavigate();

    async function onSubmit(e: FormEvent) {
        e.preventDefault();
        setError(null);
        setBusy(true);
        try {
            if (mode === 'login') {
                await login({ username, password });
            } else {
                await register({ username, email, password });
            }
            navigate('/dashboard');
        } catch (err: any) {
            setError(err.message || 'Something went wrong');
        } finally {
            setBusy(false);
        }
    }

    const tab = (m: 'login' | 'register', label: string) => (
        <button
            type="button"
            onClick={() => { setMode(m); setError(null); }}
            className={`flex-1 rounded-lg py-2 font-mono text-[11px] uppercase tracking-[0.1em] transition-colors ${
                mode === m ? 'bg-cyan/[0.12] text-cyan' : 'text-ink-faint hover:text-ink-dim'
            }`}
        >
            {label}
        </button>
    );

    const field = (
        label: string,
        value: string,
        set: (v: string) => void,
        type = 'text',
    ) => (
        <label className="block">
            <span className="lbl">{label}</span>
            <input
                type={type}
                value={value}
                onChange={(e) => set(e.target.value)}
                required
                className="mt-1.5 w-full rounded-lg border border-line-b bg-bg/60 px-3 py-2.5 font-mono text-sm text-ink outline-none transition-colors focus:border-cyan focus:shadow-glow-cyan"
            />
        </label>
    );

    return (
        <div className="relative z-[1] flex min-h-screen items-center justify-center px-4">
            <div className="w-full max-w-[400px]">
                <div className="mb-7 flex flex-col items-center gap-3 text-center">
                    <div className="logo-mark !h-12 !w-12" aria-hidden="true" />
                    <div>
                        <h1 className="m-0 text-xl font-semibold tracking-tight">
                            DFS<span className="text-cyan">·</span>LIVE
                        </h1>
                        <p className="mt-1 font-mono text-[11px] tracking-[0.1em] text-ink-faint">
                            DISTRIBUTED FILE SYSTEM CONTROL
                        </p>
                    </div>
                </div>

                <div className="panel p-6">
                    <div className="mb-5 flex gap-1 rounded-xl border border-line bg-bg/40 p-1">
                        {tab('login', 'Sign in')}
                        {tab('register', 'Register')}
                    </div>

                    <form onSubmit={onSubmit} className="flex flex-col gap-4">
                        {field('Username', username, setUsername)}
                        {mode === 'register' && field('Email', email, setEmail, 'email')}
                        {field('Password', password, setPassword, 'password')}

                        {error && (
                            <div className="rounded-lg border border-bad/30 bg-bad/[0.08] px-3 py-2 font-mono text-xs text-bad">
                                {error}
                            </div>
                        )}

                        <button type="submit" disabled={busy} className="btn btn-primary justify-center py-2.5">
                            {busy ? 'Please wait…' : mode === 'login' ? 'Sign in' : 'Create account'}
                        </button>
                    </form>
                </div>

                <p className="mt-5 text-center font-mono text-[10.5px] tracking-[0.08em] text-ink-faint">
                    STATELESS JWT AUTH · AES-256 ENCRYPTED STORAGE
                </p>
            </div>
        </div>
    );
}
