import { createPortal } from 'react-dom';
import { Trash2 } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';

interface ConfirmDialogProps {
    isOpen: boolean;
    title: string;
    message: React.ReactNode;   // acepta string O JSX (los consumidores pasan <>...</>)
    confirmText?: string;
    onConfirm: () => void;
    onCancel: () => void;
    icon?: React.ComponentType<{ className?: string }>;
}

/**
 * Diálogo de confirmación reutilizable (acción destructiva).
 * Props: isOpen, title, message (string o JSX), confirmText, onConfirm, onCancel, icon.
 */
export default function ConfirmDialog({
    isOpen,
    title,
    message,
    confirmText = 'Eliminar',
    onConfirm,
    onCancel,
    icon: Icon = Trash2,
}: ConfirmDialogProps) {
    return createPortal(
        <AnimatePresence>
            {isOpen && (
                <motion.div
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    exit={{ opacity: 0 }}
                    className="fixed inset-0 z-50 flex items-center justify-center p-4"
                >
                    {/* OVERLAY (clic fuera = cancelar) */}
                    <motion.div
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        exit={{ opacity: 0 }}
                        onClick={onCancel}
                        className="absolute inset-0 bg-black/50 backdrop-blur-sm"
                    />

                    {/* TARJETA */}
                    <motion.div
                        initial={{ opacity: 0, scale: 0.9 }}
                        animate={{ opacity: 1, scale: 1 }}
                        exit={{ opacity: 0, scale: 0.9 }}
                        transition={{ type: "spring", stiffness: 300, damping: 25 }}
                        className="relative w-full max-w-sm bg-white/90 dark:bg-dark-surface/90 backdrop-blur-2xl rounded-3xl shadow-2xl border border-white/20 dark:border-dark-border p-8 text-center"
                    >
                        <div className="inline-block p-3 rounded-2xl bg-red-50 dark:bg-red-500/10 mb-4">
                            <Icon className="w-7 h-7 text-red-500" />
                        </div>
                        <h3 className="text-xl font-bold text-gray-900 dark:text-white">{title}</h3>
                        <p className="text-gray-500 dark:text-gray-400 text-sm mt-2">{message}</p>
                        <div className="flex gap-3 mt-6">
                            <button
                                onClick={onCancel}
                                className="flex-1 py-2.5 rounded-xl border border-gray-200 dark:border-dark-border text-gray-600 dark:text-gray-400 font-semibold hover:bg-gray-50 dark:hover:bg-dark-bg transition-colors"
                            >
                                Cancelar
                            </button>
                            <button
                                onClick={onConfirm}
                                className="flex-1 py-2.5 rounded-xl bg-red-500 hover:bg-red-600 text-white font-bold transition-colors shadow-lg shadow-red-500/25"
                            >
                                {confirmText}
                            </button>
                        </div>
                    </motion.div>
                </motion.div>
            )}
        </AnimatePresence>,
        document.body
    );
}
