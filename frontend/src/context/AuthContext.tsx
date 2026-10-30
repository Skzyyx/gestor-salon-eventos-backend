import { createContext, useState, useContext, type ReactNode } from 'react';
import api from '../api/axiosConfig';

/* ---------- Tipos ---------- */

// Unión de literales = el "enum" de TS. Espejo de tu enum Rol.java.
// Cuando el RBAC agregue EMPLEADO en Java, lo añades aquí y TS te marcará
// cada sitio que todavía no contempla ese caso. Esa es su magia.
export type Rol = 'SUPERADMIN' | 'ADMIN';

export interface Usuario {
    nombre: string;
    rol: Rol;
}

// Forma de la respuesta de /auth/login y /auth/me (tu AuthResponse.java).
interface AuthResponse {
    token: string;
    nombre: string;
    rol: Rol; // en Java es String, pero el enum garantiza que solo sea uno de estos
}

// El contrato de lo que ofrece el contexto (lo que hoy pones en value={{...}}).
interface AuthContextValue {
    user: Usuario | null;
    isAuthenticated: boolean;
    login: (correo: string, password: string) => Promise<boolean>;
    logout: () => void;
    syncSession: () => Promise<boolean>;
}

/* ---------- Contexto ---------- */

// Detalle INTERNO: nadie lo toca desde fuera; se accede solo vía useAuth().
const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export const AuthProvider = ({ children }: { children: ReactNode }) => {
    const [user, setUser] = useState<Usuario | null>(() => {
        const token = localStorage.getItem('token');
        const nombre = localStorage.getItem('nombre');
        const rol = localStorage.getItem('rol');

        // localStorage devuelve string|null; el guard estrecha los 3 a string.
        if (token && nombre && rol) {
            return { nombre, rol: rol as Rol }; // afirmamos que ese string es un Rol
        }
        return null;
    });

    const [isAuthenticated, setIsAuthenticated] = useState(() => {
        return !!localStorage.getItem('token'); // TS infiere boolean solo
    });

    const login = async (correo: string, password: string): Promise<boolean> => {
        try {
            const response = await api.post<AuthResponse>('/auth/login', { correo, password });
            const { token, nombre, rol } = response.data; // rol YA es Rol, sin cast

            localStorage.setItem('token', token);
            localStorage.setItem('nombre', nombre);
            localStorage.setItem('rol', rol);

            setUser({ nombre, rol });
            setIsAuthenticated(true);
            return true;
        } catch (error) {
            console.error('Error en login:', error);
            return false;
        }
    };

    const logout = () => {
        localStorage.removeItem('token');
        localStorage.removeItem('nombre');
        localStorage.removeItem('rol');
        setUser(null);
        setIsAuthenticated(false);
    };

    const syncSession = async (): Promise<boolean> => {
        try {
            const response = await api.get<AuthResponse>('/auth/me');
            const { token, nombre, rol } = response.data;

            localStorage.setItem('token', token);
            localStorage.setItem('nombre', nombre);
            localStorage.setItem('rol', rol);

            setUser({ nombre, rol });
            setIsAuthenticated(true);
            return true;
        } catch (error) {
            console.error('Error al sincronizar sesión, cerrando sesión:', error);
            logout();
            return false;
        }
    };

    return (
        <AuthContext.Provider value={{ user, isAuthenticated, login, logout, syncSession }}>
            {children}
        </AuthContext.Provider>
    );
};

// Accesor recomendado: además de tipar, avisa si lo usas fuera del Provider.
export const useAuth = (): AuthContextValue => {
    const ctx = useContext(AuthContext);
    if (!ctx) {
        throw new Error('useAuth debe usarse dentro de <AuthProvider>');
    }
    return ctx;
};
