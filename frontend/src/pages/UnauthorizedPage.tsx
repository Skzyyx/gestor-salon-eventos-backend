import { useState } from 'react';
import { motion } from 'framer-motion';
import { ShieldAlert, RefreshCw, ArrowLeft } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useNavigate } from 'react-router-dom';

export default function UnauthorizedPage() {
    const { syncSession } = useAuth();
    const navigate = useNavigate();
    const [isSyncing, setIsSyncing] = useState(false);

    const handleSync = async () => {
        setIsSyncing(true);
        const success = await syncSession();
        setIsSyncing(false);
        if (success) {
            navigate('/', { replace: true });
        }
    };

    return (
        <div className="min-h-screen w-full flex items-center justify-center bg-gradient-to-br from-indigo-50 via-white to-purple-50 p-6 relative overflow-hidden">
            {/* Círculos decorativos de fondo (Glassmorphism) */}
            <div className="absolute top-[-10%] left-[-10%] w-96 h-96 bg-purple-300 rounded-full mix-blend-multiply filter blur-3xl opacity-30 animate-blob"></div>
            <div className="absolute top-[-10%] right-[-10%] w-96 h-96 bg-indigo-300 rounded-full mix-blend-multiply filter blur-3xl opacity-30 animate-blob animation-delay-2000"></div>
            <div className="absolute bottom-[-20%] left-[20%] w-96 h-96 bg-pink-300 rounded-full mix-blend-multiply filter blur-3xl opacity-30 animate-blob animation-delay-4000"></div>

            <motion.div
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.5 }}
                className="relative z-10 max-w-md w-full"
            >
                {/* Tarjeta Glassmorphism */}
                <div className="bg-white/70 backdrop-blur-xl border border-white/40 shadow-2xl rounded-3xl p-8 text-center overflow-hidden">
                    {/* Icono animado */}
                    <motion.div
                        initial={{ scale: 0 }}
                        animate={{ scale: 1 }}
                        transition={{ type: "spring", stiffness: 200, damping: 15, delay: 0.2 }}
                        className="w-24 h-24 bg-red-100 rounded-full flex items-center justify-center mx-auto mb-6 shadow-inner"
                    >
                        <ShieldAlert className="w-12 h-12 text-red-500" strokeWidth={1.5} />
                    </motion.div>

                    <h1 className="text-3xl font-bold text-gray-800 mb-3 tracking-tight">Acceso Denegado</h1>

                    <p className="text-gray-600 mb-8 leading-relaxed">
                        Ups, parece que te perdiste o tus permisos locales están desincronizados. No tienes autorización para ver esta página.
                    </p>

                    <div className="space-y-4">
                        <button
                            onClick={handleSync}
                            disabled={isSyncing}
                            className="w-full relative group overflow-hidden rounded-xl p-[1px]"
                        >
                            <span className="absolute inset-0 bg-gradient-to-r from-purple-500 to-indigo-500 rounded-xl opacity-70 group-hover:opacity-100 transition-opacity duration-300"></span>
                            <div className="relative bg-white/90 backdrop-blur-sm px-6 py-3 rounded-xl flex items-center justify-center gap-2 transition-all duration-300 group-hover:bg-transparent">
                                {isSyncing ? (
                                    <>
                                        <RefreshCw className="w-5 h-5 text-purple-600 group-hover:text-white animate-spin" />
                                        <span className="font-semibold text-purple-600 group-hover:text-white">Sincronizando...</span>
                                    </>
                                ) : (
                                    <>
                                        <RefreshCw className="w-5 h-5 text-purple-600 group-hover:text-white transition-colors" />
                                        <span className="font-semibold text-purple-600 group-hover:text-white transition-colors">Arreglar mi sesión</span>
                                    </>
                                )}
                            </div>
                        </button>
                    </div>
                </div>
            </motion.div>
        </div>
    );
}
