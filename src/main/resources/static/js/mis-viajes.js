import { apiGet, apiPut, apiPost } from './core/api.js';
import { exigirRoles, guardarSesion, limpiarSesion, abrirComprobante } from './core/session.js';
import { escaparHtml, moneda, badgeEstadoVenta } from './core/formatters.js';

document.addEventListener('DOMContentLoaded', async () => {
    const sesion = exigirRoles(['CLIENTE', 'VENDEDOR', 'EMBARQUE', 'ADMIN']);
    if (!sesion) return;

    await cargarPerfilActual();
    await cargarMisCompras();

    document.getElementById('btnRecargarCompras')?.addEventListener('click', cargarMisCompras);
    document.getElementById('btnCerrarSesionCliente')?.addEventListener('click', async () => {
        try { await apiPost('/auth/logout'); } catch {}
        limpiarSesion();
        window.location.href = 'index.html';
    });

    document.getElementById('formActualizarPerfil')?.addEventListener('submit', async (e) => {
        e.preventDefault();
        const msg = document.getElementById('mensajePerfil');
        msg.textContent = '';
        try {
            const actualizado = await apiPut('/auth/perfil', {
                nombre: document.getElementById('perf-nombre').value.trim(),
                apellido: document.getElementById('perf-apellido').value.trim(),
                telefono: document.getElementById('perf-telefono').value.trim(),
                contrasenaActual: document.getElementById('perf-pass-actual').value,
                contrasenaNueva: document.getElementById('perf-pass-nueva').value
            });
            guardarSesion(actualizado);
            document.getElementById('perf-pass-actual').value = '';
            document.getElementById('perf-pass-nueva').value = '';
            document.getElementById('perfilNombreTitulo').textContent = actualizado.nombreCompleto;
            msg.className = 'small fw-bold mb-2 text-success';
            msg.textContent = actualizado.mensaje;
        } catch (err) {
            msg.className = 'small fw-bold mb-2 text-danger';
            msg.textContent = err.message;
        }
    });
});

async function cargarPerfilActual() {
    try {
        const me = await apiGet('/auth/me');
        guardarSesion(me);
        document.getElementById('perfilNombreTitulo').textContent = me.nombreCompleto;
        document.getElementById('perfilRolBadge').textContent = me.rol;
        document.getElementById('perf-nombre').value = me.nombre || '';
        document.getElementById('perf-apellido').value = me.apellido || '';
        document.getElementById('perf-doc-correo').value = `${me.tipoDocumento}: ${me.numeroDocumento} · ${me.correo}`;
        document.getElementById('perf-telefono').value = me.telefono || '';
    } catch (err) {
        console.error(err);
    }
}

async function cargarMisCompras() {
    const contenedor = document.getElementById('contenedorMisCompras');
    try {
        const compras = await apiGet('/cliente/mis-compras');
        if (!compras || compras.length === 0) {
            contenedor.innerHTML = `
                <div class="text-center py-5 border rounded-4 bg-light">
                    <i class="bi bi-luggage fs-1 text-muted d-block mb-2"></i>
                    <h6 class="fw-bold">Aún no registras viajes comprados</h6>
                    <p class="small text-muted mb-3">Explora nuestras salidas diarias y reserva tus asientos en minutos.</p>
                    <a href="index.html" class="btn btn-warning btn-sm rounded-pill px-4 fw-bold">Buscar pasajes</a>
                </div>`;
            return;
        }

        contenedor.innerHTML = compras.map(v => {
            const listaBoletos = (v.boletos || []).map(b => `
                <span class="badge bg-light text-dark border me-1 mb-1">
                    Asiento ${b.numeroAsiento} · ${escaparHtml(b.pasajeroNombreCompleto)} · <code>${escaparHtml(b.codigoBoleto)}</code>
                    (${escaparHtml(b.estadoAbordaje)})
                </span>`).join('');

            return `
                <div class="border rounded-4 p-3 mb-3 bg-white shadow-xs">
                    <div class="d-flex flex-wrap justify-content-between align-items-center gap-2 border-bottom pb-2 mb-2">
                        <div>
                            <span class="badge bg-dark me-1">${escaparHtml(v.codigoReserva)}</span>
                            <span class="fw-bold text-primary">${escaparHtml(v.origenCiudad)} → ${escaparHtml(v.destinoCiudad)}</span>
                            <small class="text-muted ms-2"><i class="bi bi-clock"></i> Salida: ${escaparHtml(v.fechaHoraSalida)}</small>
                        </div>
                        <div class="d-flex align-items-center gap-2">
                            ${badgeEstadoVenta(v.estadoPago)}
                            <span class="fw-bold fs-6">${moneda(v.montoTotal)}</span>
                        </div>
                    </div>
                    <div class="mb-2">${listaBoletos}</div>
                    <div class="d-flex justify-content-between align-items-center small text-muted">
                        <span>${escaparHtml(v.serieNumero)} · Pago: ${escaparHtml(v.metodoPago)}</span>
                        <button type="button" class="btn btn-outline-primary btn-sm rounded-pill px-3 fw-bold" data-ver-comprobante="${v.idVenta}">
                            <i class="bi bi-file-earmark-pdf"></i> Ver Comprobante y QR
                        </button>
                    </div>
                </div>`;
        }).join('');

        contenedor.querySelectorAll('[data-ver-comprobante]').forEach(btn => {
            btn.addEventListener('click', () => {
                const venta = compras.find(x => String(x.idVenta) === btn.dataset.verComprobante);
                abrirComprobante(venta || btn.dataset.verComprobante);
            });
        });
    } catch (err) {
        contenedor.innerHTML = `<div class="text-danger text-center py-4">${escaparHtml(err.message)}</div>`;
    }
}
