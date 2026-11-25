import { obtenerSesion, limpiarSesion } from './session.js';

export async function apiFetch(ruta, opciones = {}) {
    const url = ruta.startsWith('/api') ? ruta : `/api${ruta.startsWith('/') ? '' : '/'}${ruta}`;
    const sesion = obtenerSesion();

    const headers = Object.assign(
        { 'Accept': 'application/json' },
        opciones.body ? { 'Content-Type': 'application/json' } : {},
        opciones.headers || {}
    );

    if (sesion && sesion.token) {
        headers['Authorization'] = `Bearer ${sesion.token}`;
    }

    const respuesta = await fetch(url, Object.assign({}, opciones, { headers }));
    let data = null;
    const contentType = respuesta.headers.get('content-type') || '';
    if (contentType.includes('application/json')) {
        data = await respuesta.json();
    }

    if (respuesta.status === 401 && !url.includes('/api/auth/login')) {
        limpiarSesion();
        if (!opciones.silenciar401) {
            alert((data && data.mensaje) || 'Tu sesión ha expirado. Por favor vuelve a ingresar.');
            window.location.href = 'index.html?login=1';
        }
        const err = new Error((data && data.mensaje) || 'Sesión expirada');
        err.status = 401;
        throw err;
    }

    if (!respuesta.ok) {
        const err = new Error((data && data.mensaje) || `Error HTTP ${respuesta.status}`);
        err.status = respuesta.status;
        throw err;
    }

    return data;
}

export const apiGet = (ruta, opts = {}) => apiFetch(ruta, Object.assign({ method: 'GET' }, opts));
export const apiPost = (ruta, body, opts = {}) => apiFetch(ruta, Object.assign({ method: 'POST', body: body !== undefined ? JSON.stringify(body) : undefined }, opts));
export const apiPut = (ruta, body, opts = {}) => apiFetch(ruta, Object.assign({ method: 'PUT', body: JSON.stringify(body) }, opts));
export const apiPatch = (ruta, body, opts = {}) => apiFetch(ruta, Object.assign({ method: 'PATCH', body: JSON.stringify(body) }, opts));
