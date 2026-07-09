import { Download, Trash2, Lock } from 'lucide-react';
import type { FilePlacement, NodeInfo } from '../types';
import { formatBytes } from '../utils/formatters';

interface Props {
    placement: FilePlacement[];
    nodes: NodeInfo[];
    busyPath: string | null;
    onDownload: (p: FilePlacement) => void;
    onDelete: (p: FilePlacement) => void;
}

export function PlacementTable({ placement, nodes, busyPath, onDownload, onDelete }: Props) {
    const healthy = new Set(nodes.filter((n) => n.healthy).map((n) => n.nodeId));

    return (
        <div className="overflow-x-auto">
            <table className="w-full min-w-[680px] border-collapse">
                <thead>
                    <tr>
                        {['Name', 'Size', 'Replicas', 'Placement', 'Encryption', ''].map((h) => (
                            <th
                                key={h}
                                className="border-b border-line px-3 py-2 text-left font-mono text-[10px] uppercase tracking-[0.12em] text-ink-faint"
                            >
                                {h}
                            </th>
                        ))}
                    </tr>
                </thead>
                <tbody>
                    {placement.length === 0 && (
                        <tr>
                            <td colSpan={6} className="px-3 py-8 text-center font-mono text-xs text-ink-faint">
                                No files yet — upload one to watch it replicate across the cluster.
                            </td>
                        </tr>
                    )}
                    {placement.map((p) => {
                        const healthyReplicas = p.nodeIds.filter((id) => healthy.has(id)).length;
                        const ok = healthyReplicas >= p.replicationFactor;
                        return (
                            <tr key={p.filePath} className="border-b border-line/50 hover:bg-cyan/[0.03]">
                                <td className="px-3 py-3 font-mono text-[12.5px]">{p.fileName}</td>
                                <td className="num px-3 py-3 text-ink-dim">{formatBytes(p.size)}</td>
                                <td className="px-3 py-3">
                                    <span className="inline-flex items-center gap-1.5">
                                        {Array.from({ length: p.replicationFactor }).map((_, i) => (
                                            <span
                                                key={i}
                                                className={`h-2.5 w-2.5 rounded-sm ${
                                                    i < healthyReplicas
                                                        ? 'bg-good shadow-[0_0_6px_rgba(63,208,122,0.5)]'
                                                        : 'bg-line-b'
                                                }`}
                                            />
                                        ))}
                                        <span
                                            className={`num ml-1 text-[11px] ${ok ? 'text-ink-dim' : 'text-warn'}`}
                                        >
                                            {healthyReplicas}/{p.replicationFactor}
                                        </span>
                                    </span>
                                </td>
                                <td className="px-3 py-3">
                                    <div className="flex flex-wrap gap-1">
                                        {p.nodeIds.map((id) => (
                                            <span
                                                key={id}
                                                className={`font-mono text-[10px] rounded px-1.5 py-0.5 ${
                                                    healthy.has(id)
                                                        ? 'bg-good/[0.1] text-good'
                                                        : 'bg-bad/[0.12] text-bad line-through'
                                                }`}
                                            >
                                                {id}
                                            </span>
                                        ))}
                                    </div>
                                </td>
                                <td className="px-3 py-3">
                                    <span className="inline-flex items-center gap-1.5 font-mono text-[11px] text-violet">
                                        <Lock size={12} /> AES-256
                                    </span>
                                </td>
                                <td className="px-3 py-3">
                                    <div className="flex items-center gap-1.5">
                                        <button
                                            onClick={() => onDownload(p)}
                                            disabled={busyPath === p.filePath}
                                            className="btn !px-2 !py-1.5"
                                            title="Download & decrypt"
                                        >
                                            <Download size={13} />
                                        </button>
                                        <button
                                            onClick={() => onDelete(p)}
                                            disabled={busyPath === p.filePath}
                                            className="btn btn-danger !px-2 !py-1.5"
                                            title="Delete from all nodes"
                                        >
                                            <Trash2 size={13} />
                                        </button>
                                    </div>
                                </td>
                            </tr>
                        );
                    })}
                </tbody>
            </table>
        </div>
    );
}
