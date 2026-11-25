export function escaparHtml(texto) {
    return String(texto ?? '').replace(/[&<>"']/g, c => ({
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#39;'
    }[c]));
}

export function moneda(valor) {
    const num = Number(valor) || 0;
    return `S/ ${num.toFixed(2)}`;
}

export function fechaHoyISO() {
    const d = new Date();
    const mes = String(d.getMonth() + 1).padStart(2, '0');
    const dia = String(d.getDate()).padStart(2, '0');
    return `${d.getFullYear()}-${mes}-${dia}`;
}

export function formatearFechaISO(iso) {
    if (!iso) return '-';
    const limpio = String(iso).split('T')[0];
    const partes = limpio.split('-');
    if (partes.length !== 3) return iso;
    return `${partes[2]}/${partes[1]}/${partes[0]}`;
}

export function badgeEstadoVenta(estado) {
    const est = String(estado || '').toUpperCase();
    if (est === 'PAGADO') return '<span class="badge bg-success">PAGADO</span>';
    if (est === 'ANULACION_SOLICITADA') return '<span class="badge bg-warning text-dark">ANULACIÓN PENDIENTE</span>';
    if (est === 'ANULADO') return '<span class="badge bg-danger">ANULADO</span>';
    if (est === 'RESERVADO') return '<span class="badge bg-info text-dark">RESERVADO</span>';
    return `<span class="badge bg-secondary">${escaparHtml(est)}</span>`;
}

export function badgeEstadoViaje(estado) {
    const est = String(estado || '').toUpperCase();
    if (est === 'PROGRAMADO') return '<span class="badge bg-primary">PROGRAMADO</span>';
    if (est === 'EMBARCANDO') return '<span class="badge bg-warning text-dark">EMBARCANDO</span>';
    if (est === 'EN_RUTA') return '<span class="badge bg-info text-dark">EN RUTA</span>';
    if (est === 'FINALIZADO') return '<span class="badge bg-success">FINALIZADO</span>';
    if (est === 'CANCELADO') return '<span class="badge bg-danger">CANCELADO</span>';
    return `<span class="badge bg-secondary">${escaparHtml(est)}</span>`;
}
