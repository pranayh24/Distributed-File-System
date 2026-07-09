import { useMemo, useRef, useState } from 'react';
import { UploadCloud, FileUp } from 'lucide-react';
import type { DfsEvent } from '../types';
import { fileService } from '../services/fileService';
import { formatBytes } from '../utils/formatters';

const STEPS = ['Received', 'Encrypted', 'Replicated', 'Committed'] as const;
const MAX_UPLOAD = 150 * 1024 * 1024; // keep in sync with backend multipart limit

export function UploadPanel({ events, onUploaded }: { events: DfsEvent[]; onUploaded: () => void }) {
    const [file, setFile] = useState<File | null>(null);
    const [activeName, setActiveName] = useState<string | null>(null);
    const [busy, setBusy] = useState(false);
    const [msg, setMsg] = useState<string | null>(null);
    const inputRef = useRef<HTMLInputElement>(null);

    const { stepIndex, replicaInfo } = useMemo(() => {
        if (!activeName) return { stepIndex: -1, replicaInfo: '' };
        const evs = events.filter((e) => e.fileName === activeName);
        let idx = -1;
        let replicas = 0;
        let factor = 3;
        for (const e of evs) {
            if (e.type === 'UPLOAD_STARTED') idx = Math.max(idx, 0);
            if (e.type === 'ENCRYPTED') idx = Math.max(idx, 1);
            if (e.type === 'REPLICA_STORED') {
                idx = Math.max(idx, 2);
                replicas = e.replicaIndex ?? replicas;
                factor = e.replicationFactor ?? factor;
            }
            if (e.type === 'UPLOAD_COMMITTED') {
                idx = 3;
                replicas = e.replicas ?? replicas;
                factor = e.replicationFactor ?? factor;
            }
        }
        return { stepIndex: idx, replicaInfo: replicas ? `${replicas}/${factor}` : '' };
    }, [events, activeName]);

    async function doUpload() {
        if (!file || busy) return;
        if (file.size > MAX_UPLOAD) {
            setMsg(`${file.name} is ${formatBytes(file.size)} — over the 150 MB demo limit.`);
            return;
        }
        setBusy(true);
        setMsg(null);
        setActiveName(file.name);
        try {
            const res = await fileService.upload(file, '/', 3);
            if (res.success) {
                setMsg(`Committed ${file.name}`);
                setFile(null);
                if (inputRef.current) inputRef.current.value = '';
                onUploaded();
            } else {
                setMsg(res.error || res.message || 'Upload failed');
            }
        } catch (e: any) {
            const serverMsg = e.response?.data?.error || e.response?.data?.message;
            setMsg(
                serverMsg ||
                    (!e.response
                        ? 'Upload failed — the file may exceed the 512 MB limit, or the server is unreachable.'
                        : e.message) ||
                    'Upload failed',
            );
        } finally {
            setBusy(false);
        }
    }

    return (
        <div className="flex flex-col gap-4">
            {/* pipeline */}
            <div className="flex items-stretch gap-1">
                {STEPS.map((label, i) => {
                    const done = stepIndex > i || (i === 3 && stepIndex === 3);
                    const active = stepIndex === i && !(i === 3 && stepIndex === 3);
                    const isEncrypt = i === 1;
                    return (
                        <div key={label} className="flex flex-1 items-center gap-1">
                            <div
                                className={`flex-1 rounded-lg border px-2 py-2 text-center transition-all ${
                                    done
                                        ? 'border-good/40 bg-good/[0.06] text-good'
                                        : active
                                          ? isEncrypt
                                              ? 'border-violet/50 bg-violet/[0.08] text-violet shadow-glow-violet'
                                              : 'border-cyan/50 bg-cyan/[0.08] text-cyan shadow-glow-cyan'
                                          : 'border-line-b bg-panel-3 text-ink-faint'
                                }`}
                            >
                                <div className="font-mono text-[10px] uppercase tracking-[0.05em]">{label}</div>
                                <div className="num mt-0.5 h-3 text-[9.5px]">
                                    {i === 2 && (active || done) ? replicaInfo : ''}
                                </div>
                            </div>
                            {i < STEPS.length - 1 && <div className="h-[2px] w-3 shrink-0 bg-line-bright" />}
                        </div>
                    );
                })}
            </div>

            {/* control */}
            <div className="flex flex-wrap items-center gap-3">
                <input
                    ref={inputRef}
                    type="file"
                    onChange={(e) => setFile(e.target.files?.[0] ?? null)}
                    className="hidden"
                    id="file-input"
                />
                <label htmlFor="file-input" className="btn cursor-pointer">
                    <FileUp size={14} /> Choose file
                </label>
                <span className="font-mono text-xs text-ink-dim">
                    {file ? `${file.name} · ${formatBytes(file.size)}` : 'no file selected'}
                </span>
                <button
                    onClick={doUpload}
                    disabled={!file || busy}
                    className="btn btn-primary ml-auto"
                >
                    <UploadCloud size={14} /> {busy ? 'Uploading…' : 'Upload & replicate'}
                </button>
            </div>

            {msg && <div className="font-mono text-[11px] text-ink-dim">{msg}</div>}
        </div>
    );
}
