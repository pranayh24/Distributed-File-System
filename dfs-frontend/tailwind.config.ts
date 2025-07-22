/** @type {import('tailwindcss').Config} */
export default {
    darkMode: 'class',
    content: [
        "./index.html",
        "./src/**/*.{js,ts,jsx,tsx}",
    ],
    theme: {
        fontFamily: {
            sans: ['Inter', 'ui-sans-serif', 'system-ui'],
        },
        extend: {
            colors: {
                primary: {
                    50: '#f0f7ff',
                    100: '#e0edff',
                    500: '#437ef7',
                    600: '#1d5ddb',
                    700: '#103177',
                },
            },
            boxShadow: {
                glass: '0 8px 32px 0 rgba(31, 38, 135, 0.2)',
            },
            backdropBlur: {
                md: '8px',
            },
            borderRadius: {
                xl: '1rem',
            },
        },
    },
    plugins: [],
}