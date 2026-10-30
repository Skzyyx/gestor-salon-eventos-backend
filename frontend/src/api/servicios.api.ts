import api from './axiosConfig';
import { Categoria } from './categorias.api';

/* ---------- Tipos del dominio "Servicio" ---------- */

// Enum de Java → unión de literales (la mitad 'type' de la regla híbrida). Espejo de TipoCobro.java.
export type TipoCobro = 'POR_EVENTO' | 'POR_HORA' | 'POR_PERSONA' | 'POR_UNIDAD';

// Un servicio COMO LO DEVUELVE el backend (ServicioDTO de salida).
export interface Servicio {
    id: number;
    nombre: string;
    descripcion: string;
    precio: number;           // BigDecimal → number
    imagenUrl?: string;
    activo: boolean;
    tipoCobro: TipoCobro;
    categoriaId?: number | null;
    categoria?: Categoria | null;
}

// Lo que se ENVÍA al crear/actualizar. El server ignora id/activo/categoria.
export interface GuardarServicio {
    nombre: string;
    descripcion?: string;
    precio: number;
    imagenUrl?: string;
    tipoCobro: TipoCobro;
    categoriaId?: number;      // solo el id al guardar
}

/* ---------- Llamadas a la API ---------- */

export const getServicios = async (): Promise<Servicio[]> => {
    const response = await api.get<Servicio[]>('/servicios');
    return response.data;
};

export const createServicio = async (data: GuardarServicio): Promise<Servicio> => {
    const response = await api.post<Servicio>('/servicios', data);
    return response.data;
};

export const updateServicio = async (id: number, data: GuardarServicio): Promise<Servicio> => {
    const response = await api.put<Servicio>(`/servicios/${id}`, data);
    return response.data;
};

// DELETE → 204 sin cuerpo (igual que paquetes).
export const deactivateServicio = async (id: number): Promise<void> => {
    await api.delete(`/servicios/${id}`);
};
