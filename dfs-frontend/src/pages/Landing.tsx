import { Link } from 'react-router-dom';
import {
    ArrowRight,
    Boxes,
    Lock,
    Radio,
    Hash,
    KeyRound,
    HeartPulse,
    Cpu,
    Database,
    Server,
    Network,
    Zap,
} from 'lucide-react';
import { Reveal } from '../components/Reveal';

function HeroCluster() {
    const sats = [
        { x: 170, y: 28 },
        { x: 262, y: 82 },
        { x: 262, y: 188 },
        { x: 170, y: 242 },
        { x: 78, y: 188 },
        { x: 78, y: 82 },
    ];
    return (
        <svg viewBox="0 0 340 270" className="h-full w-full" aria-hidden="true">
            {sats.map((s, i) => (
                <line
                    key={`l${i}`}
                    x1="170"
                    y1="135"
                    x2={s.x}
                    y2={s.y}
                    stroke="#34e5d0"
                    strokeWidth="1.5"
                    strokeOpacity="0.5"
                    strokeDasharray="4 6"
                    style={{ animation: `dashmove ${1.6 + i * 0.2}s linear infinite` }}
                />
            ))}
            {sats.map((s, i) => (
                <g key={`n${i}`} style={{ animation: `floaty ${3 + i * 0.4}s ease-in-out infinite` }}>
                    <circle cx={s.x} cy={s.y} r="15" fill="#131c2a" stroke="#28394f" strokeWidth="1" />
                    <circle cx={s.x} cy={s.y} r="4" fill="#34e5d0">
                        <animate attributeName="opacity" values="0.4;1;0.4" dur={`${2 + i * 0.3}s`} repeatCount="indefinite" />
                    </circle>
                </g>
            ))}
            <rect x="146" y="111" width="48" height="48" rx="12" fill="#182234" stroke="#34e5d0" strokeOpacity="0.5" />
            <rect x="146" y="111" width="48" height="48" rx="12" fill="none" stroke="#34e5d0" strokeOpacity="0.15" strokeWidth="8" />
            <circle cx="170" cy="135" r="6" fill="#34e5d0">
                <animate attributeName="opacity" values="1;0.5;1" dur="1.8s" repeatCount="indefinite" />
            </circle>
        </svg>
    );
}

const PIPELINE = [
    { step: 'Arrives', body: 'Your file lands at the coordinator over a signed, authenticated request.' },
    { step: 'Locked', body: 'It gets encrypted with your own key. After this point, only scrambled bytes move around.' },
    { step: 'Copied', body: 'Those bytes get written to three separate nodes, picked from across the cluster.' },
    { step: 'Recorded', body: 'The system notes exactly where every copy lives, and your file is safe.' },
];

const STACK = [
    { icon: <Cpu size={15} />, label: 'Java 17 · Spring Boot' },
    { icon: <Database size={15} />, label: 'PostgreSQL' },
    { icon: <Server size={15} />, label: 'In-process node cluster' },
    { icon: <Network size={15} />, label: 'React · TypeScript' },
    { icon: <Zap size={15} />, label: 'Docker · Caddy' },
];

export function Landing() {
    return (
        <div className="relative z-[1]">
            {/* nav */}
            <nav className="mx-auto flex max-w-[1180px] items-center gap-3 px-5 py-5 sm:px-8">
                <div className="logo-mark" aria-hidden="true" />
                <span className="font-semibold tracking-tight">
                    DFS<span className="text-cyan">·</span>LIVE
                </span>
                <Link to="/dashboard" className="btn btn-primary ml-auto !py-2">
                    Launch console <ArrowRight size={14} />
                </Link>
            </nav>

            {/* hero */}
            <header className="mx-auto grid max-w-[1180px] items-center gap-8 px-5 pb-10 pt-8 sm:px-8 lg:grid-cols-[1.1fr_0.9fr] lg:pt-14">
                <div>
                    <span className="chip chip-live">Distributed systems, made visible</span>
                    <h1 className="mt-5 text-balance text-4xl font-semibold leading-[1.08] tracking-tight sm:text-5xl lg:text-[54px]">
                        A file system you can <span className="text-gradient">actually watch.</span>
                    </h1>
                    <p className="mt-5 max-w-[52ch] text-[15px] leading-relaxed text-ink-dim">
                        Most storage is a black box. You upload something and just trust it is safe somewhere. I built
                        DFS·LIVE to open that box up. It encrypts every file, spreads copies across a cluster of nodes,
                        and shows you all of it as it happens, right down to a node dying and the cluster quietly
                        rebuilding what was lost.
                    </p>
                    <div className="mt-7 flex flex-wrap items-center gap-3">
                        <Link to="/dashboard" className="btn btn-primary !py-2.5">
                            Try the live console <ArrowRight size={15} />
                        </Link>
                        <a href="#how" className="btn !py-2.5">
                            How it works
                        </a>
                    </div>
                    <div className="mt-8 flex flex-wrap gap-x-6 gap-y-2 font-mono text-[11px] tracking-[0.08em] text-ink-faint">
                        <span>8 NODES</span>
                        <span className="text-cyan">×3 COPIES</span>
                        <span className="text-violet">AES-256</span>
                        <span className="text-good">SELF-HEALING</span>
                        <span>LIVE FEED</span>
                    </div>
                </div>
                <div className="relative mx-auto h-[280px] w-full max-w-[380px]">
                    <div className="absolute inset-0 rounded-3xl bg-cyan/[0.04] blur-2xl" />
                    <div className="panel relative h-full p-4">
                        <div className="mb-1 flex items-center justify-between">
                            <span className="lbl">Cluster</span>
                            <span className="chip chip-live">replicating</span>
                        </div>
                        <HeroCluster />
                    </div>
                </div>
            </header>

            {/* thesis */}
            <section className="border-y border-line/60 bg-panel/30">
                <Reveal className="mx-auto max-w-[1180px] px-5 py-14 sm:px-8">
                    <p className="max-w-[68ch] text-balance text-2xl font-medium leading-snug tracking-tight sm:text-[28px]">
                        I kept reading about replication and fault tolerance, but never actually{' '}
                        <span className="text-cyan">seeing</span> them. So I built a system that puts them on screen.
                    </p>
                    <p className="mt-4 max-w-[58ch] text-[15px] text-ink-dim">
                        Watch scrambled bytes fan out to nodes, a copy count climb to three, a node go down, and its
                        files show up somewhere else a second later. The hard parts of a distributed system, out in the
                        open where you can see them.
                    </p>
                </Reveal>
            </section>

            {/* features — bento */}
            <section className="mx-auto max-w-[1180px] px-5 py-16 sm:px-8">
                <Reveal>
                    <span className="lbl">What it does</span>
                    <h2 className="mt-2 text-balance text-2xl font-semibold tracking-tight sm:text-3xl">
                        The parts that actually matter
                    </h2>
                </Reveal>

                <div className="mt-9 grid gap-4 lg:grid-cols-6">
                    {/* replication */}
                    <Reveal className="lg:col-span-3">
                        <div className="panel flex h-full flex-col p-6">
                            <div className="flex items-center gap-2 text-cyan">
                                <Boxes size={18} />
                                <span className="font-mono text-[11px] uppercase tracking-[0.1em]">Replication</span>
                            </div>
                            <h3 className="mt-3 text-lg font-semibold">Three copies, never just one</h3>
                            <p className="mt-2 text-[13.5px] leading-relaxed text-ink-dim">
                                Every file goes onto three separate nodes. You can see exactly which three, and if one
                                drops off, your file is still there on the other two.
                            </p>
                            <div className="mt-auto flex items-center gap-2 pt-5 font-mono text-[11px]">
                                <span className="text-ink-faint">report.pdf</span>
                                <span className="flex gap-1">
                                    <b className="h-2.5 w-2.5 rounded-sm bg-good shadow-[0_0_6px_rgba(63,208,122,0.5)]" />
                                    <b className="h-2.5 w-2.5 rounded-sm bg-good shadow-[0_0_6px_rgba(63,208,122,0.5)]" />
                                    <b className="h-2.5 w-2.5 rounded-sm bg-good shadow-[0_0_6px_rgba(63,208,122,0.5)]" />
                                </span>
                                <span className="text-ink-dim">node2 · node5 · node6</span>
                            </div>
                        </div>
                    </Reveal>

                    {/* encryption */}
                    <Reveal className="lg:col-span-3" delay={80}>
                        <div className="panel flex h-full flex-col p-6">
                            <div className="flex items-center gap-2 text-violet">
                                <Lock size={18} />
                                <span className="font-mono text-[11px] uppercase tracking-[0.1em]">Encryption</span>
                            </div>
                            <h3 className="mt-3 text-lg font-semibold">The nodes never see your files</h3>
                            <p className="mt-2 text-[13.5px] leading-relaxed text-ink-dim">
                                Everything is locked with your own AES-256 key before it leaves. The nodes only ever
                                hold scrambled bytes, so even they have no idea what they are storing.
                            </p>
                            <div className="mt-auto flex items-center gap-2 pt-5 font-mono text-[11px]">
                                <span className="text-ink-dim">report.pdf</span>
                                <ArrowRight size={12} className="text-ink-faint" />
                                <span className="truncate text-violet">8f3a1c9e4d7b02c15a…</span>
                            </div>
                        </div>
                    </Reveal>

                    {/* observability */}
                    <Reveal className="lg:col-span-2" delay={0}>
                        <div className="panel h-full p-6">
                            <div className="flex items-center gap-2 text-cyan">
                                <Radio size={18} />
                                <span className="font-mono text-[11px] uppercase tracking-[0.1em]">Live feed</span>
                            </div>
                            <h3 className="mt-3 text-[15px] font-semibold">Nothing is hidden</h3>
                            <p className="mt-2 text-[13px] leading-relaxed text-ink-dim">
                                A live stream shows every move the system makes, the moment it makes it. Encryptions,
                                copies, failures, recoveries.
                            </p>
                        </div>
                    </Reveal>

                    {/* integrity */}
                    <Reveal className="lg:col-span-2" delay={80}>
                        <div className="panel h-full p-6">
                            <div className="flex items-center gap-2 text-good">
                                <Hash size={18} />
                                <span className="font-mono text-[11px] uppercase tracking-[0.1em]">Integrity</span>
                            </div>
                            <h3 className="mt-3 text-[15px] font-semibold">Corruption shows up</h3>
                            <p className="mt-2 text-[13px] leading-relaxed text-ink-dim">
                                Each file carries a SHA-256 fingerprint, so if a single byte ever goes wrong in transit,
                                it does not slip by unnoticed.
                            </p>
                        </div>
                    </Reveal>

                    {/* auth */}
                    <Reveal className="lg:col-span-2" delay={160}>
                        <div className="panel h-full p-6">
                            <div className="flex items-center gap-2 text-violet">
                                <KeyRound size={18} />
                                <span className="font-mono text-[11px] uppercase tracking-[0.1em]">Your space</span>
                            </div>
                            <h3 className="mt-3 text-[15px] font-semibold">Signed in, walled off</h3>
                            <p className="mt-2 text-[13px] leading-relaxed text-ink-dim">
                                You get a signed token instead of a session, and your files live in your own space with
                                your own key. Nobody else sees them.
                            </p>
                        </div>
                    </Reveal>
                </div>
            </section>

            {/* how it works */}
            <section id="how" className="border-y border-line/60 bg-panel/30">
                <div className="mx-auto max-w-[1180px] px-5 py-16 sm:px-8">
                    <Reveal>
                        <span className="lbl">Under the hood</span>
                        <h2 className="mt-2 text-balance text-2xl font-semibold tracking-tight sm:text-3xl">
                            What actually happens when you upload
                        </h2>
                    </Reveal>
                    <div className="mt-9 grid gap-x-4 gap-y-6 md:grid-cols-4">
                        {PIPELINE.map((p, i) => (
                            <Reveal key={p.step} delay={i * 90}>
                                <div className="border-l-2 border-cyan/40 pl-4">
                                    <div className="num text-[12px] text-cyan">step 0{i + 1}</div>
                                    <h3 className="mt-1.5 text-base font-semibold">{p.step}</h3>
                                    <p className="mt-1.5 text-[13px] leading-relaxed text-ink-dim">{p.body}</p>
                                </div>
                            </Reveal>
                        ))}
                    </div>
                </div>
            </section>

            {/* self-heal */}
            <section className="mx-auto max-w-[1180px] px-5 py-16 sm:px-8">
                <div className="grid items-center gap-8 lg:grid-cols-[0.85fr_1.15fr]">
                    <Reveal>
                        <div className="flex items-center gap-2 text-good">
                            <HeartPulse size={18} />
                            <span className="lbl !text-good/80">The best part</span>
                        </div>
                        <h2 className="mt-3 text-balance text-2xl font-semibold tracking-tight sm:text-3xl">
                            Kill a node and it fixes itself
                        </h2>
                        <p className="mt-4 text-[14px] leading-relaxed text-ink-dim">
                            Take down any node from the console and watch what happens. The health check notices it is
                            gone, the files that lived there drop below three copies, and the system quietly pulls them
                            from a surviving copy onto a healthy node until there are three again. Nothing lost. And if
                            you take down every node holding a file, it genuinely becomes unavailable, which is the whole
                            reason for keeping copies in the first place.
                        </p>
                        <Link to="/dashboard" className="btn btn-primary mt-6 !py-2.5">
                            Go break something <ArrowRight size={15} />
                        </Link>
                    </Reveal>
                    <Reveal delay={120}>
                        <div className="panel p-5 font-mono text-[12px] leading-[2]">
                            <div className="text-bad">node4 just went down</div>
                            <div className="text-warn">report.pdf is down to 2 of 3 copies</div>
                            <div className="text-warn">pulling a fresh copy from a node that still has it…</div>
                            <div className="text-good">copied report.pdf onto node7 (from node2)</div>
                            <div className="text-good">report.pdf is back to 3 of 3. all good.</div>
                            <div className="mt-1 text-ink-faint">all of that, in about a second, live on screen</div>
                        </div>
                    </Reveal>
                </div>
            </section>

            {/* stack */}
            <section className="border-t border-line/60 bg-panel/30">
                <div className="mx-auto max-w-[1180px] px-5 py-14 sm:px-8">
                    <Reveal>
                        <span className="lbl">Built with</span>
                        <div className="mt-4 flex flex-wrap gap-2.5">
                            {STACK.map((s) => (
                                <span
                                    key={s.label}
                                    className="inline-flex items-center gap-2 rounded-lg border border-line-b bg-panel-3 px-3 py-2 font-mono text-[12px] text-ink-dim"
                                >
                                    <span className="text-cyan">{s.icon}</span>
                                    {s.label}
                                </span>
                            ))}
                        </div>
                    </Reveal>
                </div>
            </section>

            {/* CTA */}
            <section className="mx-auto max-w-[1180px] px-5 py-20 text-center sm:px-8">
                <Reveal>
                    <h2 className="text-balance text-3xl font-semibold tracking-tight sm:text-4xl">
                        Upload a file. Kill a node. <span className="text-gradient">Watch it come back.</span>
                    </h2>
                    <p className="mx-auto mt-4 max-w-[44ch] text-[15px] text-ink-dim">
                        The console is live and it is all real. Make an account and go poke at the cluster yourself.
                    </p>
                    <Link to="/dashboard" className="btn btn-primary mx-auto mt-7 !px-6 !py-3">
                        Try the live console <ArrowRight size={16} />
                    </Link>
                </Reveal>
            </section>

            <footer className="border-t border-line/60">
                <div className="mx-auto flex max-w-[1180px] flex-wrap items-center gap-3 px-5 py-6 font-mono text-[11px] text-ink-faint sm:px-8">
                    <div className="logo-mark !h-6 !w-6" aria-hidden="true" />
                    <span>DFS·LIVE</span>
                    <span className="ml-auto">encrypted, replicated, and healing on its own</span>
                </div>
            </footer>
        </div>
    );
}
