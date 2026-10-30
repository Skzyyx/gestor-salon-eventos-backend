import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Plugin nativo de Tailwind v4. 
// Reemplaza la necesidad de tailwind.config.js y postcss.config.js
import tailwindcss from '@tailwindcss/vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    tailwindcss(),
    react(),
  ],
})
