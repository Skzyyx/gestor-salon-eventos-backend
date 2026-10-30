import { Pencil, Trash2 } from 'lucide-react';
import { motion } from 'framer-motion';
import { Turno } from '../api/turnos.api';

interface TurnoCardProps {
    turno: Turno;
    index?: number;
    onEdit?: () => void;
    onDelete?: () => void;
}

export default function TurnoCard({ turno, index = 0, onEdit, onDelete }: TurnoCardProps) {
    const inicio = turno.horaInicio?.substring(0, 5);
    const fin = turno.horaFin?.substring(0, 5);

    return (
        <motion.div
            initial={{ opacity: 0, y: 8 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: index * 0.04 }}
            className="group flex justify-between items-center bg-gray-50 dark:bg-dark-bg p-3 rounded-xl border border-gray-200 dark:border-dark-border hover:border-brand-300 dark:hover:border-brand-500/30 transition-colors"
        >
            <div>
                <span className="text-xs font-bold uppercase tracking-wide text-brand-600 dark:text-brand-300 bg-brand-100 dark:bg-brand-500/15 px-2 py-1 rounded-md">
                    {turno.tipoTurno}
                </span>
                <p className="text-sm font-semibold mt-2 text-gray-700 dark:text-gray-200 tabular-nums">
                    {inicio} - {fin}
                </p>
            </div>

            <div className="flex items-center gap-0.5 flex-shrink-0">
                {onEdit && (
                    <button
                        onClick={onEdit}
                        className="p-2 rounded-lg text-gray-400 hover:text-brand-500 hover:bg-brand-50 dark:hover:bg-brand-500/10 transition-colors"
                        title="Editar"
                    >
                        <Pencil size={16} />
                    </button>
                )}
                {onDelete && (
                    <button
                        onClick={onDelete}
                        className="p-2 rounded-lg text-gray-400 hover:text-red-500 hover:bg-red-50 dark:hover:bg-red-500/10 transition-colors"
                        title="Eliminar"
                    >
                        <Trash2 size={16} />
                    </button>
                )}
            </div>
        </motion.div>
    );
}
