import { apiGet, apiPost, apiPut } from './core/api.js';
import { exigirRoles, limpiarSesion, abrirComprobante } from './core/session.js';
import { escaparHtml, moneda, badgeEstadoVenta, badgeEstadoViaje } from './core/formatters.js';

let ventasGlobalCache = [];
let viajesAdminCache = [];
let rutasAdminCache = [];
let busesAdminCache = [];
let empleadosAdminCache = [];
let terminalesAdminCache = [];
let usuariosAdminCache = [];

document.addEventListener('DOMContentLoaded', async () => {
    const sesion = exigirRoles(['ADMIN']);
    if (!sesion) return;

    document.getElementById('adminUsuarioHeader').innerHTML =
        `<i class="bi bi-shield-lock-fill text-warning"></i> ${escaparHtml(sesion.nombreCompleto || sesion.nombre)}`;

    configurarNavegacionSidebar();
    configurarFormulariosAdmin(sesion);

    document.getElementById('btnLogoutAdmin')?.addEventListener('click', async () => {
        try { await apiPost('/auth/logout'); } catch {}
        limpiarSesion();
        window.location.href = 'index.html';
    });

    await recargarTodoAdmin();
});

function configurarNavegacionSidebar() {
    document.querySelectorAll('#menuAdminSidebar [data-vista]').forEach(btn => {
        btn.addEventListener('click', () => activarVista(btn.dataset.vista));
    });
}

function activarVista(nombreVista) {
    document.querySelectorAll('#menuAdminSidebar [data-vista]').forEach(b => {
        b.classList.toggle('active', b.dataset.vista === nombreVista);
    });
    document.querySelectorAll('main [data-seccion]').forEach(sec => {
        sec.classList.toggle('d-none', sec.dataset.seccion !== nombreVista);
    });
}

async function recargarTodoAdmin() {
    await Promise.all([
        cargarDashboard(),
        cargarVentasAdmin(),
        cargarCatalogosOperativos(),
        cargarUsuariosYCupones()
    ]);
}

// ==================== 1. DASHBOARD Y CSV ====================

async function cargarDashboard() {
    try {
        const d = await apiGet('/admin/dashboard');
        document.getElementById('kpiIngresosTotales').textContent = moneda(d.ingresosTotales);
        document.getElementById('kpiSubCanales').textContent = `Web: ${moneda(d.ingresosWeb)} · Ventanilla: ${moneda(d.ingresosVentanilla)}`;
        document.getElementById('kpiBoletos').textContent = `${d.boletosVendidos} boletos`;
        document.getElementById('kpiOperaciones').textContent = `${d.ventasConfirmadas} ventas activas`;
        document.getElementById('kpiFlota').textContent = `${d.busesActivos} / ${d.totalBuses} buses`;
        document.getElementById('kpiViajesRutas').textContent = `${d.totalRutas} rutas · ${d.totalViajes} salidas`;
        document.getElementById('kpiPendientesAnular').textContent = d.solicitudesAnulacionPendientes;
        document.getElementById('kpiTotalAnuladas').textContent = `${d.ventasAnuladas} anuladas históricas`;

        const badgeSide = document.getElementById('badgePendientesSidebar');
        if (d.solicitudesAnulacionPendientes > 0) {
            badgeSide.textContent = d.solicitudesAnulacionPendientes;
            badgeSide.classList.remove('d-none');
        } else {
            badgeSide.classList.add('d-none');
        }

        renderizarMapaKpi('tablaKpiRutas', d.ingresosPorRuta, 'Sin ventas registradas');
        renderizarMapaKpi('tablaKpiMetodos', d.ingresosPorMetodo, 'Sin cobros registrados');
        renderizarMapaKpi('tablaKpiVendedores', d.rankingVendedores, 'Sin ventas en ventanilla aún');
    } catch (err) {
        console.error('Error dashboard:', err);
    }
}

function renderizarMapaKpi(containerId, objeto, vacioTexto) {
    const el = document.getElementById(containerId);
    const entradas = Object.entries(objeto || {});
    if (!entradas.length) {
        el.innerHTML = `<div class="text-muted py-3 text-center">${escaparHtml(vacioTexto)}</div>`;
        return;
    }
    el.innerHTML = entradas.map(([clave, valor]) => `
        <div class="d-flex justify-content-between align-items-center py-2 border-bottom">
            <span class="fw-semibold text-dark">${escaparHtml(clave)}</span>
            <span class="badge bg-primary-subtle text-primary fs-6">${moneda(valor)}</span>
        </div>`).join('');
}

function exportarVentasCSV() {
    if (!ventasGlobalCache.length) {
        alert('No hay ventas para exportar.');
        return;
    }
    const cabeceras = ['ID', 'Reserva', 'Serie', 'Canal', 'AtendidoPor', 'Origen', 'Destino', 'Salida', 'Estado', 'MetodoPago', 'TotalPEN'];
    const filas = ventasGlobalCache.map(v => [
        v.idVenta,
        v.codigoReserva,
        v.serieNumero,
        v.canalVenta,
        `"${(v.vendedorNombre || '').replace(/"/g, '""')}"`,
        v.origenCiudad,
        v.destinoCiudad,
        `"${v.fechaHoraSalida}"`,
        v.estadoPago,
        v.metodoPago,
        v.montoTotal
    ].join(','));

    const csv = '\uFEFF' + [cabeceras.join(','), ...filas].join('\n');
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = `reporte_ventas_urbanride_${new Date().toISOString().slice(0, 10)}.csv`;
    link.click();
}

// ==================== 2. AUDITORÍA DE VENTAS Y ANULACIONES ====================

async function cargarVentasAdmin() {
    const estado = document.getElementById('filtroEstadoVentasAdmin')?.value || '';
    const canal = document.getElementById('filtroCanalVentasAdmin')?.value || '';
    const tbody = document.getElementById('tablaAdminVentas');

    try {
        const params = new URLSearchParams();
        if (estado) params.set('estado', estado);
        if (canal) params.set('canal', canal);
        ventasGlobalCache = await apiGet(`/admin/ventas?${params.toString()}`);

        if (!ventasGlobalCache.length) {
            tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted py-4">No hay ventas con los filtros seleccionados.</td></tr>';
            return;
        }

        tbody.innerHTML = ventasGlobalCache.map(v => {
            const pasajerosHtml = (v.boletos || [])
                .map(b => `<span class="badge bg-dark me-1">A${b.numeroAsiento}</span>${escaparHtml(b.pasajeroNombreCompleto)}`)
                .join('<br>');

            const esSolicitud = v.estadoPago === 'ANULACION_SOLICITADA';
            const esPagado = v.estadoPago === 'PAGADO';

            return `
                <tr class="${esSolicitud ? 'table-warning' : ''}">
                    <td>
                        <div class="fw-bold font-monospace">${escaparHtml(v.codigoReserva)}</div>
                        <small class="text-muted">${escaparHtml(v.serieNumero)} · ${escaparHtml(v.fechaVenta)}</small>
                    </td>
                    <td>
                        <span class="badge ${v.canalVenta === 'WEB' ? 'bg-info text-dark' : 'bg-secondary'}">${escaparHtml(v.canalVenta)}</span>
                        <div class="small text-muted mt-1">${escaparHtml(v.vendedorNombre)}</div>
                    </td>
                    <td>
                        <div class="fw-semibold">${escaparHtml(v.origenCiudad)} → ${escaparHtml(v.destinoCiudad)}</div>
                        <small class="text-muted">${escaparHtml(v.fechaHoraSalida)}</small>
                    </td>
                    <td class="small">${pasajerosHtml}</td>
                    <td class="text-center">
                        ${badgeEstadoVenta(v.estadoPago)}
                        ${v.motivoAnulacion ? `<div class="small text-muted mt-1">${escaparHtml(v.motivoAnulacion)}</div>` : ''}
                        ${v.solicitanteAnulacionNombre ? `<div class="small text-dark fw-semibold">Solicita: ${escaparHtml(v.solicitanteAnulacionNombre)}</div>` : ''}
                    </td>
                    <td class="text-end fw-bold">${moneda(v.montoTotal)}</td>
                    <td class="text-center">
                        <div class="d-flex flex-wrap justify-content-center gap-1">
                            <button type="button" class="btn btn-outline-primary btn-sm" title="Ver Boleta / QR" data-adm-boleta="${v.idVenta}">
                                <i class="bi bi-file-earmark-pdf"></i>
                            </button>
                            ${esSolicitud ? `
                                <button type="button" class="btn btn-danger btn-sm fw-bold" data-adm-aprobar-anulacion="${v.idVenta}">
                                    Aprobar Anulación
                                </button>
                                <button type="button" class="btn btn-outline-secondary btn-sm" data-adm-rechazar-anulacion="${v.idVenta}">
                                    Rechazar
                                </button>` : ''}
                            ${esPagado ? `
                                <button type="button" class="btn btn-outline-danger btn-sm" data-adm-anular-directo="${v.idVenta}">
                                    Anular
                                </button>` : ''}
                        </div>
                    </td>
                </tr>`;
        }).join('');

        tbody.querySelectorAll('[data-adm-boleta]').forEach(btn => {
            btn.addEventListener('click', () => {
                const v = ventasGlobalCache.find(x => String(x.idVenta) === btn.dataset.admBoleta);
                if (v) abrirComprobante(v);
            });
        });

        tbody.querySelectorAll('[data-adm-aprobar-anulacion]').forEach(btn => {
            btn.addEventListener('click', async () => {
                if (!confirm('¿Aprobar la anulación y liberar inmediatamente los asientos de esta venta?')) return;
                await apiPost(`/admin/ventas/${btn.dataset.admAprobarAnulacion}/anular`, {});
                await Promise.all([cargarDashboard(), cargarVentasAdmin(), cargarCatalogosOperativos()]);
            });
        });

        tbody.querySelectorAll('[data-adm-rechazar-anulacion]').forEach(btn => {
            btn.addEventListener('click', async () => {
                const obs = prompt('Motivo de rechazo de la solicitud (opcional):') || 'Rechazada por Administración';
                await apiPost(`/admin/ventas/${btn.dataset.admRechazarAnulacion}/rechazar-anulacion`, { motivo: obs });
                await Promise.all([cargarDashboard(), cargarVentasAdmin()]);
            });
        });

        tbody.querySelectorAll('[data-adm-anular-directo]').forEach(btn => {
            btn.addEventListener('click', async () => {
                const motivo = prompt('Ingresa el motivo de auditoría para anular esta venta y liberar sus asientos:');
                if (!motivo || !motivo.trim()) return;
                await apiPost(`/admin/ventas/${btn.dataset.admAnularDirecto}/anular`, { motivo: motivo.trim() });
                await Promise.all([cargarDashboard(), cargarVentasAdmin(), cargarCatalogosOperativos()]);
            });
        });
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="7" class="text-danger text-center py-4">${escaparHtml(err.message)}</td></tr>`;
    }
}

// ==================== 3, 4 y 5. OPERACIONES, FLOTA Y TRIPULACIÓN ====================

async function cargarCatalogosOperativos() {
    const [terminales, rutas, buses, empleados, viajes] = await Promise.all([
        apiGet('/admin/terminales'),
        apiGet('/admin/rutas'),
        apiGet('/admin/buses'),
        apiGet('/admin/empleados'),
        apiGet('/admin/viajes')
    ]);

    terminalesAdminCache = terminales;
    rutasAdminCache = rutas;
    busesAdminCache = buses;
    empleadosAdminCache = empleados;
    viajesAdminCache = viajes;

    // Poblar tabla y selects de Terminales
    document.getElementById('tablaAdminTerminales').innerHTML = terminales.map(t => `
        <tr>
            <td class="fw-bold">${escaparHtml(t.ciudad)}</td>
            <td>${escaparHtml(t.nombre)}<div class="text-muted" style="font-size:0.72rem;">${escaparHtml(t.direccion || '')}</div></td>
            <td><span class="badge ${t.activo ? 'bg-success' : 'bg-secondary'}">${t.activo ? 'Activo' : 'Inactivo'}</span></td>
        </tr>`).join('');

    const optsTerm = terminales.map(t => `<option value="${t.idTerminal}">${escaparHtml(t.ciudad)} — ${escaparHtml(t.nombre)}</option>`).join('');
    document.getElementById('rutaOrigen').innerHTML = optsTerm;
    document.getElementById('rutaDestino').innerHTML = optsTerm;

    // Poblar Rutas
    document.getElementById('tablaAdminRutas').innerHTML = rutas.map(r => `
        <tr>
            <td>#${r.idRuta}</td>
            <td class="fw-semibold">${escaparHtml(r.origen?.ciudad)} → ${escaparHtml(r.destino?.ciudad)}</td>
            <td class="text-end fw-bold">${moneda(r.precioBase)}</td>
            <td class="text-center">${r.horasEstimadas} hrs</td>
        </tr>`).join('');

    document.getElementById('viajeRuta').innerHTML = rutas.map(r =>
        `<option value="${r.idRuta}" data-precio="${r.precioBase}">${escaparHtml(r.origen?.ciudad)} → ${escaparHtml(r.destino?.ciudad)} (Base ${moneda(r.precioBase)})</option>`
    ).join('');

    // Poblar Buses
    document.getElementById('tablaAdminBuses').innerHTML = buses.map(b => `
        <tr>
            <td class="fw-bold font-monospace">${escaparHtml(b.placa)}</td>
            <td>${escaparHtml(b.marca)} ${escaparHtml(b.modelo)} <small class="text-muted">(${b.capacidadAsientos} as.)</small></td>
            <td><span class="badge ${b.estado === 'Activo' ? 'bg-success' : 'bg-warning text-dark'}">${escaparHtml(b.estado)}</span></td>
            <td class="text-center">
                <button type="button" class="btn btn-outline-dark btn-sm" data-toggle-bus="${b.idBus}">
                    ${b.estado === 'Activo' ? 'A Mantenimiento' : 'Activar'}
                </button>
            </td>
        </tr>`).join('');

    document.querySelectorAll('[data-toggle-bus]').forEach(btn => {
        btn.addEventListener('click', async () => {
            const b = busesAdminCache.find(x => String(x.idBus) === btn.dataset.toggleBus);
            if (!b) return;
            const nuevoEstado = b.estado === 'Activo' ? 'Mantenimiento' : 'Activo';
            await apiPut(`/admin/buses/${b.idBus}`, { ...b, estado: nuevoEstado });
            await Promise.all([cargarDashboard(), cargarCatalogosOperativos()]);
        });
    });

    document.getElementById('viajeBus').innerHTML = buses
        .filter(b => b.estado === 'Activo')
        .map(b => `<option value="${b.idBus}">${escaparHtml(b.placa)} · ${escaparHtml(b.modelo)}</option>`)
        .join('');

    // Poblar Empleados
    document.getElementById('tablaAdminEmpleados').innerHTML = empleados.map(e => `
        <tr>
            <td class="font-monospace">${escaparHtml(e.dniEmpleado)}</td>
            <td class="fw-semibold">${escaparHtml(e.nombre)} ${escaparHtml(e.apellido)}</td>
            <td><span class="badge bg-light text-dark border">${escaparHtml(e.cargo)}</span> <small class="text-muted">${escaparHtml(e.licencia || '')}</small></td>
            <td class="text-end fw-bold">${moneda(e.sueldo)}</td>
        </tr>`).join('');

    const conductoresOpts = '<option value="">Sin asignar</option>' + empleados
        .filter(e => e.cargo.toLowerCase() === 'conductor' && e.activo)
        .map(e => `<option value="${e.idEmpleado}">${escaparHtml(e.nombre)} ${escaparHtml(e.apellido)}</option>`).join('');

    const copilotosOpts = '<option value="">Sin asignar</option>' + empleados
        .filter(e => e.cargo.toLowerCase() === 'copiloto' && e.activo)
        .map(e => `<option value="${e.idEmpleado}">${escaparHtml(e.nombre)} ${escaparHtml(e.apellido)}</option>`).join('');

    document.getElementById('viajeConductor').innerHTML = conductoresOpts;
    document.getElementById('viajeCopiloto').innerHTML = copilotosOpts;

    // Poblar Viajes
    document.getElementById('tablaAdminViajes').innerHTML = viajes.map(v => `
        <tr>
            <td>#${v.idViaje}</td>
            <td class="fw-semibold">${escaparHtml(v.ruta?.origen?.ciudad)} → ${escaparHtml(v.ruta?.destino?.ciudad)}</td>
            <td>${escaparHtml((v.fechaHoraSalida || '').replace('T', ' ').slice(0, 16))}</td>
            <td class="small">${escaparHtml(v.bus?.placa || 'S/B')} · ${escaparHtml(v.conductor ? v.conductor.nombre + ' ' + v.conductor.apellido : 'Sin chofer')}</td>
            <td class="text-end fw-bold">${moneda(v.precio)}</td>
            <td class="text-center">${v.asientosDisponibles}</td>
            <td class="text-center">${badgeEstadoViaje(v.estado)}</td>
            <td class="text-center">
                <button type="button" class="btn btn-outline-primary btn-sm" data-editar-viaje="${v.idViaje}">
                    <i class="bi bi-pencil-square"></i>
                </button>
            </td>
        </tr>`).join('');

    document.querySelectorAll('[data-editar-viaje]').forEach(btn => {
        btn.addEventListener('click', () => cargarViajeEnFormulario(Number(btn.dataset.editarViaje)));
    });
}

function cargarViajeEnFormulario(idViaje) {
    const v = viajesAdminCache.find(x => x.idViaje === idViaje);
    if (!v) return;
    document.getElementById('tituloFormViaje').textContent = `Editando Viaje #${v.idViaje}`;
    document.getElementById('viajeIdEditar').value = v.idViaje;
    if (v.ruta) document.getElementById('viajeRuta').value = v.ruta.idRuta;
    if (v.bus) document.getElementById('viajeBus').value = v.bus.idBus;
    document.getElementById('viajeConductor').value = v.conductor?.idEmpleado || '';
    document.getElementById('viajeCopiloto').value = v.copiloto?.idEmpleado || '';
    document.getElementById('viajeFechaHora').value = (v.fechaHoraSalida || '').slice(0, 16);
    document.getElementById('viajePrecio').value = v.precio;
    document.getElementById('viajeEstado').value = v.estado || 'PROGRAMADO';
    document.getElementById('viajeServicio').value = v.servicio || 'Doble Piso VIP';
}

// ==================== 6. USUARIOS, ROLES Y CUPONES ====================

async function cargarUsuariosYCupones() {
    const rolFiltro = document.getElementById('filtroRolUsuarios')?.value || '';
    const [usuarios, cupones] = await Promise.all([
        apiGet(`/admin/usuarios${rolFiltro ? `?rol=${encodeURIComponent(rolFiltro)}` : ''}`),
        apiGet('/admin/cupones')
    ]);
    usuariosAdminCache = usuarios;

    document.getElementById('tablaAdminUsuarios').innerHTML = usuarios.map(u => `
        <tr>
            <td>
                <div class="fw-bold">${escaparHtml(u.nombre)} ${escaparHtml(u.apellido)}</div>
                <small class="text-muted">ID #${u.idUsuario}</small>
            </td>
            <td>
                <div>${escaparHtml(u.correo)}</div>
                <small class="text-muted">${escaparHtml(u.tipoDocumento)}: ${escaparHtml(u.numeroDocumento)}</small>
            </td>
            <td class="text-center">
                <select class="form-select form-select-sm fw-bold" data-cambiar-rol-usr="${u.idUsuario}">
                    ${['ADMIN', 'VENDEDOR', 'EMBARQUE', 'CLIENTE'].map(r =>
                        `<option value="${r}" ${u.rol === r ? 'selected' : ''}>${r}</option>`
                    ).join('')}
                </select>
            </td>
            <td class="text-center">
                <span class="badge ${u.activo ? 'bg-success' : 'bg-danger'}">${u.activo ? 'Activo' : 'Bloqueado'}</span>
            </td>
            <td class="text-center">
                <button type="button" class="btn btn-sm ${u.activo ? 'btn-outline-danger' : 'btn-outline-success'}" data-toggle-usr="${u.idUsuario}">
                    ${u.activo ? 'Desactivar' : 'Reactivar'}
                </button>
                <button type="button" class="btn btn-sm btn-outline-secondary" title="Resetear clave" data-reset-pass-usr="${u.idUsuario}">
                    <i class="bi bi-key"></i>
                </button>
            </td>
        </tr>`).join('');

    document.querySelectorAll('[data-cambiar-rol-usr]').forEach(sel => {
        sel.addEventListener('change', async () => {
            try {
                await apiPut(`/admin/usuarios/${sel.dataset.cambiarRolUsr}`, { rol: sel.value });
                await cargarUsuariosYCupones();
            } catch (err) {
                alert(err.message);
                await cargarUsuariosYCupones();
            }
        });
    });

    document.querySelectorAll('[data-toggle-usr]').forEach(btn => {
        btn.addEventListener('click', async () => {
            const u = usuariosAdminCache.find(x => String(x.idUsuario) === btn.dataset.toggleUsr);
            if (!u) return;
            try {
                await apiPut(`/admin/usuarios/${u.idUsuario}`, { activo: !u.activo });
                await cargarUsuariosYCupones();
            } catch (err) {
                alert(err.message);
            }
        });
    });

    document.querySelectorAll('[data-reset-pass-usr]').forEach(btn => {
        btn.addEventListener('click', async () => {
            const nueva = prompt('Ingresa la nueva contraseña temporal para este usuario (mín. 6 caracteres):');
            if (!nueva) return;
            try {
                await apiPut(`/admin/usuarios/${btn.dataset.resetPassUsr}`, { nuevaContrasena: nueva });
                alert('Contraseña actualizada y sesiones previas revocadas.');
            } catch (err) {
                alert(err.message);
            }
        });
    });

    document.getElementById('listaAdminCupones').innerHTML = cupones.map(c => `
        <div class="d-flex justify-content-between align-items-center border-bottom py-2">
            <div>
                <strong class="font-monospace text-primary">${escaparHtml(c.codigo)}</strong> · ${moneda(c.montoDescuento)}
                <div class="text-muted" style="font-size:0.75rem;">Usos: ${c.usosActuales}/${c.usosMaximos} · ${escaparHtml(c.descripcion || '')}</div>
            </div>
            <span class="badge ${c.activo ? 'bg-success' : 'bg-secondary'}">${c.activo ? 'Activo' : 'Inactivo'}</span>
        </div>`).join('');
}

// ==================== LISTENERS DE FORMULARIOS ====================

function configurarFormulariosAdmin() {
    document.getElementById('btnRefrescarDashboard')?.addEventListener('click', recargarTodoAdmin);
    document.getElementById('btnExportarCsv')?.addEventListener('click', exportarVentasCSV);
    document.getElementById('filtroEstadoVentasAdmin')?.addEventListener('change', cargarVentasAdmin);
    document.getElementById('filtroCanalVentasAdmin')?.addEventListener('change', cargarVentasAdmin);
    document.getElementById('filtroRolUsuarios')?.addEventListener('change', cargarUsuariosYCupones);

    document.getElementById('viajeRuta')?.addEventListener('change', (e) => {
        const opt = e.target.selectedOptions[0];
        if (opt?.dataset.precio && !document.getElementById('viajeIdEditar').value) {
            document.getElementById('viajePrecio').value = opt.dataset.precio;
        }
    });

    document.getElementById('btnLimpiarViaje')?.addEventListener('click', () => {
        document.getElementById('formAdminViaje').reset();
        document.getElementById('viajeIdEditar').value = '';
        document.getElementById('tituloFormViaje').textContent = 'Programar Nueva Salida';
    });

    document.getElementById('formAdminViaje')?.addEventListener('submit', async (e) => {
        e.preventDefault();
        const idEditar = document.getElementById('viajeIdEditar').value;
        const payload = {
            idRuta: Number(document.getElementById('viajeRuta').value),
            idBus: Number(document.getElementById('viajeBus').value) || null,
            idConductor: Number(document.getElementById('viajeConductor').value) || null,
            idCopiloto: Number(document.getElementById('viajeCopiloto').value) || null,
            fechaHoraSalida: document.getElementById('viajeFechaHora').value,
            precio: Number(document.getElementById('viajePrecio').value),
            servicio: document.getElementById('viajeServicio').value.trim(),
            estado: document.getElementById('viajeEstado').value
        };
        try {
            if (idEditar) {
                await apiPut(`/admin/viajes/${idEditar}`, payload);
            } else {
                await apiPost('/admin/viajes', payload);
            }
            document.getElementById('btnLimpiarViaje').click();
            await Promise.all([cargarDashboard(), cargarCatalogosOperativos()]);
        } catch (err) {
            alert(err.message);
        }
    });

    document.getElementById('formAdminTerminal')?.addEventListener('submit', async (e) => {
        e.preventDefault();
        try {
            await apiPost('/admin/terminales', {
                ciudad: document.getElementById('termCiudad').value.trim(),
                nombre: document.getElementById('termNombre').value.trim(),
                direccion: document.getElementById('termDireccion').value.trim(),
                activo: true
            });
            e.target.reset();
            await cargarCatalogosOperativos();
        } catch (err) {
            alert(err.message);
        }
    });

    document.getElementById('formAdminRuta')?.addEventListener('submit', async (e) => {
        e.preventDefault();
        try {
            await apiPost('/admin/rutas', {
                idTerminalOrigen: Number(document.getElementById('rutaOrigen').value),
                idTerminalDestino: Number(document.getElementById('rutaDestino').value),
                precioBase: Number(document.getElementById('rutaPrecio').value),
                horasEstimadas: Number(document.getElementById('rutaHoras').value),
                activo: true
            });
            e.target.reset();
            await Promise.all([cargarDashboard(), cargarCatalogosOperativos()]);
        } catch (err) {
            alert(err.message);
        }
    });

    document.getElementById('formAdminBus')?.addEventListener('submit', async (e) => {
        e.preventDefault();
        try {
            await apiPost('/admin/buses', {
                placa: document.getElementById('busPlaca').value.trim(),
                numeroInterno: document.getElementById('busInterno').value.trim(),
                marca: document.getElementById('busMarca').value.trim(),
                modelo: document.getElementById('busModelo').value.trim(),
                pisos: 2,
                capacidadAsientos: 64,
                estado: 'Activo'
            });
            e.target.reset();
            await Promise.all([cargarDashboard(), cargarCatalogosOperativos()]);
        } catch (err) {
            alert(err.message);
        }
    });

    document.getElementById('formAdminEmpleado')?.addEventListener('submit', async (e) => {
        e.preventDefault();
        try {
            await apiPost('/admin/empleados', {
                dniEmpleado: document.getElementById('empDni').value.trim(),
                nombre: document.getElementById('empNombre').value.trim(),
                apellido: document.getElementById('empApellido').value.trim(),
                cargo: document.getElementById('empCargo').value,
                licencia: document.getElementById('empLicencia').value.trim(),
                sueldo: Number(document.getElementById('empSueldo').value),
                activo: true
            });
            e.target.reset();
            await cargarCatalogosOperativos();
        } catch (err) {
            alert(err.message);
        }
    });

    document.getElementById('formAdminUsuario')?.addEventListener('submit', async (e) => {
        e.preventDefault();
        try {
            await apiPost('/admin/usuarios', {
                nombre: document.getElementById('usrNombre').value.trim(),
                apellido: document.getElementById('usrApellido').value.trim(),
                tipoDocumento: 'DNI',
                numeroDocumento: document.getElementById('usrDoc').value.trim(),
                telefono: document.getElementById('usrTel').value.trim(),
                correo: document.getElementById('usrCorreo').value.trim(),
                rol: document.getElementById('usrRol').value,
                contrasena: document.getElementById('usrPass').value
            });
            e.target.reset();
            await Promise.all([cargarDashboard(), cargarUsuariosYCupones()]);
        } catch (err) {
            alert(err.message);
        }
    });

    document.getElementById('formAdminCupon')?.addEventListener('submit', async (e) => {
        e.preventDefault();
        try {
            await apiPost('/admin/cupones', {
                codigo: document.getElementById('cupCodigo').value.trim(),
                montoDescuento: Number(document.getElementById('cupMonto').value),
                descripcion: document.getElementById('cupDesc').value.trim(),
                usosMaximos: 100,
                activo: true
            });
            e.target.reset();
            await cargarUsuariosYCupones();
        } catch (err) {
            alert(err.message);
        }
    });
}
