import api from './axiosConfig';
import { Trabajador, CrearTrabajadorRequest, ActualizarTrabajadorRequest } from '../types/equipo';

export const getEquipo = async (): Promise<Trabajador[]> => {
    const response = await api.get('/equipo');
    return response.data;
};

export const crearTrabajador = async (data: CrearTrabajadorRequest): Promise<Trabajador> => {
    const response = await api.post('/equipo', data);
    return response.data;
};

export const actualizarTrabajador = async (id: number, data: ActualizarTrabajadorRequest): Promise<Trabajador> => {
    const response = await api.put(`/equipo/${id}`, data);
    return response.data;
};

export const cambiarEstadoTrabajador = async (id: number, activo: boolean): Promise<Trabajador> => {
    const response = await api.patch(`/equipo/${id}/estado`, null, { params: { activo } });
    return response.data;
};
