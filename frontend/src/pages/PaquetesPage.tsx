import axios from 'axios';
import { useState, useEffect, useMemo } from 'react';
import { getPaquetes, createPaquete, updatePaquete, deactivatePaquete, type Paquete, type GuardarPaquete } from '../api/paquetes.api';
import { getServicios, type Servicio } from '../api/servicios.api';
import { Boxes, Plus, AlertCircle, Loader2, Package, Search, X, SearchX } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import CatalogItemCard, { Badge } from '../components/CatalogItemCard';
import PaqueteFormModal from '../components/PaqueteFormModal';
import CustomSelect from '../components/CustomSelect';
import ConfirmDialog from '../components/ConfirmDialog';
import Toast, { type ToastMessage, type ToastTipo } from '../components/Toast';

const SORT_OPTIONS = [
    { value: 'nombre_asc', label: 'Nombre (A-Z)' },
    { value: 'nombre_desc', label: 'Nombre (Z-A)' },
    { value: 'precio_asc', label: 'Precio (menor a mayor)' },
    { value: 'precio_desc', label: 'Precio (mayor a menor)' },
];

export default function PaquetesPage() {
    const [paquetes, setPaquetes] = useState<Paquete[]>([]);
    const [servicios, setServicios] = useState<Servicio[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [toast, setToast] = useState<ToastMessage | null>(null);

    const [showModal, setShowModal] = useState(false);
    const [paqueteToEdit, setPaqueteToEdit] = useState<Paquete | null>(null);
    const [confirmDelete, setConfirmDelete] = useState<Paquete | null>(null);

    const [searchTerm, setSearchTerm] = useState('');
    const [sortOrder, setSortOrder] = useState('');

    const hasActiveFilters = searchTerm || sortOrder;
    const clearFilters = () => {
        setSearchTerm('');
        setSortOrder('');
    };

    const showToast = (text: string, type: ToastTipo = 'success') => setToast({ text, type });

    useEffect(() => {
        if (!toast) return;
        const t = setTimeout(() => setToast(null), 3500);
        return () => clearTimeout(t);
    }, [toast]);

    const filteredPaquetes = useMemo(() => {
        const term = searchTerm.trim().toLowerCase();
        let result = paquetes.filter((p) => {
            if (!term) return true;
            return (
                p.nombre?.toLowerCase().includes(term) ||
                p.descripcion?.toLowerCase().includes(term)
            );
        });

        if (sortOrder) {
            result = [...result].sort((a, b) => {
                switch (sortOrder) {
                    case 'nombre_asc': return (a.nombre || '').localeCompare(b.nombre || '');
                    case 'nombre_desc': return (b.nombre || '').localeCompare(a.nombre || '');
                    case 'precio_asc': return (a.precio ?? 0) - (b.precio ?? 0);
                    case 'precio_desc': return (b.precio ?? 0) - (a.precio ?? 0);
                    default: return 0;
                }
            });
        }
        return result;
    }, [paquetes, searchTerm, sortOrder]);

    const fetchData = async () => {
        try {
            setLoading(true);
            const [paquetesData, serviciosData] = await Promise.all([getPaquetes(), getServicios()]);
            setPaquetes(paquetesData.filter((p) => p.activo));
            setServicios(serviciosData.filter((s) => s.activo));
        } catch (err) {
            setError('Error al cargar paquetes');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchData();
    }, []);

    const openCreateModal = () => {
        // FA-07.4: no se puede crear un paquete sin servicios activos.
        if (servicios.length === 0) {
            showToast('No hay servicios registrados. Crea al menos uno antes de crear un paquete.', 'error');
            return;
        }
        setPaqueteToEdit(null);
        setError('');
        setShowModal(true);
    };

    const openEditModal = (paquete: Paquete) => {
        setPaqueteToEdit(paquete);
        setError('');
        setShowModal(true);
    };

    const handleSave = async (payload: GuardarPaquete, setSaving: (value: boolean) => void) => {
        setError('');
        try {
            if (paqueteToEdit) {
                await updatePaquete(paqueteToEdit.id, payload);
                showToast('Paquete actualizado correctamente.');
            } else {
                await createPaquete(payload);
                showToast('Paquete registrado correctamente.');
            }
            setShowModal(false);
            setPaqueteToEdit(null);
            fetchData();
        } catch (err) {
            const msg = axios.isAxiosError(err) ? err.response?.data?.message : undefined;
            setError(msg || (paqueteToEdit ? 'Error al actualizar paquete' : 'Error al crear paquete'));
            setSaving(false);
        }
    };

    const handleDeactivate = async (id: number) => {
        try {
            await deactivatePaquete(id);
            setConfirmDelete(null);
            showToast('Paquete desactivado.');
            fetchData();
        } catch (err) {
            setConfirmDelete(null);
            showToast('No se pudo desactivar el paquete.', 'error');
        }
    };

    return (
        <div className="space-y-6">

            {/* HEADER */}
            <header className="flex flex-col sm:flex-row justify-between items-start sm:items-end gap-4">
                <div>
                    <h1 className="text-3xl font-black text-gray-900 dark:text-white flex items-center gap-2">
                        <Boxes className="text-brand-500" /> Gestión de Paquetes
                    </h1>
                    <p className="text-gray-500 dark:text-gray-400 mt-1">Agrupaciones de servicios con precio conjunto</p>
                </div>
                <motion.button
                    whileHover={{ scale: 1.03 }}
                    whileTap={{ scale: 0.97 }}
                    onClick={openCreateModal}
                    className="flex items-center gap-2 bg-brand-600 hover:bg-brand-700 dark:bg-brand-500 dark:hover:bg-brand-600 text-white font-semibold px-5 py-2.5 rounded-xl transition-all shadow-lg shadow-brand-500/25"
                >
                    <Plus size={18} />
                    Nuevo Paquete
                </motion.button>
            </header>

            {/* BÚSQUEDA + ORDEN */}
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
                        placeholder="Buscar por nombre o descripción..."
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

            {/* Resumen + limpiar */}
            <AnimatePresence>
                {hasActiveFilters && !loading && (
                    <motion.div
                        initial={{ opacity: 0, height: 0 }}
                        animate={{ opacity: 1, height: 'auto' }}
                        exit={{ opacity: 0, height: 0 }}
                        className="flex items-center justify-between text-sm -mt-2 px-1"
                    >
                        <span className="text-gray-500 dark:text-gray-400">
                            {filteredPaquetes.length} de {paquetes.length} paquete{paquetes.length !== 1 ? 's' : ''}
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

            {/* ERROR GENERAL */}
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

            {/* LISTA */}
            {loading ? (
                <div className="flex justify-center items-center h-48">
                    <Loader2 className="animate-spin text-brand-500" size={32} />
                </div>
            ) : paquetes.length === 0 ? (
                <motion.div
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    className="bg-white/80 dark:bg-dark-surface/80 backdrop-blur-xl p-12 rounded-2xl border border-gray-100 dark:border-dark-border text-center"
                >
                    <Package className="mx-auto text-gray-300 dark:text-gray-600 mb-4" size={48} />
                    <p className="text-gray-500 dark:text-gray-400 text-lg font-medium">No hay paquetes registrados.</p>
                    <p className="text-gray-400 dark:text-gray-500 text-sm mt-1">
                        {servicios.length === 0
                            ? 'Primero crea servicios; luego podrás agruparlos en paquetes.'
                            : 'Usa el botón "Nuevo Paquete" para crear el primero.'}
                    </p>
                </motion.div>
            ) : filteredPaquetes.length === 0 ? (
                <motion.div
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    className="bg-white/80 dark:bg-dark-surface/80 backdrop-blur-xl p-12 rounded-2xl border border-gray-100 dark:border-dark-border text-center"
                >
                    <SearchX className="mx-auto text-gray-300 dark:text-gray-600 mb-4" size={48} />
                    <p className="text-gray-500 dark:text-gray-400 text-lg font-medium">No se encontraron paquetes.</p>
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
                    {filteredPaquetes.map((paquete, index) => {
                        const items = paquete.items ?? [];
                        const numServicios = items.length;
                        const totalPiezas = items.reduce((acc, it) => acc + (it.cantidad ?? 1), 0);
                        const badges: Badge[] = [
                            { text: `${numServicios} servicio${numServicios !== 1 ? 's' : ''}`, variant: 'brand' },
                        ];
                        if (totalPiezas !== numServicios) {
                            badges.push({ text: `${totalPiezas} piezas`, variant: 'gray' });
                        }
                        return (
                            <CatalogItemCard
                                key={paquete.id}
                                index={index}
                                icon={Boxes}
                                title={paquete.nombre}
                                description={paquete.descripcion}
                                price={paquete.precio}
                                imageUrl={paquete.imagenUrl}
                                badges={badges}
                                onEdit={() => openEditModal(paquete)}
                                onDelete={() => setConfirmDelete(paquete)}
                            />
                        );
                    })}
                </div>
            )}

            {/* MODAL */}
            <PaqueteFormModal
                isOpen={showModal}
                onClose={() => {
                    setShowModal(false);
                    setError('');
                }}
                paqueteToEdit={paqueteToEdit}
                serviciosDisponibles={servicios}
                onSave={handleSave}
                error={error}
            />

            {/* CONFIRMACIÓN DE DESACTIVACIÓN */}
            <ConfirmDialog
                isOpen={!!confirmDelete}
                title="¿Desactivar paquete?"
                confirmText="Desactivar"
                onCancel={() => setConfirmDelete(null)}
                onConfirm={() => confirmDelete && handleDeactivate(confirmDelete.id)}
                message={confirmDelete && (
                    <>
                        Se desactivará <span className="font-semibold text-gray-700 dark:text-gray-200">"{confirmDelete.nombre}"</span>. No afectará cotizaciones existentes. Podrás reactivarlo después.
                    </>
                )}
            />

            {/* TOAST DE FEEDBACK */}
            <Toast message={toast} onClose={() => setToast(null)} />
        </div>
    );
}
