import { useCallback, useEffect, useRef, useState } from 'react';
import type { DfsEvent, DfsEventType } from '../types';

const MAX_EVENTS = 100;

const EVENT_TYPES: DfsEventType[] = [
    'UPLOAD_STARTED',
    'ENCRYPTED',
    'REPLICA_STORED',
    'UPLOAD_COMMITTED',
    'NODE_KILLED',
    'NODE_REVIVED',
    'HEAL_STARTED',
    'REPLICA_RESTORED',
    'HEAL_COMPLETED',
    'FILE_DELETED',
];

/**
 * Subscribes to the backend SSE stream (/api/events/stream). EventSource cannot set
 * headers, so the JWT rides along as a query param — the backend accepts ?token=.
 */
export function useEventStream(token: string | null) {
    const [events, setEvents] = useState<DfsEvent[]>([]);
    const [connected, setConnected] = useState(false);
    const esRef = useRef<EventSource | null>(null);

    useEffect(() => {
        if (!token) return;

        const es = new EventSource(`/api/events/stream?token=${encodeURIComponent(token)}`);
        esRef.current = es;

        const onConnected = () => setConnected(true);
        es.addEventListener('connected', onConnected);
        es.onopen = () => setConnected(true);
        es.onerror = () => setConnected(false);

        const handler = (e: MessageEvent) => {
            try {
                const data = JSON.parse(e.data) as DfsEvent;
                data._id = `${data.timestamp}-${Math.random().toString(36).slice(2, 8)}`;
                setEvents((prev) => [...prev, data].slice(-MAX_EVENTS));
            } catch {
                /* ignore malformed frame */
            }
        };
        EVENT_TYPES.forEach((t) => es.addEventListener(t, handler as EventListener));

        return () => {
            es.close();
            esRef.current = null;
            setConnected(false);
        };
    }, [token]);

    const clear = useCallback(() => setEvents([]), []);
    return { events, connected, clear };
}
