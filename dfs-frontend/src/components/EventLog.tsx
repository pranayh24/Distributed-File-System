import { useEffect, useRef } from 'react';
import type { DfsEvent, DfsEventType } from '../types';
import { clockTime } from '../utils/formatters';

const COLOR: Record<DfsEventType, string> = {
    UPLOAD_STARTED: 'text-cyan',
    ENCRYPTED: 'text-violet',
    REPLICA_STORED: 'text-good',
    UPLOAD_COMMITTED: 'text-good',
    NODE_KILLED: 'text-bad',
    NODE_REVIVED: 'text-cyan',
    HEAL_STARTED: 'text-warn',
    REPLICA_RESTORED: 'text-good',
    HEAL_COMPLETED: 'text-good',
    FILE_DELETED: 'text-ink-dim',
};

export function EventLog({ events }: { events: DfsEvent[] }) {
    const ref = useRef<HTMLDivElement>(null);
    useEffect(() => {
        if (ref.current) ref.current.scrollTop = ref.current.scrollHeight;
    }, [events.length]);

    return (
        <div
            ref={ref}
            className="h-[220px] overflow-y-auto rounded-lg border border-line bg-bg/60 p-3 font-mono text-[11.5px] leading-[1.9]"
        >
            {events.length === 0 && <div className="text-ink-faint">Waiting for cluster activity…</div>}
            {events.map((e) => (
                <div key={e._id} className="flex animate-logln gap-2.5 whitespace-nowrap">
                    <span className="shrink-0 text-ink-faint">{clockTime(e.timestamp)}</span>
                    <span className={`${COLOR[e.type] ?? 'text-ink-dim'} overflow-hidden text-ellipsis`}>
                        {e.message}
                    </span>
                </div>
            ))}
        </div>
    );
}
