import api from './axiosConfig';

// El interceptor de axiosConfig ya adjunta el token del SUPERADMIN automáticamente,
// así que aquí solo nos preocupamos por las rutas y los datos.

export interface CrearNegocio {
    nombre: string;
    slug: string;
    adminNombre: string;
    adminCorreo: string;
    adminPassword: string;
}

export interface Negocio {
    id: number;
    nombre: string;
    slug: string;
    logoUrl?: string;
    telefono?: string;
    email?: string;
    descripcion?: string;
    activo?: boolean;
}

/**
 * Trae todos los negocios de la plataforma. (GET /admin/negocios)
 */
export const getNegocios = async (): Promise<Negocio[]> => {
    const response = await api.get('/admin/negocios');
    return response.data;
};

/**
 * Crea un negocio nuevo junto con su admin inicial. (POST /admin/negocios)
 * @param {object} data - { nombre, slug, adminNombre, adminCorreo, adminPassword }
 * Devuelve { negocio, admin } según nuestro NegocioAdminResponse del backend.
 */
export const crearNegocio = async (data: CrearNegocio): Promise<Negocio> => {
    const response = await api.post('/admin/negocios', data);
    return response.data;
};
