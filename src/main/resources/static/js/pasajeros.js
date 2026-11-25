import { exigirRoles, limpiarSesion } from './core/session.js';
import { escaparHtml, moneda, formatearFechaISO } from './core/formatters.js';

let intervalId = null;

window.redirigirInicio = function () {
    sessionStorage.removeItem('asientosComprando');
    sessionStorage.removeItem('limiteTiempoCompra');
    sessionStorage.removeItem('viajeSeleccionadoId');
    sessionStorage.removeItem('datosPasajerosComprando');
    window.location.href = 'index.html';
};

document.addEventListener('DOMContentLoaded', () => {
    const sesion = exigirRoles(['CLIENTE', 'VENDEDOR', 'EMBARQUE', 'ADMIN']);
    if (!sesion) return;

    document.getElementById('usuarioHeader').textContent = sesion.nombreCompleto || sesion.nombre;

    const asientosRaw = sessionStorage.getItem('asientosComprando');
    if (!asientosRaw) {
        window.location.href = 'asientos.html';
        return;
    }

    const infoViaje = JSON.parse(sessionStorage.getItem('resumenViajeComprando') || 'null');
    if (infoViaje) {
        document.getElementById('rutaResumenHeader').innerHTML =
            `<i class="bi bi-geo-alt text-warning"></i> ${escaparHtml(infoViaje.origenCiudad)} → ${escaparHtml(infoViaje.destinoCiudad)} (${formatearFechaISO(infoViaje.fechaSalida)} ${escaparHtml(infoViaje.horaSalida)})`;
    }

    const asientos = JSON.parse(asientosRaw);
    document.getElementById('totalPasajerosBadge').textContent = `${asientos.length} a bordo`;

    renderizarFormularios(asientos, sesion);
    renderizarDesglose(asientos);
    iniciarTemporizador();

    document.getElementById('formPasajeros')?.addEventListener('submit', (e) => procesarDatosPasajeros(e, asientos));
});

function renderizarFormularios(asientos, sesion) {
    const contenedor = document.getElementById('contenedorFormulariosPasajeros');
    contenedor.innerHTML = '';

    const previos = JSON.parse(sessionStorage.getItem('datosPasajerosComprando') || '[]');

    asientos.forEach((asiento, index) => {
        const previo = previos.find(p => p.numeroAsiento === asiento.num);
        const usarSesion = !previo && index === 0;

        const tipoDoc = previo?.tipoDocumento || (usarSesion ? (sesion.tipoDocumento || 'DNI') : 'DNI');
        const numDoc = previo?.numeroDocumento || (usarSesion ? (sesion.numeroDocumento || '') : '');
        const nombre = previo?.nombre || (usarSesion ? (sesion.nombre || '') : '');
        const apellido = previo?.apellido || (usarSesion ? (sesion.apellido || '') : '');
        const correo = previo?.correo || (usarSesion ? (sesion.correo || '') : '');
        const telefono = previo?.telefono || (usarSesion ? (sesion.telefono || '') : '');

        const tarjeta = document.createElement('div');
        tarjeta.className = 'card-pasajero p-4 mb-4';
        tarjeta.innerHTML = `
            <div class="d-flex align-items-center justify-content-between mb-3 border-bottom pb-2">
                <div class="d-flex align-items-center gap-2">
                    <span class="badge-asiento">Asiento ${asiento.num}</span>
                    <span class="fw-bold text-dark">Pasajero ${index + 1} · <small class="text-muted">${escaparHtml(asiento.categoria || '')}</small></span>
                </div>
                <span class="text-muted small">Tarifa: <strong>${moneda(asiento.precio)}</strong></span>
            </div>

            <div class="row g-3">
                <div class="col-12 col-md-4">
                    <label class="form-label small fw-bold text-muted">Tipo Documento</label>
                    <select class="form-select bg-white rounded-3" name="tipoDoc_${asiento.num}" required>
                        <option value="DNI" ${tipoDoc === 'DNI' ? 'selected' : ''}>DNI</option>
                        <option value="PASAPORTE" ${tipoDoc === 'PASAPORTE' ? 'selected' : ''}>Pasaporte</option>
                        <option value="CE" ${tipoDoc === 'CE' ? 'selected' : ''}>Carnet de Extranjería</option>
                    </select>
                </div>
                <div class="col-12 col-md-8">
                    <label class="form-label small fw-bold text-muted">Número de Documento</label>
                    <input type="text" class="form-control bg-white rounded-3" name="numDoc_${asiento.num}" value="${escaparHtml(numDoc)}" maxlength="20" required>
                </div>

                <div class="col-12 col-md-6">
                    <label class="form-label small fw-bold text-muted">Nombres</label>
                    <input type="text" class="form-control bg-white rounded-3" name="nombres_${asiento.num}" value="${escaparHtml(nombre)}" required>
                </div>
                <div class="col-12 col-md-6">
                    <label class="form-label small fw-bold text-muted">Apellidos</label>
                    <input type="text" class="form-control bg-white rounded-3" name="apellidos_${asiento.num}" value="${escaparHtml(apellido)}" required>
                </div>

                <div class="col-12 col-md-6">
                    <label class="form-label small fw-bold text-muted">Correo de contacto</label>
                    <input type="email" class="form-control bg-white rounded-3" name="correo_${asiento.num}" value="${escaparHtml(correo)}" required>
                </div>
                <div class="col-12 col-md-6">
                    <label class="form-label small fw-bold text-muted">Teléfono / WhatsApp</label>
                    <input type="tel" class="form-control bg-white rounded-3" name="telefono_${asiento.num}" value="${escaparHtml(telefono)}" maxlength="15">
                </div>
            </div>`;
        contenedor.appendChild(tarjeta);
    });
}

function renderizarDesglose(asientos) {
    const desglose = document.getElementById('desglosePrecios');
    const totalEl = document.getElementById('montoTotalPasajeros');
    let total = 0;

    desglose.innerHTML = asientos.map((a, i) => {
        total += Number(a.precio) || 0;
        return `
            <div class="d-flex justify-content-between small text-muted mb-2">
                <span>Pasajero ${i + 1} (Asiento ${a.num})</span>
                <span class="fw-bold text-dark">${moneda(a.precio)}</span>
            </div>`;
    }).join('');

    totalEl.textContent = moneda(total);
}

function iniciarTemporizador() {
    const exp = Number(sessionStorage.getItem('limiteTiempoCompra')) || (Date.now() + 15 * 60 * 1000);
    const reloj = document.getElementById('relojTemporizador');

    function tick() {
        const restante = exp - Date.now();
        if (restante <= 0) {
            clearInterval(intervalId);
            if (reloj) reloj.textContent = '00:00';
            new bootstrap.Modal(document.getElementById('modalExpirado')).show();
            return;
        }
        const m = Math.floor(restante / 60000);
        const s = Math.floor((restante % 60000) / 1000);
        if (reloj) reloj.textContent = `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
    }

    tick();
    intervalId = setInterval(tick, 1000);
}

function procesarDatosPasajeros(e, asientos) {
    e.preventDefault();
    const listaPasajeros = asientos.map(a => ({
        numeroAsiento: a.num,
        precio: a.precio,
        categoria: a.categoria,
        tipoDocumento: document.querySelector(`[name="tipoDoc_${a.num}"]`).value,
        numeroDocumento: document.querySelector(`[name="numDoc_${a.num}"]`).value.trim(),
        nombre: document.querySelector(`[name="nombres_${a.num}"]`).value.trim(),
        apellido: document.querySelector(`[name="apellidos_${a.num}"]`).value.trim(),
        correo: document.querySelector(`[name="correo_${a.num}"]`).value.trim(),
        telefono: document.querySelector(`[name="telefono_${a.num}"]`).value.trim()
    }));

    sessionStorage.setItem('datosPasajerosComprando', JSON.stringify(listaPasajeros));
    window.location.href = 'pago.html';
}
