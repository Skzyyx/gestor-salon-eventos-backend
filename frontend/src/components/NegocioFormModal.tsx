import { useState, useEffect, type SubmitEvent, type Dispatch, type SetStateAction } from 'react';
import { Building2, Store, Link as LinkIcon, User, Mail, Lock, Loader2 } from 'lucide-react';
import Modal from './Modal';
import InputField from './InputField';
import { CrearNegocio } from '../api/plataforma.api';


// Props del modal. (onSave lo dispara el padre, que hace la llamada real al API.)
interface NegocioFormModalProps {
    isOpen: boolean;
    onClose: () => void;
    onSave: (payload: CrearNegocio, setSaving: (value: boolean) => void) => Promise<void>;
    error?: string;
}

/**
 * Modal para que el SUPERADMIN dé de alta un negocio nuevo junto con su admin.
 */
export default function NegocioFormModal({ isOpen, onClose, onSave, error }: NegocioFormModalProps) {
    // useState INFIERE el tipo del valor inicial: '' → string, false → boolean. No anotamos.
    const [nombre, setNombre] = useState('');
    const [slug, setSlug] = useState('');
    const [adminNombre, setAdminNombre] = useState('');
    const [adminCorreo, setAdminCorreo] = useState('');
    const [adminPassword, setAdminPassword] = useState('');

    const [saving, setSaving] = useState(false);

    // Cada vez que se abre, limpiamos el formulario
    useEffect(() => {
        if (isOpen) {
            setNombre('');
            setSlug('');
            setAdminNombre('');
            setAdminCorreo('');
            setAdminPassword('');
            setSaving(false);
        }
    }, [isOpen]);

    // EXTRAÍDO a una const → TS no puede adivinar el evento, hay que anotarlo a mano.
    const handleSubmit = async (e: SubmitEvent<HTMLFormElement>) => {
        e.preventDefault();
        setSaving(true);
        const payload: CrearNegocio = { nombre, slug, adminNombre, adminCorreo, adminPassword };
        // El padre hace la llamada al backend y controla el estado de guardado
        await onSave(payload, setSaving);
    };

    return (
        <Modal
            isOpen={isOpen}
            onClose={onClose}
            icon={Building2}
            title="Nuevo Negocio"
            subtitle="Crea un salón y su cuenta de administrador inicial."
            error={error}
            maxWidth="max-w-lg"
        >
            <form onSubmit={handleSubmit} className="space-y-5">

                {/* ---- SECCIÓN NEGOCIO ---- */}
                <p className="text-xs font-bold uppercase tracking-wider text-brand-500">Datos del Negocio</p>

                <InputField
                    label="Nombre Comercial" icon={Store} type="text" required autoFocus
                    placeholder="Ej: Salón La Herradura"
                    value={nombre} onChange={(e) => setNombre(e.target.value)}
                />
                <InputField
                    label="Enlace Público (Slug)" icon={LinkIcon} type="text" required className="font-mono text-sm"
                    placeholder="salon-la-herradura"
                    value={slug} onChange={(e) => setSlug(e.target.value)}
                    hint="Se normaliza a minúsculas y guiones automáticamente en el servidor."
                />

                {/* ---- SECCIÓN ADMIN ---- */}
                <p className="text-xs font-bold uppercase tracking-wider text-brand-500 pt-2">Administrador Inicial</p>

                <InputField
                    label="Nombre del Administrador" icon={User} type="text" required
                    placeholder="Ej: Juan Pérez"
                    value={adminNombre} onChange={(e) => setAdminNombre(e.target.value)}
                />
                <InputField
                    label="Correo del Administrador" icon={Mail} type="email" required
                    placeholder="admin@negocio.com"
                    value={adminCorreo} onChange={(e) => setAdminCorreo(e.target.value)}
                />
                <InputField
                    label="Contraseña Temporal" icon={Lock} type="password" required minLength={8}
                    placeholder="Mínimo 8 caracteres"
                    value={adminPassword} onChange={(e) => setAdminPassword(e.target.value)}
                />

                {/* BOTONES */}
                <div className="flex gap-3 pt-4">
                    <button type="button" onClick={onClose}
                        className="flex-1 py-3 rounded-xl border border-gray-200 dark:border-dark-border text-gray-600 dark:text-gray-400 font-semibold hover:bg-gray-50 dark:hover:bg-dark-bg transition-colors">
                        Cancelar
                    </button>
                    <button type="submit" disabled={saving}
                        className="flex-1 group bg-brand-600 hover:bg-brand-700 dark:bg-brand-500 dark:hover:bg-brand-600 text-white font-bold py-3 rounded-xl transition-all shadow-lg shadow-brand-500/30 flex justify-center items-center gap-2 disabled:opacity-70 disabled:cursor-not-allowed">
                        {saving ? (<><Loader2 className="animate-spin" size={18} /><span>Creando...</span></>) : (<span>Crear Negocio</span>)}
                    </button>
                </div>
            </form>
        </Modal>
    );
}
