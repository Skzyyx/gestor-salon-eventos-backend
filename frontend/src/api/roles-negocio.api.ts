import api from './axiosConfig';
import { RolNegocio } from '../types/equipo';

export const getRolesNegocio = async (): Promise<RolNegocio[]> => {
    const response = await api.get('/roles-negocio');
    return response.data;
};
