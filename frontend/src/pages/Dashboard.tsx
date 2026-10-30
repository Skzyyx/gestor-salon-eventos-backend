import { motion } from 'framer-motion';
import { CalendarDays, Users, DollarSign, TrendingUp } from 'lucide-react';

export default function Dashboard() {

    // Datos simulados (Mock data) que después traeremos del backend
    const stats = [
        { label: 'Eventos este Mes', value: '12', icon: <CalendarDays size={24} />, color: 'bg-brand-500' },
        { label: 'Nuevos Clientes', value: '48', icon: <Users size={24} />, color: 'bg-blue-500' },
        { label: 'Ingresos', value: '$45,200', icon: <DollarSign size={24} />, color: 'bg-emerald-500' },
        { label: 'Crecimiento', value: '+14%', icon: <TrendingUp size={24} />, color: 'bg-purple-500' },
    ];

    return (
        <div className="space-y-6">

            {/* ENCABEZADO */}
            <div>
                <h1 className="text-3xl font-bold text-gray-900 dark:text-white">Resumen General</h1>
                <p className="text-gray-500 dark:text-gray-400 mt-1">
                    Aquí tienes un vistazo rápido del rendimiento de tu negocio.
                </p>
            </div>

            {/* CUADRÍCULA DE TARJETAS (STAT CARDS) */}
            {/* grid-cols-1 para celular, md:grid-cols-2 para tablet, lg:grid-cols-4 para PC */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
                {stats.map((stat, index) => (
                    // motion.div con un 'delay' progresivo para que las tarjetas entren una por una (efecto cascada)
                    <motion.div
                        key={stat.label}
                        initial={{ opacity: 0, y: 20 }}
                        animate={{ opacity: 1, y: 0 }}
                        transition={{ delay: index * 0.1 }}
                        className="bg-white/80 dark:bg-dark-surface/80 backdrop-blur-xl p-6 rounded-2xl shadow-sm border border-gray-100 dark:border-dark-border flex items-center gap-4 hover:shadow-md transition-shadow"
                    >
                        {/* Círculo con el Icono */}
                        <div className={`p-4 rounded-xl text-white shadow-lg ${stat.color}`}>
                            {stat.icon}
                        </div>

                        {/* Texto e Información */}
                        <div>
                            <p className="text-sm font-medium text-gray-500 dark:text-gray-400">{stat.label}</p>
                            <p className="text-2xl font-bold text-gray-900 dark:text-white">{stat.value}</p>
                        </div>
                    </motion.div>
                ))}
            </div>

            {/* SECCIÓN DE GRÁFICA / CONTENIDO SECUNDARIO (Mock) */}
            <motion.div
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.4 }}
                className="bg-white/80 dark:bg-dark-surface/80 backdrop-blur-xl rounded-2xl shadow-sm border border-gray-100 dark:border-dark-border p-8 min-h-[400px] flex items-center justify-center"
            >
                <div className="text-center text-gray-400 dark:text-gray-500">
                    <p>Espacio reservado para la gráfica de ingresos 📊</p>
                    <p className="text-sm mt-2">Se implementará en el Sprint 4.</p>
                </div>
            </motion.div>

        </div>
    );
}
