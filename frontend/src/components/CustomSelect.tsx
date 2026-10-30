import { useState, useRef, useEffect } from 'react';
import { ChevronDown, type LucideIcon } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';

interface SelectOption {
    value: string;
    label: string;
}

interface CustomSelectProps {
    options: SelectOption[];
    value: string;
    onChange: (value: string) => void;
    placeholder?: string;
    icon?: LucideIcon;
    required?: boolean;
    name?: string;
}

export default function CustomSelect({
    options,
    value,
    onChange,
    placeholder = "Seleccione...",
    icon: Icon,
    required = false,
    name
}: CustomSelectProps) {
    const [isOpen, setIsOpen] = useState(false);
    const selectRef = useRef<HTMLDivElement>(null);

    // Cerrar al hacer clic fuera
    useEffect(() => {
        const handleClickOutside = (event: MouseEvent) => {
            if (selectRef.current && !selectRef.current.contains(event.target as Node)) {
                setIsOpen(false);
            }
        };
        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    const selectedOption = options.find(opt => opt.value === value);

    return (
        <div className="relative" ref={selectRef}>
            {/* Input oculto para forzar la validación nativa HTML5 de requerido */}
            {required && (
                <input
                    type="text"
                    name={name}
                    required={required}
                    value={value || ''}
                    onChange={() => { }}
                    className="absolute opacity-0 pointer-events-none -z-10 w-full h-full inset-0"
                    tabIndex={-1}
                />
            )}

            {/* Ícono de la izquierda */}
            {Icon && (
                <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none text-gray-400 z-10">
                    <Icon size={18} />
                </div>
            )}

            {/* Botón principal del selector */}
            <button
                type="button"
                onClick={() => setIsOpen(!isOpen)}
                className={`w-full text-left flex items-center justify-between py-3 border rounded-xl transition-all shadow-sm focus:outline-none focus:ring-2 focus:ring-brand-500/50 focus:border-brand-500
                    ${Icon ? 'pl-11 pr-4' : 'px-4'}
                    ${isOpen ? 'border-brand-500 ring-2 ring-brand-500/20' : 'border-gray-200 dark:border-dark-border'}
                    bg-white dark:bg-dark-bg text-gray-900 dark:text-white`}
            >
                <span className={`block truncate ${!selectedOption ? 'text-gray-400' : ''}`}>
                    {selectedOption ? selectedOption.label : placeholder}
                </span>
                <ChevronDown size={18} className={`text-gray-400 transition-transform duration-200 flex-shrink-0 ${isOpen ? 'rotate-180' : ''}`} />
            </button>

            {/* Menú desplegable animado */}
            <AnimatePresence>
                {isOpen && (
                    <motion.div
                        initial={{ opacity: 0, y: -10 }}
                        animate={{ opacity: 1, y: 0 }}
                        exit={{ opacity: 0, y: -10 }}
                        transition={{ duration: 0.15 }}
                        className="absolute z-20 w-full mt-2 bg-white dark:bg-dark-surface border border-gray-100 dark:border-dark-border rounded-xl shadow-xl overflow-hidden"
                    >
                        <div className="max-h-60 overflow-y-auto p-1 scrollbar-thin scrollbar-thumb-gray-200 dark:scrollbar-thumb-gray-700">
                            {/* Opción para limpiar / valor por defecto */}
                            <button
                                type="button"
                                onClick={() => {
                                    onChange('');
                                    setIsOpen(false);
                                }}
                                className={`w-full text-left px-4 py-2.5 rounded-lg text-sm transition-colors text-gray-500 hover:bg-gray-50 dark:hover:bg-dark-bg`}
                            >
                                {placeholder}
                            </button>

                            {/* Opciones reales */}
                            {options.map((option) => (
                                <button
                                    key={option.value}
                                    type="button"
                                    onClick={() => {
                                        onChange(option.value);
                                        setIsOpen(false);
                                    }}
                                    className={`w-full text-left px-4 py-2.5 rounded-lg text-sm transition-colors mt-0.5
                                        ${value === option.value
                                            ? 'bg-brand-50 text-brand-700 dark:bg-brand-500/20 dark:text-brand-300 font-semibold'
                                            : 'text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-dark-bg'
                                        }`}
                                >
                                    {option.label}
                                </button>
                            ))}
                        </div>
                    </motion.div>
                )}
            </AnimatePresence>
        </div>
    );
}
