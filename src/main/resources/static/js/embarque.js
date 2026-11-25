import { apiGet, apiPost, apiPatch } from './core/api.js';
import { exigirRoles, limpiarSesion } from './core/session.js';
import { escaparHtml, badgeEstadoViaje } from './core/formatters.js';

let viajesOperativos = [];
let idViajeSeleccionado = null;

document.addEventListener('DOMContentLoaded', async () => {
    const sesion = exigirRoles(['EMBARQUE', 'ADMIN']);
    if (!sesion) return;

    document.getElementById('usuarioHeaderEmbarque').innerHTML =
        `<i class="bi bi-person-check text-warning"></i> ${escaparHtml(sesion.nombreCompleto || sesion.nombre)} (${escaparHtml(sesion.rol)})`;

    if (sesion.rol === 'ADMIN') {
        document.getElementById('badgeCargoEmbarque').textContent = 'ADMIN · MODO EMBARQUE';
        document.getElementById('linkVolverAdminEmb')?.classList.remove('d-none');
    }

    document.getElementById('selectViajeEmbarque')?.addEventListener('change', (e) => {
        idViajeSeleccionado = Number(e.target.value) || null;
        cargarManifiestoActual();
    });

    document.getElementById('btnRefrescarEmbarque')?.addEventListener('click', cargarViajesOperativos);

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
                #${v.idViaje} · ${escaparHtml(v.origen)} → ${escaparHtml(v.destino)} (${escaparHtml(v.fechaHoraFormateada)}) · [${escaparHtml(v.estado)}] · ${v.abordados}/${v.vendidos} a bordo
            </option>`).join('');

        if (previo && viajesOperativos.some(v => v.idViaje === previo)) {
            select.value = String(previo);
            idViajeSeleccionado = previo;
        } else {
            idViajeSeleccionado = viajesOperativos[0].idViaje;
        }

        await cargarManifiestoActual();
    } catch (err) {
        select.innerHTML = `<option value="">Error: ${escaparHtml(err.message)}</option>`;
    }
}

async function cargarManifiestoActual() {
    if (!idViajeSeleccionado) return;
    const boxDetalle = document.getElementById('detalleUnidadEmbarque');
    const tbody = document.getElementById('tablaManifiesto');

    try {
        const man = await apiGet(`/embarque/viajes/${idViajeSeleccionado}/manifiesto`);
        const porcentaje = man.totalPasajeros > 0
            ? Math.round((man.totalAbordados / man.totalPasajeros) * 100) : 0;

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
            `Viaje #${man.idViaje} · Bus ${man.placaBus} · ${man.pendientesAbordar} pendientes de subir`;

        const pasajeros = man.pasajeros || [];
        if (!pasajeros.length) {
            tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted py-4">Aún no hay boletos vendidos en esta salida.</td></tr>';
            return;
        }

        tbody.innerHTML = pasajeros.map(p => {
            const abordado = p.estadoAbordaje === 'ABORDADO';
            return `
                <tr>
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
                               <div class="text-muted" style="font-size:0.7rem;">${escaparHtml(p.fechaAbordaje || '')}</div>`
                            : '<span class="badge bg-warning text-dark">PENDIENTE</span>'}
                    </td>
                    <td class="text-center">
                        ${abordado
                            ? '<span class="text-success small fw-bold">Verificado</span>'
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
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="7" class="text-center text-danger py-4">${escaparHtml(err.message)}</td></tr>`;
    }
}

async function ejecutarValidacion(payload) {
    const box = document.getElementById('resultadoValidacionBox');
    try {
        const res = await apiPost('/embarque/abordaje/validar', payload);
        const b = res.boleto;

        box.className = 'p-3 rounded-4 border border-success bg-success bg-opacity-10 text-start';
        box.innerHTML = `
            <div class="d-flex align-items-center gap-3">
                <i class="bi bi-patch-check-fill text-success" style="font-size: 2.6rem;"></i>
                <div>
                    <div class="fw-bold text-success fs-6">PASE VERIFICADO — ABORDAJE REGISTRADO</div>
                    <div class="fw-bold text-dark">${escaparHtml(b.pasajeroNombreCompleto)} (${escaparHtml(b.pasajeroTipoDoc)}: ${escaparHtml(b.pasajeroDocumento)})</div>
                    <div class="small text-muted">Boleto: <code>${escaparHtml(b.codigoBoleto)}</code> · <strong>Asiento ${b.numeroAsiento} (Piso ${b.piso})</strong> · Bus ${escaparHtml(res.placaBus)}</div>
                </div>
            </div>`;

        document.getElementById('inputCodigoQr').value = '';
        document.getElementById('inputDniAbordaje').value = '';
        await cargarViajesOperativos();
    } catch (err) {
        box.className = 'p-3 rounded-4 border border-danger bg-danger bg-opacity-10 text-start';
        box.innerHTML = `
            <div class="d-flex align-items-center gap-3">
                <i class="bi bi-shield-exclamation text-danger" style="font-size: 2.4rem;"></i>
                <div>
                    <div class="fw-bold text-danger">ABORDAJE RECHAZADO</div>
                    <div class="small text-dark fw-semibold">${escaparHtml(err.message)}</div>
                </div>
            </div>`;
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
