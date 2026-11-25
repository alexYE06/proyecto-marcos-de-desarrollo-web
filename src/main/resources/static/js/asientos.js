import { apiGet, apiPost } from './core/api.js';
import { exigirRoles } from './core/session.js';
import { escaparHtml, moneda, formatearFechaISO } from './core/formatters.js';
import { crearControladorMapaBus } from './components/bus-map.js';

const LIMITE_ASIENTOS = 5;

document.addEventListener('DOMContentLoaded', async () => {
    const sesion = exigirRoles(['CLIENTE', 'VENDEDOR', 'EMBARQUE', 'ADMIN']);
    if (!sesion) return;

    document.getElementById('usuarioInfo').textContent = `Pasajero: ${sesion.nombreCompleto || sesion.nombre}`;

    const idViaje = Number(sessionStorage.getItem('viajeSeleccionadoId'));
    if (!idViaje) {
        alert('Primero selecciona una salida disponible.');
        window.location.href = 'index.html';
        return;
    }

    const ayudante = document.getElementById('ayudante');
    document.getElementById('ayudanteCerrar')?.addEventListener('click', () => ayudante?.classList.add('oculto'));

    let infoViaje = null;

    const mapa = crearControladorMapaBus({
        elPiso1: document.getElementById('deckPiso1'),
        elPiso2: document.getElementById('deckPiso2'),
        limiteAsientos: LIMITE_ASIENTOS,
        onSeleccionCambio: (seleccionados) => actualizarResumen(seleccionados)
    });

    document.querySelectorAll('#contenedorFiltrosTarifa .filter-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('#contenedorFiltrosTarifa .filter-btn').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            mapa.filtrarPorNivel(btn.dataset.nivel);
        });
    });

    try {
        infoViaje = await apiGet(`/catalogo/viajes/${idViaje}/asientos`);

        document.getElementById('detalleRutaHeader').innerHTML =
            `${escaparHtml(infoViaje.origenCiudad)} <i class="bi bi-arrow-right text-warning"></i> ${escaparHtml(infoViaje.destinoCiudad)} · ${formatearFechaISO(infoViaje.fechaSalida)} ${escaparHtml(infoViaje.horaSalida)}`;
        document.getElementById('servicioViajeTitulo').textContent = infoViaje.servicio || 'Servicio Doble Piso VIP';
        document.getElementById('subtituloItinerario').textContent =
            `${infoViaje.origenTerminal} → ${infoViaje.destinoTerminal} · Bus ${infoViaje.placaBus} (${infoViaje.asientosDisponibles} libres)`;

        document.getElementById('btnFiltroPiso2').textContent = `${moneda(infoViaje.tarifaPiso2)}`;
        document.getElementById('btnFiltroVip').textContent = `${moneda(infoViaje.tarifaPiso1Vip)}`;
        document.getElementById('btnFiltroPanoramico').textContent = `${moneda(infoViaje.tarifaPiso1Panoramico)}`;
        document.getElementById('leyendaPiso2').textContent = `${moneda(infoViaje.tarifaPiso2)} (Piso 2)`;
        document.getElementById('leyendaVip').textContent = `${moneda(infoViaje.tarifaPiso1Vip)} (VIP)`;
        document.getElementById('leyendaPanoramico').textContent = `${moneda(infoViaje.tarifaPiso1Panoramico)} (180°)`;

        mapa.cargarDatosMapa({
            ocupados: infoViaje.asientosOcupados || [],
            preseleccionados: infoViaje.asientosReservadosPorMi || [],
            tarifas: {
                piso2: infoViaje.tarifaPiso2,
                vip: infoViaje.tarifaPiso1Vip,
                panoramico: infoViaje.tarifaPiso1Panoramico
            }
        });
    } catch (err) {
        alert(err.message || 'No se pudo cargar el mapa de asientos.');
        window.location.href = 'index.html';
        return;
    }

    document.getElementById('btnContinuarPago')?.addEventListener('click', async () => {
        const seleccionados = mapa.obtenerSeleccionados();
        const msgError = document.getElementById('mensajeErrorAsientos');
        const btn = document.getElementById('btnContinuarPago');
        msgError.textContent = '';

        if (seleccionados.length === 0) return;

        btn.disabled = true;
        btn.textContent = 'Reservando asientos en servidor...';

        try {
            const reserva = await apiPost('/cliente/reservas', {
                idViaje,
                asientos: seleccionados.map(a => a.num)
            });

            sessionStorage.setItem('asientosComprando', JSON.stringify(seleccionados));
            sessionStorage.setItem('resumenViajeComprando', JSON.stringify(infoViaje));
            sessionStorage.setItem('reservaActivaId', String(reserva.idReserva));
            sessionStorage.setItem('limiteTiempoCompra', String(reserva.expiraEnEpochMs));

            window.location.href = 'pasajeros.html';
        } catch (err) {
            msgError.textContent = err.message;
            // Recargar disponibilidad real por si alguien ganó el asiento hace segundos
            const actualizado = await apiGet(`/catalogo/viajes/${idViaje}/asientos`);
            mapa.cargarDatosMapa({
                ocupados: actualizado.asientosOcupados || [],
                tarifas: {
                    piso2: actualizado.tarifaPiso2,
                    vip: actualizado.tarifaPiso1Vip,
                    panoramico: actualizado.tarifaPiso1Panoramico
                }
            });
            btn.disabled = false;
            btn.textContent = 'Bloquear asientos (15 min) y continuar';
        }
    });
});

function actualizarResumen(seleccionados) {
    const contador = document.getElementById('contadorAsientos');
    const lista = document.getElementById('listaAsientosSeleccionados');
    const totalEl = document.getElementById('precioTotal');
    const btn = document.getElementById('btnContinuarPago');

    contador.textContent = `${seleccionados.length} / ${LIMITE_ASIENTOS}`;

    if (seleccionados.length === 0) {
        lista.innerHTML = `<span class="text-muted small fst-italic">Ningún asiento marcado</span>`;
        totalEl.textContent = 'S/ 0.00';
        btn.disabled = true;
        return;
    }

    lista.innerHTML = seleccionados
        .map(a => `<span class="badge bg-primary px-3 py-2 rounded-pill">Asiento ${a.num} (${moneda(a.precio)})</span>`)
        .join('');

    const total = seleccionados.reduce((sum, a) => sum + a.precio, 0);
    totalEl.textContent = moneda(total);
    btn.disabled = false;
}
