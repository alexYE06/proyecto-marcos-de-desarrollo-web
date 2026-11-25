import { apiGet, apiPost, apiPatch } from './core/api.js';
import { exigirRoles, limpiarSesion } from './core/session.js';
import { escaparHtml, badgeEstadoViaje } from './core/formatters.js';

let viajesOperativos = [];
let idViajeSeleccionado = null;
let manifiestoActual = null;

document.addEventListener('DOMContentLoaded', async () => {
    const sesion = exigirRoles(['EMBARQUE', 'ADMIN']);
    if (!sesion) return;

    document.getElementById('usuarioHeaderEmbarque').innerHTML =
        `<i class="bi bi-person-check text-warning"></i> ${escaparHtml(sesion.nombreCompleto || sesion.nombre)} (${escaparHtml(sesion.rol)})`;

    if (sesion.rol === 'ADMIN') {
        document.getElementById('badgeCargoEmbarque').textContent = 'ADMIN · MODO EMBARQUE';
        document.getElementById('linkVolverAdminEmb')?.classList.remove('d-none');
    }

    // Navegación real por pestañas en el sidebar (arregla el "click muerto" en Manifiesto del Bus)
    document.querySelectorAll('#menuEmbarqueSidebar [data-vista-embarque]').forEach(btn => {
        btn.addEventListener('click', () => activarVistaEmbarque(btn.dataset.vistaEmbarque));
    });

    document.getElementById('btnIrAManifiestoRapido')?.addEventListener('click', () => activarVistaEmbarque('manifiesto'));

    document.getElementById('selectViajeEmbarque')?.addEventListener('change', (e) => {
        idViajeSeleccionado = Number(e.target.value) || null;
        cargarManifiestoActual();
    });

    document.getElementById('btnRefrescarEmbarque')?.addEventListener('click', cargarViajesOperativos);
    document.getElementById('filtroBuscarManifiesto')?.addEventListener('input', renderizarTablaManifiesto);
    document.getElementById('filtroEstadoManifiesto')?.addEventListener('change', renderizarTablaManifiesto);
    document.getElementById('btnExportarManifiestoCsv')?.addEventListener('click', exportarManifiestoCSV);

    document.querySelectorAll('[data-cambiar-estado]').forEach(btn => {
        btn.addEventListener('click', () => actualizarEstadoViaje(btn.dataset.cambiarEstado));
    });

    document.getElementById('formValidarAbordaje')?.addEventListener('submit', async (e) => {
        e.preventDefault();
        const codigoEntrada = document.getElementById('inputCodigoQr').value.trim();
        const documentoPasajero = document.getElementById('inputDniAbordaje').value.trim();
        await ejecutarValidacion({ idViaje: idViajeSeleccionado, codigoEntrada, documentoPasajero });
    });

    document.getElementById('btnLogoutEmbarque')?.addEventListener('click', async () => {
        try { await apiPost('/auth/logout'); } catch {}
        limpiarSesion();
        window.location.href = 'index.html';
    });

    await cargarViajesOperativos();
});

function activarVistaEmbarque(nombreVista) {
    document.querySelectorAll('#menuEmbarqueSidebar [data-vista-embarque]').forEach(btn => {
        btn.classList.toggle('active', btn.dataset.vistaEmbarque === nombreVista);
    });
    document.querySelectorAll('main [data-seccion-embarque]').forEach(sec => {
        sec.classList.toggle('d-none', sec.dataset.seccionEmbarque !== nombreVista);
    });
}

async function cargarViajesOperativos() {
    const select = document.getElementById('selectViajeEmbarque');
    const previo = idViajeSeleccionado;

    try {
        viajesOperativos = await apiGet('/embarque/viajes');
        if (!viajesOperativos.length) {
            select.innerHTML = '<option value="">Sin salidas programadas</option>';
            return;
        }

        select.innerHTML = viajesOperativos.map(v => `
            <option value="${v.idViaje}">
                #${v.idViaje} · ${escaparHtml(v.origen)} → ${escaparHtml(v.destino)} (${escaparHtml(v.fechaHoraFormateada)}) · Bus ${escaparHtml(v.placaBus)} · [${escaparHtml(v.estado)}] · ${v.abordados}/${v.vendidos} a bordo
            </option>`).join('');

        if (previo && viajesOperativos.some(v => v.idViaje === previo)) {
            select.value = String(previo);
            idViajeSeleccionado = previo;
        } else {
            // Priorizar la primera salida que tenga pasajeros vendidos para una experiencia inmediata
            const conPasajeros = viajesOperativos.find(v => (v.vendidos || 0) > 0) || viajesOperativos[0];
            idViajeSeleccionado = conPasajeros.idViaje;
            select.value = String(idViajeSeleccionado);
        }

        await cargarManifiestoActual();
    } catch (err) {
        select.innerHTML = `<option value="">Error: ${escaparHtml(err.message)}</option>`;
    }
}

async function cargarManifiestoActual() {
    if (!idViajeSeleccionado) return;
    const boxDetalle = document.getElementById('detalleUnidadEmbarque');

    try {
        const man = await apiGet(`/embarque/viajes/${idViajeSeleccionado}/manifiesto`);
        manifiestoActual = man;
        const porcentaje = man.totalPasajeros > 0
            ? Math.round((man.totalAbordados / man.totalPasajeros) * 100) : 0;

        document.getElementById('badgeTotalManifiesto').textContent = man.totalPasajeros;
        document.getElementById('manKpiTotal').textContent = man.totalPasajeros;
        document.getElementById('manKpiAbordados').textContent = man.totalAbordados;
        document.getElementById('manKpiPendientes').textContent = man.pendientesAbordar;
        document.getElementById('manKpiTripulacion').innerHTML =
            `Bus <strong>${escaparHtml(man.placaBus)}</strong><br>Cond.: ${escaparHtml(man.conductor)}`;

        boxDetalle.innerHTML = `
            <div class="d-flex justify-content-between align-items-center mb-2">
                <strong class="text-dark fs-6">${escaparHtml(man.origen)} → ${escaparHtml(man.destino)}</strong>
                ${badgeEstadoViaje(man.estado)}
            </div>
            <div class="mb-1"><i class="bi bi-calendar-event"></i> Salida: <strong>${escaparHtml(man.fechaHoraFormateada)}</strong> · Bus: <strong>${escaparHtml(man.placaBus)}</strong></div>
            <div class="mb-2"><i class="bi bi-person-vcard"></i> Conductor: <strong>${escaparHtml(man.conductor)}</strong> · Copiloto: ${escaparHtml(man.copiloto)}</div>
            <div class="d-flex justify-content-between small fw-bold mb-1">
                <span>Progreso de abordaje:</span>
                <span class="text-success">${man.totalAbordados} / ${man.totalPasajeros} pasajeros (${porcentaje}%)</span>
            </div>
            <div class="progress" style="height: 8px;">
                <div class="progress-bar bg-success" style="width: ${porcentaje}%;"></div>
            </div>`;

        document.getElementById('subtituloManifiesto').textContent =
            `Viaje #${man.idViaje} · ${man.origen} → ${man.destino} · Bus ${man.placaBus} · ${man.pendientesAbordar} pendientes de subir`;

        // Chips rápidos de códigos pendientes en el escáner
        const chipsBox = document.getElementById('chipsPendientesEscaneo');
        const pendientes = (man.pasajeros || []).filter(p => p.estadoAbordaje !== 'ABORDADO');
        if (!pendientes.length) {
            chipsBox.innerHTML = '<span class="text-muted fst-italic">No hay boletos pendientes en esta salida.</span>';
        } else {
            chipsBox.innerHTML = pendientes.map(p => `
                <button type="button" class="btn btn-sm btn-outline-primary rounded-pill py-0 px-2 font-monospace"
                        data-cargar-qr="${escaparHtml(p.codigoBoleto)}" title="${escaparHtml(p.pasajeroNombreCompleto)} - Asiento ${p.numeroAsiento}">
                    ${escaparHtml(p.codigoBoleto)} (A${p.numeroAsiento})
                </button>`).join('');
            chipsBox.querySelectorAll('[data-cargar-qr]').forEach(chip => {
                chip.addEventListener('click', () => {
                    document.getElementById('inputCodigoQr').value = chip.dataset.cargarQr;
                    document.getElementById('inputCodigoQr').focus();
                });
            });
        }

        renderizarTablaManifiesto();
    } catch (err) {
        document.getElementById('tablaManifiesto').innerHTML =
            `<tr><td colspan="7" class="text-center text-danger py-4">${escaparHtml(err.message)}</td></tr>`;
    }
}

function renderizarTablaManifiesto() {
    const tbody = document.getElementById('tablaManifiesto');
    if (!manifiestoActual) return;

    const texto = (document.getElementById('filtroBuscarManifiesto')?.value || '').trim().toLowerCase();
    const estadoFiltro = document.getElementById('filtroEstadoManifiesto')?.value || '';

    const pasajeros = (manifiestoActual.pasajeros || []).filter(p => {
        const matchEstado = !estadoFiltro || p.estadoAbordaje === estadoFiltro;
        const matchTexto = !texto
            || (p.pasajeroNombreCompleto || '').toLowerCase().includes(texto)
            || (p.pasajeroDocumento || '').toLowerCase().includes(texto)
            || (p.codigoBoleto || '').toLowerCase().includes(texto)
            || String(p.numeroAsiento) === texto;
        return matchEstado && matchTexto;
    });

    if (!pasajeros.length) {
        tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted py-4">No se encontraron pasajeros en este manifiesto con el filtro aplicado.</td></tr>';
        return;
    }

    tbody.innerHTML = pasajeros.map(p => {
        const abordado = p.estadoAbordaje === 'ABORDADO';
        return `
            <tr class="${abordado ? 'table-success bg-opacity-25' : ''}">
                <td class="text-center fw-bold"><span class="badge bg-dark fs-6">${p.numeroAsiento}</span></td>
                <td class="text-center">Piso ${p.piso}</td>
                <td>
                    <div class="fw-semibold">${escaparHtml(p.pasajeroNombreCompleto)}</div>
                    <small class="text-muted">${escaparHtml(p.categoriaAsiento)}</small>
                </td>
                <td class="text-center">${escaparHtml(p.pasajeroTipoDoc)} ${escaparHtml(p.pasajeroDocumento)}</td>
                <td class="text-center font-monospace fw-bold text-primary">${escaparHtml(p.codigoBoleto)}</td>
                <td class="text-center">
                    ${abordado
                        ? `<span class="badge bg-success"><i class="bi bi-check-lg"></i> ABORDADO</span>
                           <div class="text-muted" style="font-size:0.7rem;">${escaparHtml(p.fechaAbordaje || '')} · ${escaparHtml(p.inspectorNombre || '')}</div>`
                        : '<span class="badge bg-warning text-dark">PENDIENTE</span>'}
                </td>
                <td class="text-center">
                    ${abordado
                        ? '<span class="text-success small fw-bold"><i class="bi bi-patch-check-fill"></i> Verificado</span>'
                        : `<button type="button" class="btn btn-sm btn-success rounded-pill px-3 fw-bold" data-checkin-codigo="${escaparHtml(p.codigoBoleto)}">
                               <i class="bi bi-box-arrow-in-right"></i> Abordar
                           </button>`}
                </td>
            </tr>`;
    }).join('');

    tbody.querySelectorAll('[data-checkin-codigo]').forEach(btn => {
        btn.addEventListener('click', () => ejecutarValidacion({
            idViaje: idViajeSeleccionado,
            codigoEntrada: btn.dataset.checkinCodigo,
            documentoPasajero: ''
        }));
    });
}

function exportarManifiestoCSV() {
    if (!manifiestoActual || !manifiestoActual.pasajeros?.length) {
        alert('No hay pasajeros en el manifiesto para exportar.');
        return;
    }
    const headers = ['Asiento', 'Piso', 'Pasajero', 'TipoDoc', 'Documento', 'CodigoBoleto', 'EstadoAbordaje', 'HoraAbordaje'];
    const rows = manifiestoActual.pasajeros.map(p => [
        p.numeroAsiento,
        p.piso,
        `"${(p.pasajeroNombreCompleto || '').replace(/"/g, '""')}"`,
        p.pasajeroTipoDoc,
        p.pasajeroDocumento,
        p.codigoBoleto,
        p.estadoAbordaje,
        `"${p.fechaAbordaje || ''}"`
    ].join(','));

    const csv = '\uFEFF' + [headers.join(','), ...rows].join('\n');
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = `manifiesto_viaje_${manifiestoActual.idViaje}_${manifiestoActual.placaBus}.csv`;
    a.click();
}

async function ejecutarValidacion(payload) {
    const box = document.getElementById('resultadoValidacionBox');
    try {
        const res = await apiPost('/embarque/abordaje/validar', payload);
        const b = res.boleto;

        if (box) {
            box.className = 'p-3 rounded-4 border border-success bg-success bg-opacity-10 text-start mb-3';
            box.innerHTML = `
                <div class="d-flex align-items-center gap-3">
                    <i class="bi bi-patch-check-fill text-success" style="font-size: 2.6rem;"></i>
                    <div>
                        <div class="fw-bold text-success fs-6">PASE VERIFICADO — ABORDAJE REGISTRADO</div>
                        <div class="fw-bold text-dark">${escaparHtml(b.pasajeroNombreCompleto)} (${escaparHtml(b.pasajeroTipoDoc)}: ${escaparHtml(b.pasajeroDocumento)})</div>
                        <div class="small text-muted">Boleto: <code>${escaparHtml(b.codigoBoleto)}</code> · <strong>Asiento ${b.numeroAsiento} (Piso ${b.piso})</strong> · Bus ${escaparHtml(res.placaBus)}</div>
                    </div>
                </div>`;
        }

        const inpQr = document.getElementById('inputCodigoQr');
        const inpDni = document.getElementById('inputDniAbordaje');
        if (inpQr) inpQr.value = '';
        if (inpDni) inpDni.value = '';
        await cargarViajesOperativos();
    } catch (err) {
        if (box) {
            box.className = 'p-3 rounded-4 border border-danger bg-danger bg-opacity-10 text-start mb-3';
            box.innerHTML = `
                <div class="d-flex align-items-center gap-3">
                    <i class="bi bi-shield-exclamation text-danger" style="font-size: 2.4rem;"></i>
                    <div>
                        <div class="fw-bold text-danger">ABORDAJE RECHAZADO</div>
                        <div class="small text-dark fw-semibold">${escaparHtml(err.message)}</div>
                    </div>
                </div>`;
        } else {
            alert(err.message);
        }
    }
}

async function actualizarEstadoViaje(nuevoEstado) {
    if (!idViajeSeleccionado) return;
    try {
        await apiPatch(`/embarque/viajes/${idViajeSeleccionado}/estado`, { estado: nuevoEstado });
        await cargarViajesOperativos();
    } catch (err) {
        alert(err.message);
    }
}
