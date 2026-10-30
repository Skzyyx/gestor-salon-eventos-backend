import type { TipoTurno } from '../api/turnos.api';

// Etiquetas en español para cada tipo de turno (presentación). Paralelo a dias.ts.
export const NOMBRE_TURNO: Record<TipoTurno, string> = {
    MAÑANA: 'Mañana',
    TARDE: 'Tarde',
    NOCHE: 'Noche',
};
export const ORDEN_TIPO: TipoTurno[] = ['MAÑANA', 'TARDE', 'NOCHE'];
