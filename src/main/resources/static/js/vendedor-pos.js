import { apiGet, apiPost } from './core/api.js';
import { exigirRoles, limpiarSesion, abrirComprobante } from './core/session.js';
import { escaparHtml, moneda, formatearFechaISO, badgeEstadoVenta } from './core/formatters.js';
import { crearControladorMapaBus } from './components/bus-map.js';
import { inicializarSelectorPago } from './components/payment-selector.js';

let viajesDisponibles = [];
let infoMapaActual = null;
let descuentoCupon = 0;
let codigoCuponActivo = null;
let subtotalActual = 0;
let misVentasCache = [];

document.addEventListener('DOMContentLoaded', async () => {
    const sesion = exigirRoles(['VENDEDOR', 'ADMIN']);
    if (!sesion) return;

    document.getElementById('usuarioHeaderPos').innerHTML =
        `<i class="bi bi-person-badge text-warning"></i> ${escaparHtml(sesion.nombreCompleto || sesion.nombre)} (${escaparHtml(sesion.rol)})`;

    if (sesion.rol === 'ADMIN') {
        document.getElementById('badgeCargoPos').textContent = 'ADMIN · MODO POS';
        document.getElementById('linkVolverAdminPos')?.classList.remove('d-none');
    }

    const selectorPago = inicializarSelectorPago({
        contenedorOpciones: document.getElementById('posOpcionesPago'),
        contenedorDetalle: document.getElementById('posDetallePago'),
        modoVentanilla: true,
        metodoInicial: 'EFECTIVO',
        obtenerMontoActual: () => Math.max(0, subtotalActual - descuentoCupon)
    });

    const mapaBus = crearControladorMapaBus({
        elPiso1: document.getElementById('posDeckPiso1'),
        elPiso2: document.getElementById('posDeckPiso2'),
        limiteAsientos: 5,
        onSeleccionCambio: (seleccionados) => {
            renderizarFormulariosPasajerosPos(seleccionados);
            recalcularTotalesPos(seleccionados, selectorPago);
        }
    });

    document.querySelectorAll('#filtrosPosTarifa .filter-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('#filtrosPosTarifa .filter-btn').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            mapaBus.filtrarPorNivel(btn.dataset.nivel);
        });
    });

    document.getElementById('posCheckFactura')?.addEventListener('change', (e) => {
        document.getElementById('posSeccionFactura')?.classList.toggle('d-none', !e.target.checked);
    });

    document.getElementById('posBtnValidarCupon')?.addEventListener('click', async () => {
        const codigo = document.getElementById('posInputCupon').value.trim();
        const msg = document.getElementById('posMensajeCupon');
        if (!codigo) return;
        try {
            const c = await apiGet(`/catalogo/cupones/validar?codigo=${encodeURIComponent(codigo)}`);
            descuentoCupon = Number(c.montoDescuento) || 0;
            codigoCuponActivo = c.codigo;
            msg.className = 'small mt-1 text-success fw-bold';
            msg.textContent = `Descuento aplicado: - ${moneda(descuentoCupon)}`;
        } catch (err) {
            descuentoCupon = 0;
            codigoCuponActivo = null;
            msg.className = 'small mt-1 text-danger fw-bold';
            msg.textContent = err.message;
        }
        recalcularTotalesPos(mapaBus.obtenerSeleccionados(), selectorPago);
    });

    document.getElementById('selectViajePos')?.addEventListener('change', (e) => {
        cargarMapaDelViajePos(Number(e.target.value), mapaBus);
    });

    document.getElementById('btnRefrescarViajesPos')?.addEventListener('click', async () => {
        await cargarListaViajesPos(mapaBus);
    });

    document.getElementById('btnRecargarHistorialPos')?.addEventListener('click', async () => {
        await Promise.all([cargarArqueoPos(), cargarHistorialPos()]);
    });

    document.getElementById('btnLogoutPos')?.addEventListener('click', async () => {
        try { await apiPost('/auth/logout'); } catch {}
        limpiarSesion();
        window.location.href = 'index.html';
    });

    document.getElementById('formPosVenta')?.addEventListener('submit', (e) =>
        emitirVentaMostrador(e, mapaBus, selectorPago));

    await Promise.all([
        cargarListaViajesPos(mapaBus),
        cargarArqueoPos(),
        cargarHistorialPos()
    ]);
});

async function cargarListaViajesPos(mapaBus) {
    const select = document.getElementById('selectViajePos');
    const valorPrevio = select.value;

    try {
        viajesDisponibles = await apiGet('/catalogo/viajes?incluirHoyCompleto=true');
        if (!viajesDisponibles.length) {
            select.innerHTML = '<option value="">No hay viajes programados activos</option>';
            return;
        }

        select.innerHTML = viajesDisponibles.map(v => {
            const orig = v.ruta?.origen?.ciudad || '-';
            const dest = v.ruta?.destino?.ciudad || '-';
            const fecha = formatearFechaISO(v.fechaHoraSalida);
            const hora = v.fechaHoraSalida ? v.fechaHoraSalida.split('T')[1]?.slice(0, 5) : '--:--';
            const placa = v.bus?.placa || 'S/P';
            return `<option value="${v.idViaje}">${escaparHtml(orig)} → ${escaparHtml(dest)} | ${fecha} ${hora} | Bus ${escaparHtml(placa)} (${v.asientosDisponibles} libres) · Desde ${moneda(v.precio)}</option>`;
        }).join('');

        if (valorPrevio && viajesDisponibles.some(v => String(v.idViaje) === valorPrevio)) {
            select.value = valorPrevio;
        }

        await cargarMapaDelViajePos(Number(select.value), mapaBus);
    } catch (err) {
        select.innerHTML = `<option value="">Error: ${escaparHtml(err.message)}</option>`;
    }
}

async function cargarMapaDelViajePos(idViaje, mapaBus) {
    if (!idViaje) return;
    try {
        infoMapaActual = await apiGet(`/catalogo/viajes/${idViaje}/asientos`);

        document.getElementById('resumenViajePosBox').innerHTML = `
            <div class="fw-bold text-primary">${escaparHtml(infoMapaActual.origenCiudad)} → ${escaparHtml(infoMapaActual.destinoCiudad)}</div>
            <div class="text-muted">Piso 2: <strong>${moneda(infoMapaActual.tarifaPiso2)}</strong> · VIP: <strong>${moneda(infoMapaActual.tarifaPiso1Vip)}</strong> · 180°: <strong>${moneda(infoMapaActual.tarifaPiso1Panoramico)}</strong></div>
        `;

        document.getElementById('posBtnPiso2').textContent = moneda(infoMapaActual.tarifaPiso2);
        document.getElementById('posBtnVip').textContent = moneda(infoMapaActual.tarifaPiso1Vip);
        document.getElementById('posBtnPanoramico').textContent = moneda(infoMapaActual.tarifaPiso1Panoramico);

        const ocupados = [
            ...(infoMapaActual.asientosOcupados || []),
            ...(infoMapaActual.asientosReservadosPorMi || [])
        ];

        mapaBus.cargarDatosMapa({
            ocupados,
            tarifas: {
                piso2: infoMapaActual.tarifaPiso2,
                vip: infoMapaActual.tarifaPiso1Vip,
                panoramico: infoMapaActual.tarifaPiso1Panoramico
            }
        });
    } catch (err) {
        mostrarMensajePos(err.message, 'danger');
    }
}

function renderizarFormulariosPasajerosPos(seleccionados) {
    const contenedor = document.getElementById('posContenedorPasajeros');
    document.getElementById('posContadorAsientos').textContent = `${seleccionados.length} / 5`;

    // Conservar valores ya escritos si se agrega otro asiento
    const valoresPrevios = {};
    contenedor.querySelectorAll('[data-asiento-form]').forEach(bloque => {
        const n = bloque.dataset.asientoForm;
        valoresPrevios[n] = {
            tipoDoc: bloque.querySelector(`[name="pos_tipo_${n}"]`)?.value || 'DNI',
            numDoc: bloque.querySelector(`[name="pos_doc_${n}"]`)?.value || '',
            nombre: bloque.querySelector(`[name="pos_nom_${n}"]`)?.value || '',
            apellido: bloque.querySelector(`[name="pos_ape_${n}"]`)?.value || '',
            telefono: bloque.querySelector(`[name="pos_tel_${n}"]`)?.value || ''
        };
    });

    if (seleccionados.length === 0) {
        contenedor.innerHTML = `
            <div class="p-3 bg-light rounded-3 border text-muted small text-center">
                Marca uno o más asientos en el bus para ingresar el DNI y nombre de cada pasajero.
            </div>`;
        return;
    }

    contenedor.innerHTML = seleccionados.map(a => {
        const prev = valoresPrevios[a.num] || { tipoDoc: 'DNI', numDoc: '', nombre: '', apellido: '', telefono: '' };
        return `
            <div class="border rounded-3 p-2 mb-2 bg-light" data-asiento-form="${a.num}">
                <div class="d-flex justify-content-between align-items-center mb-1">
                    <span class="badge bg-dark">Asiento ${a.num} (Piso ${a.piso})</span>
                    <span class="small fw-bold text-primary">${moneda(a.precio)}</span>
                </div>
                <div class="row g-1">
                    <div class="col-4">
                        <select class="form-select form-select-sm" name="pos_tipo_${a.num}">
                            <option value="DNI" ${prev.tipoDoc === 'DNI' ? 'selected' : ''}>DNI</option>
                            <option value="CE" ${prev.tipoDoc === 'CE' ? 'selected' : ''}>CE</option>
                            <option value="PASAPORTE" ${prev.tipoDoc === 'PASAPORTE' ? 'selected' : ''}>Pasaporte</option>
                        </select>
                    </div>
                    <div class="col-8">
                        <input type="text" class="form-control form-control-sm" name="pos_doc_${a.num}" value="${escaparHtml(prev.numDoc)}" placeholder="N° Documento *" required>
                    </div>
                    <div class="col-6">
                        <input type="text" class="form-control form-control-sm" name="pos_nom_${a.num}" value="${escaparHtml(prev.nombre)}" placeholder="Nombres *" required>
                    </div>
                    <div class="col-6">
                        <input type="text" class="form-control form-control-sm" name="pos_ape_${a.num}" value="${escaparHtml(prev.apellido)}" placeholder="Apellidos *" required>
                    </div>
                    <div class="col-12">
                        <input type="tel" class="form-control form-control-sm" name="pos_tel_${a.num}" value="${escaparHtml(prev.telefono)}" placeholder="Teléfono / WhatsApp (opcional)">
                    </div>
                </div>
            </div>`;
    }).join('');
}

function recalcularTotalesPos(seleccionados, selectorPago) {
    subtotalActual = seleccionados.reduce((sum, a) => sum + a.precio, 0);
    const total = Math.max(0, subtotalActual - descuentoCupon);

    document.getElementById('posTextoTotalBoton').textContent = moneda(total);
    document.getElementById('btnEmitirPos').disabled = seleccionados.length === 0;
    selectorPago.refrescarMonto();
}

async function emitirVentaMostrador(e, mapaBus, selectorPago) {
    e.preventDefault();
    mostrarMensajePos('', 'muted');

    const idViaje = Number(document.getElementById('selectViajePos').value);
    const seleccionados = mapaBus.obtenerSeleccionados();
    if (!idViaje || seleccionados.length === 0) {
        return mostrarMensajePos('Selecciona un viaje y al menos un asiento.', 'danger');
    }

    const pasajeros = [];
    for (const a of seleccionados) {
        const tipoDocumento = document.querySelector(`[name="pos_tipo_${a.num}"]`)?.value || 'DNI';
        const numeroDocumento = document.querySelector(`[name="pos_doc_${a.num}"]`)?.value.trim() || '';
        const nombre = document.querySelector(`[name="pos_nom_${a.num}"]`)?.value.trim() || '';
        const apellido = document.querySelector(`[name="pos_ape_${a.num}"]`)?.value.trim() || '';
        const telefono = document.querySelector(`[name="pos_tel_${a.num}"]`)?.value.trim() || '';

        if (!numeroDocumento || !nombre || !apellido) {
            return mostrarMensajePos(`Completa documento, nombres y apellidos del Asiento ${a.num}.`, 'danger');
        }
        pasajeros.push({ numeroAsiento: a.num, tipoDocumento, numeroDocumento, nombre, apellido, correo: null, telefono });
    }

    const esFactura = document.getElementById('posCheckFactura')?.checked || false;
    const rucFactura = document.getElementById('posRuc')?.value.trim() || '';
    const razonSocialFactura = document.getElementById('posRazonSocial')?.value.trim() || '';

    const btn = document.getElementById('btnEmitirPos');
    btn.disabled = true;

    try {
        const emitida = await apiPost('/vendedor/ventas', {
            idViaje,
            tipoComprobante: esFactura ? 'FACTURA' : 'BOLETA',
            rucFactura: esFactura ? rucFactura : null,
            razonSocialFactura: esFactura ? razonSocialFactura : null,
            compradorNombre: esFactura ? razonSocialFactura : `${pasajeros[0].nombre} ${pasajeros[0].apellido}`,
            compradorTipoDoc: esFactura ? 'RUC' : pasajeros[0].tipoDocumento,
            compradorDocumento: esFactura ? rucFactura : pasajeros[0].numeroDocumento,
            compradorTelefono: pasajeros[0].telefono,
            metodoPago: selectorPago.obtenerMetodo(),
            codigoCupon: codigoCuponActivo,
            pasajeros
        });

        descuentoCupon = 0;
        codigoCuponActivo = null;
        document.getElementById('posInputCupon').value = '';
        document.getElementById('posMensajeCupon').textContent = '';

        mostrarMensajePos(`¡Venta ${emitida.codigoReserva} emitida! Abriendo comprobante y pases QR...`, 'success');
        mapaBus.limpiarSeleccion();

        await Promise.all([
            cargarListaViajesPos(mapaBus),
            cargarArqueoPos(),
            cargarHistorialPos()
        ]);

        abrirComprobante(emitida);
    } catch (err) {
        mostrarMensajePos(err.message, 'danger');
        btn.disabled = false;
    }
}

async function cargarArqueoPos() {
    try {
        const arq = await apiGet('/vendedor/arqueo');
        document.getElementById('posStatOperaciones').textContent = arq.operacionesHoy ?? 0;
        document.getElementById('posStatBoletos').textContent = arq.boletosHoy ?? 0;
        document.getElementById('posStatRecaudado').textContent = moneda(arq.totalRecaudadoHoy);

        const medios = arq.desglosePorMetodo || {};
        document.getElementById('posDesgloseMedios').innerHTML = Object.entries(medios)
            .map(([m, val]) => `<span class="me-2"><strong>${escaparHtml(m)}:</strong> ${moneda(val)}</span>`)
            .join(' · ');
    } catch (err) {
        console.error('Error arqueo:', err);
    }
}

async function cargarHistorialPos() {
    const tbody = document.getElementById('tablaMisVentasPos');
    try {
        misVentasCache = await apiGet('/vendedor/mis-ventas');
        if (!misVentasCache.length) {
            tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted py-4">Aún no has emitido ventas en tu caja.</td></tr>';
            return;
        }

        tbody.innerHTML = misVentasCache.map(v => {
            const detalleAsientos = (v.boletos || [])
                .map(b => `<span class="badge bg-dark me-1">A${b.numeroAsiento}</span>${escaparHtml(b.pasajeroNombreCompleto)}`)
                .join('<br>');

            const puedeSolicitarAnulacion = v.estadoPago === 'PAGADO';

            return `
                <tr>
                    <td>
                        <div class="fw-bold font-monospace">${escaparHtml(v.codigoReserva)}</div>
                        <small class="text-muted">${escaparHtml(v.serieNumero)}</small>
                    </td>
                    <td>
                        <div class="fw-semibold">${escaparHtml(v.origenCiudad)} → ${escaparHtml(v.destinoCiudad)}</div>
                        <small class="text-muted">${escaparHtml(v.fechaHoraSalida)}</small>
                    </td>
                    <td class="small">${detalleAsientos}</td>
                    <td class="text-center">${escaparHtml(v.metodoPago)}</td>
                    <td class="text-center">
                        ${badgeEstadoVenta(v.estadoPago)}
                        ${v.motivoAnulacion ? `<div class="text-muted mt-1" style="font-size:0.72rem;">${escaparHtml(v.motivoAnulacion)}</div>` : ''}
                    </td>
                    <td class="text-end fw-bold">${moneda(v.montoTotal)}</td>
                    <td class="text-center">
                        <button type="button" class="btn btn-outline-primary btn-sm me-1" title="Reimprimir Boleta / QR" data-pos-boleta="${v.idVenta}">
                            <i class="bi bi-printer"></i>
                        </button>
                        ${puedeSolicitarAnulacion ? `
                            <button type="button" class="btn btn-outline-warning text-dark btn-sm" title="Solicitar anulación al Administrador" data-pos-anular="${v.idVenta}">
                                <i class="bi bi-exclamation-octagon"></i> Solicitar Anulación
                            </button>` : ''}
                    </td>
                </tr>`;
        }).join('');

        tbody.querySelectorAll('[data-pos-boleta]').forEach(btn => {
            btn.addEventListener('click', () => {
                const v = misVentasCache.find(x => String(x.idVenta) === btn.dataset.posBoleta);
                if (v) abrirComprobante(v);
            });
        });

        tbody.querySelectorAll('[data-pos-anular]').forEach(btn => {
            btn.addEventListener('click', () => solicitarAnulacionMostrador(Number(btn.dataset.posAnular)));
        });
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="7" class="text-center text-danger py-4">${escaparHtml(err.message)}</td></tr>`;
    }
}

async function solicitarAnulacionMostrador(idVenta) {
    const motivo = prompt('Indica el motivo para solicitar la anulación al Administrador General (ej. error de digitación de DNI o cambio de horario):');
    if (!motivo || !motivo.trim()) return;

    try {
        await apiPost(`/vendedor/ventas/${idVenta}/solicitar-anulacion`, { motivo: motivo.trim() });
        alert('Solicitud enviada. El Administrador General revisará y liberará los asientos tras aprobarla.');
        await Promise.all([cargarArqueoPos(), cargarHistorialPos()]);
    } catch (err) {
        alert(err.message);
    }
}

function mostrarMensajePos(texto, tipo = 'danger') {
    const el = document.getElementById('posMensajeVenta');
    if (!el) return;
    el.className = `small fw-bold mb-2 text-${tipo}`;
    el.textContent = texto;
}
