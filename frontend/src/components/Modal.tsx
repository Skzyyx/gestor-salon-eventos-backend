import { createPortal } from 'react-dom';
import { X } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';

interface ModalProps {
    isOpen: boolean;
    onClose: () => void;
    icon: React.ComponentType<{ className?: string }>;
    title: string;
    subtitle: string;
    error?: string;   // opcional: a veces no hay error (llega '' o undefined)
    maxWidth?: string;
    children: React.ReactNode;
}

/**
 * Shell reutilizable de modal: overlay con glassmorphism, tarjeta flotante con spring,
 * botón de cerrar, encabezado (icono + título + subtítulo) y caja de error opcional.
 * El contenido (el <form>, normalmente) se pasa como children.
 *
 * Props: isOpen, onClose, icon (componente lucide), title, subtitle, error, maxWidth, children.
 */
export default function Modal({ isOpen, onClose, icon: Icon, title, subtitle, error, maxWidth = 'max-w-md', children }: ModalProps) {
    // Portal a <body>: si el modal se renderiza dentro del motion.div del Layout (que
    // tiene transform), un position:fixed se ancla a ESE ancestro y no cubre el sidebar.
    // Saliendo a body, el overlay difumina toda la pantalla (igual que el Toast).
    return createPortal(
        <AnimatePresence>
            {isOpen && (
                <motion.div
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    exit={{ opacity: 0 }}
                    className="fixed inset-0 z-50 flex items-center justify-center p-4 overflow-y-auto"
                >
                    {/* OVERLAY OSCURO (clic fuera = cerrar) */}
                    <motion.div
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        exit={{ opacity: 0 }}
                        onClick={onClose}
                        className="fixed inset-0 bg-black/50 backdrop-blur-sm"
                    />

                    {/* TARJETA DEL MODAL */}
                    {/* max-h-[90vh] + flex-col: la tarjeta nunca supera la pantalla; el
                        encabezado queda fijo arriba y el cuerpo tiene su propio scroll,
                        así se ve todo desde el inicio y los botones siempre son alcanzables. */}
                    <motion.div
                        initial={{ opacity: 0, scale: 0.9, y: 30 }}
                        animate={{ opacity: 1, scale: 1, y: 0 }}
                        exit={{ opacity: 0, scale: 0.9, y: 30 }}
                        transition={{ type: "spring", stiffness: 300, damping: 25 }}
                        className={`relative w-full ${maxWidth} bg-white/90 dark:bg-dark-surface/90 backdrop-blur-2xl rounded-3xl shadow-2xl border border-white/20 dark:border-dark-border my-8 max-h-[90vh] flex flex-col overflow-hidden`}
                    >
                        {/* BOTÓN CERRAR */}
                        <button
                            onClick={onClose}
                            className="absolute top-4 right-4 z-10 p-2 rounded-full text-gray-400 hover:text-gray-600 dark:hover:text-gray-200 hover:bg-gray-100 dark:hover:bg-dark-bg transition-colors"
                        >
                            <X size={20} />
                        </button>

                        {/* ENCABEZADO (fijo) */}
                        <div className="shrink-0 px-8 pt-8 pb-6 text-center space-y-2 border-b border-gray-100 dark:border-dark-border">
                            {Icon && (
                                <div className="inline-block p-3 rounded-2xl bg-brand-50 dark:bg-brand-900/20">
                                    <Icon className="w-7 h-7 text-brand-600 dark:text-brand-400" />
                                </div>
                            )}
                            <h2 className="text-2xl font-black text-gray-900 dark:text-white tracking-tight">{title}</h2>
                            {subtitle && <p className="text-gray-500 dark:text-gray-400 text-sm">{subtitle}</p>}
                        </div>

                        {/* CUERPO (desplazable) */}
                        <div className="flex-1 overflow-y-auto px-8 py-6 scrollbar-thin scrollbar-thumb-gray-200 dark:scrollbar-thumb-gray-700">
                            {/* ERROR (opcional) */}
                            <AnimatePresence>
                                {error && (
                                    <motion.div
                                        initial={{ opacity: 0, scale: 0.95 }}
                                        animate={{ opacity: 1, scale: 1 }}
                                        exit={{ opacity: 0, scale: 0.95 }}
                                        className="bg-red-50 dark:bg-red-500/10 border border-red-200 dark:border-red-500/20 text-red-600 dark:text-red-400 p-3 rounded-xl text-sm text-center font-medium mb-6"
                                    >
                                        {error}
                                    </motion.div>
                                )}
                            </AnimatePresence>

                            {children}
                        </div>
                    </motion.div>
                </motion.div>
            )}
        </AnimatePresence>,
        document.body
    );
}
