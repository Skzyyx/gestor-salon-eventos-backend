import { createPortal } from 'react-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { CheckCircle2, AlertCircle, X } from 'lucide-react';


export type ToastTipo = 'success' | 'error';

export interface ToastMessage {
    text: string;
    type: ToastTipo;
}


interface ToastProps {
    message: ToastMessage | null;
    onClose: () => void;
}


/**
 * Toast de feedback flotante y fijo (portal a body) para que SIEMPRE sea visible
 * sin importar el scroll. Aparece abajo-centro. El auto-descarte vive en el padre.
 *
 * Props: message = { text, type: 'success' | 'error' }, onClose.
 */
export default function Toast({ message, onClose }: ToastProps) {
    return createPortal(
        <AnimatePresence>
            {message?.text && (
                <motion.div
                    key="toast"
                    initial={{ opacity: 0, y: 24, scale: 0.95 }}
                    animate={{ opacity: 1, y: 0, scale: 1 }}
                    exit={{ opacity: 0, y: 24, scale: 0.95 }}
                    transition={{ type: 'spring', stiffness: 400, damping: 30 }}
                    className="fixed bottom-24 left-1/2 -translate-x-1/2 z-50 w-[calc(100%-2rem)] max-w-md"
                >
                    <div className={`px-4 py-3 rounded-xl shadow-xl backdrop-blur-xl border flex items-center gap-3 font-medium ${message.type === 'success'
                        ? 'bg-emerald-50/95 dark:bg-emerald-500/15 text-emerald-700 dark:text-emerald-300 border-emerald-200 dark:border-emerald-500/30'
                        : 'bg-red-50/95 dark:bg-red-500/15 text-red-700 dark:text-red-300 border-red-200 dark:border-red-500/30'
                        }`}>
                        {message.type === 'success' ? <CheckCircle2 size={20} className="shrink-0" /> : <AlertCircle size={20} className="shrink-0" />}
                        <span className="flex-1 text-sm">{message.text}</span>
                        <button type="button" onClick={onClose} className="shrink-0 opacity-60 hover:opacity-100 transition-opacity">
                            <X size={16} />
                        </button>
                    </div>
                </motion.div>
            )}
        </AnimatePresence>,
        document.body
    );
}
