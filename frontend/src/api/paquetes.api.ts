import api from './axiosConfig';
import type { Servicio } from './servicios.api';


/* ---------- Tipos del dominio "Paquete" ---------- */

// Una LÍNEA del paquete tal como la DEVUELVE el backend.
// interface → es una "forma de objeto" (regla híbrida).
export interface PaqueteItem {
    servicioId: number;
    cantidad: number;
    precioUnitario: number;   // lo congela el servidor (solo viene de salida)
    servicio?: Servicio;
}

// Un paquete COMO LO DEVUELVE el backend (PaqueteDTO de salida).
export interface Paquete {
    id: number;
    nombre: string;
    descripcion: string;
    precio: number;           // BigDecimal en Java → number en JSON
    imagenUrl: string;
    activo: boolean;
    items: PaqueteItem[];     // un paquete agrupa varias líneas
}

// Lo que se ENVÍA al crear/actualizar. El server ignora id/activo/precioUnitario.
export interface GuardarPaquete {
    nombre: string;
    descripcion?: string | null;   // el modal manda null si está vacío
    precio: number;
    imagenUrl?: string | null;
    items: { servicioId: number; cantidad: number }[];
    activo?: boolean;              // el modal manda activo: true
}

/* ---------- Llamadas a la API ---------- */

export const getPaquetes = async (): Promise<Paquete[]> => {
    const response = await api.get<Paquete[]>('/paquetes');
    return response.data;
};

export const createPaquete = async (data: GuardarPaquete): Promise<Paquete> => {
    const response = await api.post<Paquete>('/paquetes', data);
    return response.data;
};

export const updatePaquete = async (id: number, data: GuardarPaquete): Promise<Paquete> => {
    const response = await api.put<Paquete>(`/paquetes/${id}`, data);
    return response.data;
};

// DELETE devuelve 204 sin cuerpo → no hay nada útil que devolver (Promise<void>).
export const deactivatePaquete = async (id: number): Promise<void> => {
    await api.delete(`/paquetes/${id}`);
};
