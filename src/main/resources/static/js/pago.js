import { apiGet, apiPost } from './core/api.js';
import { exigirRoles, abrirComprobante } from './core/session.js';
import { escaparHtml, moneda, formatearFechaISO } from './core/formatters.js';
import { inicializarSelectorPago } from './components/payment-selector.js';

let descuentoAplicado = 0;
let codigoCuponValidado = null;
let subtotalGeneral = 0;
let cronometroInterval = null;
let ultimaVentaGuardada = null;

window.redirigirInicio = function () {
    sessionStorage.removeItem('asientosComprando');
    sessionStorage.removeItem('limiteTiempoCompra');
    sessionStorage.removeItem('viajeSeleccionadoId');
    sessionStorage.removeItem('datosPasajerosComprando');
    sessionStorage.removeItem('reservaActivaId');
    window.location.href = 'index.html';
};

window.abrirBoletaPDF = function () {
    if (ultimaVentaGuardada) {
        abrirComprobante(ultimaVentaGuardada);
    }
};

document.addEventListener('DOMContentLoaded', () => {
    const sesion = exigirRoles(['CLIENTE', 'VENDEDOR', 'EMBARQUE', 'ADMIN']);
    if (!sesion) return;

    document.getElementById('usuarioHeader').textContent = sesion.nombreCompleto || sesion.nombre;

    const pasajerosRaw = sessionStorage.getItem('datosPasajerosComprando');
    const idViaje = Number(sessionStorage.getItem('viajeSeleccionadoId'));
    if (!pasajerosRaw || !idViaje) {
        window.location.href = 'asientos.html';
        return;
    }

    const pasajeros = JSON.parse(pasajerosRaw);
    const infoViaje = JSON.parse(sessionStorage.getItem('resumenViajeComprando') || 'null');

    if (infoViaje) {
        const textoRuta = `${infoViaje.origenCiudad} → ${infoViaje.destinoCiudad} (${formatearFechaISO(infoViaje.fechaSalida)} ${infoViaje.horaSalida})`;
        document.getElementById('headerRutaResumen').innerHTML = `<i class="bi bi-geo-alt text-warning"></i> ${escaparHtml(textoRuta)}`;
        document.getElementById('resumenOrigenDestino').textContent = `${textoRuta} · ${infoViaje.servicio}`;
    }

    autocompletarComprador(pasajeros[0]);
    calcularTotales(pasajeros);
    iniciarTemporizadorSincronizado();

    const selectorPago = inicializarSelectorPago({
        contenedorOpciones: document.getElementById('contenedorMetodosPago'),
        contenedorDetalle: document.getElementById('contenedorDetallePago'),
        modoVentanilla: false,
        metodoInicial: 'TARJETA'
    });

    document.getElementById('checkMismoPasajero')?.addEventListener('change', (e) => {
        if (e.target.checked) autocompletarComprador(pasajeros[0]);
    });

    document.getElementById('checkFactura')?.addEventListener('change', (e) => {
        document.getElementById('seccionRuc')?.classList.toggle('d-none', !e.target.checked);
    });

    document.getElementById('btnAplicarPromo')?.addEventListener('click', validarCuponServidor);
    document.getElementById('btnFinalizarCompra')?.addEventListener('click', () =>
        procesarPagoEnServidor(idViaje, pasajeros, selectorPago.obtenerMetodo()));
});

function autocompletarComprador(p) {
    if (!p) return;
    document.getElementById('comp-nombre').value = p.nombre || '';
    document.getElementById('comp-apePaterno').value = p.apellido || '';
    document.getElementById('comp-telefono').value = p.telefono || '';
    document.getElementById('comp-correo').value = p.correo || '';
}

function calcularTotales(pasajeros) {
    subtotalGeneral = pasajeros.reduce((sum, p) => sum + (Number(p.precio) || 0), 0);
    document.getElementById('desglosePasajerosSide').innerHTML = pasajeros.map(p => `
        <div class="d-flex justify-content-between text-muted small mb-2">
            <span>${escaparHtml(p.nombre)} ${escaparHtml(p.apellido)} (Asiento ${p.numeroAsiento})</span>
            <span class="fw-bold text-dark">${moneda(p.precio)}</span>
        </div>`).join('');

    document.getElementById('resumenNombresPasajeros').textContent =
        pasajeros.map(p => `${p.nombre} ${p.apellido} (Asiento ${p.numeroAsiento})`).join(', ');

    actualizarTotalEnPantalla();
}

function actualizarTotalEnPantalla() {
    const totalPagar = Math.max(0, subtotalGeneral - descuentoAplicado);
    document.getElementById('totalLateralPagar').textContent = moneda(totalPagar);
    document.getElementById('btnMontoPagar').textContent = moneda(totalPagar);
}

async function validarCuponServidor() {
    const input = document.getElementById('inputCodigoPromo');
    const mensaje = document.getElementById('mensajePromo');
    const filaDescuento = document.getElementById('filaDescuento');
    const montoDescuentoEl = document.getElementById('montoDescuento');

    const codigo = (input?.value || '').trim();
    if (!codigo) return;

    try {
        const data = await apiGet(`/catalogo/cupones/validar?codigo=${encodeURIComponent(codigo)}`);
        descuentoAplicado = Number(data.montoDescuento) || 0;
        codigoCuponValidado = data.codigo;
        mensaje.className = 'small mt-2 text-success fw-bold';
        mensaje.textContent = `¡Cupón ${data.codigo} verificado en servidor! (- ${moneda(descuentoAplicado)})`;
        filaDescuento.classList.remove('d-none');
        montoDescuentoEl.textContent = `- ${moneda(descuentoAplicado)}`;
    } catch (err) {
        descuentoAplicado = 0;
        codigoCuponValidado = null;
        mensaje.className = 'small mt-2 text-danger fw-bold';
        mensaje.textContent = err.message;
        filaDescuento.classList.add('d-none');
    }
    actualizarTotalEnPantalla();
}

function iniciarTemporizadorSincronizado() {
    const expiracion = Number(sessionStorage.getItem('limiteTiempoCompra')) || (Date.now() + 15 * 60 * 1000);
    const reloj = document.getElementById('relojTemporizador');

    function tick() {
        const restante = expiracion - Date.now();
        if (restante <= 0) {
            clearInterval(cronometroInterval);
            if (reloj) reloj.textContent = '00:00';
            new bootstrap.Modal(document.getElementById('modalExpirado')).show();
            return;
        }
        const m = Math.floor(restante / 60000);
        const s = Math.floor((restante % 60000) / 1000);
        if (reloj) reloj.textContent = `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
    }

    tick();
    cronometroInterval = setInterval(tick, 1000);
}

async function procesarPagoEnServidor(idViaje, pasajeros, metodoPago) {
    if (!document.getElementById('checkTerminos')?.checked) {
        alert('Debes aceptar los términos y condiciones para finalizar tu compra.');
        return;
    }

    const esFactura = document.getElementById('checkFactura')?.checked || false;
    const rucFactura = document.getElementById('facturaRuc')?.value.trim() || '';
    const razonSocialFactura = document.getElementById('facturaRazonSocial')?.value.trim() || '';

    if (esFactura && (!/^\d{11}$/.test(rucFactura) || !razonSocialFactura)) {
        alert('Para emitir Factura Electrónica ingresa un RUC válido de 11 dígitos y la Razón Social.');
        return;
    }

    const nombreComp = [
        document.getElementById('comp-nombre')?.value.trim(),
        document.getElementById('comp-apePaterno')?.value.trim(),
        document.getElementById('comp-apeMaterno')?.value.trim()
    ].filter(Boolean).join(' ');

    const btn = document.getElementById('btnFinalizarCompra');
    btn.disabled = true;
    btn.innerHTML = `<span class="spinner-border spinner-border-sm"></span> Guardando compra en PostgreSQL...`;

    try {
        const ventaGuardada = await apiPost('/cliente/compras', {
            idReservaExistente: Number(sessionStorage.getItem('reservaActivaId')) || null,
            idViaje,
            tipoComprobante: esFactura ? 'FACTURA' : 'BOLETA',
            rucFactura: esFactura ? rucFactura : null,
            razonSocialFactura: esFactura ? razonSocialFactura : null,
            compradorNombre: esFactura ? razonSocialFactura : nombreComp,
            compradorTipoDoc: esFactura ? 'RUC' : (pasajeros[0]?.tipoDocumento || 'DNI'),
            compradorDocumento: esFactura ? rucFactura : (pasajeros[0]?.numeroDocumento || ''),
            compradorCorreo: document.getElementById('comp-correo')?.value.trim() || '',
            compradorTelefono: document.getElementById('comp-telefono')?.value.trim() || '',
            metodoPago,
            codigoCupon: codigoCuponValidado,
            pasajeros: pasajeros.map(p => ({
                numeroAsiento: p.numeroAsiento,
                tipoDocumento: p.tipoDocumento,
                numeroDocumento: p.numeroDocumento,
                nombre: p.nombre,
                apellido: p.apellido,
                correo: p.correo,
                telefono: p.telefono
            }))
        });

        ultimaVentaGuardada = ventaGuardada;
        localStorage.setItem('ultimaVentaUrbanRide', JSON.stringify(ventaGuardada));

        sessionStorage.removeItem('asientosComprando');
        sessionStorage.removeItem('limiteTiempoCompra');
        sessionStorage.removeItem('datosPasajerosComprando');
        sessionStorage.removeItem('reservaActivaId');

        document.getElementById('voucherResumen').innerHTML = `
            <div class="mb-1"><strong>Código de Reserva:</strong> <span class="badge bg-dark">${escaparHtml(ventaGuardada.codigoReserva)}</span></div>
            <div class="mb-1"><strong>Serie / Comprobante:</strong> ${escaparHtml(ventaGuardada.serieNumero)} (${escaparHtml(ventaGuardada.tipoComprobante)})</div>
            <div class="mb-1"><strong>Ruta:</strong> ${escaparHtml(ventaGuardada.origenCiudad)} → ${escaparHtml(ventaGuardada.destinoCiudad)} (${escaparHtml(ventaGuardada.fechaHoraSalida)})</div>
            <div class="mb-1"><strong>Método de pago:</strong> ${escaparHtml(ventaGuardada.metodoPago)}</div>
            <div class="mb-1"><strong>Boletos con QR:</strong> ${ventaGuardada.boletos.length} emitidos</div>
            <div class="mb-1"><strong>Total Pagado:</strong> <span class="fw-bold text-primary">${moneda(ventaGuardada.montoTotal)}</span></div>
        `;

        clearInterval(cronometroInterval);
        new bootstrap.Modal(document.getElementById('modalExito')).show();
    } catch (err) {
        alert(err.message || 'No se pudo procesar la compra.');
        btn.disabled = false;
        const totalPagar = Math.max(0, subtotalGeneral - descuentoAplicado);
        btn.innerHTML = `Finalizar compra por <span id="btnMontoPagar">${moneda(totalPagar)}</span> <i class="bi bi-chevron-double-right"></i>`;
    }
}
