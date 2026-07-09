import { Power, RotateCcw } from 'lucide-react';
import type { NodeInfo } from '../types';

interface Props {
    nodes: NodeInfo[];
    counts: Record<string, number>;
    pulses: Set<string>;
    busy: string | null;
    onKill: (id: string) => void;
    onRevive: (id: string) => void;
}

export function NodeGrid({ nodes, counts, pulses, busy, onKill, onRevive }: Props) {
    return (
        <div className="grid grid-cols-2 gap-2.5 sm:grid-cols-4">
            {nodes.map((n) => {
                const active = pulses.has(n.nodeId) && n.healthy;
                return (
                    <div
                        key={n.nodeId}
                        className={`relative rounded-xl border p-3 transition-all duration-300 ${
                            !n.healthy
                                ? 'border-bad/40 bg-panel opacity-70'
                                : active
                                  ? 'border-cyan/60 bg-panel-2 shadow-glow-cyan'
                                  : 'border-line bg-panel'
                        }`}
                    >
                        <div className="flex items-center justify-between">
                            <span className="font-mono text-[13px] text-ink">{n.nodeId}</span>
                            <span
                                className={`h-2 w-2 rounded-full ${
                                    n.healthy
                                        ? 'bg-good shadow-[0_0_8px_#3fd07a]'
                                        : 'bg-bad shadow-[0_0_8px_#f9556c]'
                                }`}
                            />
                        </div>
                        <div className="mt-0.5 font-mono text-[10px] text-ink-faint">:{n.port}</div>
                        <div className="num mt-2 text-[11px] text-ink-dim">
                            {counts[n.nodeId] ?? 0} replicas
                        </div>
                        <button
                            disabled={busy === n.nodeId}
                            onClick={() => (n.healthy ? onKill(n.nodeId) : onRevive(n.nodeId))}
                            className={`btn mt-2.5 w-full justify-center !py-1.5 !text-[10px] ${
                                n.healthy ? 'btn-danger' : ''
                            }`}
                        >
                            {busy === n.nodeId ? (
                                '…'
                            ) : n.healthy ? (
                                <>
                                    <Power size={11} /> Kill
                                </>
                            ) : (
                                <>
                                    <RotateCcw size={11} /> Revive
                                </>
                            )}
                        </button>
                    </div>
                );
            })}
        </div>
    );
}
