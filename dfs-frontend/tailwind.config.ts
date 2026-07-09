/** @type {import('tailwindcss').Config} */
module.exports = {
    content: [
        "./index.html",
        "./src/**/*.{js,jsx,ts,tsx}",
    ],
    theme: {
        extend: {
            colors: {
                bg: '#080b11',
                panel: {
                    DEFAULT: '#0e141f',
                    2: '#131c2a',
                    3: '#182234',
                },
                line: {
                    DEFAULT: '#1c2838',
                    bright: '#2b3e56',
                    b: '#28394f',
                },
                ink: {
                    DEFAULT: '#e8eef5',
                    dim: '#8697ad',
                    faint: '#556377',
                },
                cyan: { DEFAULT: '#34e5d0', deep: '#1fb9a8' },
                good: '#3fd07a',
                warn: '#e3b341',
                bad: '#f9556c',
                violet: '#9a7cf6',
            },
            fontFamily: {
                mono: ['ui-monospace', 'SF Mono', 'Cascadia Code', 'JetBrains Mono', 'Consolas', 'monospace'],
                sans: ['Segoe UI', 'system-ui', '-apple-system', 'Helvetica Neue', 'Arial', 'sans-serif'],
            },
            boxShadow: {
                'glow-cyan': '0 0 22px rgba(52,229,208,0.35)',
                'glow-violet': '0 0 22px rgba(154,124,246,0.35)',
            },
            keyframes: {
                beat: {
                    '0%': { boxShadow: '0 0 0 0 rgba(63,208,122,0.5)' },
                    '70%': { boxShadow: '0 0 0 7px rgba(63,208,122,0)' },
                    '100%': { boxShadow: '0 0 0 0 rgba(63,208,122,0)' },
                },
                logln: {
                    from: { opacity: '0', transform: 'translateY(3px)' },
                    to: { opacity: '1', transform: 'translateY(0)' },
                },
                flow: { to: { transform: 'translateX(100%)' } },
            },
            animation: {
                beat: 'beat 2.4s infinite',
                logln: 'logln 0.25s ease',
            },
        },
    },
    plugins: [],
}
