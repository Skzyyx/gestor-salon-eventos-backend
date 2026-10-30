import { useState, useEffect, useMemo, type SubmitEvent } from 'react';
import { Package, DollarSign, Loader2, Search, Plus, Minus, Trash2, Briefcase, AlertCircle, Check } from 'lucide-react';
import axios from 'axios';
import Modal from './Modal';
import ImageUploadZone from './ImageUploadZone';
import { uploadImage } from '../api/images.api';
import { inputClass, labelClass, iconWrapClass } from './InputField';
import type { Paquete, GuardarPaquete } from '../api/paquetes.api';
import type { Servicio } from '../api/servicios.api';

// La línea de servicio DENTRO del modal: además de servicioId/cantidad lleva
// datos del servicio (nombre, imagen, precio) SOLO para mostrar en la UI.
interface ItemSeleccionado {
    servicioId: number;
    cantidad: number;
    precioUnitario: number;
    nombre: string;
    imagenUrl: string | null;
}

interface PaqueteFormModalProps {
    isOpen: boolean;
    onClose: () => void;
    paqueteToEdit: Paquete | null;
    serviciosDisponibles?: Servicio[];
    onSave: (payload: GuardarPaquete, setSaving: (value: boolean) => void) => Promise<void>;
    error?: string;
}

export default function PaqueteFormModal({ isOpen, onClose, paqueteToEdit, serviciosDisponibles = [], onSave, error }: PaqueteFormModalProps) {
    const [nombre, setNombre] = useState('');
    const [descripcion, setDescripcion] = useState('');
    const [precio, setPrecio] = useState('');
    const [items, setItems] = useState<ItemSeleccionado[]>([]);
    const [servicioSearch, setServicioSearch] = useState('');

    const [selectedFile, setSelectedFile] = useState<File | null>(null);
    const [currentImageUrl, setCurrentImageUrl] = useState('');

    const [localError, setLocalError] = useState('');
    const [saving, setSaving] = useState(false);

    useEffect(() => {
        if (isOpen) {
            setLocalError('');
            setSaving(false);
            setServicioSearch('');
            setSelectedFile(null);
            if (paqueteToEdit) {
                setNombre(paqueteToEdit.nombre || '');
                setDescripcion(paqueteToEdit.descripcion || '');
                setPrecio(paqueteToEdit.precio?.toString() || '');
                setCurrentImageUrl(paqueteToEdit.imagenUrl || '');
                setItems((paqueteToEdit.items || []).map((it) => ({
                    servicioId: it.servicioId ?? it.servicio?.id ?? 0,
                    cantidad: it.cantidad ?? 1,
                    precioUnitario: Number(it.precioUnitario ?? it.servicio?.precio ?? 0),
                    nombre: it.servicio?.nombre ?? 'Servicio',
                    imagenUrl: it.servicio?.imagenUrl || null,
                })));
            } else {
                setNombre('');
                setDescripcion('');
                setPrecio('');
                setCurrentImageUrl('');
                setItems([]);
            }
        }
    }, [isOpen, paqueteToEdit]);

    const selectedIds = useMemo(() => new Set(items.map((i) => i.servicioId)), [items]);

    const addServicio = (s: Servicio) => {
        if (selectedIds.has(s.id)) return;
        setItems((prev) => [
            ...prev,
            { servicioId: s.id, cantidad: 1, precioUnitario: Number(s.precio) || 0, nombre: s.nombre, imagenUrl: s.imagenUrl || null },
        ]);
    };

    const removeServicio = (servicioId: number) =>
        setItems((prev) => prev.filter((i) => i.servicioId !== servicioId));

    const setCantidad = (servicioId: number, cantidad: number) =>
        setItems((prev) => prev.map((i) => (i.servicioId === servicioId ? { ...i, cantidad: Math.max(1, cantidad) } : i)));

    const serviciosFiltrados = useMemo(() => {
        const term = servicioSearch.trim().toLowerCase();
        if (!term) return serviciosDisponibles;
        return serviciosDisponibles.filter((s) => s.nombre?.toLowerCase().includes(term));
    }, [serviciosDisponibles, servicioSearch]);

    const sumaServicios = useMemo(
        () => items.reduce((acc, i) => acc + (Number(i.precioUnitario) || 0) * (i.cantidad || 0), 0),
        [items]
    );

    const fmt = (n: number) => `$${Number(n || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;

    const handleSubmit = async (e: SubmitEvent<HTMLFormElement>) => {
        e.preventDefault();
        setLocalError('');

        if (items.length === 0) {
            setLocalError('Un paquete debe incluir al menos un servicio.');
            return;
        }

        setSaving(true);

        let finalImageUrl = currentImageUrl;
        if (selectedFile) {
            try {
                const uploadRes = await uploadImage(selectedFile);
                finalImageUrl = uploadRes.url;
            } catch (err) {
                const msg = axios.isAxiosError(err) ? err.response?.data?.message : undefined;
                setLocalError(msg || 'Error al subir la imagen. Verifica que sea válida y menor a 5MB.');
                setSaving(false);
                return;
            }
        }

        const payload: GuardarPaquete = {
            nombre: nombre.trim(),
            descripcion: descripcion.trim() || null,
            precio: parseFloat(precio),
            imagenUrl: finalImageUrl || null,
            items: items.map((i) => ({ servicioId: i.servicioId, cantidad: i.cantidad })),
            activo: true,
        };
        await onSave(payload, setSaving);
    };


    return (
        <Modal
            isOpen={isOpen}
            onClose={onClose}
            icon={Package}
            title={paqueteToEdit ? 'Editar Paquete' : 'Nuevo Paquete'}
            subtitle={paqueteToEdit ? 'Modifica los datos, servicios e imagen del paquete.' : 'Agrupa servicios con cantidades en un paquete con precio conjunto.'}
            error={localError || error}
            maxWidth="max-w-4xl"
        >
            <form onSubmit={handleSubmit}>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-8">

                    {/* ============ COLUMNA IZQUIERDA: datos + imagen ============ */}
                    <div className="space-y-4">
                        <div className="space-y-1.5">
                            <label className={labelClass}>Nombre del Paquete</label>
                            <div className="relative">
                                <div className={iconWrapClass}><Package size={18} /></div>
                                <input
                                    type="text" required autoFocus className={inputClass}
                                    placeholder="Ej: Paquete Boda Plata"
                                    value={nombre} onChange={(e) => setNombre(e.target.value)}
                                />
                            </div>
                        </div>

                        <div className="space-y-1.5">
                            <label className={labelClass}>Precio del Paquete</label>
                            <div className="relative">
                                <div className={iconWrapClass}><DollarSign size={18} /></div>
                                <input
                                    type="number" step="0.01" min="0.01" required className={inputClass}
                                    placeholder="0.00"
                                    value={precio} onChange={(e) => setPrecio(e.target.value)}
                                />
                            </div>
                            {items.length > 0 && (
                                <p className="text-xs text-gray-400 dark:text-gray-500 ml-1">
                                    Suma de servicios sueltos: <span className="font-semibold text-gray-500 dark:text-gray-400">{fmt(sumaServicios)}</span>
                                </p>
                            )}
                        </div>

                        <div className="space-y-1.5">
                            <label className={labelClass}>Descripción <span className="text-gray-400 font-normal">(opcional)</span></label>
                            <textarea
                                className="w-full p-4 border border-gray-200 dark:border-dark-border rounded-xl focus:outline-none focus:ring-2 focus:ring-brand-500/50 focus:border-brand-500 bg-white dark:bg-dark-bg text-gray-900 dark:text-white h-16 resize-none transition-all shadow-sm scrollbar-thin scrollbar-thumb-gray-200 dark:scrollbar-thumb-gray-700"
                                placeholder="Describe qué incluye este paquete..."
                                value={descripcion} onChange={(e) => setDescripcion(e.target.value)}
                            />
                        </div>

                        <ImageUploadZone
                            label="Imagen del Paquete (opcional)"
                            minHeightClass="min-h-[130px]"
                            grow={false}
                            currentImageUrl={currentImageUrl}
                            onFileSelect={(file) => setSelectedFile(file)}
                            onClear={() => {
                                setSelectedFile(null);
                                setCurrentImageUrl('');
                            }}
                        />
                    </div>

                    {/* ============ COLUMNA DERECHA: servicios ============ */}
                    <div className="flex flex-col">
                        <div className="flex items-center justify-between mb-2">
                            <label className={labelClass}>Servicios del paquete</label>
                            <span className={`text-xs font-semibold px-2 py-0.5 rounded-md ${items.length > 0
                                ? 'bg-brand-50 text-brand-700 dark:bg-brand-500/10 dark:text-brand-300'
                                : 'bg-gray-100 text-gray-500 dark:bg-dark-bg dark:text-gray-400'
                                }`}>
                                {items.length} seleccionado{items.length !== 1 ? 's' : ''}
                            </span>
                        </div>

                        {/* Seleccionados (persistente) */}
                        {items.length === 0 ? (
                            <div className="flex flex-col items-center justify-center text-center border border-dashed border-gray-200 dark:border-dark-border rounded-xl py-5 px-4">
                                <Package className="text-gray-300 dark:text-gray-600 mb-1.5" size={24} />
                                <p className="text-sm text-gray-400">Aún no agregas servicios.</p>
                                <p className="text-xs text-gray-400 mt-0.5">Elige del catálogo de abajo.</p>
                            </div>
                        ) : (
                            <div className="max-h-[180px] overflow-y-auto space-y-2 pr-1 scrollbar-thin scrollbar-thumb-gray-200 dark:scrollbar-thumb-gray-700">
                                {items.map((it) => (
                                    <div
                                        key={it.servicioId}
                                        className="flex items-center gap-2.5 p-2 rounded-xl border border-gray-200 dark:border-dark-border bg-white dark:bg-dark-bg"
                                    >
                                        {it.imagenUrl ? (
                                            <img src={it.imagenUrl} alt={it.nombre} className="shrink-0 w-9 h-9 rounded-lg object-cover" />
                                        ) : (
                                            <span className="shrink-0 w-9 h-9 rounded-lg bg-gradient-to-br from-brand-400 to-brand-600 flex items-center justify-center text-white">
                                                <Briefcase size={16} />
                                            </span>
                                        )}

                                        <span className="flex-1 min-w-0">
                                            <span className="block font-semibold text-sm text-gray-800 dark:text-gray-100 truncate">{it.nombre}</span>
                                            <span className="block text-xs text-gray-400">
                                                {fmt(it.precioUnitario)} c/u · <span className="text-brand-600 dark:text-brand-400 font-medium">{fmt(it.precioUnitario * it.cantidad)}</span>
                                            </span>
                                        </span>

                                        {/* Stepper */}
                                        <div className="shrink-0 flex items-center gap-0.5 bg-gray-100 dark:bg-dark-surface rounded-lg p-0.5">
                                            <button type="button" onClick={() => setCantidad(it.servicioId, it.cantidad - 1)} disabled={it.cantidad <= 1}
                                                className="w-6 h-6 flex items-center justify-center rounded-md text-gray-500 hover:bg-white dark:hover:bg-dark-bg disabled:opacity-40 disabled:cursor-not-allowed transition-colors">
                                                <Minus size={13} />
                                            </button>
                                            <input
                                                type="number" min="1" value={it.cantidad}
                                                onChange={(e) => setCantidad(it.servicioId, parseInt(e.target.value) || 1)}
                                                className="w-8 text-center text-sm font-semibold bg-transparent text-gray-800 dark:text-gray-100 focus:outline-none [appearance:textfield] [&::-webkit-outer-spin-button]:appearance-none [&::-webkit-inner-spin-button]:appearance-none"
                                            />
                                            <button type="button" onClick={() => setCantidad(it.servicioId, it.cantidad + 1)}
                                                className="w-6 h-6 flex items-center justify-center rounded-md text-gray-500 hover:bg-white dark:hover:bg-dark-bg transition-colors">
                                                <Plus size={13} />
                                            </button>
                                        </div>

                                        <button type="button" onClick={() => removeServicio(it.servicioId)}
                                            className="shrink-0 p-1.5 rounded-lg text-gray-400 hover:text-red-500 hover:bg-red-50 dark:hover:bg-red-500/10 transition-colors" title="Quitar">
                                            <Trash2 size={15} />
                                        </button>
                                    </div>
                                ))}
                            </div>
                        )}

                        {items.length > 0 && (
                            <div className="flex items-center justify-between mt-2.5 text-sm">
                                <span className="text-gray-500 dark:text-gray-400">Suma de servicios</span>
                                <span className="font-bold text-gray-800 dark:text-gray-100">{fmt(sumaServicios)}</span>
                            </div>
                        )}

                        {/* Agregar (buscador + catálogo) */}
                        <div className="mt-4 pt-4 border-t border-gray-100 dark:border-dark-border flex flex-col">
                            <label className={`${labelClass} mb-2`}>Agregar servicio</label>

                            {serviciosDisponibles.length === 0 ? (
                                <div className="flex flex-col items-center justify-center text-center border border-dashed border-gray-200 dark:border-dark-border rounded-xl py-5 px-4">
                                    <AlertCircle className="text-amber-500 mb-1.5" size={24} />
                                    <p className="text-sm font-medium text-gray-600 dark:text-gray-300">No hay servicios activos.</p>
                                    <p className="text-xs text-gray-400 mt-0.5">Crea al menos un servicio antes de armar un paquete.</p>
                                </div>
                            ) : (
                                <>
                                    <div className="relative mb-2">
                                        <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-gray-400">
                                            <Search size={16} />
                                        </div>
                                        <input
                                            type="text" value={servicioSearch} onChange={(e) => setServicioSearch(e.target.value)}
                                            placeholder="Buscar servicio..."
                                            className="w-full pl-9 pr-3 py-2 text-sm border border-gray-200 dark:border-dark-border rounded-lg focus:outline-none focus:ring-2 focus:ring-brand-500/50 focus:border-brand-500 bg-white dark:bg-dark-bg text-gray-900 dark:text-white"
                                        />
                                    </div>

                                    <div className="max-h-[180px] overflow-y-auto space-y-2 pr-1 scrollbar-thin scrollbar-thumb-gray-200 dark:scrollbar-thumb-gray-700">
                                        {serviciosFiltrados.length === 0 ? (
                                            <p className="text-sm text-gray-400 text-center py-6">Sin coincidencias.</p>
                                        ) : (
                                            serviciosFiltrados.map((s) => {
                                                const yaAgregado = selectedIds.has(s.id);
                                                return (
                                                    <button
                                                        type="button" key={s.id} onClick={() => addServicio(s)} disabled={yaAgregado}
                                                        className={`w-full flex items-center gap-3 p-2 rounded-xl border text-left transition-all ${yaAgregado
                                                            ? 'border-brand-200 bg-brand-50/60 dark:bg-brand-500/10 dark:border-brand-500/30 cursor-default'
                                                            : 'border-gray-200 dark:border-dark-border hover:border-brand-300 dark:hover:border-brand-500/40 bg-white dark:bg-dark-bg'
                                                            }`}
                                                    >
                                                        {s.imagenUrl ? (
                                                            <img src={s.imagenUrl} alt={s.nombre} className="shrink-0 w-9 h-9 rounded-lg object-cover" />
                                                        ) : (
                                                            <span className="shrink-0 w-9 h-9 rounded-lg bg-gradient-to-br from-brand-400 to-brand-600 flex items-center justify-center text-white">
                                                                <Briefcase size={16} />
                                                            </span>
                                                        )}
                                                        <span className="flex-1 min-w-0">
                                                            <span className="block font-semibold text-sm text-gray-800 dark:text-gray-100 truncate">{s.nombre}</span>
                                                            <span className="block text-xs text-brand-600 dark:text-brand-400 font-medium">{fmt(s.precio)}</span>
                                                        </span>
                                                        <span className={`shrink-0 ${yaAgregado ? 'text-brand-500' : 'text-gray-400'}`}>
                                                            {yaAgregado ? <Check size={18} /> : <Plus size={18} />}
                                                        </span>
                                                    </button>
                                                );
                                            })
                                        )}
                                    </div>
                                </>
                            )}
                        </div>
                    </div>
                </div>

                {/* BOTONERA */}
                <div className="flex gap-3 pt-6 mt-8 border-t border-gray-100 dark:border-dark-border">
                    <button type="button" onClick={onClose}
                        className="flex-1 py-3 rounded-xl border border-gray-200 dark:border-dark-border text-gray-600 dark:text-gray-400 font-semibold hover:bg-gray-50 dark:hover:bg-dark-bg transition-colors">
                        Cancelar
                    </button>
                    <button type="submit" disabled={saving || serviciosDisponibles.length === 0}
                        className="flex-1 group bg-brand-600 hover:bg-brand-700 dark:bg-brand-500 dark:hover:bg-brand-600 text-white font-bold py-3 rounded-xl transition-all shadow-lg shadow-brand-500/30 flex justify-center items-center gap-2 disabled:opacity-70 disabled:cursor-not-allowed">
                        {saving ? (
                            <><Loader2 className="animate-spin" size={18} /><span>{selectedFile ? 'Subiendo imagen...' : 'Guardando...'}</span></>
                        ) : (
                            <span>{paqueteToEdit ? 'Guardar Cambios' : 'Registrar Paquete'}</span>
                        )}
                    </button>
                </div>
            </form>
        </Modal>
    );
}
