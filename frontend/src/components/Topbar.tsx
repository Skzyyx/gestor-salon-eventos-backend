import { useTheme } from '../context/ThemeContext';
import { useAuth } from '../context/AuthContext';
import { Moon, Sun, Bell } from 'lucide-react';
import { motion } from 'framer-motion';

export const Topbar = () => {
    // 1. Nos traemos las herramientas de nuestras "nubes"
    const { theme, toggleTheme } = useTheme();
    const { user } = useAuth();

    return (
        // motion.header: Anima la barra para que caiga desde arriba (y: -100 a y: 0)
        // sticky top-0 z-10: Hace que la barra se quede "pegada" al techo aunque el usuario haga scroll hacia abajo.
        <motion.header
            initial={{ y: -100 }}
            animate={{ y: 0 }}
            transition={{ type: "spring", stiffness: 100, damping: 20 }}
            className="h-20 bg-white/70 dark:bg-dark-surface/70 backdrop-blur-xl border-b border-gray-200 dark:border-dark-border flex items-center justify-between px-8 sticky top-0 z-10"
        >
            {/* LADO IZQUIERDO: Saludo */}
            <div className="flex items-center gap-2">
                <h2 className="text-xl font-semibold text-gray-800 dark:text-white">
                    ¡Hola, <span className="text-brand-500">{user?.nombre || 'Administrador'}</span>! 👋
                </h2>
            </div>

            {/* LADO DERECHO: Botones y Avatar */}
            <div className="flex items-center gap-6">

                {/* Botón de Notificaciones (Por ahora solo es estético) */}
                <button className="relative p-2 text-gray-500 hover:text-brand-500 dark:text-gray-400 transition-colors">
                    <Bell size={22} />
                    {/* El puntito rojo de notificación nueva */}
                    <span className="absolute top-1 right-1 w-2.5 h-2.5 bg-red-500 rounded-full border-2 border-white dark:border-dark-surface"></span>
                </button>

                {/* Switch de Modo Oscuro */}
                <button
                    onClick={toggleTheme}
                    className="p-2 rounded-full bg-gray-100 dark:bg-dark-bg text-gray-600 dark:text-yellow-400 hover:scale-110 transition-transform"
                >
                    {/* Usamos un Operador Ternario: Si es 'dark' mostramos el sol, si no, la luna */}
                    {theme === 'dark' ? <Sun size={20} /> : <Moon size={20} />}
                </button>

                {/* Perfil del Usuario */}
                <div className="flex items-center gap-3 pl-6 border-l border-gray-200 dark:border-dark-border">
                    {/* Avatar: Círculo con gradiente y la primera letra del nombre */}
                    <div className="w-10 h-10 rounded-full bg-gradient-to-tr from-brand-400 to-brand-600 flex items-center justify-center text-white font-bold shadow-md">
                        {user?.nombre?.charAt(0).toUpperCase() || 'A'}
                    </div>
                    {/* Nombre y Rol. "hidden md:block" lo oculta en celulares para no amontonar la pantalla */}
                    <div className="hidden md:block">
                        <p className="text-sm font-medium text-gray-900 dark:text-white">{user?.nombre || 'Admin'}</p>
                        <p className="text-xs text-gray-500 dark:text-gray-400 capitalize">{user?.rol?.toLowerCase() || 'Dueño'}</p>
                    </div>
                </div>
            </div>
        </motion.header>
    );
};
