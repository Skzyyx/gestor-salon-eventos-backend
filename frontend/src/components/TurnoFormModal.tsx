import { useState, useEffect, type SubmitEvent } from 'react';
import { Clock, CalendarDays, Loader2, Hourglass } from 'lucide-react';
import CustomSelect from './CustomSelect';
import CustomTimePicker from './CustomTimePicker';
import Modal from './Modal';
import type { CrearTurno, DiaSemana, TipoTurno, Turno } from '../api/turnos.api';
import { NOMBRE_DIA, ORDEN_SEMANA } from '../constants/dias';
import { NOMBRE_TURNO, ORDEN_TIPO } from '../constants/tipoTurnos';

// Opciones de los selects (formato { value, label } que pide CustomSelect), derivadas de las constantes.
const DIA_OPTIONS = ORDEN_SEMANA.map((dow) => ({ value: dow, label: NOMBRE_DIA[dow] }));
const TIPO_OPTIONS = ORDEN_TIPO.map((t) => ({ value: t, label: NOMBRE_TURNO[t] }));

interface TurnoFormModalProps {
    isOpen: boolean;
    onClose: () => void;
    turnoToEdit: Turno | null;
    onSave: (turnoData: CrearTurno, setSaving: (saving: boolean) => void) => void;
    error: string;
    turnos: Turno[];
}

export default function TurnoFormModal({ isOpen, onClose, turnoToEdit, onSave, error, turnos }: TurnoFormModalProps) {
    const [diaSemana, setDiaSemana] = useState<DiaSemana | null>(null);
    const [tipoTurno, setTipoTurno] = useState<TipoTurno | null>(null);
    const [horaInicio, setHoraInicio] = useState('08:00');
    const [horaFin, setHoraFin] = useState('14:00');
    const [saving, setSaving] = useState(false);
    const [localError, setLocalError] = useState('');

    useEffect(() => {
        if (isOpen) {
            if (turnoToEdit) {
                setDiaSemana(turnoToEdit.diaSemana);
                setTipoTurno(turnoToEdit.tipoTurno);
                setHoraInicio(turnoToEdit.horaInicio.substring(0, 5));
                setHoraFin(turnoToEdit.horaFin.substring(0, 5));
            } else {
                setDiaSemana(null);
                setTipoTurno(null);
                setHoraInicio('08:00');
                setHoraFin('14:00');
            }
            setSaving(false);
            setLocalError('');
        }
    }, [isOpen, turnoToEdit]);

    // Limpia el aviso de validación en cuanto el usuario ajusta cualquier campo
    useEffect(() => {
        setLocalError('');
    }, [diaSemana, tipoTurno, horaInicio, horaFin]);

    const handleSubmit = async (e: SubmitEvent<HTMLFormElement>) => {
        e.preventDefault();

        // Día y tipo son obligatorios: tras este guard, TS sabe que NO son null.
        if (!diaSemana || !tipoTurno) {
            setLocalError('Selecciona el día y el tipo de turno.');
            return;
        }

        // --- Validaciones de cliente (feedback instantáneo, sin ir al backend) ---
        // 1) La hora de inicio debe ser anterior a la de fin (FA-05.1)
        if (horaInicio >= horaFin) {
            setLocalError('La hora de inicio debe ser anterior a la hora de fin.');
            return;
        }
        // 2) El turno no debe solaparse con otro del mismo día (FA-05.2)
        const conflicto = turnos.find((t) =>
            t.diaSemana === diaSemana &&
            t.id !== turnoToEdit?.id &&
            horaInicio < t.horaFin.substring(0, 5) &&
            horaFin > t.horaInicio.substring(0, 5)
        );
        if (conflicto) {
            setLocalError(
                `Se solapa con el turno ${conflicto.tipoTurno.toLowerCase()} (${conflicto.horaInicio.substring(0, 5)}–${conflicto.horaFin.substring(0, 5)}). Ajusta el horario.`
            );
            return;
        }

        // --- Pasó las validaciones: enviamos ---
        setSaving(true);
        const payload: CrearTurno = {
            diaSemana,
            tipoTurno,
            horaInicio: horaInicio + ':00',
            horaFin: horaFin + ':00',
        };
        await onSave(payload, setSaving);
    };

    return (
        <Modal
            isOpen={isOpen}
            onClose={onClose}
            icon={Clock}
            title={turnoToEdit ? 'Editar Turno' : 'Nuevo Turno'}
            subtitle={turnoToEdit ? 'Modifica el horario seleccionado.' : 'Configura la disponibilidad para las reservas.'}
            error={localError || error}
        >
            <form onSubmit={handleSubmit} className="space-y-5">
                {/* Día de la semana */}
                <div className="space-y-1.5">
                    <label className="text-sm font-semibold text-gray-700 dark:text-gray-300 ml-1">Día de la semana</label>
                    <CustomSelect
                        options={DIA_OPTIONS}
                        value={diaSemana ?? ''}
                        onChange={(v) => setDiaSemana(v ? (v as DiaSemana) : null)}
                        placeholder="Seleccione un día"
                        icon={CalendarDays}
                    />
                </div>

                {/* Tipo de turno */}
                <div className="space-y-1.5">
                    <label className="text-sm font-semibold text-gray-700 dark:text-gray-300 ml-1">Tipo de turno</label>
                    <CustomSelect
                        options={TIPO_OPTIONS}
                        value={tipoTurno ?? ''}
                        onChange={(v) => setTipoTurno(v ? (v as TipoTurno) : null)}
                        placeholder="Seleccione un tipo"
                        icon={Hourglass}
                    />
                </div>

                {/* Horas */}
                <div className="grid grid-cols-2 gap-4">
                    <div className="space-y-1.5">
                        <label className="text-sm font-semibold text-gray-700 dark:text-gray-300 ml-1">Hora inicio</label>
                        <CustomTimePicker value={horaInicio} onChange={setHoraInicio} placeholder="--:--" />
                    </div>
                    <div className="space-y-1.5">
                        <label className="text-sm font-semibold text-gray-700 dark:text-gray-300 ml-1">Hora fin</label>
                        <CustomTimePicker value={horaFin} onChange={setHoraFin} placeholder="--:--" />
                    </div>
                </div>

                {/* Botones del modal */}
                <div className="flex gap-3 pt-6 mt-2 border-t border-gray-100 dark:border-dark-border">
                    <button
                        type="button"
                        onClick={onClose}
                        className="flex-1 py-3 rounded-xl border border-gray-200 dark:border-dark-border text-gray-600 dark:text-gray-400 font-semibold hover:bg-gray-50 dark:hover:bg-dark-bg transition-colors"
                    >
                        Cancelar
                    </button>
                    <button
                        type="submit"
                        disabled={saving}
                        className="flex-1 bg-brand-600 hover:bg-brand-700 dark:bg-brand-500 dark:hover:bg-brand-600 text-white font-bold py-3 rounded-xl transition-all shadow-lg shadow-brand-500/30 flex justify-center items-center gap-2 disabled:opacity-70 disabled:cursor-not-allowed"
                    >
                        {saving ? (
                            <>
                                <Loader2 className="animate-spin" size={18} />
                                <span>{turnoToEdit ? 'Guardando...' : 'Registrando...'}</span>
                            </>
                        ) : (
                            <span>{turnoToEdit ? 'Guardar Cambios' : 'Registrar Turno'}</span>
                        )}
                    </button>
                </div>
            </form>
        </Modal>
    );
}
