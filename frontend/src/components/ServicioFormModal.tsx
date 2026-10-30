import { useState, useEffect } from 'react';
import { Briefcase, DollarSign, Loader2, Tag, CreditCard } from 'lucide-react';
import CustomSelect from './CustomSelect';
import ImageUploadZone from './ImageUploadZone';
import { uploadImage } from '../api/images.api';
import Modal from './Modal';
import axios from 'axios';

export interface ServicioFormModalProps {
    isOpen: boolean;
    onClose: () => void;
    servicioToEdit?: any;
    categorias?: any[];
    onSave: (payload: any, setSaving: (isSaving: boolean) => void) => void;
    error?: string;
}

export default function ServicioFormModal({ isOpen, onClose, servicioToEdit, categorias = [], onSave, error }: ServicioFormModalProps) {
    const [nombre, setNombre] = useState('');
    const [descripcion, setDescripcion] = useState('');
    const [precio, setPrecio] = useState('');
    const [categoriaId, setCategoriaId] = useState('');
    const [tipoCobro, setTipoCobro] = useState('');

    // Estados para la imagen
    const [selectedFile, setSelectedFile] = useState<File | null>(null);
    const [currentImageUrl, setCurrentImageUrl] = useState('');
    const [localError, setLocalError] = useState('');

    const [saving, setSaving] = useState(false);

    useEffect(() => {
        if (isOpen) {
            setLocalError('');
            if (servicioToEdit) {
                setNombre(servicioToEdit.nombre);
                setDescripcion(servicioToEdit.descripcion || '');
                setPrecio(servicioToEdit.precio.toString());
                setCategoriaId(servicioToEdit.categoria?.id?.toString() || '');
                setTipoCobro(servicioToEdit.tipoCobro || '');
                setCurrentImageUrl(servicioToEdit.imagenUrl || '');
                setSelectedFile(null);
            } else {
                setNombre('');
                setDescripcion('');
                setPrecio('');
                setCategoriaId('');
                setTipoCobro('');
                setCurrentImageUrl('');
                setSelectedFile(null);
            }
            setSaving(false);
        }
    }, [isOpen, servicioToEdit]);

    const handleSubmit = async (e: React.SubmitEvent<HTMLFormElement>) => {
        e.preventDefault();
        setSaving(true);
        setLocalError('');

        let finalImageUrl = currentImageUrl;

        // Si el usuario seleccionó un archivo nuevo, lo subimos a Cloudinary primero
        if (selectedFile) {
            try {
                const uploadRes = await uploadImage(selectedFile);
                finalImageUrl = uploadRes.url;
            } catch (err) {
                const msg = axios.isAxiosError(err) ? err.response?.data?.message : undefined;
                setLocalError(msg || 'Error al subir la imagen. Verifica que sea una imagen válida menor a 5MB.');
                setSaving(false);
                return; // Abortar guardado
            }
        }

        const payload = {
            nombre,
            descripcion,
            precio: parseFloat(precio),
            categoriaId: categoriaId ? parseInt(categoriaId) : null,
            tipoCobro: tipoCobro || null,
            imagenUrl: finalImageUrl || null,
            activo: true
        };
        await onSave(payload, setSaving);
    };

    return (
        <Modal
            isOpen={isOpen}
            onClose={onClose}
            icon={Briefcase}
            title={servicioToEdit ? 'Editar Servicio' : 'Nuevo Servicio'}
            subtitle={servicioToEdit ? 'Modifica los datos del servicio seleccionado.' : 'Registra un nuevo servicio en tu catálogo.'}
            error={localError || error}
            maxWidth="max-w-4xl"
        >
            {/* FORMULARIO */}
            <form onSubmit={handleSubmit}>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-8">

                    {/* COLUMNA IZQUIERDA: Textos y Opciones */}
                    <div className="space-y-5">
                        {/* Nombre */}
                        <div className="space-y-1.5">
                            <label className="text-sm font-semibold text-gray-700 dark:text-gray-300 ml-1">Nombre del Servicio</label>
                            <div className="relative">
                                <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none text-gray-400">
                                    <Briefcase size={18} />
                                </div>
                                <input
                                    type="text"
                                    required
                                    autoFocus
                                    className="w-full pl-11 pr-4 py-3 border border-gray-200 dark:border-dark-border rounded-xl focus:outline-none focus:ring-2 focus:ring-brand-500/50 focus:border-brand-500 bg-white dark:bg-dark-bg text-gray-900 dark:text-white transition-all shadow-sm"
                                    placeholder="Ej: Pista iluminada"
                                    value={nombre}
                                    onChange={(e) => setNombre(e.target.value)}
                                />
                            </div>
                        </div>

                        {/* Fila: Precio y Tipo de Cobro */}
                        <div className="flex flex-col sm:flex-row gap-4">
                            <div className="space-y-1.5 flex-1">
                                <label className="text-sm font-semibold text-gray-700 dark:text-gray-300 ml-1">Precio Base</label>
                                <div className="relative">
                                    <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none text-gray-400">
                                        <DollarSign size={18} />
                                    </div>
                                    <input
                                        type="number"
                                        step="0.01"
                                        min="0.01"
                                        required
                                        className="w-full pl-11 pr-4 py-3 border border-gray-200 dark:border-dark-border rounded-xl focus:outline-none focus:ring-2 focus:ring-brand-500/50 focus:border-brand-500 bg-white dark:bg-dark-bg text-gray-900 dark:text-white transition-all shadow-sm"
                                        placeholder="0.00"
                                        value={precio}
                                        onChange={(e) => setPrecio(e.target.value)}
                                    />
                                </div>
                            </div>

                            <div className="space-y-1.5 flex-1">
                                <label className="text-sm font-semibold text-gray-700 dark:text-gray-300 ml-1">Tipo de Cobro</label>
                                <CustomSelect
                                    options={[
                                        { value: 'POR_EVENTO', label: 'Por Evento' },
                                        { value: 'POR_HORA', label: 'Por Hora' },
                                        { value: 'POR_PERSONA', label: 'Por Persona' },
                                        { value: 'POR_UNIDAD', label: 'Por Unidad' }
                                    ]}
                                    value={tipoCobro}
                                    onChange={setTipoCobro}
                                    placeholder="Seleccione..."
                                    icon={CreditCard}
                                    required={true}
                                    name="tipoCobro"
                                />
                            </div>
                        </div>

                        {/* Categoría */}
                        <div className="space-y-1.5">
                            <label className="text-sm font-semibold text-gray-700 dark:text-gray-300 ml-1">Categoría</label>
                            <CustomSelect
                                options={categorias.map(cat => ({ value: cat.id.toString(), label: cat.nombre }))}
                                value={categoriaId}
                                onChange={setCategoriaId}
                                placeholder="Sin categoría"
                                icon={Tag}
                            />
                        </div>

                        {/* Descripción */}
                        <div className="space-y-1.5">
                            <label className="text-sm font-semibold text-gray-700 dark:text-gray-300 ml-1">Descripción <span className="text-gray-400 font-normal">(opcional)</span></label>
                            <textarea
                                className="w-full p-4 border border-gray-200 dark:border-dark-border rounded-xl focus:outline-none focus:ring-2 focus:ring-brand-500/50 focus:border-brand-500 bg-white dark:bg-dark-bg text-gray-900 dark:text-white h-24 resize-none transition-all shadow-sm scrollbar-thin scrollbar-thumb-gray-200 dark:scrollbar-thumb-gray-700"
                                placeholder="Describe brevemente este servicio..."
                                value={descripcion}
                                onChange={(e) => setDescripcion(e.target.value)}
                            />
                        </div>
                    </div>

                    {/* COLUMNA DERECHA: Imagen */}
                    <div className="space-y-5 h-full flex flex-col">
                        {/* Zona de subida de imagen */}
                        <div className="flex-1 flex flex-col">
                            <ImageUploadZone
                                currentImageUrl={currentImageUrl}
                                onFileSelect={(file) => setSelectedFile(file)}
                                onClear={() => {
                                    setSelectedFile(null);
                                    setCurrentImageUrl('');
                                }}
                            />
                            <p className="text-xs text-gray-400 dark:text-gray-500 mt-4 text-center">
                                La imagen recomendada debe tener una proporción cuadrada (1:1) o ligeramente panorámica (4:3) para que luzca bien en el catálogo.
                            </p>
                        </div>
                    </div>

                </div>

                {/* Botones del modal */}
                <div className="flex gap-3 pt-6 mt-8 border-t border-gray-100 dark:border-dark-border">
                    <button
                        type="button"
                        onClick={onClose}
                        className="flex-1 py-3 rounded-xl border border-gray-200 dark:border-dark-border text-gray-600 dark:text-gray-400 font-semibold hover:bg-gray-50 dark:hover:bg-dark-bg transition-colors"
                    >
                        Cancelar
                    </button>
                    <button
                        type="submit"
                        disabled={saving}
                        className="flex-1 group bg-brand-600 hover:bg-brand-700 dark:bg-brand-500 dark:hover:bg-brand-600 text-white font-bold py-3 rounded-xl transition-all shadow-lg shadow-brand-500/30 flex justify-center items-center gap-2 disabled:opacity-70 disabled:cursor-not-allowed"
                    >
                        {saving ? (
                            <>
                                <Loader2 className="animate-spin" size={18} />
                                <span>{selectedFile ? 'Subiendo imagen...' : (servicioToEdit ? 'Guardando...' : 'Registrando...')}</span>
                            </>
                        ) : (
                            <>
                                <span>{servicioToEdit ? 'Guardar Cambios' : 'Registrar Servicio'}</span>
                            </>
                        )}
                    </button>
                </div>
            </form>
        </Modal>
    );
}
