/// <reference types="vite/client" />
// Tipos del cliente de Vite: declaran los imports de assets (*.css, *.svg, imágenes, ?raw...)
// e import.meta.env. Sin esto, TS no reconoce `import './index.css'` (error TS2882).

// Variables de frontend/.env. Todo lo que empieza con VITE_ termina en el JavaScript que
// descarga el navegador: nunca pongas un secreto aquí.
interface ImportMetaEnv {
    readonly VITE_API_URL?: string;
}