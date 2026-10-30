import axios from 'axios';
import { useState, useEffect } from 'react';
import { getTurnos, createTurno, updateTurno, deleteTurno } from '../api/turnos.api';
import { Clock, Plus, AlertCircle, Loader2, AlertTriangle } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import TurnoCard from '../components/TurnoCard';
import TurnoFormModal from '../components/TurnoFormModal';
import ConfirmDialog from '../components/ConfirmDialog';
import type { CrearTurno, Turno, DiaSemana } from '../api/turnos.api';
import { ORDEN_SEMANA } from '../constants/dias';

// El día viene del backend como DayOfWeek ('MONDAY'...); aquí su etiqueta en español.
const NOMBRE_DIA: Record<DiaSemana, string> = {
    MONDAY: 'Lunes',
    TUESDAY: 'Martes',
    WEDNESDAY: 'Miércoles',
    THURSDAY: 'Jueves',
    FRIDAY: 'Viernes',
    SATURDAY: 'Sábado',
    SUNDAY: 'Domingo',
};

export default function TurnosPage() {
    const [turnos, setTurnos] = useState<Turno[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    // Modal state
    const [showModal, setShowModal] = useState(false);
    const [turnoToEdit, setTurnoToEdit] = useState<Turno | null>(null);

    // Confirm delete modal state
    const [confirmDelete, setConfirmDelete] = useState<Turno | null>(null);

    const fetchTurnos = async () => {
        try {
            setLoading(true);
            const data = await getTurnos();
            setTurnos(data);
        } catch (err) {
            setError('Error al cargar los turnos');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchTurnos();
    }, []);

    const openCreateModal = () => {
        setTurnoToEdit(null);
        setError('');
        setShowModal(true);
    };

    const openEditModal = (turno: Turno) => {
        setTurnoToEdit(turno);
        setError('');
        setShowModal(true);
    };

    const handleSave = async (payload: CrearTurno, setSaving: (saving: boolean) => void) => {
        setError('');
        try {
            if (turnoToEdit) {
                await updateTurno(turnoToEdit.id, payload);
            } else {
                await createTurno(payload);
            }
            setShowModal(false);
            setTurnoToEdit(null);
            fetchTurnos();
        } catch (err) {
            const msg = axios.isAxiosError(err) ? err.response?.data?.message : undefined;
            setError(msg || (turnoToEdit ? 'Error al actualizar turno' : 'Error al crear turno'));
            setSaving(false);
        }
    };

    const handleDelete = async (id: number) => {
        try {
            await deleteTurno(id);
            setConfirmDelete(null);
            fetchTurnos();
        } catch (err) {
            setError('Error al eliminar');
            setConfirmDelete(null);
        }
    };

    // Días que realmente tienen turnos configurados (en orden de la semana)
    const diasConTurnos = ORDEN_SEMANA
        .map((dow) => ({ dow, nombre: NOMBRE_DIA[dow], turnos: turnos.filter(t => t.diaSemana === dow) }))
        .filter(d => d.turnos.length > 0);

    return (
        <div className="space-y-6">

            {/* HEADER CON BOTÓN DE AGREGAR */}
            <header className="flex flex-col sm:flex-row justify-between items-start sm:items-end gap-4">
                <div>
                    <h1 className="text-3xl font-black text-gray-900 dark:text-white flex items-center gap-2">
                        <Clock className="text-brand-500" /> Horarios por Turno
                    </h1>
                    <p className="text-gray-500 dark:text-gray-400 mt-1">Configura la disponibilidad para las reservas</p>
                </div>
                <motion.button
                    whileHover={{ scale: 1.03 }}
                    whileTap={{ scale: 0.97 }}
                    onClick={openCreateModal}
                    className="flex items-center gap-2 bg-brand-600 hover:bg-brand-700 dark:bg-brand-500 dark:hover:bg-brand-600 text-white font-semibold px-5 py-2.5 rounded-xl transition-all shadow-lg shadow-brand-500/25"
                >
                    <Plus size={18} />
                    Nuevo Turno
                </motion.button>
            </header>

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

            {/* LISTA DE TURNOS AGRUPADOS POR DÍA */}
            {loading ? (
                <div className="flex justify-center items-center h-48">
                    <Loader2 className="animate-spin text-brand-500" size={32} />
                </div>
            ) : diasConTurnos.length === 0 ? (
                <motion.div
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    className="bg-amber-50 dark:bg-amber-500/10 p-12 rounded-2xl border border-amber-200 dark:border-amber-500/20 text-center"
                >
                    <AlertTriangle className="mx-auto text-amber-500 mb-4" size={48} />
                    <p className="text-amber-700 dark:text-amber-400 text-lg font-semibold">No hay turnos configurados</p>
                    <p className="text-amber-600/80 dark:text-amber-500/70 text-sm mt-1.5 max-w-md mx-auto">
                        Tus clientes no podrán consultar disponibilidad ni solicitar cotizaciones hasta que agregues al menos un turno.
                    </p>
                </motion.div>
            ) : (
                <div className="space-y-4">
                    {diasConTurnos.map((dia, idx) => (
                        <motion.div
                            key={dia.dow}
                            initial={{ opacity: 0, y: 10 }}
                            animate={{ opacity: 1, y: 0 }}
                            transition={{ delay: idx * 0.05 }}
                            className="bg-white dark:bg-dark-surface p-5 rounded-2xl shadow-sm border border-gray-100 dark:border-dark-border"
                        >
                            {/* CABECERA DEL DÍA */}
                            <div className="flex items-center gap-3 mb-4">
                                <h2 className="text-lg font-bold text-gray-800 dark:text-white">{dia.nombre}</h2>
                                <span className="text-xs font-semibold text-gray-500 dark:text-gray-400 bg-gray-100 dark:bg-dark-bg px-2 py-0.5 rounded-full">
                                    {dia.turnos.length} {dia.turnos.length === 1 ? 'turno' : 'turnos'}
                                </span>
                            </div>

                            {/* CAJITAS DE TURNOS DEL DÍA */}
                            <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-3">
                                {dia.turnos.map((turno, index) => (
                                    <TurnoCard
                                        key={turno.id}
                                        turno={turno}
                                        index={index}
                                        onEdit={() => openEditModal(turno)}
                                        onDelete={() => setConfirmDelete(turno)}
                                    />
                                ))}
                            </div>
                        </motion.div>
                    ))}
                </div>
            )}

            {/* ==================== MODAL DE CREAR/EDITAR ==================== */}
            <TurnoFormModal
                isOpen={showModal}
                onClose={() => {
                    setShowModal(false);
                    setError('');
                }}
                turnoToEdit={turnoToEdit}
                onSave={handleSave}
                error={error}
                turnos={turnos}
            />


            {/* ==================== CONFIRMACIÓN DE BORRADO ==================== */}
            <ConfirmDialog
                isOpen={!!confirmDelete}
                title="¿Eliminar turno?"
                confirmText="Eliminar"
                onCancel={() => setConfirmDelete(null)}
                onConfirm={() => confirmDelete && handleDelete(confirmDelete.id)}
                message={confirmDelete && (
                    <>
                        Se eliminará el turno{' '}
                        <span className="font-semibold text-gray-700 dark:text-gray-200">
                            {confirmDelete.tipoTurno?.toLowerCase()} de {NOMBRE_DIA[confirmDelete.diaSemana]} ({confirmDelete.horaInicio?.substring(0, 5)} - {confirmDelete.horaFin?.substring(0, 5)})
                        </span>
                        . Esta acción no se puede deshacer.
                    </>
                )}
            />
        </div>
    );
}
