import type { ComponentProps, ReactNode } from 'react';
import type { LucideIcon } from 'lucide-react';

/**
 * Campo de texto reutilizable: label + icono + input, con los estilos premium del proyecto.
 * Exporta también las clases compartidas para reusarlas en inputs "a mano" (textareas, etc.).
 *
 * Props: label, icon (componente lucide), hint (texto/JSX de ayuda), className (clases extra
 * para el input) y cualquier prop nativa de <input> (type, value, onChange, placeholder...).
 */
export const inputClass = "w-full pl-11 pr-4 py-3 border border-gray-200 dark:border-dark-border rounded-xl focus:outline-none focus:ring-2 focus:ring-brand-500/50 focus:border-brand-500 bg-white dark:bg-dark-bg text-gray-900 dark:text-white transition-all shadow-sm";
export const labelClass = "text-sm font-semibold text-gray-700 dark:text-gray-300 ml-1";
export const iconWrapClass = "absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none text-gray-400";

// Props propias + TODAS las nativas de <input> (type, value, onChange, required, placeholder...).
interface InputFieldProps extends ComponentProps<'input'> {
    label: string;       // obligatoria
    icon?: LucideIcon;   // componente de ícono de lucide (opcional) — se usa como <Icon />
    hint?: ReactNode;    // texto o JSX de ayuda (opcional)
    // 'className' NO se declara: ya viene heredada de ComponentProps<'input'>.
}

export default function InputField({ label, icon: Icon, hint, className = '', ...props }: InputFieldProps) {
    return (
        <div className="space-y-1.5">
            <label className={labelClass}>{label}</label>
            <div className="relative">
                {Icon && <div className={iconWrapClass}><Icon size={18} /></div>}
                <input className={`${inputClass} ${className}`} {...props} />
            </div>
            {hint && <p className="text-xs text-gray-400 ml-1">{hint}</p>}
        </div>
    );
}
