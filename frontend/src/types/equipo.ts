export interface RolNegocio {
    id: number;
    nombre: string;
    descripcion: string;
    esSistema: boolean;
}

export interface Trabajador {
    id: number;
    nombre: string;
    correo: string;
    activo: boolean;
    rol: RolNegocio;
}

export interface CrearTrabajadorRequest {
    nombre: string;
    correo: string;
    password?: string;
    rolNegocioId: number;
}

export interface ActualizarTrabajadorRequest {
    nombre: string;
    rolNegocioId: number;
}
