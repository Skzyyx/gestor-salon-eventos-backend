import axios from 'axios';
import { useState, useEffect, useMemo } from 'react';
import { getClientes, createCliente, updateCliente, deactivateCliente, Cliente, CrearCliente } from '../api/clientes.api';
import { Users, Plus, AlertCircle, Loader2, Search, X, SearchX, UserPlus } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import CatalogItemCard, { Badge } from '../components/CatalogItemCard';
import ClienteFormModal from '../components/ClienteFormModal';
import CustomSelect from '../components/CustomSelect';
import ConfirmDialog from '../components/ConfirmDialog';
import Toast, { ToastMessage, ToastTipo } from '../components/Toast';

const SORT_OPTIONS = [
    { value: 'nombre_asc', label: 'Nombre (A-Z)' },
    { value: 'nombre_desc', label: 'Nombre (Z-A)' },
];

export default function ClientesPage() {
    const [clientes, setClientes] = useState<Cliente[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [toast, setToast] = useState<ToastMessage | null>(null);

    const [showModal, setShowModal] = useState(false);
    const [clienteToEdit, setClienteToEdit] = useState<Cliente | null>(null);
    const [confirmDelete, setConfirmDelete] = useState<Cliente | null>(null);

    const [searchTerm, setSearchTerm] = useState('');
    const [sortOrder, setSortOrder] = useState('');

    const hasActiveFilters = searchTerm || sortOrder;

    const clearFilters = () => {
        setSearchTerm('');
        setSortOrder('');
    };

    // Toast con auto-descarte
    const showToast = (text: string, type: ToastTipo) => setToast({ text, type });


    useEffect(() => {
        if (!toast) return;
        const t = setTimeout(() => setToast(null), 3500);
        return () => clearTimeout(t);
    }, [toast]);

    const filteredClientes = useMemo(() => {
        const term = searchTerm.trim().toLowerCase();
        let result = clientes.filter((c) => {
            if (!term) return true;
            return (
                c.nombre?.toLowerCase().includes(term) ||
                c.telefono?.toLowerCase().includes(term) ||
                c.email?.toLowerCase().includes(term)
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
    }, [clientes, searchTerm, sortOrder]);

    const fetchClientes = async () => {
        try {
            setLoading(true);
            const data = await getClientes();
            setClientes(data.filter((c) => c.activo));
        } catch (err) {
            setError('Error al cargar clientes');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchClientes();
    }, []);

    const openCreateModal = () => {
        setClienteToEdit(null);
        setError('');
        setShowModal(true);
    };

    const openEditModal = (cliente: Cliente) => {
        setClienteToEdit(cliente);
        setError('');
        setShowModal(true);
    };

    const handleSave = async (payload: CrearCliente, setSaving: (value: boolean) => void) => {
        setError('');
        try {
            if (clienteToEdit) {
                await updateCliente(clienteToEdit.id, payload);
                showToast('Cliente actualizado correctamente.', 'success');
            } else {
                await createCliente(payload);
                showToast('Cliente registrado correctamente.', 'success');
            }
            setShowModal(false);
            setClienteToEdit(null);
            fetchClientes();
        } catch (err) {
            const msg = axios.isAxiosError(err) ? err.response?.data?.message : undefined;
            setError(msg || (clienteToEdit ? 'Error al actualizar cliente' : 'Error al crear cliente'));
            setSaving(false);
        }
    };

    const handleDeactivate = async (id: number) => {
        try {
            await deactivateCliente(id);
            setConfirmDelete(null);
            showToast('Cliente desactivado.', 'success');
            fetchClientes();
        } catch (err) {
            setConfirmDelete(null);
            showToast('No se pudo desactivar el cliente.', 'error');
        }
    };

    return (
        <div className="space-y-6">

            {/* HEADER */}
            <header className="flex flex-col sm:flex-row justify-between items-start sm:items-end gap-4">
                <div>
                    <h1 className="text-3xl font-black text-gray-900 dark:text-white flex items-center gap-2">
                        <Users className="text-brand-500" /> Gestión de Clientes
                    </h1>
                    <p className="text-gray-500 dark:text-gray-400 mt-1">Directorio de clientes de tu salón</p>
                </div>
                <motion.button
                    whileHover={{ scale: 1.03 }}
                    whileTap={{ scale: 0.97 }}
                    onClick={openCreateModal}
                    className="flex items-center gap-2 bg-brand-600 hover:bg-brand-700 dark:bg-brand-500 dark:hover:bg-brand-600 text-white font-semibold px-5 py-2.5 rounded-xl transition-all shadow-lg shadow-brand-500/25"
                >
                    <Plus size={18} />
                    Nuevo Cliente
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
                        placeholder="Buscar por nombre, teléfono o correo..."
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
                            {filteredClientes.length} de {clientes.length} cliente{clientes.length !== 1 ? 's' : ''}
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
            ) : clientes.length === 0 ? (
                <motion.div
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    className="bg-white/80 dark:bg-dark-surface/80 backdrop-blur-xl p-12 rounded-2xl border border-gray-100 dark:border-dark-border text-center"
                >
                    <UserPlus className="mx-auto text-gray-300 dark:text-gray-600 mb-4" size={48} />
                    <p className="text-gray-500 dark:text-gray-400 text-lg font-medium">Aún no tienes clientes registrados.</p>
                    <p className="text-gray-400 dark:text-gray-500 text-sm mt-1">Usa el botón "Nuevo Cliente" para agregar el primero.</p>
                </motion.div>
            ) : filteredClientes.length === 0 ? (
                <motion.div
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    className="bg-white/80 dark:bg-dark-surface/80 backdrop-blur-xl p-12 rounded-2xl border border-gray-100 dark:border-dark-border text-center"
                >
                    <SearchX className="mx-auto text-gray-300 dark:text-gray-600 mb-4" size={48} />
                    <p className="text-gray-500 dark:text-gray-400 text-lg font-medium">No se encontraron clientes.</p>
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
                    {filteredClientes.map((cliente, index) => {
                        const badges: Badge[] = [];
                        if (cliente.direccion?.ciudad) {
                            badges.push({ text: cliente.direccion.ciudad, variant: 'brand' });
                        }
                        if (cliente.email) {
                            badges.push({ text: cliente.email, variant: 'gray' });
                        }
                        return (
                            <CatalogItemCard
                                key={cliente.id}
                                index={index}
                                icon={Users}
                                title={cliente.nombre}
                                description={cliente.telefono}
                                badges={badges}
                                onEdit={() => openEditModal(cliente)}
                                onDelete={() => setConfirmDelete(cliente)}
                            />
                        );
                    })}
                </div>
            )}

            {/* MODAL */}
            <ClienteFormModal
                isOpen={showModal}
                onClose={() => {
                    setShowModal(false);
                    setError('');
                }}
                clienteToEdit={clienteToEdit}
                clientesExistentes={clientes}
                onSave={handleSave}
                error={error}
            />

            {/* CONFIRMACIÓN DE DESACTIVACIÓN */}
            <ConfirmDialog
                isOpen={!!confirmDelete}
                title="¿Desactivar cliente?"
                confirmText="Desactivar"
                onCancel={() => setConfirmDelete(null)}
                onConfirm={() => handleDeactivate(confirmDelete!.id)}
                message={confirmDelete && (
                    <>
                        Se desactivará <span className="font-semibold text-gray-700 dark:text-gray-200">"{confirmDelete.nombre}"</span>. Sus eventos no se cancelan. Podrás reactivarlo después.
                    </>
                )}
            />

            {/* TOAST DE FEEDBACK */}
            <Toast message={toast} onClose={() => setToast(null)} />
        </div>
    );
}
