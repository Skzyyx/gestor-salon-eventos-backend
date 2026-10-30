import { Direccion } from '../types/direccion';
import api from './axiosConfig';



export interface Cliente {
    id: number;
    nombre: string;
    telefono: string;
    email: string | null;
    direccion: Direccion;
    activo: boolean;
}

export interface CrearCliente {
    nombre: string;
    telefono: string;
    email: string | null;
    direccion: Direccion | null;
}

export const getClientes = async (): Promise<Cliente[]> => {
    const response = await api.get('/clientes');
    return response.data;
};

export const createCliente = async (clienteData: CrearCliente): Promise<Cliente> => {
    const response = await api.post('/clientes', clienteData);
    return response.data;
};

export const updateCliente = async (id: number, clienteData: CrearCliente): Promise<Cliente> => {
    const response = await api.put(`/clientes/${id}`, clienteData);
    return response.data;
};

export const deactivateCliente = async (id: number): Promise<void> => {
    await api.delete(`/clientes/${id}`);
};
