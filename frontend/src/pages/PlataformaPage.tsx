import axios from 'axios';
import { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { Building2, Plus, Link as LinkIcon, Loader2, CheckCircle2, XCircle } from 'lucide-react';
import { getNegocios, crearNegocio, Negocio, CrearNegocio } from '../api/plataforma.api';
import NegocioFormModal from '../components/NegocioFormModal';

export default function PlataformaPage() {
    const [negocios, setNegocios] = useState<Negocio[]>([]);
    const [loading, setLoading] = useState(true);
    const [modalOpen, setModalOpen] = useState(false);
    const [formError, setFormError] = useState('');

    // Trae la lista de negocios desde el backend (GET /admin/negocios)
    const fetchNegocios = async () => {
        try {
            const data = await getNegocios();
            setNegocios(data);
        } catch (error) {
            console.error('Error al cargar negocios:', error);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchNegocios();
    }, []);

    const handleOpenModal = () => {
        setFormError('');
        setModalOpen(true);
    };

    // El modal nos entrega (payload, setSaving). Aquí hacemos la llamada real.
    const handleSave = async (payload: CrearNegocio, setSaving: (saving: boolean) => void) => {
        setFormError('');
        try {
            await crearNegocio(payload);
            setModalOpen(false);
            fetchNegocios(); // recargamos la lista para que aparezca el nuevo
        } catch (error) {
            const msg = axios.isAxiosError(error) ? error.response?.data?.message : undefined;
            setFormError(msg);
            setSaving(false); // reactivamos el botón del modal para reintentar
        }
    };

    return (
        <div className="max-w-5xl mx-auto space-y-6">
            {/* ENCABEZADO + BOTÓN */}
            <div className="flex items-center justify-between flex-wrap gap-4">
                <div>
                    <h1 className="text-3xl font-bold text-gray-900 dark:text-white">Plataforma</h1>
                    <p className="text-gray-500 dark:text-gray-400 mt-1">
                        Administra todos los negocios registrados en el sistema.
                    </p>
                </div>
                <button
                    onClick={handleOpenModal}
                    className="bg-brand-600 hover:bg-brand-700 text-white font-bold py-3 px-5 rounded-xl transition-all shadow-lg shadow-brand-500/30 flex items-center gap-2"
                >
                    <Plus size={18} /> Nuevo Negocio
                </button>
            </div>

            {/* CONTENIDO: cargando / vacío / listado */}
            {loading ? (
                <div className="flex justify-center items-center h-64">
                    <Loader2 className="animate-spin text-brand-500" size={32} />
                </div>
            ) : negocios.length === 0 ? (
                <div className="text-center py-20 bg-white/60 dark:bg-dark-surface/60 rounded-2xl border border-dashed border-gray-200 dark:border-dark-border">
                    <Building2 className="mx-auto text-gray-300 dark:text-gray-600 mb-3" size={48} />
                    <p className="text-gray-500 dark:text-gray-400 text-lg font-medium">Aún no hay negocios registrados.</p>
                    <p className="text-gray-400 text-sm">Crea el primero con el botón "Nuevo Negocio".</p>
                </div>
            ) : (
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
                    {negocios.map((negocio, index) => (
                        <motion.div
                            key={negocio.id}
                            initial={{ opacity: 0, y: 20 }}
                            animate={{ opacity: 1, y: 0 }}
                            transition={{ delay: index * 0.05 }}
                            className="bg-white/80 dark:bg-dark-surface/80 backdrop-blur-xl p-6 rounded-2xl shadow-sm border border-gray-100 dark:border-dark-border hover:shadow-lg hover:shadow-brand-500/10 transition-shadow"
                        >
                            <div className="flex items-start justify-between mb-4">
                                <div className="p-3 rounded-xl bg-brand-50 dark:bg-brand-900/20">
                                    <Building2 className="w-6 h-6 text-brand-600 dark:text-brand-400" />
                                </div>
                                {negocio.activo ? (
                                    <span className="flex items-center gap-1 text-xs font-semibold text-emerald-600 bg-emerald-50 dark:bg-emerald-500/10 px-2.5 py-1 rounded-full">
                                        <CheckCircle2 size={13} /> Activo
                                    </span>
                                ) : (
                                    <span className="flex items-center gap-1 text-xs font-semibold text-gray-500 bg-gray-100 dark:bg-gray-700/30 px-2.5 py-1 rounded-full">
                                        <XCircle size={13} /> Inactivo
                                    </span>
                                )}
                            </div>
                            <h3 className="text-lg font-bold text-gray-900 dark:text-white truncate">{negocio.nombre}</h3>
                            <p className="flex items-center gap-1.5 text-sm text-gray-400 font-mono mt-1 truncate">
                                <LinkIcon size={13} /> {negocio.slug}
                            </p>
                        </motion.div>
                    ))}
                </div>
            )}

            {/* MODAL DE CREACIÓN (el que hicimos en 8.2) */}
            <NegocioFormModal
                isOpen={modalOpen}
                onClose={() => setModalOpen(false)}
                onSave={handleSave}
                error={formError}
            />
        </div>
    );
}
