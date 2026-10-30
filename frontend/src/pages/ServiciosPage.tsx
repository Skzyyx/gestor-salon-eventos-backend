import { useState, useEffect, useMemo } from 'react';
import { getServicios, createServicio, updateServicio, deactivateServicio, Servicio, GuardarServicio } from '../api/servicios.api';
import { getCategorias, Categoria } from '../api/categorias.api';
import { Briefcase, Plus, AlertCircle, Loader2, Package, Search, X, SearchX } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import CatalogItemCard, { Badge } from '../components/CatalogItemCard';
import ServicioFormModal from '../components/ServicioFormModal';
import CustomSelect from '../components/CustomSelect';
import ConfirmDialog from '../components/ConfirmDialog';
import axios from 'axios';

// Opciones de filtro por tipo de cobro (alineadas con el enum TipoCobro del backend)
const TIPO_COBRO_OPTIONS = [
    { value: 'POR_EVENTO', label: 'Por evento' },
    { value: 'POR_HORA', label: 'Por hora' },
    { value: 'POR_PERSONA', label: 'Por persona' },
    { value: 'POR_UNIDAD', label: 'Por unidad' },
];

// Opciones de ordenamiento
const SORT_OPTIONS = [
    { value: 'nombre_asc', label: 'Nombre (A-Z)' },
    { value: 'nombre_desc', label: 'Nombre (Z-A)' },
    { value: 'precio_asc', label: 'Precio (menor a mayor)' },
    { value: 'precio_desc', label: 'Precio (mayor a menor)' },
];

export default function ServiciosPage() {
    const [servicios, setServicios] = useState<Servicio[]>([]);
    const [categorias, setCategorias] = useState<Categoria[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    // Modal state
    const [showModal, setShowModal] = useState(false);
    const [servicioToEdit, setServicioToEdit] = useState<Servicio | null>(null);

    // Confirm delete modal state
    const [confirmDelete, setConfirmDelete] = useState<Servicio | null>(null);

    // Filtros y búsqueda
    const [searchTerm, setSearchTerm] = useState('');
    const [filterCategoria, setFilterCategoria] = useState('');
    const [filterTipoCobro, setFilterTipoCobro] = useState('');
    const [sortOrder, setSortOrder] = useState('');

    const hasActiveFilters = searchTerm || filterCategoria || filterTipoCobro || sortOrder;

    const clearFilters = () => {
        setSearchTerm('');
        setFilterCategoria('');
        setFilterTipoCobro('');
        setSortOrder('');
    };

    const categoriaOptions = useMemo(
        () => categorias.map((c) => ({ value: String(c.id), label: c.nombre })),
        [categorias]
    );

    // Lista filtrada + ordenada (en cliente). Se recalcula solo cuando cambia algo relevante.
    const filteredServicios = useMemo(() => {
        const term = searchTerm.trim().toLowerCase();

        let result = servicios.filter((s) => {
            const matchTerm =
                !term ||
                s.nombre?.toLowerCase().includes(term) ||
                s.descripcion?.toLowerCase().includes(term);
            const matchCategoria =
                !filterCategoria || (s.categoria && String(s.categoria.id) === filterCategoria);
            const matchTipoCobro = !filterTipoCobro || s.tipoCobro === filterTipoCobro;
            return matchTerm && matchCategoria && matchTipoCobro;
        });

        if (sortOrder) {
            result = [...result].sort((a, b) => {
                switch (sortOrder) {
                    case 'nombre_asc':
                        return (a.nombre || '').localeCompare(b.nombre || '');
                    case 'nombre_desc':
                        return (b.nombre || '').localeCompare(a.nombre || '');
                    case 'precio_asc':
                        return (a.precio ?? 0) - (b.precio ?? 0);
                    case 'precio_desc':
                        return (b.precio ?? 0) - (a.precio ?? 0);
                    default:
                        return 0;
                }
            });
        }

        return result;
    }, [servicios, searchTerm, filterCategoria, filterTipoCobro, sortOrder]);

    const fetchServicios = async () => {
        try {
            setLoading(true);
            const [serviciosData, categoriasData] = await Promise.all([
                getServicios(),
                getCategorias()
            ]);
            setServicios(serviciosData.filter(s => s.activo));
            setCategorias(categoriasData.filter(c => c.activa !== false));
        } catch (err) {
            setError('Error al cargar servicios');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchServicios();
    }, []);

    const openCreateModal = () => {
        setServicioToEdit(null);
        setError('');
        setShowModal(true);
    };

    const openEditModal = (servicio: Servicio) => {
        setServicioToEdit(servicio);
        setError('');
        setShowModal(true);
    };

    const handleSave = async (payload: GuardarServicio, setSaving: (saving: boolean) => void) => {
        setError('');
        try {
            if (servicioToEdit) {
                await updateServicio(servicioToEdit.id, payload);
            } else {
                await createServicio(payload);
            }
            setShowModal(false);
            setServicioToEdit(null);
            fetchServicios();
        } catch (err) {
            const msg = axios.isAxiosError(err) ? err.response?.data?.message : undefined;
            setError(msg || (servicioToEdit ? 'Error al actualizar servicio' : 'Error al crear servicio'));
            setSaving(false);
        }
    };

    const handleDeactivate = async (id: number) => {
        try {
            await deactivateServicio(id);
            setConfirmDelete(null);
            fetchServicios();
        } catch (err) {
            setError('Error al desactivar');
            setConfirmDelete(null);
        }
    };

    return (
        <div className="space-y-6">

            {/* HEADER CON BOTÓN DE AGREGAR */}
            <header className="flex flex-col sm:flex-row justify-between items-start sm:items-end gap-4">
                <div>
                    <h1 className="text-3xl font-black text-gray-900 dark:text-white flex items-center gap-2">
                        <Briefcase className="text-brand-500" /> Gestión de Servicios
                    </h1>
                    <p className="text-gray-500 dark:text-gray-400 mt-1">Catálogo de extras ofrecidos para eventos</p>
                </div>
                <motion.button
                    whileHover={{ scale: 1.03 }}
                    whileTap={{ scale: 0.97 }}
                    onClick={openCreateModal}
                    className="flex items-center gap-2 bg-brand-600 hover:bg-brand-700 dark:bg-brand-500 dark:hover:bg-brand-600 text-white font-semibold px-5 py-2.5 rounded-xl transition-all shadow-lg shadow-brand-500/25"
                >
                    <Plus size={18} />
                    Nuevo Servicio
                </motion.button>
            </header>

            {/* ==================== BARRA DE BÚSQUEDA + FILTROS ==================== */}
            <motion.div
                initial={{ opacity: 0, y: -10 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.3 }}
                className="relative z-30 flex flex-col lg:flex-row gap-3 bg-white/70 dark:bg-dark-surface/70 backdrop-blur-xl p-3 rounded-2xl border border-gray-100 dark:border-dark-border shadow-sm"
            >
                {/* Búsqueda dinámica por texto */}
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

                {/* Filtros + orden */}
                <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 lg:flex">
                    <div className="w-full lg:w-44">
                        <CustomSelect
                            options={categoriaOptions}
                            value={filterCategoria}
                            onChange={setFilterCategoria}
                            placeholder="Categoría"
                        />
                    </div>
                    <div className="w-full lg:w-44">
                        <CustomSelect
                            options={TIPO_COBRO_OPTIONS}
                            value={filterTipoCobro}
                            onChange={setFilterTipoCobro}
                            placeholder="Tipo de cobro"
                        />
                    </div>
                    <div className="w-full lg:w-48">
                        <CustomSelect
                            options={SORT_OPTIONS}
                            value={sortOrder}
                            onChange={setSortOrder}
                            placeholder="Ordenar por"
                        />
                    </div>
                </div>
            </motion.div>

            {/* Resumen de resultados + limpiar filtros */}
            <AnimatePresence>
                {hasActiveFilters && !loading && (
                    <motion.div
                        initial={{ opacity: 0, height: 0 }}
                        animate={{ opacity: 1, height: 'auto' }}
                        exit={{ opacity: 0, height: 0 }}
                        className="flex items-center justify-between text-sm -mt-2 px-1"
                    >
                        <span className="text-gray-500 dark:text-gray-400">
                            {filteredServicios.length} de {servicios.length} servicio{servicios.length !== 1 ? 's' : ''}
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

            {/* LISTA DE TARJETAS HORIZONTALES */}
            {loading ? (
                <div className="flex justify-center items-center h-48">
                    <Loader2 className="animate-spin text-brand-500" size={32} />
                </div>
            ) : servicios.length === 0 ? (
                <motion.div
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    className="bg-white/80 dark:bg-dark-surface/80 backdrop-blur-xl p-12 rounded-2xl border border-gray-100 dark:border-dark-border text-center"
                >
                    <Package className="mx-auto text-gray-300 dark:text-gray-600 mb-4" size={48} />
                    <p className="text-gray-500 dark:text-gray-400 text-lg font-medium">No hay servicios registrados.</p>
                    <p className="text-gray-400 dark:text-gray-500 text-sm mt-1">Usa el botón "Nuevo Servicio" para agregar tu primer servicio.</p>
                </motion.div>
            ) : filteredServicios.length === 0 ? (
                <motion.div
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    className="bg-white/80 dark:bg-dark-surface/80 backdrop-blur-xl p-12 rounded-2xl border border-gray-100 dark:border-dark-border text-center"
                >
                    <SearchX className="mx-auto text-gray-300 dark:text-gray-600 mb-4" size={48} />
                    <p className="text-gray-500 dark:text-gray-400 text-lg font-medium">No se encontraron servicios.</p>
                    <p className="text-gray-400 dark:text-gray-500 text-sm mt-1">Prueba ajustando la búsqueda o los filtros aplicados.</p>
                    <button
                        onClick={clearFilters}
                        className="mt-4 inline-flex items-center gap-1.5 text-sm font-semibold text-brand-600 dark:text-brand-400 hover:text-brand-700 dark:hover:text-brand-300 transition-colors"
                    >
                        <X size={14} /> Limpiar filtros
                    </button>
                </motion.div>
            ) : (
                <div className="space-y-3">
                    {filteredServicios.map((servicio, index) => {
                        const badges: Badge[] = [];
                        if (servicio.categoria) {
                            badges.push({ text: servicio.categoria.nombre, variant: 'brand' });
                        }
                        if (servicio.tipoCobro) {
                            badges.push({ text: servicio.tipoCobro.replace('_', ' '), variant: 'gray' });
                        }

                        return (
                            <CatalogItemCard
                                key={servicio.id}
                                index={index}
                                title={servicio.nombre}
                                description={servicio.descripcion}
                                price={servicio.precio}
                                imageUrl={servicio.imagenUrl}
                                badges={badges}
                                onEdit={() => openEditModal(servicio)}
                                onDelete={() => setConfirmDelete(servicio)}
                            />
                        );
                    })}
                </div>
            )}

            {/* ==================== MODAL DE CREAR/EDITAR ==================== */}
            <ServicioFormModal
                isOpen={showModal}
                onClose={() => {
                    setShowModal(false);
                    setError('');
                }}
                servicioToEdit={servicioToEdit}
                categorias={categorias}
                onSave={handleSave}
                error={error}
            />

            {/* ==================== CONFIRMACIÓN DE DESACTIVACIÓN ==================== */}
            <ConfirmDialog
                isOpen={!!confirmDelete}
                title="¿Desactivar servicio?"
                confirmText="Desactivar"
                onCancel={() => setConfirmDelete(null)}
                onConfirm={() => handleDeactivate(confirmDelete!.id)}
                message={confirmDelete && (
                    <>
                        Se desactivará <span className="font-semibold text-gray-700 dark:text-gray-200">"{confirmDelete.nombre}"</span>. Podrás reactivarlo después.
                    </>
                )}
            />
        </div>
    );
}
