import { useCallback, useEffect, useMemo, useRef, useState, type ReactNode } from 'react';
import { Activity, Server, ShieldCheck, Database } from 'lucide-react';
import { Layout } from '../components/Layout';
import { NodeGrid } from '../components/NodeGrid';
import { EventLog } from '../components/EventLog';
import { UploadPanel } from '../components/UploadPanel';
import { PlacementTable } from '../components/PlacementTable';
import { useEventStream } from '../hooks/useEventStream';
import { authService } from '../services/authService';
import { systemService } from '../services/systemService';
import { fileService } from '../services/fileService';
import { formatBytes } from '../utils/formatters';
import type { FilePlacement, NodeInfo } from '../types';

function Kpi({ label, value, unit, icon, accent }: {
    label: string;
    value: string;
    unit?: string;
    icon: ReactNode;
    accent?: string;
}) {
    return (
        <div className="panel p-4">
            <div className="flex items-center justify-between">
                <span className="lbl">{label}</span>
                <span className="text-ink-faint">{icon}</span>
            </div>
            <div className={`num mt-2 text-[26px] tracking-tight ${accent ?? ''}`}>
                {value}
                {unit && <span className="ml-1 text-[13px] text-ink-dim">{unit}</span>}
            </div>
        </div>
    );
}

function Panel({ title, tag, live, children }: {
    title: string;
    tag?: string;
    live?: boolean;
    children: ReactNode;
}) {
    return (
        <div className="panel p-4">
            <div className="mb-3.5 flex items-center gap-2.5">
                <h2 className="m-0 text-[13px] font-semibold">{title}</h2>
                {tag && <span className={`chip ml-auto ${live ? 'chip-live' : ''}`}>{tag}</span>}
            </div>
            {children}
        </div>
    );
}

export function Dashboard() {
    const token = authService.getToken();
    const { events, connected } = useEventStream(token);

    const [nodes, setNodes] = useState<NodeInfo[]>([]);
    const [placement, setPlacement] = useState<FilePlacement[]>([]);
    const [pulses, setPulses] = useState<Set<string>>(new Set());
    const [busyNode, setBusyNode] = useState<string | null>(null);
    const [busyPath, setBusyPath] = useState<string | null>(null);
    const lastHandled = useRef<string | undefined>(undefined);

    const refresh = useCallback(async () => {
        try {
            const [n, p] = await Promise.all([systemService.getNodes(), systemService.getPlacement()]);
            setNodes(n.sort((a, b) => a.nodeId.localeCompare(b.nodeId, undefined, { numeric: true })));
            setPlacement(p);
        } catch {
            /* transient */
        }
    }, []);

    useEffect(() => {
        refresh();
        const id = setInterval(refresh, 4000);
        return () => clearInterval(id);
    }, [refresh]);

    // react to the newest live event: pulse active nodes and refresh on structural changes
    useEffect(() => {
        const latest = events[events.length - 1];
        if (!latest || latest._id === lastHandled.current) return;
        lastHandled.current = latest._id;

        if ((latest.type === 'REPLICA_STORED' || latest.type === 'REPLICA_RESTORED') && latest.nodeId) {
            const id = latest.nodeId;
            setPulses((prev) => new Set(prev).add(id));
            setTimeout(() => {
                setPulses((prev) => {
                    const next = new Set(prev);
                    next.delete(id);
                    return next;
                });
            }, 1200);
        }
        if (
            ['UPLOAD_COMMITTED', 'NODE_KILLED', 'NODE_REVIVED', 'HEAL_STARTED', 'HEAL_COMPLETED', 'FILE_DELETED'].includes(
                latest.type,
            )
        ) {
            refresh();
        }
    }, [events, refresh]);

    const kill = async (id: string) => {
        setBusyNode(id);
        try {
            await systemService.killNode(id);
            await refresh();
        } finally {
            setBusyNode(null);
        }
    };
    const revive = async (id: string) => {
        setBusyNode(id);
        try {
            await systemService.reviveNode(id);
            await refresh();
        } finally {
            setBusyNode(null);
        }
    };

    const download = async (p: FilePlacement) => {
        setBusyPath(p.filePath);
        try {
            await fileService.download(p.fileName);
        } catch {
            /* ignore */
        } finally {
            setBusyPath(null);
        }
    };
    const remove = async (p: FilePlacement) => {
        setBusyPath(p.filePath);
        try {
            await fileService.remove(p.fileName);
            await refresh();
        } finally {
            setBusyPath(null);
        }
    };

    const online = nodes.filter((n) => n.healthy).length;
    const encryptedBytes = useMemo(() => placement.reduce((sum, p) => sum + (p.size || 0), 0), [placement]);
    const factor = placement[0]?.replicationFactor ?? 3;

    // authoritative per-node replica counts derived from placement (matches the Files table)
    const nodeCounts = useMemo(() => {
        const m: Record<string, number> = {};
        placement.forEach((p) => p.nodeIds.forEach((id) => { m[id] = (m[id] ?? 0) + 1; }));
        return m;
    }, [placement]);

    return (
        <Layout connected={connected}>
            {/* KPI ribbon */}
            <div className="mb-4 grid grid-cols-2 gap-3 lg:grid-cols-4">
                <Kpi label="Nodes online" value={`${online}`} unit={`/ ${nodes.length}`} icon={<Server size={15} />} />
                <Kpi label="Replication" value={`×${factor}`} unit="factor" icon={<ShieldCheck size={15} />} />
                <Kpi label="Files" value={`${placement.length}`} icon={<Activity size={15} />} />
                <Kpi
                    label="Encrypted"
                    value={formatBytes(encryptedBytes)}
                    icon={<Database size={15} />}
                    accent="text-violet"
                />
            </div>

            {/* main grid */}
            <div className="grid gap-4 lg:grid-cols-[1.1fr_0.9fr]">
                <div className="flex flex-col gap-4">
                    <Panel title="Upload Pipeline" tag="live" live>
                        <UploadPanel events={events} onUploaded={refresh} />
                    </Panel>
                    <Panel title="Event Stream" tag="SSE · /events">
                        <EventLog events={events} />
                    </Panel>
                </div>

                <Panel title="Cluster Topology" tag="real-time" live>
                    <NodeGrid nodes={nodes} counts={nodeCounts} pulses={pulses} busy={busyNode} onKill={kill} onRevive={revive} />
                    <p className="mt-3.5 font-mono text-[11px] leading-relaxed text-ink-dim">
                        Kill a node to watch the cluster detect the failure and re-replicate its files to restore{' '}
                        <span className="text-good">{factor}/{factor}</span> — self-healing, live.
                    </p>
                </Panel>
            </div>

            {/* files */}
            <div className="mt-4">
                <Panel title="Files" tag="per-user · AES-256">
                    <PlacementTable
                        placement={placement}
                        nodes={nodes}
                        busyPath={busyPath}
                        onDownload={download}
                        onDelete={remove}
                    />
                </Panel>
            </div>
        </Layout>
    );
}
