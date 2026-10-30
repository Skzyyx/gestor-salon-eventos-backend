import axios from 'axios';

// Creamos una instancia de Axios apuntando al puerto donde vive Spring Boot
const api = axios.create({
    baseURL: 'http://localhost:8080/api/v1',
});

// EL INTERCEPTOR: Se ejecuta ANTES de que cualquier petición salga de React
api.interceptors.request.use(
    (config) => {
        // Buscamos si hay un token guardado en el LocalStorage del navegador
        const token = localStorage.getItem('token');

        // Si hay token, se lo pegamos en la cabecera (exactamente como lo espera nuestro JwtAuthenticationFilter)
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

// EL INTERCEPTOR DE RESPUESTA: Atrapa los errores del Backend
api.interceptors.response.use(
    (response) => {
        // Si todo sale bien, solo devolvemos la respuesta
        return response;
    },
    (error) => {
        // 401 = Unauthorized (Token falso, expirado o ausente). Expulsión total.
        if (error.response && error.response.status === 401) {
            console.error("Seguridad: Token inválido o expirado. Expulsando usuario...");
            localStorage.removeItem('token');
            localStorage.removeItem('nombre');
            localStorage.removeItem('rol');
            window.location.href = '/login';
        }
        // 403 = Forbidden (Token válido, pero no tienes permisos para esta acción en específico)
        else if (error.response && error.response.status === 403) {
            console.error("Seguridad: No tienes permisos para realizar esta acción. Estado del frontend posiblemente corrupto. Redirigiendo a pantalla de sincronización...");
            // Redirigimos a nuestra nueva pantalla 403 para que el usuario pueda sincronizar sus datos
            window.location.href = '/restringido';
        }
        return Promise.reject(error);
    }
);

export default api;
