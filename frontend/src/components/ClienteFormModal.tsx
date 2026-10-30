import { useState, useEffect, useMemo, type SubmitEvent } from 'react';
import { User, Phone, Mail, MapPin, Hash, Building, Map, Loader2, AlertTriangle } from 'lucide-react';
import Modal from './Modal';
import InputField from './InputField';
import { Cliente, CrearCliente } from '../api/clientes.api';
import { Direccion } from '../types/direccion';

const EMPTY_DIRECCION = { calle: '', numero: '', colonia: '', ciudad: '', estado: '', codigoPostal: '' };

interface ClienteFormModalProps {
    isOpen: boolean;
    onClose: () => void;
    clienteToEdit: Cliente | null;
    clientesExistentes: Cliente[];
    onSave: (payload: CrearCliente, setSaving: (value: boolean) => void) => Promise<void>;
    error: string;
}

/**
 * Modal de alta/edición de Cliente (CU-08). El correo y la dirección son opcionales.
 * Implementa la advertencia NO bloqueante por teléfono duplicado (FA-08.3): avisa,
 * pero permite guardar de todas formas.
 *
 * Props:
 *  - isOpen, onClose, error (del padre)
 *  - clienteToEdit: cliente a editar (o null para alta)
 *  - clientesExistentes: lista actual, para detectar teléfonos repetidos en cliente
 *  - onSave: async (payload, setSaving) => ...
 */
export default function ClienteFormModal({ isOpen, onClose, clienteToEdit, clientesExistentes = [], onSave, error }: ClienteFormModalProps) {
    const [nombre, setNombre] = useState('');
    const [telefono, setTelefono] = useState('');
    const [email, setEmail] = useState('');
    const [direccion, setDireccion] = useState<Direccion | null>(null);

    const [saving, setSaving] = useState(false);
    const [confirmarDuplicado, setConfirmarDuplicado] = useState(false);

    const setDir = (campo: string, valor: string) => setDireccion((prev) => ({ ...prev, [campo]: valor }));

    useEffect(() => {
        if (isOpen) {
            setConfirmarDuplicado(false);
            setSaving(false);
            if (clienteToEdit) {
                setNombre(clienteToEdit.nombre || '');
                setTelefono(clienteToEdit.telefono || '');
                setEmail(clienteToEdit.email || '');
                setDireccion({ ...EMPTY_DIRECCION, ...(clienteToEdit.direccion || {}) });
            } else {
                setNombre('');
                setTelefono('');
                setEmail('');
                setDireccion(EMPTY_DIRECCION);
            }
        }
    }, [isOpen, clienteToEdit]);

    // FA-08.3: ¿ya hay otro cliente con este mismo teléfono? (en vivo, sin viaje al servidor)
    const telefonoDuplicado = useMemo(() => {
        const tel = telefono.trim();
        if (!tel) return false;
        return clientesExistentes.some(
            (c) => c.telefono?.trim() === tel && c.id !== clienteToEdit?.id
        );
    }, [telefono, clientesExistentes, clienteToEdit]);

    // Cualquier cambio en el teléfono reinicia la confirmación previa.
    useEffect(() => {
        setConfirmarDuplicado(false);
    }, [telefono]);

    const handleSubmit = async (e: SubmitEvent<HTMLFormElement>) => {
        e.preventDefault();

        // Advertencia no bloqueante: el primer intento con teléfono repetido pide confirmar.
        if (telefonoDuplicado && !confirmarDuplicado) {
            setConfirmarDuplicado(true);
            return;
        }

        setSaving(true);

        // Solo mandamos la dirección si el usuario escribió algo; si no, va null.
        const algunCampoDireccion = Object.values(direccion || {}).some((v) => v && v.trim());

        const payload = {
            nombre: nombre.trim(),
            telefono: telefono.trim(),
            email: email.trim() || null,
            direccion: algunCampoDireccion ? direccion : null,
            activo: true,
        };
        await onSave(payload, setSaving);
    };

    return (
        <Modal
            isOpen={isOpen}
            onClose={onClose}
            icon={User}
            title={clienteToEdit ? 'Editar Cliente' : 'Nuevo Cliente'}
            subtitle={clienteToEdit ? 'Modifica los datos del cliente.' : 'Registra un nuevo cliente en tu directorio.'}
            error={error}
            maxWidth="max-w-2xl"
        >
            <form onSubmit={handleSubmit} className="space-y-5">

                {/* ---- DATOS DE CONTACTO ---- */}
                <p className="text-xs font-bold uppercase tracking-wider text-brand-500">Datos de contacto</p>

                <InputField
                    label="Nombre" icon={User} type="text" required autoFocus
                    placeholder="Ej: María González"
                    value={nombre} onChange={(e) => setNombre(e.target.value)}
                />

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    <div>
                        <InputField
                            label="Teléfono" icon={Phone} type="tel" required
                            placeholder="Ej: 644-123-4567"
                            value={telefono} onChange={(e) => setTelefono(e.target.value)}
                        />
                        {/* Advertencia no bloqueante por teléfono duplicado (FA-08.3) */}
                        {telefonoDuplicado && (
                            <p className="mt-1.5 ml-1 text-xs text-amber-600 dark:text-amber-400 flex items-center gap-1.5">
                                <AlertTriangle size={13} className="shrink-0" />
                                Ya existe un cliente con este teléfono.
                            </p>
                        )}
                    </div>
                    <InputField
                        label="Correo electrónico" icon={Mail} type="email"
                        placeholder="opcional@correo.com"
                        value={email} onChange={(e) => setEmail(e.target.value)}
                        hint="Opcional."
                    />
                </div>

                {/* ---- DIRECCIÓN (OPCIONAL) ---- */}
                <p className="text-xs font-bold uppercase tracking-wider text-brand-500 pt-2">
                    Dirección <span className="text-gray-400 font-medium normal-case">(opcional)</span>
                </p>

                <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                    <div className="sm:col-span-2">
                        <InputField
                            label="Calle" icon={MapPin} type="text"
                            placeholder="Ej: Av. Reforma"
                            value={direccion?.calle} onChange={(e) => setDir('calle', e.target.value)}
                        />
                    </div>
                    <InputField
                        label="Número" icon={Hash} type="text"
                        placeholder="Ej: 742-A / S/N"
                        value={direccion?.numero} onChange={(e) => setDir('numero', e.target.value)}
                    />
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    <InputField
                        label="Colonia" icon={Building} type="text"
                        placeholder="Ej: Centro"
                        value={direccion?.colonia} onChange={(e) => setDir('colonia', e.target.value)}
                    />
                    <InputField
                        label="Código Postal" icon={Hash} type="text" maxLength={10}
                        placeholder="Ej: 85000"
                        value={direccion?.codigoPostal} onChange={(e) => setDir('codigoPostal', e.target.value)}
                    />
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    <InputField
                        label="Ciudad" icon={Map} type="text"
                        placeholder="Ej: Ciudad Obregón"
                        value={direccion?.ciudad} onChange={(e) => setDir('ciudad', e.target.value)}
                    />
                    <InputField
                        label="Estado" icon={Map} type="text"
                        placeholder="Ej: Sonora"
                        value={direccion?.estado} onChange={(e) => setDir('estado', e.target.value)}
                    />
                </div>

                {/* BOTONES */}
                <div className="flex gap-3 pt-4 border-t border-gray-100 dark:border-dark-border">
                    <button type="button" onClick={onClose}
                        className="flex-1 py-3 rounded-xl border border-gray-200 dark:border-dark-border text-gray-600 dark:text-gray-400 font-semibold hover:bg-gray-50 dark:hover:bg-dark-bg transition-colors">
                        Cancelar
                    </button>
                    <button type="submit" disabled={saving}
                        className={`flex-1 group text-white font-bold py-3 rounded-xl transition-all shadow-lg flex justify-center items-center gap-2 disabled:opacity-70 disabled:cursor-not-allowed ${confirmarDuplicado
                            ? 'bg-amber-500 hover:bg-amber-600 shadow-amber-500/30'
                            : 'bg-brand-600 hover:bg-brand-700 dark:bg-brand-500 dark:hover:bg-brand-600 shadow-brand-500/30'
                            }`}>
                        {saving ? (
                            <><Loader2 className="animate-spin" size={18} /><span>Guardando...</span></>
                        ) : confirmarDuplicado ? (
                            <span>Guardar de todas formas</span>
                        ) : (
                            <span>{clienteToEdit ? 'Guardar Cambios' : 'Registrar Cliente'}</span>
                        )}
                    </button>
                </div>
            </form>
        </Modal>
    );
}
