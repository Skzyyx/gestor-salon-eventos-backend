import { useState, useRef, useEffect } from 'react';
import { Clock, ChevronDown } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';

// Generamos las opciones una sola vez (00..23 horas / 00..59 minutos)
const HORAS = Array.from({ length: 24 }, (_, i) => String(i).padStart(2, '0'));
const MINUTOS = Array.from({ length: 60 }, (_, i) => String(i).padStart(2, '0'));

interface CustomTimePickerProps {
    value: string;
    onChange: (value: string) => void;
    placeholder?: string;
}

export default function CustomTimePicker({ value, onChange, placeholder = "Seleccione hora" }: CustomTimePickerProps) {
    const [isOpen, setIsOpen] = useState(false);
    const [dropUp, setDropUp] = useState(false);
    // useRef necesita el tipo del elemento al que apunta (igual que useState necesita su tipo).
    // pickerRef apunta al <div> contenedor; sin <HTMLDivElement>, `current` sería `null` y no
    // podríamos llamar .getBoundingClientRect()/.contains() sobre él.
    const pickerRef = useRef<HTMLDivElement>(null);
    // Estos dos apuntan a los <button> de la opción activa, para centrarlos con scrollIntoView al abrir.
    const horaSelRef = useRef<HTMLButtonElement>(null);
    const minutoSelRef = useRef<HTMLButtonElement>(null);

    // Altura aproximada del panel; si no cabe abajo, lo abrimos hacia arriba
    const ALTURA_PANEL = 260;

    const toggleOpen = () => {
        if (!isOpen && pickerRef.current) {
            const rect = pickerRef.current.getBoundingClientRect();
            const espacioAbajo = window.innerHeight - rect.bottom;
            setDropUp(espacioAbajo < ALTURA_PANEL && rect.top > espacioAbajo);
        }
        setIsOpen(!isOpen);
    };

    // Partimos el valor "HH:MM" en sus dos piezas
    const [horaActual, minutoActual] = value && value.includes(':') ? value.split(':') : ['', ''];

    // Cerrar al hacer clic fuera
    useEffect(() => {
        // Listener nativo de 'mousedown' → su parámetro es el MouseEvent del DOM (hay que anotarlo
        // o sería 'any' implícito con strict).
        const handleClickOutside = (event: MouseEvent) => {
            // .contains() pide un Node, pero event.target es EventTarget (más general) → lo afirmamos con `as Node`.
            if (pickerRef.current && !pickerRef.current.contains(event.target as Node)) {
                setIsOpen(false);
            }
        };
        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    // Al abrir, centramos la opción seleccionada en cada columna
    useEffect(() => {
        if (isOpen) {
            const id = requestAnimationFrame(() => {
                horaSelRef.current?.scrollIntoView({ block: 'center' });
                minutoSelRef.current?.scrollIntoView({ block: 'center' });
            });
            return () => cancelAnimationFrame(id);
        }
    }, [isOpen]);

    // h y m salen de HORAS/MINUTOS, que son arreglos de string → los tipamos como string
    // (si no, serían 'any' implícito y TS se quejaría con strict).
    const setHora = (h: string) => onChange(`${h}:${minutoActual || '00'}`);
    const setMinuto = (m: string) => onChange(`${horaActual || '00'}:${m}`);

    return (
        <div className="relative" ref={pickerRef}>
            {/* Ícono de reloj */}
            <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none text-gray-400 z-10">
                <Clock size={18} />
            </div>

            {/* Botón principal */}
            <button
                type="button"
                onClick={toggleOpen}
                className={`w-full text-left flex items-center justify-between pl-11 pr-4 py-3 border rounded-xl transition-all shadow-sm focus:outline-none focus:ring-2 focus:ring-brand-500/50 focus:border-brand-500
                    ${isOpen ? 'border-brand-500 ring-2 ring-brand-500/20' : 'border-gray-200 dark:border-dark-border'}
                    bg-white dark:bg-dark-bg text-gray-900 dark:text-white`}
            >
                <span className={`block truncate tabular-nums ${!value ? 'text-gray-400' : ''}`}>
                    {value ? value : placeholder}
                </span>
                <ChevronDown size={18} className={`text-gray-400 transition-transform duration-200 flex-shrink-0 ${isOpen ? 'rotate-180' : ''}`} />
            </button>

            {/* Panel desplegable con columnas Hora / Minuto */}
            <AnimatePresence>
                {isOpen && (
                    <motion.div
                        initial={{ opacity: 0, y: dropUp ? 10 : -10 }}
                        animate={{ opacity: 1, y: 0 }}
                        exit={{ opacity: 0, y: dropUp ? 10 : -10 }}
                        transition={{ duration: 0.15 }}
                        className={`absolute z-20 w-full bg-white dark:bg-dark-surface border border-gray-100 dark:border-dark-border rounded-xl shadow-xl overflow-hidden ${dropUp ? 'bottom-full mb-2' : 'top-full mt-2'}`}
                    >
                        <div className="flex divide-x divide-gray-100 dark:divide-dark-border">
                            {/* COLUMNA HORAS */}
                            <div className="flex-1">
                                <p className="text-center text-[10px] font-bold uppercase tracking-wider text-gray-400 py-1.5 border-b border-gray-100 dark:border-dark-border sticky top-0 bg-white dark:bg-dark-surface">
                                    Hora
                                </p>
                                <div className="max-h-48 overflow-y-auto p-1 scrollbar-thin scrollbar-thumb-gray-200 dark:scrollbar-thumb-gray-700">
                                    {HORAS.map((h) => {
                                        const activa = h === horaActual;
                                        return (
                                            <button
                                                key={h}
                                                ref={activa ? horaSelRef : null}
                                                type="button"
                                                onClick={() => setHora(h)}
                                                className={`w-full text-center px-2 py-2 rounded-lg text-sm tabular-nums transition-colors mt-0.5
                                                    ${activa
                                                        ? 'bg-brand-50 text-brand-700 dark:bg-brand-500/20 dark:text-brand-300 font-semibold'
                                                        : 'text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-dark-bg'
                                                    }`}
                                            >
                                                {h}
                                            </button>
                                        );
                                    })}
                                </div>
                            </div>

                            {/* COLUMNA MINUTOS */}
                            <div className="flex-1">
                                <p className="text-center text-[10px] font-bold uppercase tracking-wider text-gray-400 py-1.5 border-b border-gray-100 dark:border-dark-border sticky top-0 bg-white dark:bg-dark-surface">
                                    Min
                                </p>
                                <div className="max-h-48 overflow-y-auto p-1 scrollbar-thin scrollbar-thumb-gray-200 dark:scrollbar-thumb-gray-700">
                                    {MINUTOS.map((m) => {
                                        const activo = m === minutoActual;
                                        return (
                                            <button
                                                key={m}
                                                ref={activo ? minutoSelRef : null}
                                                type="button"
                                                onClick={() => setMinuto(m)}
                                                className={`w-full text-center px-2 py-2 rounded-lg text-sm tabular-nums transition-colors mt-0.5
                                                    ${activo
                                                        ? 'bg-brand-50 text-brand-700 dark:bg-brand-500/20 dark:text-brand-300 font-semibold'
                                                        : 'text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-dark-bg'
                                                    }`}
                                            >
                                                {m}
                                            </button>
                                        );
                                    })}
                                </div>
                            </div>
                        </div>
                    </motion.div>
                )}
            </AnimatePresence>
        </div>
    );
}
