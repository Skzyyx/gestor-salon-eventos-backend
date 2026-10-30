import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';

export type ThemeValues = 'light' | 'dark';

interface ThemeContextType {
    theme: ThemeValues;
    toggleTheme: () => void;
}

// 1. Creamos "La Nube" para guardar qué tema está usando el usuario (Claro u Oscuro)
const ThemeContext = createContext<ThemeContextType | null>(null);

interface ThemeProviderProps {
    children: ReactNode;
}

export const ThemeProvider = ({ children }: ThemeProviderProps) => {
    // 2. Leemos el tema actual. 
    // useState acepta una función para calcular el valor inicial.
    const [theme, setTheme] = useState<ThemeValues>(() => {
        // Primero, revisamos si el usuario ya había entrado a nuestra página y guardó su preferencia
        const savedTheme = localStorage.getItem('theme');
        if (savedTheme) return savedTheme as ThemeValues;

        // Si es su primera vez, le preguntamos a su sistema operativo (Windows/Mac) qué tema prefiere
        // matchMedia es una API nativa de JavaScript para leer configuraciones del sistema.
        return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
    });

    // 3. Este Efecto (useEffect) se ejecuta mágicamente cada vez que la variable 'theme' cambia
    useEffect(() => {
        // Obtenemos la etiqueta principal <html> de la página web
        const root = window.document.documentElement;

        if (theme === 'dark') {
            // Si el tema es oscuro, le inyectamos la clase "dark".
            // Al hacer esto, Tailwind activa todos los colores que escribimos como "dark:bg-algo"
            root.classList.add('dark');
        } else {
            // Si es claro, se la quitamos
            root.classList.remove('dark');
        }

        // Guardamos su elección en el disco duro del navegador para que no se pierda al recargar
        localStorage.setItem('theme', theme);
    }, [theme]); // <--- La lista de dependencias: "Ejecútate solo cuando 'theme' cambie"

    // 4. Función de ayuda para alternar el switch
    const toggleTheme = () => {
        // prev es el valor anterior. Si era dark, lo cambiamos a light, y viceversa.
        setTheme(prev => (prev === 'dark' ? 'light' : 'dark'));
    };

    // 5. Entregamos las herramientas a cualquier componente que viva dentro de esta nube
    return (
        <ThemeContext.Provider value={{ theme, toggleTheme }}>
            {children}
        </ThemeContext.Provider>
    );
};

// Hook personalizado para usarlo fácilmente (igual que hicimos con useAuth)
export const useTheme = () => {
    const ctx = useContext(ThemeContext);
    if (!ctx) throw new Error('useTheme debe usarse dentro de <ThemeProvider>');
    return ctx;
};

