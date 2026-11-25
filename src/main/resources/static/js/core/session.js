const STORAGE_KEY = 'sesionUrbanRide';

export function obtenerSesion() {
    try {
        const raw = localStorage.getItem(STORAGE_KEY);
        if (!raw) return null;
        const data = JSON.parse(raw);
        return data && data.token ? data : null;
    } catch {
        return null;
    }
}

export function guardarSesion(authData) {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(authData));
    // Limpiar llaves antiguas si existieran
    localStorage.removeItem('usuarioUrbanRide');
    localStorage.removeItem('adminUrbanRide');
}

export function limpiarSesion() {
    localStorage.removeItem(STORAGE_KEY);
    localStorage.removeItem('usuarioUrbanRide');
    localStorage.removeItem('adminUrbanRide');
    sessionStorage.removeItem('asientosComprando');
    sessionStorage.removeItem('datosPasajerosComprando');
    sessionStorage.removeItem('limiteTiempoCompra');
    sessionStorage.removeItem('reservaActivaId');
}

export function urlPanelPorRol(rol) {
    switch ((rol || '').toUpperCase()) {
        case 'ADMIN': return 'admin-panel.html';
        case 'VENDEDOR': return 'vendedor-pos.html';
        case 'EMBARQUE': return 'embarque.html';
        default: return 'mis-viajes.html';
    }
}

export function etiquetaCargo(rol) {
    switch ((rol || '').toUpperCase()) {
        case 'ADMIN': return 'Administrador General';
        case 'VENDEDOR': return 'Vendedor de Ventanilla';
        case 'EMBARQUE': return 'Inspector de Embarque';
        default: return 'Pasajero / Cliente';
    }
}

export function exigirRoles(rolesPermitidos = []) {
    const sesion = obtenerSesion();
    if (!sesion) {
        alert('Debes iniciar sesión para acceder a este módulo.');
        window.location.href = 'index.html?login=1';
        return null;
    }
    const rolActual = (sesion.rol || '').toUpperCase();
    const permitidos = rolesPermitidos.map(r => r.toUpperCase());
    if (permitidos.length > 0 && !permitidos.includes(rolActual)) {
        alert(`Acceso restringido: tu cargo actual (${etiquetaCargo(rolActual)}) no tiene permisos para este panel.`);
        window.location.href = urlPanelPorRol(rolActual);
        return null;
    }
    return sesion;
}

export function abrirComprobante(ventaDetalle) {
    if (ventaDetalle && typeof ventaDetalle === 'object') {
        localStorage.setItem('ultimaVentaUrbanRide', JSON.stringify(ventaDetalle));
        const query = ventaDetalle.idVenta ? `?idVenta=${ventaDetalle.idVenta}` : '';
        window.open(`boleta.html${query}`, '_blank');
    } else if (ventaDetalle) {
        window.open(`boleta.html?idVenta=${encodeURIComponent(ventaDetalle)}`, '_blank');
    }
}
