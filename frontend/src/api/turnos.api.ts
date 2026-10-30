import api from './axiosConfig';

// El backend ahora usa java.time.DayOfWeek → Jackson lo serializa como el nombre en inglés.
export type DiaSemana = 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY';
export type TipoTurno = 'MAÑANA' | 'TARDE' | 'NOCHE';

const NOMBRE_DIA: Record<DiaSemana, string> = {
    MONDAY: 'Lunes',
    TUESDAY: 'Martes',
    WEDNESDAY: 'Miércoles',
    THURSDAY: 'Jueves',
    FRIDAY: 'Viernes',
    SATURDAY: 'Sábado',
    SUNDAY: 'Domingo',
};

export interface Turno {
    id: number;
    diaSemana: DiaSemana;
    tipoTurno: TipoTurno;
    horaInicio: string;   // LocalTime → "HH:mm:ss"
    horaFin: string;
    activo: boolean;
}

export interface CrearTurno {
    diaSemana: DiaSemana;
    tipoTurno: TipoTurno;
    horaInicio: string;
    horaFin: string;
}

export const getTurnos = async (): Promise<Turno[]> => {
    const response = await api.get<Turno[]>('/turnos');
    return response.data;
};

export const createTurno = async (turnoData: CrearTurno): Promise<Turno> => {
    const response = await api.post<Turno>('/turnos', turnoData);
    return response.data;
};

export const updateTurno = async (id: number, turnoData: CrearTurno): Promise<Turno> => {
    const response = await api.put<Turno>(`/turnos/${id}`, turnoData);
    return response.data;
};

export const deleteTurno = async (id: number): Promise<void> => {
    await api.delete(`/turnos/${id}`);
};
