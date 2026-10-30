
import axios from 'axios';
import { useState, useEffect, type ChangeEvent, type SubmitEvent } from 'react';
import { createPortal } from 'react-dom';
import { motion } from 'framer-motion';
import {
    Store, Link as LinkIcon, AlignLeft, Save, Loader2,
    Palette, Contact, Mail, Phone, MapPin, Hash, Building, Building2, Map, Mailbox
} from 'lucide-react';
import api from '../api/axiosConfig';
import ImageUploadZone from '../components/ImageUploadZone';
import InputField, { inputClass, labelClass, iconWrapClass } from '../components/InputField';
import Toast, { ToastMessage } from '../components/Toast';
import { uploadImage } from '../api/images.api';

// Estado inicial de la dirección (objeto anidado, espeja al DireccionDTO del backend)
const DIRECCION_VACIA = { calle: '', numero: '', colonia: '', ciudad: '', estado: '', codigoPostal: '' };

export default function Configuracion() {
    // Variables para guardar los datos del formulario
    const [formData, setFormData] = useState({
        nombre: '', slug: '', logoUrl: '',
        telefono: '', email: '', descripcion: '',
        direccion: { ...DIRECCION_VACIA }
    });

    // Archivo de logo seleccionado pero AÚN no subido (se sube al guardar)
    const [selectedFile, setSelectedFile] = useState<File | null>(null);

    // Variables para manejar el estado de la pantalla (cargando, errores, éxito)
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [message, setMessage] = useState<ToastMessage | null>(null); // type: 'success' | 'error'

    // 1. OBTENER LOS DATOS (Equivalente al @GetMapping("/me") de Spring Boot)
    useEffect(() => {
        const fetchMiNegocio = async () => {
            try {
                // Fíjate que no pasamos ningún ID. El Backend sabe quiénes somos gracias al token JWT.
                const response = await api.get('/negocios/me');
                const d = response.data;
                const dir = d.direccion || {};

                // Spring Boot nos responde con un NegocioResponse. Lo inyectamos en nuestro formulario:
                setFormData({
                    nombre: d.nombre || '',
                    slug: d.slug || '',
                    logoUrl: d.logoUrl || '',
                    telefono: d.telefono || '',
                    email: d.email || '',
                    descripcion: d.descripcion || '',
                    direccion: {
                        calle: dir.calle || '',
                        numero: dir.numero || '',
                        colonia: dir.colonia || '',
                        ciudad: dir.ciudad || '',
                        estado: dir.estado || '',
                        codigoPostal: dir.codigoPostal || ''
                    }
                });
            } catch (error) {
                setMessage({ text: 'Error al cargar los datos del negocio', type: 'error' });
            } finally {
                setLoading(false);
            }
        };

        fetchMiNegocio();
    }, []); // Se ejecuta solo una vez al abrir la página

    // UX: el aviso (toast) se descarta solo. Éxito a los 3.5s; error a los 6s
    // (más tiempo para leerlo). También se puede cerrar manualmente con la X.
    useEffect(() => {
        if (!message?.text) return;
        const ms = message.type === 'success' ? 3500 : 6000;
        const timer = setTimeout(() => setMessage(null), ms);
        return () => clearTimeout(timer);
    }, [message]);

    // Actualiza un campo de primer nivel del formulario
    const updateField = (key: string) => (e: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) =>
        setFormData((prev) => ({ ...prev, [key]: e.target.value }));


    // Actualiza un campo anidado dentro de 'direccion'
    const updateDireccion = (key: string) => (e: ChangeEvent<HTMLInputElement>) => setFormData((prev) => ({
        ...prev,
        direccion: { ...prev.direccion, [key]: e.target.value }
    }));

    // El "número" de calle no es aritmético: permitimos letras/dígitos y / - espacio
    // (para "S/N", "742-A", "Bis"), pero filtramos símbolos raros mientras se escribe.
    const handleNumeroChange = (e: ChangeEvent<HTMLInputElement>) => {
        const limpio = e.target.value.replace(/[^a-zA-Z0-9\/\- ]/g, '');
        setFormData((prev) => ({ ...prev, direccion: { ...prev.direccion, numero: limpio } }));
    };

    // Sanitiza el slug mientras el usuario escribe: minúsculas, espacios -> guion,
    // y elimina cualquier carácter que no sea letra minúscula, número o guion.
    // Así el backend (FA-04.2) casi nunca tendrá que rechazarlo.
    const handleSlugChange = (e: ChangeEvent<HTMLInputElement>) => {
        const slugLimpio = e.target.value
            .toLowerCase()
            .replace(/\s+/g, '-')        // espacios -> guion
            .replace(/[^a-z0-9-]/g, ''); // quita acentos, símbolos, etc.
        setFormData((prev) => ({ ...prev, slug: slugLimpio }));
    };

    // 2. GUARDAR LOS DATOS (Equivalente al @PutMapping("/me") de Spring Boot)
    const handleSubmit = async (e: SubmitEvent<HTMLFormElement>) => {
        e.preventDefault();
        setSaving(true);
        setMessage(null);

        try {
            let finalLogoUrl = formData.logoUrl;

            // Si el usuario eligió un logo nuevo, lo subimos a Cloudinary PRIMERO
            // y nos quedamos solo con el link que nos devuelve.
            if (selectedFile) {
                try {
                    const uploadRes = await uploadImage(selectedFile);
                    finalLogoUrl = uploadRes.url;
                } catch (err) {
                    const msg = axios.isAxiosError(err) ? err.response?.data?.message : undefined;
                    setMessage({
                        text: msg || 'Error al subir la imagen. Verifica que sea una imagen válida menor a 5MB.',
                        type: 'error'
                    });
                    setSaving(false);
                    return; // Abortamos: no guardamos si la imagen falló
                }
            }

            // Mandamos el NegocioUpdateRequest a Java (con el link del logo, no el archivo)
            const payload = { ...formData, logoUrl: finalLogoUrl };
            await api.put('/negocios/me', payload);

            // Sincronizamos el estado: el logo ya es una URL y limpiamos el archivo pendiente
            setFormData((prev) => ({ ...prev, logoUrl: finalLogoUrl }));
            setSelectedFile(null);
            setMessage({ text: '¡Cambios guardados exitosamente!', type: 'success' });
        } catch (error) {
            const msg = axios.isAxiosError(error) ? error.response?.data?.message : undefined;
            setMessage({ text: msg || 'Error al guardar los cambios', type: 'error' });
        } finally {
            setSaving(false);
        }
    };

    // Si aún está cargando la info inicial, mostramos un pequeño loader
    if (loading) {
        return <div className="flex justify-center items-center h-64"><Loader2 className="animate-spin text-brand-500" size={32} /></div>;
    }

    return (
        <div className="max-w-5xl mx-auto space-y-6 pb-24">

            <div>
                <h1 className="text-3xl font-bold text-gray-900 dark:text-white">Configuración del Negocio</h1>
                <p className="text-gray-500 dark:text-gray-400 mt-1">
                    Modifica la identidad visual y los datos públicos de tu salón de eventos.
                </p>
            </div>

            <form id="negocio-form" onSubmit={handleSubmit} className="space-y-6">

                {/* ===================== TARJETA 1: IDENTIDAD VISUAL ===================== */}
                <motion.div
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    className="bg-white/80 dark:bg-dark-surface/80 backdrop-blur-xl p-8 rounded-2xl shadow-sm border border-gray-100 dark:border-dark-border"
                >
                    <div className="flex items-center gap-3 mb-6 border-b border-gray-100 dark:border-dark-border pb-4">
                        <div className="p-2.5 rounded-xl bg-brand-50 dark:bg-brand-900/20 text-brand-600 dark:text-brand-400">
                            <Palette size={20} />
                        </div>
                        <div>
                            <h2 className="font-bold text-gray-900 dark:text-white">Identidad Visual</h2>
                            <p className="text-xs text-gray-500 dark:text-gray-400">Cómo te verán tus clientes en el portal público.</p>
                        </div>
                    </div>

                    {/* Logo a la izquierda; Nombre + Slug + Descripción apilados a la derecha */}
                    <div className="flex flex-col sm:flex-row gap-8 items-start">
                        <div className="w-full sm:w-64 shrink-0">
                            <ImageUploadZone
                                label="Logotipo del Negocio"
                                currentImageUrl={formData.logoUrl}
                                onFileSelect={(file) => setSelectedFile(file)}
                                onClear={() => {
                                    setSelectedFile(null);
                                    setFormData((prev) => ({ ...prev, logoUrl: '' }));
                                }}
                            />
                        </div>

                        <div className="flex-1 w-full space-y-5">
                            <InputField label="Nombre Comercial" icon={Store} type="text" required
                                value={formData.nombre} onChange={updateField('nombre')} />

                            <div className="space-y-1.5">
                                <label className={labelClass}>Enlace Público (Slug)</label>
                                <div className="relative">
                                    <div className={iconWrapClass}><LinkIcon size={18} /></div>
                                    <input
                                        type="text"
                                        required
                                        className={`${inputClass} font-mono text-sm`}
                                        value={formData.slug}
                                        onChange={handleSlugChange}
                                    />
                                </div>
                                <p className="text-xs text-gray-500 dark:text-gray-400 ml-1">Tus clientes entrarán a: <span className="font-semibold text-brand-500">app.gestorsalon.com/reservar/{formData.slug || '...'}</span></p>
                            </div>

                            {/* Descripción: presentación pública del salón (misma columna que Nombre y Slug) */}
                            <div className="space-y-1.5">
                                <label className={labelClass}>Descripción</label>
                                <div className="relative">
                                    <div className="absolute top-3.5 left-0 pl-4 flex items-start pointer-events-none text-gray-400"><AlignLeft size={18} /></div>
                                    <textarea
                                        rows={4}
                                        maxLength={500}
                                        className={`${inputClass} resize-none`}
                                        placeholder="Describe tu salón para que los clientes lo conozcan en el portal público..."
                                        value={formData.descripcion}
                                        onChange={updateField('descripcion')}
                                    />
                                </div>
                                <p className="text-xs text-gray-500 dark:text-gray-400 ml-1 text-right">{formData.descripcion.length}/500</p>
                            </div>
                        </div>
                    </div>
                </motion.div>

                {/* ===================== TARJETA 2: DATOS DE CONTACTO ===================== */}
                <motion.div
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ delay: 0.1 }}
                    className="bg-white/80 dark:bg-dark-surface/80 backdrop-blur-xl p-8 rounded-2xl shadow-sm border border-gray-100 dark:border-dark-border"
                >
                    <div className="flex items-center gap-3 mb-6 border-b border-gray-100 dark:border-dark-border pb-4">
                        <div className="p-2.5 rounded-xl bg-brand-50 dark:bg-brand-900/20 text-brand-600 dark:text-brand-400">
                            <Contact size={20} />
                        </div>
                        <div>
                            <h2 className="font-bold text-gray-900 dark:text-white">Datos de Contacto</h2>
                            <p className="text-xs text-gray-500 dark:text-gray-400">Información de ubicación y contacto del salón.</p>
                        </div>
                    </div>

                    <div className="space-y-6">
                        {/* Correo + Teléfono */}
                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
                            <InputField label="Correo Empresarial" icon={Mail} type="email" placeholder="contacto@misalon.com"
                                value={formData.email} onChange={updateField('email')} />
                            <InputField label="Teléfono" icon={Phone} type="tel" placeholder="644 123 4567"
                                value={formData.telefono} onChange={updateField('telefono')} />
                        </div>

                        {/* Subsección: Dirección */}
                        <div className="pt-2">
                            <p className="text-xs font-bold uppercase tracking-wide text-gray-400 dark:text-gray-500 ml-1 mb-4">Dirección</p>
                            <div className="space-y-6">
                                {/* Calle (2/3) + Número (1/3) */}
                                <div className="grid grid-cols-1 sm:grid-cols-3 gap-6">
                                    <div className="sm:col-span-2">
                                        <InputField label="Calle" icon={MapPin} type="text" placeholder="Av. Siempre Viva"
                                            value={formData.direccion.calle} onChange={updateDireccion('calle')} />
                                    </div>
                                    <InputField label="Número" icon={Hash} type="text" placeholder="742 / S/N"
                                        value={formData.direccion.numero} onChange={handleNumeroChange} />
                                </div>

                                {/* Colonia */}
                                <InputField label="Colonia" icon={Building} type="text" placeholder="Centro"
                                    value={formData.direccion.colonia} onChange={updateDireccion('colonia')} />

                                {/* Ciudad + Estado + C.P. */}
                                <div className="grid grid-cols-1 sm:grid-cols-3 gap-6">
                                    <InputField label="Ciudad" icon={Building2} type="text" placeholder="Ciudad Obregón"
                                        value={formData.direccion.ciudad} onChange={updateDireccion('ciudad')} />
                                    <InputField label="Estado" icon={Map} type="text" placeholder="Sonora"
                                        value={formData.direccion.estado} onChange={updateDireccion('estado')} />
                                    <InputField label="Código Postal" icon={Mailbox} type="text" placeholder="85000"
                                        value={formData.direccion.codigoPostal} onChange={updateDireccion('codigoPostal')} />
                                </div>
                            </div>
                        </div>
                    </div>
                </motion.div>

            </form>

            {/* Toast de feedback (componente reutilizable, portal a body, siempre visible) */}
            <Toast message={message} onClose={() => setMessage(null)} />

            {/* Botón Guardar flotante: fijo abajo a la derecha de la PANTALLA.
                Se renderiza en document.body (vía portal) para que 'fixed' se ancle
                al viewport y no al motion.div del Layout (que usa transform).
                form="negocio-form" lo conecta con el formulario para poder enviar. */}
            {createPortal(
                <div className="fixed bottom-6 right-6 z-50">
                    <button
                        type="submit"
                        form="negocio-form"
                        disabled={saving}
                        className="bg-brand-600 hover:bg-brand-700 text-white font-bold py-3.5 px-7 rounded-2xl transition-all shadow-xl shadow-brand-600/40 flex items-center gap-2 disabled:opacity-70 hover:scale-105 active:scale-95"
                    >
                        {saving ? <Loader2 className="animate-spin" size={18} /> : <Save size={18} />}
                        <span>{saving ? (selectedFile ? 'Subiendo logo...' : 'Guardando...') : 'Guardar Cambios'}</span>
                    </button>
                </div>,
                document.body
            )}
        </div>
    );
}
