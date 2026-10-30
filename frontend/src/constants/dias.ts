import type { DiaSemana } from '../api/turnos.api';

export const NOMBRE_DIA: Record<DiaSemana, string> = {
    MONDAY: 'Lunes', TUESDAY: 'Martes', WEDNESDAY: 'Miércoles', THURSDAY: 'Jueves',
    FRIDAY: 'Viernes', SATURDAY: 'Sábado', SUNDAY: 'Domingo',
};
export const ORDEN_SEMANA: DiaSemana[] = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];
