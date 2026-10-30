import axios from 'axios';
import { useState, useEffect, useMemo } from 'react';
import { getEquipo, crearTrabajador, actualizarTrabajador, cambiarEstadoTrabajador } from '../api/equipo.api';
import { getRolesNegocio } from '../api/roles-negocio.api';
import { Trabajador, RolNegocio, CrearTrabajadorRequest, ActualizarTrabajadorRequest } from '../types/equipo';
import { ShieldCheck, Plus, AlertCircle, Loader2, Search, X, SearchX, UserPlus, ToggleLeft, ToggleRight } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import CatalogItemCard, { Badge } from '../components/CatalogItemCard';
import TrabajadorFormModal from '../components/TrabajadorFormModal';
import CustomSelect from '../components/CustomSelect';
import ConfirmDialog from '../components/ConfirmDialog';
import Toast, { ToastMessage, ToastTipo } from '../components/Toast';

const SORT_OPTIONS = [
    { value: 'nombre_asc', label: 'Nombre (A-Z)' },
    { value: 'nombre_desc', label: 'Nombre (Z-A)' },
];

export default function PersonalPage() {
    const [equipo, setEquipo] = useState<Trabajador[]>([]);
    const [roles, setRoles] = useState<RolNegocio[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [toast, setToast] = useState<ToastMessage | null>(null);

    const [showModal, setShowModal] = useState(false);
    const [trabajadorToEdit, setTrabajadorToEdit] = useState<Trabajador | null>(null);
    const [confirmToggle, setConfirmToggle] = useState<{ id: number, activar: boolean, nombre: string } | null>(null);

    const [searchTerm, setSearchTerm] = useState('');
    const [sortOrder, setSortOrder] = useState('');

    const hasActiveFilters = searchTerm || sortOrder;

    const clearFilters = () => {
        setSearchTerm('');
        setSortOrder('');
    };

    const showToast = (text: string, type: ToastTipo) => setToast({ text, type });

    useEffect(() => {
        if (!toast) return;
        const t = setTimeout(() => setToast(null), 3500);
        return () => clearTimeout(t);
    }, [toast]);

    const filteredEquipo = useMemo(() => {
        const term = searchTerm.trim().toLowerCase();
        let result = equipo.filter((t) => {
            if (!term) return true;
            return (
                t.nombre?.toLowerCase().includes(term) ||
                t.correo?.toLowerCase().includes(term) ||
                t.rol?.nombre?.toLowerCase().includes(term)
            );
        });

        if (sortOrder) {
            result = [...result].sort((a, b) =>
                sortOrder === 'nombre_asc'
                    ? (a.nombre || '').localeCompare(b.nombre || '')
                    : (b.nombre || '').localeCompare(a.nombre || '')
            );
        }
        return result;
    }, [equipo, searchTerm, sortOrder]);

    const fetchData = async () => {
        try {
            setLoading(true);
            const [dataEquipo, dataRoles] = await Promise.all([
                getEquipo(),
                getRolesNegocio()
            ]);
            setEquipo(dataEquipo);
            setRoles(dataRoles);
        } catch (err) {
            setError('Error al cargar datos del personal');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchData();
    }, []);

    const openCreateModal = () => {
        setTrabajadorToEdit(null);
        setError('');
        setShowModal(true);
    };

    const openEditModal = (trabajador: Trabajador) => {
        setTrabajadorToEdit(trabajador);
        setError('');
        setShowModal(true);
    };

    const handleSave = async (payload: CrearTrabajadorRequest | ActualizarTrabajadorRequest, setSaving: (value: boolean) => void) => {
        setError('');
        try {
            if (trabajadorToEdit) {
                await actualizarTrabajador(trabajadorToEdit.id, payload as ActualizarTrabajadorRequest);
                showToast('Trabajador actualizado correctamente.', 'success');
            } else {
                await crearTrabajador(payload as CrearTrabajadorRequest);
                showToast('Trabajador registrado correctamente.', 'success');
            }
            setShowModal(false);
            setTrabajadorToEdit(null);
            fetchData();
        } catch (err) {
            const msg = axios.isAxiosError(err) ? err.response?.data?.message : undefined;
            setError(msg || (trabajadorToEdit ? 'Error al actualizar trabajador' : 'Error al crear trabajador'));
            setSaving(false);
        }
    };

    const handleToggleState = async () => {
        if (!confirmToggle) return;
        try {
            await cambiarEstadoTrabajador(confirmToggle.id, confirmToggle.activar);
            setConfirmToggle(null);
            showToast(`Trabajador ${confirmToggle.activar ? 'activado' : 'desactivado'}.`, 'success');
            fetchData();
        } catch (err) {
            const msg = axios.isAxiosError(err) ? err.response?.data?.message : undefined;
            setConfirmToggle(null);
            showToast(msg || 'No se pudo cambiar el estado.', 'error');
        }
    };

    return (
        <div className="space-y-6">
            <header className="flex flex-col sm:flex-row justify-between items-start sm:items-end gap-4">
                <div>
                    <h1 className="text-3xl font-black text-gray-900 dark:text-white flex items-center gap-2">
                        <ShieldCheck className="text-brand-500" /> Gestión de Personal
                    </h1>
                    <p className="text-gray-500 dark:text-gray-400 mt-1">Administra los accesos y roles de tu equipo</p>
                </div>
                <motion.button
                    whileHover={{ scale: 1.03 }}
                    whileTap={{ scale: 0.97 }}
                    onClick={openCreateModal}
                    className="flex items-center gap-2 bg-brand-600 hover:bg-brand-700 dark:bg-brand-500 dark:hover:bg-brand-600 text-white font-semibold px-5 py-2.5 rounded-xl transition-all shadow-lg shadow-brand-500/25"
                >
                    <Plus size={18} />
                    Nuevo Trabajador
                </motion.button>
            </header>

            <motion.div
                initial={{ opacity: 0, y: -10 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.3 }}
                className="relative z-30 flex flex-col lg:flex-row gap-3 bg-white/70 dark:bg-dark-surface/70 backdrop-blur-xl p-3 rounded-2xl border border-gray-100 dark:border-dark-border shadow-sm"
            >
                <div className="relative flex-1">
                    <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none text-gray-400">
                        <Search size={18} />
                    </div>
                    <input
                        type="text"
                        value={searchTerm}
                        onChange={(e) => setSearchTerm(e.target.value)}
                        placeholder="Buscar por nombre, correo o rol..."
                        className="w-full pl-11 pr-10 py-3 border rounded-xl transition-all shadow-sm focus:outline-none focus:ring-2 focus:ring-brand-500/50 focus:border-brand-500 border-gray-200 dark:border-dark-border bg-white dark:bg-dark-bg text-gray-900 dark:text-white placeholder:text-gray-400"
                    />
                    <AnimatePresence>
                        {searchTerm && (
                            <motion.button
                                initial={{ opacity: 0, scale: 0.8 }}
                                animate={{ opacity: 1, scale: 1 }}
                                exit={{ opacity: 0, scale: 0.8 }}
                                type="button"
                                onClick={() => setSearchTerm('')}
                                className="absolute inset-y-0 right-0 pr-4 flex items-center text-gray-400 hover:text-gray-600 dark:hover:text-gray-200"
                            >
                                <X size={18} />
                            </motion.button>
                        )}
                    </AnimatePresence>
                </div>
                <div className="w-full lg:w-48">
                    <CustomSelect
                        options={SORT_OPTIONS}
                        value={sortOrder}
                        onChange={setSortOrder}
                        placeholder="Ordenar por"
                    />
                </div>
            </motion.div>

            <AnimatePresence>
                {hasActiveFilters && !loading && (
                    <motion.div
                        initial={{ opacity: 0, height: 0 }}
                        animate={{ opacity: 1, height: 'auto' }}
                        exit={{ opacity: 0, height: 0 }}
                        className="flex items-center justify-between text-sm -mt-2 px-1"
                    >
                        <span className="text-gray-500 dark:text-gray-400">
                            {filteredEquipo.length} de {equipo.length} trabajador{equipo.length !== 1 ? 'es' : ''}
                        </span>
                        <button
                            onClick={clearFilters}
                            className="flex items-center gap-1.5 font-semibold text-brand-600 dark:text-brand-400 hover:text-brand-700 dark:hover:text-brand-300 transition-colors"
                        >
                            <X size={14} /> Limpiar filtros
                        </button>
                    </motion.div>
                )}
            </AnimatePresence>

            <AnimatePresence>
                {error && !showModal && (
                    <motion.div
                        initial={{ opacity: 0, y: -10 }}
                        animate={{ opacity: 1, y: 0 }}
                        exit={{ opacity: 0, y: -10 }}
                        className="bg-red-50 dark:bg-red-500/10 text-red-600 dark:text-red-400 border border-red-200 dark:border-red-500/20 p-4 rounded-xl flex items-center gap-2"
                    >
                        <AlertCircle size={20} />
                        <span className="font-medium">{error}</span>
                    </motion.div>
                )}
            </AnimatePresence>

            {loading ? (
                <div className="flex justify-center items-center h-48">
                    <Loader2 className="animate-spin text-brand-500" size={32} />
                </div>
            ) : equipo.length === 0 ? (
                <motion.div
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    className="bg-white/80 dark:bg-dark-surface/80 backdrop-blur-xl p-12 rounded-2xl border border-gray-100 dark:border-dark-border text-center"
                >
                    <UserPlus className="mx-auto text-gray-300 dark:text-gray-600 mb-4" size={48} />
                    <p className="text-gray-500 dark:text-gray-400 text-lg font-medium">Aún no tienes personal registrado.</p>
                    <p className="text-gray-400 dark:text-gray-500 text-sm mt-1">Usa el botón "Nuevo Trabajador" para agregar al equipo.</p>
                </motion.div>
            ) : filteredEquipo.length === 0 ? (
                <motion.div
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    className="bg-white/80 dark:bg-dark-surface/80 backdrop-blur-xl p-12 rounded-2xl border border-gray-100 dark:border-dark-border text-center"
                >
                    <SearchX className="mx-auto text-gray-300 dark:text-gray-600 mb-4" size={48} />
                    <p className="text-gray-500 dark:text-gray-400 text-lg font-medium">No se encontró personal.</p>
                    <p className="text-gray-400 dark:text-gray-500 text-sm mt-1">Prueba ajustando la búsqueda.</p>
                    <button
                        onClick={clearFilters}
                        className="mt-4 inline-flex items-center gap-1.5 text-sm font-semibold text-brand-600 dark:text-brand-400 hover:text-brand-700 dark:hover:text-brand-300 transition-colors"
                    >
                        <X size={14} /> Limpiar filtros
                    </button>
                </motion.div>
            ) : (
                <div className="space-y-3">
                    {filteredEquipo.map((trabajador, index) => {
                        const badges: Badge[] = [];
                        if (trabajador.rol) {
                            badges.push({ text: trabajador.rol.nombre, variant: 'brand' });
                        }
                        badges.push({ text: trabajador.correo, variant: 'gray' });
                        badges.push({ text: trabajador.activo ? 'Activo' : 'Inactivo', variant: trabajador.activo ? 'brand' : 'gray' });

                        return (
                            <div key={trabajador.id} className="relative group">
                                <CatalogItemCard
                                    index={index}
                                    icon={ShieldCheck}
                                    title={trabajador.nombre}
                                    description={""}
                                    badges={badges}
                                    onEdit={() => openEditModal(trabajador)}
                                />
                                <div className="absolute right-20 top-1/2 -translate-y-1/2 opacity-0 group-hover:opacity-100 transition-opacity">
                                    <button
                                        onClick={(e) => {
                                            e.stopPropagation();
                                            setConfirmToggle({ id: trabajador.id, activar: !trabajador.activo, nombre: trabajador.nombre });
                                        }}
                                        className="p-2 text-gray-400 hover:text-brand-500 transition-colors bg-white dark:bg-dark-surface rounded-full shadow-sm"
                                        title={trabajador.activo ? "Desactivar" : "Activar"}
                                    >
                                        {trabajador.activo ? <ToggleRight size={20} className="text-brand-500" /> : <ToggleLeft size={20} />}
                                    </button>
                                </div>
                            </div>
                        );
                    })}
                </div>
            )}

            <TrabajadorFormModal
                isOpen={showModal}
                onClose={() => {
                    setShowModal(false);
                    setError('');
                }}
                trabajadorToEdit={trabajadorToEdit}
                roles={roles}
                onSave={handleSave}
                error={error}
            />

            <ConfirmDialog
                isOpen={!!confirmToggle}
                title={confirmToggle?.activar ? "¿Activar trabajador?" : "¿Desactivar trabajador?"}
                confirmText={confirmToggle?.activar ? "Activar" : "Desactivar"}
                onCancel={() => setConfirmToggle(null)}
                onConfirm={handleToggleState}
                message={confirmToggle && (
                    <>
                        {confirmToggle.activar ? "Se reactivará a " : "Se desactivará el acceso de "}
                        <span className="font-semibold text-gray-700 dark:text-gray-200">"{confirmToggle.nombre}"</span>.
                    </>
                )}
            />

            <Toast message={toast} onClose={() => setToast(null)} />
        </div>
    );
}
