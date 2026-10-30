import api from './axiosConfig';

interface UploadResult {
    url: string;
}

export const uploadImage = async (file: File): Promise<UploadResult> => {
    const formData = new FormData();
    formData.append('file', file);
    const response = await api.post<UploadResult>('/images/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
    });
    return response.data;
};

