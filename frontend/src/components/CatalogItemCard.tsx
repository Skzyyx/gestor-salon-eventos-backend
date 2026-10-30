import { Briefcase, LucideIcon, Pencil, Trash2 } from 'lucide-react';
import { motion } from 'framer-motion';

export interface Badge {
    text: string;
    variant: 'brand' | 'gray';
}

interface CatalogItemCardProps {
    title: string;
    description?: string;
    price?: number;
    imageUrl?: string;
    badges?: Badge[];
    onEdit?: () => void;
    onDelete?: () => void;
    index?: number;
    icon?: LucideIcon;
}

export default function CatalogItemCard({
    title,
    description,
    price,
    imageUrl,
    badges = [],
    onEdit,
    onDelete,
    index = 0,
    icon: Icon = Briefcase
}: CatalogItemCardProps) {
    return (
        <motion.div
            initial={{ opacity: 0, x: -20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: index * 0.05 }}
            className="group bg-white/80 dark:bg-dark-surface/80 backdrop-blur-xl rounded-2xl shadow-sm border border-gray-100 dark:border-dark-border hover:shadow-md hover:border-brand-200 dark:hover:border-brand-500/30 transition-all duration-300"
        >
            <div className="flex items-center gap-5 p-5">
                {/* IMAGEN / AVATAR */}
                {imageUrl ? (
                    <img
                        src={imageUrl}
                        alt={title}
                        className="flex-shrink-0 w-16 h-16 rounded-xl object-cover shadow-lg border border-gray-100 dark:border-dark-border"
                    />
                ) : (
                    <div className="flex-shrink-0 w-16 h-16 rounded-xl bg-gradient-to-br from-brand-400 to-brand-600 flex items-center justify-center text-white shadow-lg shadow-brand-500/20">
                        <Icon size={26} />
                    </div>
                )}

                {/* INFORMACIÓN CENTRAL */}
                <div className="flex-1 min-w-0">
                    <h3 className="font-bold text-gray-900 dark:text-white text-lg truncate">
                        {title}
                    </h3>
                    <p className="text-sm text-gray-500 dark:text-gray-400 mt-0.5 line-clamp-1">
                        {description || 'Sin descripción'}
                    </p>
                    {badges.length > 0 && (
                        <div className="flex items-center gap-2 mt-2">
                            {badges.map((badge, idx) => (
                                <span
                                    key={idx}
                                    className={`inline-flex items-center px-2 py-1 rounded-md text-xs font-semibold ${badge.variant === 'brand'
                                        ? 'bg-brand-50 text-brand-700 dark:bg-brand-500/10 dark:text-brand-300 border border-brand-100 dark:border-brand-500/20'
                                        : 'bg-gray-100 text-gray-700 dark:bg-dark-bg dark:text-gray-300 border border-gray-200 dark:border-dark-border'
                                        }`}
                                >
                                    {badge.text}
                                </span>
                            ))}
                        </div>
                    )}
                </div>

                {/* PRECIO */}
                {price !== undefined && (
                    <div className="flex-shrink-0 text-right hidden sm:block">
                        <p className="text-xs font-medium text-gray-400 dark:text-gray-500 uppercase tracking-wider">Precio</p>
                        <p className="text-xl font-black text-brand-600 dark:text-brand-400">
                            ${price.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                        </p>
                    </div>
                )}

                {/* SEPARADOR VERTICAL */}
                <div className="hidden sm:block w-px h-10 bg-gray-200 dark:bg-dark-border flex-shrink-0"></div>

                {/* ACCIONES */}
                <div className="flex items-center gap-1 flex-shrink-0">
                    {onEdit && (
                        <button
                            onClick={onEdit}
                            className="p-2.5 rounded-xl text-gray-400 hover:text-brand-500 hover:bg-brand-50 dark:hover:bg-brand-500/10 transition-all duration-200"
                            title="Editar"
                        >
                            <Pencil size={18} />
                        </button>
                    )}
                    {onDelete && (
                        <button
                            onClick={onDelete}
                            className="p-2.5 rounded-xl text-gray-400 hover:text-red-500 hover:bg-red-50 dark:hover:bg-red-500/10 transition-all duration-200"
                            title="Desactivar"
                        >
                            <Trash2 size={18} />
                        </button>
                    )}
                </div>
            </div>

            {/* PRECIO VISIBLE EN MÓVIL (debajo) */}
            {price !== undefined && (
                <div className="sm:hidden px-5 pb-4 pt-0">
                    <span className="text-lg font-black text-brand-600 dark:text-brand-400">
                        ${price.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                    </span>
                </div>
            )}
        </motion.div>
    );
}
