import { useState, type SubmitEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useTheme } from '../context/ThemeContext'; // Importamos el tema para el botón del Sol/Luna
import { motion } from 'framer-motion';
import { Mail, Lock, Sun, Moon, ArrowRight, Loader2, Eye, EyeOff } from 'lucide-react';

export default function Login() {
    const [correo, setCorreo] = useState('');
    const [password, setPassword] = useState('');
    const [showPassword, setShowPassword] = useState(false);
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);

    const { login } = useAuth();
    const { theme, toggleTheme } = useTheme(); // Para poder cambiar el tema desde el Login
    const navigate = useNavigate();

    const handleSubmit = async (e: SubmitEvent<HTMLFormElement>) => {
        e.preventDefault();
        setError('');
        setLoading(true);

        const success = await login(correo, password);

        if (success) {
            navigate('/');
        } else {
            setError('Correo o contraseña incorrectos');
            setLoading(false);
        }
    };

    return (
        // Fondo general que ocupa toda la pantalla con soporte para Dark Mode
        <div className="min-h-screen bg-gray-50 dark:bg-dark-bg transition-colors duration-500 flex items-center justify-center p-4 relative overflow-hidden">

            {/* BOTÓN PARA CAMBIAR TEMA (Esquina Superior Derecha) */}
            <button
                onClick={toggleTheme}
                className="absolute top-6 right-6 p-3 rounded-full bg-white/50 dark:bg-dark-surface/50 backdrop-blur-md text-gray-600 dark:text-yellow-400 hover:scale-110 transition-transform shadow-sm z-50"
            >
                {theme === 'dark' ? <Sun size={24} /> : <Moon size={24} />}
            </button>

            {/* CÍRCULOS MÁGICOS DE FONDO (Blobs) */}
            <div className="absolute top-[-10%] left-[-10%] w-96 h-96 bg-brand-500/30 rounded-full blur-[120px] pointer-events-none"></div>
            <div className="absolute bottom-[-10%] right-[-10%] w-96 h-96 bg-blue-500/20 rounded-full blur-[120px] pointer-events-none"></div>

            {/* TARJETA DE LOGIN ANIMADA */}
            <motion.div
                initial={{ opacity: 0, y: 30 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.5, ease: "easeOut" }}
                className="max-w-md w-full bg-white/80 dark:bg-dark-surface/80 backdrop-blur-2xl rounded-3xl shadow-2xl border border-white/20 dark:border-dark-border p-10 relative z-10"
            >
                {/* Título y Subtítulo */}
                <div className="text-center space-y-3 mb-8">
                    <div className="inline-block p-4 rounded-2xl bg-brand-50 dark:bg-brand-900/20 mb-2">
                        {/* Un ícono representativo decorativo */}
                        <Lock className="w-8 h-8 text-brand-600 dark:text-brand-400" />
                    </div>
                    <h1 className="text-3xl font-black text-gray-900 dark:text-white tracking-tight">
                        Bienvenido
                    </h1>
                    <p className="text-gray-500 dark:text-gray-400 font-medium">
                        Ingresa a tu cuenta de GestorSalón
                    </p>
                </div>

                {/* Formulario */}
                <form onSubmit={handleSubmit} className="space-y-5">
                    {/* Alerta de Error */}
                    {error && (
                        <motion.div
                            initial={{ opacity: 0, scale: 0.95 }}
                            animate={{ opacity: 1, scale: 1 }}
                            className="bg-red-50 dark:bg-red-500/10 border border-red-200 dark:border-red-500/20 text-red-600 dark:text-red-400 p-4 rounded-xl text-sm text-center font-medium"
                        >
                            {error}
                        </motion.div>
                    )}

                    {/* Input de Correo */}
                    <div className="space-y-1.5">
                        <label className="text-sm font-semibold text-gray-700 dark:text-gray-300 ml-1">Correo Electrónico</label>
                        <div className="relative">
                            <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none text-gray-400">
                                <Mail size={18} />
                            </div>
                            <input
                                type="email"
                                required
                                autoFocus
                                className="w-full pl-11 pr-4 py-3 border border-gray-200 dark:border-dark-border rounded-xl focus:outline-none focus:ring-2 focus:ring-brand-500/50 focus:border-brand-500 bg-white dark:bg-dark-bg text-gray-900 dark:text-white transition-all shadow-sm"
                                placeholder="admin@laherradura.com"
                                value={correo}
                                onChange={(e) => setCorreo(e.target.value)}
                            />
                        </div>
                    </div>

                    {/* Input de Contraseña */}
                    <div className="space-y-1.5">
                        <label className="text-sm font-semibold text-gray-700 dark:text-gray-300 ml-1">Contraseña</label>
                        <div className="relative">
                            <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none text-gray-400">
                                <Lock size={18} />
                            </div>
                            <input
                                type={showPassword ? "text" : "password"}
                                required
                                className="w-full pl-11 pr-12 py-3 border border-gray-200 dark:border-dark-border rounded-xl focus:outline-none focus:ring-2 focus:ring-brand-500/50 focus:border-brand-500 bg-white dark:bg-dark-bg text-gray-900 dark:text-white transition-all shadow-sm"
                                placeholder="••••••••"
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                            />
                            <button
                                type="button"
                                onClick={() => setShowPassword(!showPassword)}
                                className="absolute inset-y-0 right-0 pr-4 flex items-center text-gray-400 hover:text-brand-500 transition-colors"
                            >
                                {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                            </button>
                        </div>
                    </div>

                    {/* Botón de Submit Animado */}
                    <button
                        type="submit"
                        disabled={loading}
                        className="w-full group bg-brand-600 hover:bg-brand-700 dark:bg-brand-500 dark:hover:bg-brand-600 text-white font-bold py-3.5 rounded-xl transition-all duration-300 shadow-lg shadow-brand-500/30 flex justify-center items-center gap-2 disabled:opacity-70 disabled:cursor-not-allowed mt-2"
                    >
                        {loading ? (
                            <>
                                <Loader2 className="animate-spin" size={20} />
                                <span>Iniciando sesión...</span>
                            </>
                        ) : (
                            <>
                                <span>Entrar al Sistema</span>
                                <ArrowRight size={18} className="group-hover:translate-x-1 transition-transform" />
                            </>
                        )}
                    </button>
                </form>

            </motion.div>
        </div>
    );
}
