import { useState, useRef, useEffect } from 'react';
import { UploadCloud, Image as ImageIcon, X } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';

export interface ImageUploadZoneProps {
    currentImageUrl?: string;
    onFileSelect?: (file: File) => void;
    onClear?: () => void;
    label?: string;
    minHeightClass?: string;
    grow?: boolean;
}

export default function ImageUploadZone({ currentImageUrl, onFileSelect, onClear, label = 'Imagen del Servicio', minHeightClass = 'min-h-[250px]', grow = true }: ImageUploadZoneProps) {
    const [isDragging, setIsDragging] = useState(false);
    const [previewUrl, setPreviewUrl] = useState(currentImageUrl || null);
    const fileInputRef = useRef<HTMLInputElement>(null);

    useEffect(() => {
        setPreviewUrl(currentImageUrl || null);
    }, [currentImageUrl]);

    const handleDragOver = (e: React.DragEvent<HTMLDivElement>) => {
        e.preventDefault();
        setIsDragging(true);
    };

    const handleDragLeave = (e: React.DragEvent<HTMLDivElement>) => {
        e.preventDefault();
        setIsDragging(false);
    };

    const handleDrop = (e: React.DragEvent<HTMLDivElement>) => {
        e.preventDefault();
        setIsDragging(false);
        const files = e.dataTransfer.files;
        if (files && files.length > 0) {
            handleFileSelection(files[0]);
        }
    };

    const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const files = e.target.files;
        if (files && files.length > 0) {
            handleFileSelection(files[0]);
        }
    };

    const handleFileSelection = (file: File) => {
        // Crear una URL local para la previsualización
        const objectUrl = URL.createObjectURL(file);
        setPreviewUrl(objectUrl);
        // Notificar al componente padre
        if (onFileSelect) onFileSelect(file);
    };

    const handleClear = (e: React.MouseEvent<HTMLButtonElement>) => {
        e.stopPropagation();
        setPreviewUrl(null);
        if (fileInputRef.current) fileInputRef.current.value = '';
        if (onClear) onClear();
    };

    return (
        <div className={`space-y-1.5 flex flex-col ${grow ? 'flex-1 h-full' : ''}`}>
            <label className="text-sm font-semibold text-gray-700 dark:text-gray-300 ml-1">{label}</label>

            <div
                className={`relative border-2 border-dashed rounded-xl transition-all overflow-hidden
                    ${isDragging ? 'border-brand-500 bg-brand-50 dark:bg-brand-500/10' : 'border-gray-300 dark:border-dark-border hover:border-brand-400 dark:hover:border-brand-500'}
                    w-full ${minHeightClass} ${grow ? 'flex-1' : ''} flex flex-col items-center justify-center cursor-pointer`}
                onDragOver={handleDragOver}
                onDragLeave={handleDragLeave}
                onDrop={handleDrop}
                onClick={() => !previewUrl && fileInputRef.current?.click()}
            >
                <input
                    type="file"
                    ref={fileInputRef}
                    className="hidden"
                    accept="image/jpeg, image/png, image/webp"
                    onChange={handleFileChange}
                />

                <AnimatePresence mode="wait">
                    {previewUrl ? (
                        <motion.div
                            key="preview"
                            initial={{ opacity: 0 }}
                            animate={{ opacity: 1 }}
                            exit={{ opacity: 0 }}
                            className="absolute inset-0 w-full h-full group"
                        >
                            <img src={previewUrl} alt="Vista previa" className="w-full h-full object-cover" />
                            {/* Overlay oscuro que aparece en hover */}
                            <div className="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center gap-4 backdrop-blur-sm">
                                <button
                                    type="button"
                                    onClick={(e) => { e.stopPropagation(); fileInputRef.current?.click(); }}
                                    className="p-2.5 bg-white text-gray-900 rounded-full hover:scale-110 transition-transform shadow-lg"
                                    title="Cambiar imagen"
                                >
                                    <ImageIcon size={20} />
                                </button>
                                <button
                                    type="button"
                                    onClick={handleClear}
                                    className="p-2.5 bg-red-500 text-white rounded-full hover:scale-110 transition-transform shadow-lg"
                                    title="Eliminar imagen"
                                >
                                    <X size={20} />
                                </button>
                            </div>
                        </motion.div>
                    ) : (
                        <motion.div
                            key="placeholder"
                            initial={{ opacity: 0 }}
                            animate={{ opacity: 1 }}
                            exit={{ opacity: 0 }}
                            className="flex flex-col items-center text-gray-500 dark:text-gray-400 p-4 text-center pointer-events-none"
                        >
                            <div className={`p-3 rounded-full mb-2 transition-colors ${isDragging ? 'bg-brand-100 dark:bg-brand-900/30 text-brand-600 dark:text-brand-400' : 'bg-gray-100 dark:bg-dark-bg text-gray-400'}`}>
                                <UploadCloud size={24} />
                            </div>
                            <span className="text-sm font-medium text-gray-700 dark:text-gray-300">
                                <span className="text-brand-600 dark:text-brand-400">Haz clic para subir</span> o arrastra y suelta
                            </span>
                            <span className="text-xs text-gray-400 dark:text-gray-500 mt-1">PNG, JPG, WEBP (Max. 5MB)</span>
                        </motion.div>
                    )}
                </AnimatePresence>
            </div>
        </div>
    );
}
