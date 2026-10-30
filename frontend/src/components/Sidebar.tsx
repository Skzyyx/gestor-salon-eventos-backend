import { NavLink } from 'react-router-dom';
// Importamos iconos profesionales de lucide-react (reemplaza a FontAwesome)
import { Home, Settings, CalendarDays, Users, LogOut, Clock, Briefcase, Building2, Boxes, ShieldCheck } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
// Framer Motion nos permite hacer animaciones espectaculares con componentes como <motion.div>
import { motion } from 'framer-motion';

export const Sidebar = () => {
    // Necesitamos el usuario (para saber su rol) y la función logout
    const { user, logout } = useAuth();

    // Ítems para un ADMIN de negocio (el menú de siempre)
    const adminItems = [
        { path: '/', icon: <Home size={20} />, label: 'Dashboard' },
        { path: '/eventos', icon: <CalendarDays size={20} />, label: 'Mis Eventos' },
        { path: '/clientes', icon: <Users size={20} />, label: 'Clientes' },
        { path: '/turnos', icon: <Clock size={20} />, label: 'Turnos' },
        { path: '/servicios', icon: <Briefcase size={20} />, label: 'Servicios' },
        { path: '/paquetes', icon: <Boxes size={20} />, label: 'Paquetes' },
        { path: '/personal', icon: <ShieldCheck size={20} />, label: 'Personal' },
        { path: '/configuracion', icon: <Settings size={20} />, label: 'Configuración' },
    ];

    // Ítems para el SUPERADMIN de plataforma. Como NO tiene negocio,
    // solo le mostramos su panel (nada de Turnos/Servicios/Configuración, que le fallarían).
    const superAdminItems = [
        { path: '/plataforma', icon: <Building2 size={20} />, label: 'Plataforma' },
    ];

    // Elegimos el menú según el rol del usuario logueado.
    const menuItems = user?.rol === 'SUPERADMIN' ? superAdminItems : adminItems;


    return (
        // Usamos motion.aside en lugar de un <aside> normal de HTML.
        // initial={{ x: -250 }}: El menú empieza oculto, 250 pixeles empujado hacia la izquierda, fuera de la pantalla.
        // animate={{ x: 0 }}: Automáticamente se desliza hasta su posición original (0).
        // transition={{ type: "spring" }}: Le da un efecto de "resorte" fluido, no es lineal y aburrido.
        <motion.aside
            initial={{ x: -250 }}
            animate={{ x: 0 }}
            transition={{ type: "spring", stiffness: 100, damping: 20 }}
            // ESTÉTICA PREMIUM TAILWIND:
            // backdrop-blur-xl: Hace el "Efecto Cristal" difuminando lo que haya detrás del menú.
            // dark:bg-dark-surface/80: En modo oscuro, usa un fondo oscuro con 80% de opacidad.
            // relative z-20: Asegura que el menú siempre flote por encima del contenido del lado derecho.
            className="w-64 h-screen bg-white/80 dark:bg-dark-surface/80 backdrop-blur-xl border-r border-gray-200 dark:border-dark-border flex flex-col justify-between shadow-xl relative z-20"
        >
            <div>
                {/* 1. SECCIÓN DEL LOGOTIPO */}
                <div className="h-20 flex items-center justify-center border-b border-gray-200 dark:border-dark-border">
                    <h1 className="text-2xl font-black bg-gradient-to-r from-brand-600 to-brand-400 bg-clip-text text-transparent tracking-tighter">
                        GestorSalón
                    </h1>
                </div>

                {/* 2. SECCIÓN DE NAVEGACIÓN */}
                <nav className="p-4 space-y-2 mt-4">
                    {/* Iteramos (map) sobre nuestro arreglo de botones */}
                    {menuItems.map((item) => (
                        // NavLink es un <a> vitaminado de React Router. 
                        // Nos regala una propiedad "isActive" que nos dice si el usuario está actualmente en esa ruta.
                        <NavLink
                            key={item.path}
                            to={item.path}
                            className={({ isActive }) => `
                                flex items-center gap-3 px-4 py-3 rounded-xl transition-all duration-300 font-medium
                                ${isActive
                                    // ESTILOS SI EL BOTÓN ESTÁ ACTIVO (Pintado de color primario)
                                    ? 'bg-brand-500 text-white shadow-lg shadow-brand-500/30'
                                    // ESTILOS SI EL BOTÓN ESTÁ INACTIVO (Grisáceo, pero cambia de color al pasar el ratón "hover")
                                    : 'text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-dark-surface-hover hover:text-gray-900 dark:hover:text-white'}
                            `}
                        >
                            {item.icon}
                            <span>{item.label}</span>
                        </NavLink>
                    ))}
                </nav>
            </div>

            {/* 3. SECCIÓN DEL BOTÓN DE CERRAR SESIÓN (Siempre al fondo gracias al flex flex-col justify-between) */}
            <div className="p-4 border-t border-gray-200 dark:border-dark-border">
                <button
                    onClick={logout}
                    className="w-full flex items-center gap-3 px-4 py-3 rounded-xl text-red-500 hover:bg-red-50 dark:hover:bg-red-500/10 transition-colors font-medium"
                >
                    <LogOut size={20} />
                    <span>Cerrar Sesión</span>
                </button>
            </div>
        </motion.aside>
    );
};
