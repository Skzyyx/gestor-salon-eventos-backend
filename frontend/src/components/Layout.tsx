// Outlet funciona como un "Portal" o un proyector de cine.
// React Router agarrará la página a la que navegues (ej. /configuracion) y la proyectará justo donde pongamos <Outlet />
import { Outlet } from 'react-router-dom';
import { Sidebar } from './Sidebar';
import { Topbar } from './Topbar';
import { motion, AnimatePresence } from 'framer-motion';

export const Layout = () => {
    return (
        // flex h-screen: Hace que este contenedor ocupe EXACTAMENTE el alto de tu monitor (100vh), ni un pixel más.
        // overflow-hidden: Evitamos que se pueda hacer scroll en toda la ventana (el scroll se lo daremos solo al centro).
        // transition-colors duration-300: Cuando cambiemos a Modo Oscuro, el fondo cambiará suavemente en 0.3 segundos.
        <div className="flex h-screen overflow-hidden bg-gray-50 dark:bg-dark-bg transition-colors duration-300">

            {/* 1. EL MENÚ LATERAL (Se queda fijo a la izquierda gracias a que estamos en un div flex) */}
            <Sidebar />

            {/* 2. TODO EL ESPACIO RESTANTE (El lado derecho) */}
            {/* flex-1: Le dice "estírate y ocupa todo el espacio que dejó el menú lateral" */}
            <div className="flex-1 flex flex-col relative">

                {/* 3. EFECTOS ESPECIALES (Las luces borrosas del fondo) */}
                {/* Son puros círculos con un "blur" extremo. "pointer-events-none" hace que no bloqueen los clicks del ratón */}
                <div className="absolute top-[-10%] left-[-10%] w-96 h-96 bg-brand-500/20 rounded-full blur-[120px] pointer-events-none"></div>
                <div className="absolute bottom-[-10%] right-[-10%] w-96 h-96 bg-blue-500/20 rounded-full blur-[120px] pointer-events-none"></div>

                {/* 4. LA BARRA SUPERIOR (Se coloca en el techo de este lado derecho) */}
                <Topbar />

                {/* 5. EL HUECO PARA LAS PÁGINAS (Dashboard, Clientes, etc.) */}
                {/* overflow-y-auto: Aquí es donde autorizamos el Scroll vertical, para no arrastrar la barra superior ni lateral. */}
                <main className="flex-1 overflow-y-auto p-8 relative z-10">

                    {/* AnimatePresence vigila si cambia de pantalla. Si cambia, hace la animación de entrada y salida */}
                    <AnimatePresence mode="wait">
                        <motion.div
                            // La nueva página entra ligeramente desde abajo (y: 20)
                            initial={{ opacity: 0, y: 20 }}
                            animate={{ opacity: 1, y: 0 }}
                            // La página vieja se sale ligeramente hacia arriba (y: -20)
                            exit={{ opacity: 0, y: -20 }}
                            transition={{ duration: 0.3 }}
                            className="h-full"
                        >
                            {/* ¡EL PORTAL MÁGICO! Aquí React inyectará el contenido de las páginas dinámicamente */}
                            <Outlet />
                        </motion.div>
                    </AnimatePresence>

                </main>
            </div>
        </div>
    );
};
