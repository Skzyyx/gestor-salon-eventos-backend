import api from './axiosConfig';

export interface Categoria {
    id: number;
    nombre: string;
    descripcion?: string;
    activa: boolean;          // ojo: 'activa' (femenino), como en el DTO
}

export const getCategorias = async (): Promise<Categoria[]> => {
    const response = await api.get<Categoria[]>('/categorias');
    return response.data;
};
