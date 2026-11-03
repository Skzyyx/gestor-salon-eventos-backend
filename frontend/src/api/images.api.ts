import api from './axiosConfig';

interface UploadResult {
    url: string;
}

// El mismo límite que el backend (spring.servlet.multipart.max-file-size) y que la Lambda de imágenes.
export const MAX_IMAGE_BYTES = 4 * 1024 * 1024;

// Vacía: las imágenes se suben al backend, como siempre. Con la Function URL de la Lambda de
// imágenes, se suben ahí. Ver docs/lambdas-plan.md.
const UPLOAD_URL = import.meta.env.VITE_UPLOAD_URL;

export const uploadImage = async (file: File): Promise<UploadResult> => {
    if (file.size > MAX_IMAGE_BYTES) {
        throw new Error('La imagen pesa más de 4 MB');
    }

    if (UPLOAD_URL) {
        // La Lambda recibe el archivo tal cual, con su tipo. El interceptor de api agrega el token.
        const response = await api.post<UploadResult>(UPLOAD_URL, file, {
            headers: { 'Content-Type': file.type || 'application/octet-stream' }
        });
        return response.data;
    }

    const formData = new FormData();
    formData.append('file', file);
    const response = await api.post<UploadResult>('/images/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
    });
    return response.data;
};
