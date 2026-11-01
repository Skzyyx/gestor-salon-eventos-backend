import { useState, useEffect } from 'react';
import Modal from './Modal';
import InputField from './InputField';
import { Loader2, UserCog } from 'lucide-react';
import { Trabajador, CrearTrabajadorRequest, ActualizarTrabajadorRequest, RolNegocio } from '../types/equipo';

interface TrabajadorFormModalProps {
    isOpen: boolean;
    onClose: () => void;
    trabajadorToEdit: Trabajador | null;
    roles: RolNegocio[];
    onSave: (payload: any, setSaving: (val: boolean) => void) => Promise<void>;
    error: string;
}

export default function TrabajadorFormModal({
    isOpen,
    onClose,
    trabajadorToEdit,
    roles,
    onSave,
    error
}: TrabajadorFormModalProps) {
    const isEditing = !!trabajadorToEdit;
    const [nombre, setNombre] = useState('');
    const [correo, setCorreo] = useState('');
    const [password, setPassword] = useState('');
    const [rolNegocioId, setRolNegocioId] = useState<number | ''>('');
    const [saving, setSaving] = useState(false);
    const [formError, setFormError] = useState('');

    useEffect(() => {
        if (isOpen) {
            setFormError('');
            if (trabajadorToEdit) {
                setNombre(trabajadorToEdit.nombre || '');
                setCorreo(trabajadorToEdit.correo || '');
                setPassword('');
                setRolNegocioId(trabajadorToEdit.rol?.id || '');
            } else {
                setNombre('');
                setCorreo('');
                setPassword('');
                setRolNegocioId('');
            }
        }
    }, [isOpen, trabajadorToEdit]);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setFormError('');

        if (!nombre.trim()) return setFormError('El nombre es obligatorio');
        if (!isEditing && !correo.trim()) return setFormError('El correo es obligatorio');
        if (!isEditing && !password.trim()) return setFormError('La contraseña temporal es obligatoria');
        if (rolNegocioId === '') return setFormError('Selecciona un rol');

        setSaving(true);
        if (isEditing) {
            const payload: ActualizarTrabajadorRequest = {
                nombre: nombre.trim(),
                rolNegocioId: Number(rolNegocioId)
            };
            await onSave(payload, setSaving);
        } else {
            const payload: CrearTrabajadorRequest = {
                nombre: nombre.trim(),
                correo: correo.trim(),
                password: password.trim(),
                rolNegocioId: Number(rolNegocioId)
            };
            await onSave(payload, setSaving);
        }
    };

    return (
        <Modal
            isOpen={isOpen}
            onClose={() => !saving && onClose()}
            icon={UserCog}
            title={isEditing ? 'Editar Trabajador' : 'Nuevo Trabajador'}
            subtitle={isEditing ? 'Modifica el nombre o el rol del trabajador.' : 'Registra a un nuevo integrante de tu equipo.'}
        >
            <form onSubmit={handleSubmit} className="p-6">
                <div className="space-y-4">
                    <InputField
                        label="Nombre completo"
                        value={nombre}
                        onChange={(e) => setNombre(e.target.value)}
                        placeholder="Ej. Juan Pérez"
                        required
                    />

                    {!isEditing && (
                        <>
                            <InputField
                                label="Correo electrónico"
                                type="email"
                                value={correo}
                                onChange={(e) => setCorreo(e.target.value)}
                                placeholder="juan@ejemplo.com"
                                required
                            />
                            <div className="space-y-1">
                                <InputField
                                    label="Contraseña temporal"
                                    type="text"
                                    value={password}
                                    onChange={(e) => setPassword(e.target.value)}
                                    placeholder="Contraseña de acceso inicial"
                                    required
                                />
                                <p className="text-xs text-gray-500 dark:text-gray-400">
                                    Esta contraseña se la debes entregar manualmente al trabajador.
                                </p>
                            </div>
                        </>
                    )}

                    <div className="space-y-1.5">
                        <label className="block text-sm font-semibold text-gray-700 dark:text-gray-300">
                            Rol del Trabajador <span className="text-brand-500">*</span>
                        </label>
                        <select
                            value={rolNegocioId}
                            onChange={(e) => setRolNegocioId(Number(e.target.value))}
                            className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-dark-border bg-white dark:bg-dark-surface focus:outline-none focus:ring-2 focus:ring-brand-500/50 transition-colors text-gray-900 dark:text-white"
                        >
                            <option value="" disabled>Selecciona un rol...</option>
                            {roles.map((rol) => (
                                <option key={rol.id} value={rol.id}>
                                    {rol.nombre}
                                </option>
                            ))}
                        </select>
                        <p className="text-xs text-gray-500 dark:text-gray-400">
                            El rol define los permisos de este trabajador en el sistema.
                        </p>
                    </div>

                    {(formError || error) && (
                        <div className="p-3 bg-red-50 dark:bg-red-500/10 border border-red-200 dark:border-red-500/20 rounded-xl text-sm text-red-600 dark:text-red-400 font-medium">
                            {formError || error}
                        </div>
                    )}
                </div>

                <div className="mt-8 flex justify-end gap-3">
                    <button
                        type="button"
                        onClick={onClose}
                        disabled={saving}
                        className="px-5 py-2.5 rounded-xl text-gray-600 dark:text-gray-400 font-medium hover:bg-gray-100 dark:hover:bg-dark-surface-hover transition-colors"
                    >
                        Cancelar
                    </button>
                    <button
                        type="submit"
                        disabled={saving}
                        className="flex items-center gap-2 px-5 py-2.5 rounded-xl bg-brand-600 hover:bg-brand-700 dark:bg-brand-500 dark:hover:bg-brand-600 text-white font-medium shadow-lg shadow-brand-500/25 transition-all disabled:opacity-70"
                    >
                        {saving ? (
                            <>
                                <Loader2 size={18} className="animate-spin" /> Guardando...
                            </>
                        ) : (
                            'Guardar Trabajador'
                        )}
                    </button>
                </div>
            </form>
        </Modal>
    );
}
