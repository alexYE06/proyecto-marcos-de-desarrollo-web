// ===================== CONFIGURACIÓN =====================
const API_VENTAS_URL = 'http://127.0.0.1:8080/api/admin/ventas';
const API_ADMIN_AUTH_URL = 'http://127.0.0.1:8080/api/admin/auth';
const LIMITE_ASIENTOS = 5;

let metodoSeleccionado = 'TARJETA';
let ventasCargadas = [];
let asientosSeleccionados = [];   // [{num, precio}]
let filtroTarifa = 'todos';

// ===================== BUS (copiado de asientos.js) =====================
// Cada fila = [izq1, izq2, pasillo, der1, der2]
// 's' = asiento | 'tv' | 'esc' | 'wc' | 'volante' | 'bodega' | '_' = vacío
const DISPOSICION_PISO1 = [
    ['volante', '_',  '_',  'esc', '_'],
    ['wc',      'wc', 'tv', '_',   '_'],
    ['s',       's',  '_',  's',   's'],
    ['s',       's',  '_',  's',   's'],
    ['s',       's',  'tv', 's',   's'],
    ['s',       's',  '_',  's',   's'],
    ['s',       's',  '_',  's',   's'],
    ['bodega',  'bodega', 'bodega', 'bodega', 'bodega']
];

const DISPOSICION_PISO2 = [
    ['s', 's', 'tv', 's',   's'],
    ['s', 's', '_',  's',   's'],
    ['s', 's', '_',  's',   's'],
    ['s', 's', '_',  'esc', 'esc'],
    ['s', 's', '_',  'esc', 'esc'],
    ['s', 's', 'tv', 's',   's'],
    ['s', 's', '_',  's',   's'],
    ['s', 's', '_',  's',   's'],
    ['s', 's', 'tv', 's',   's'],
    ['s', 's', '_',  's',   's'],
    ['s', 's', '_',  's',   's'],
    ['s', 's', 'tv', 's',   's']
];

// Mismos asientos ocupados de ejemplo que asientos.js (el servidor usa la misma lista)
const ASIENTOS_OCUPADOS_BASE = [2, 8, 11, 15, 19, 26, 27, 34, 38, 45, 46, 53, 60];

// Piso 1: asientos 1-4 = S/ 125, 5-20 = S/ 115 | Piso 2: asientos 21-64 = S/ 90
function precioDeAsiento(num) {
    if (num <= 4) return 125;
    if (num <= 20) return 115;
    return 90;
}

function construirPiso(disposicion, numInicial) {
    let n = numInicial;
    return disposicion.map(fila => fila.map(celda => {
        if (celda !== 's') return celda;
        const num = n++;
        return { num, precio: precioDeAsiento(num) };
    }));
}

const asientosDataPiso1 = construirPiso(DISPOSICION_PISO1, 1);
const asientosDataPiso2 = construirPiso(DISPOSICION_PISO2, 21);

const SVG_BUTACA = `<svg viewBox="0 0 40 44" aria-hidden="true">
    <rect x="1.5" y="17" width="8" height="21" rx="4" fill="currentColor"/>
    <rect x="30.5" y="17" width="8" height="21" rx="4" fill="currentColor"/>
    <rect x="1.5" y="17" width="8" height="21" rx="4" class="sombra"/>
    <rect x="30.5" y="17" width="8" height="21" rx="4" class="sombra"/>
    <rect x="7" y="2" width="26" height="26" rx="10" fill="currentColor"/>
    <rect x="8" y="24" width="24" height="16" rx="6" fill="currentColor"/>
    <rect x="8" y="24" width="24" height="16" rx="6" class="sombra"/>
</svg>`;

const ICONOS_BUS = {
    tv: `<svg viewBox="0 0 24 24" aria-label="TV"><rect x="3" y="6" width="18" height="12" rx="2.5" fill="none" stroke="currentColor" stroke-width="1.8"/><path d="M9 2.5l3 3.5 3-3.5M9 21h6" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/></svg>`,
    esc: `<svg viewBox="0 0 24 24" aria-label="Escalera"><path d="M3 20h5v-5h5v-5h5V5h3" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/></svg>`,
    wc: `<svg viewBox="0 0 40 44" aria-label="Baño"><rect x="3" y="5" width="34" height="34" rx="7" fill="none" stroke="currentColor" stroke-width="2"/><text x="20" y="28" text-anchor="middle" font-size="14" font-weight="800" font-family="sans-serif" fill="currentColor">WC</text></svg>`,
    volante: `<svg viewBox="0 0 40 44" aria-label="Conductor"><circle cx="20" cy="22" r="15" fill="none" stroke="currentColor" stroke-width="2.4"/><circle cx="20" cy="22" r="4" fill="currentColor"/><path d="M5.5 20h11.5M23 20h11.5M20 26v11" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round"/></svg>`,
    maleta: `<svg viewBox="0 0 24 24" aria-label="Equipaje"><rect x="4" y="8" width="16" height="12" rx="2.5" fill="none" stroke="currentColor" stroke-width="1.8"/><path d="M9 8V6a1.5 1.5 0 0 1 1.5-1.5h3A1.5 1.5 0 0 1 15 6v2M4 13h16" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/></svg>`
};

// ===================== INICIO =====================
document.addEventListener('DOMContentLoaded', () => {
    // 1. Verificar sesión de administrador
    const sesion = obtenerSesionAdmin();
    if (!sesion) {
        alert('Debes iniciar sesión como administrador para acceder al panel.');
        window.location.href = 'index.html';
        return;
    }
    document.getElementById('usuarioHeader').innerHTML =
        `<i class="bi bi-person-circle text-warning"></i> ${escaparHtml(sesion.nombre)}`;

    // 2. Fecha por defecto = hoy
    document.getElementById('ventaFecha').value = fechaHoyLocal();
    document.getElementById('ventaFecha').addEventListener('change', renderizarAsientos);

    // 3. Métodos de pago (mismo componente que pago.html)
    document.querySelectorAll('.payment-method-box').forEach(box => {
        box.addEventListener('click', () => seleccionarMetodo(box.dataset.metodo, box));
    });
    seleccionarMetodo('TARJETA', document.querySelector('.payment-method-box[data-metodo="TARJETA"]'));

    // 4. Filtros de tarifa
    document.querySelectorAll('.filter-btn').forEach(btn => {
        btn.addEventListener('click', () => filtrarPrecio(btn.dataset.tarifa, btn));
    });

    // 5. Eventos generales
    document.getElementById('formVenta').addEventListener('submit', registrarVenta);
    document.getElementById('btnRecargar').addEventListener('click', cargarVentas);
    document.getElementById('btnCerrarSesion').addEventListener('click', cerrarSesionAdmin);
    document.getElementById('tablaVentas').addEventListener('click', (e) => {
        const btn = e.target.closest('button[data-accion]');
        if (!btn) return;
        const id = Number(btn.dataset.id);
        if (btn.dataset.accion === 'boleta') verBoleta(id);
        if (btn.dataset.accion === 'eliminar') eliminarVenta(id);
    });

    renderizarAsientos();
    cargarVentas();
});

// ===================== SESIÓN =====================
function obtenerSesionAdmin() {
    try {
        const s = JSON.parse(localStorage.getItem('adminUrbanRide'));
        return s && s.token ? s : null;
    } catch (e) {
        return null;
    }
}

async function cerrarSesionAdmin(e) {
    if (e) e.preventDefault();
    const sesion = obtenerSesionAdmin();
    try {
        if (sesion) {
            await fetch(`${API_ADMIN_AUTH_URL}/logout`, { method: 'POST', headers: { 'X-Admin-Token': sesion.token } });
        }
    } catch (err) { /* aunque falle, se cierra la sesión local */ }
    localStorage.removeItem('adminUrbanRide');
    window.location.href = 'index.html';
}

// fetch que envía el token y expulsa al login si el servidor responde 401
async function fetchAdmin(url, opciones = {}) {
    const sesion = obtenerSesionAdmin();
    const headers = Object.assign({ 'Accept': 'application/json' }, opciones.headers || {}, {
        'X-Admin-Token': sesion ? sesion.token : ''
    });
    const response = await fetch(url, Object.assign({}, opciones, { headers }));
    if (response.status === 401) {
        localStorage.removeItem('adminUrbanRide');
        alert('Tu sesión de administrador expiró. Vuelve a iniciar sesión.');
        window.location.href = 'index.html';
        throw new Error('Sesión expirada');
    }
    return response;
}

// ===================== UTILIDADES =====================
function fechaHoyLocal() {
    const d = new Date();
    const mes = String(d.getMonth() + 1).padStart(2, '0');
    const dia = String(d.getDate()).padStart(2, '0');
    return `${d.getFullYear()}-${mes}-${dia}`;
}

function escaparHtml(texto) {
    return String(texto ?? '').replace(/[&<>"']/g, c => ({
        '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
    }[c]));
}

// "2026-10-25" -> "25/10/2026" (sin Date, para evitar desfase de zona horaria)
function formatearFecha(iso) {
    if (!iso) return '-';
    const [y, m, d] = String(iso).split('-');
    return `${d}/${m}/${y}`;
}

function mostrarMensaje(texto, tipo) {
    const el = document.getElementById('mensajeForm');
    el.className = `small mt-3 fw-bold text-${tipo}`;
    el.textContent = texto;
}

function numerosDe(textoAsientos) {
    return String(textoAsientos || '').split(',').map(s => parseInt(s.trim(), 10)).filter(n => !Number.isNaN(n));
}

// ===================== ASIENTOS =====================
// Asientos no disponibles = ocupados de ejemplo + vendidos desde este panel en la fecha elegida
function asientosNoDisponibles() {
    const fecha = document.getElementById('ventaFecha').value;
    const set = new Set(ASIENTOS_OCUPADOS_BASE);
    ventasCargadas.filter(v => v.fecha === fecha).forEach(v => numerosDe(v.asientos).forEach(n => set.add(n)));
    return set;
}

function renderizarAsientos() {
    const ocupados = asientosNoDisponibles();
    // Si un asiento marcado quedó ocupado (cambio de fecha), se desmarca
    asientosSeleccionados = asientosSeleccionados.filter(a => !ocupados.has(a.num));

    renderizarPiso('deckPiso1', asientosDataPiso1, ocupados);
    renderizarPiso('deckPiso2', asientosDataPiso2, ocupados);
    aplicarFiltro();
    actualizarResumen();
}

function renderizarPiso(containerId, filas, ocupados) {
    const contenedor = document.getElementById(containerId);
    contenedor.innerHTML = '';

    filas.forEach(fila => {
        fila.forEach((celda, col) => {
            if (celda === 'bodega') {
                if (col === 0) {
                    const bodega = document.createElement('div');
                    bodega.className = 'zona-equipaje';
                    bodega.innerHTML = `${ICONOS_BUS.maleta}<span>Equipaje</span>`;
                    contenedor.appendChild(bodega);
                }
                return;
            }

            if (typeof celda === 'string') {
                const deco = document.createElement('div');
                if (col === 2) {
                    deco.className = celda === '_' ? 'corredor' : 'corredor celda-pasillo';
                } else {
                    deco.className = 'celda';
                }
                deco.innerHTML = ICONOS_BUS[celda] || '';
                contenedor.appendChild(deco);
                return;
            }

            const asiento = celda;
            const ocupado = ocupados.has(asiento.num);
            const seleccionado = asientosSeleccionados.some(a => a.num === asiento.num);

            const div = document.createElement('div');
            div.className = `seat${ocupado ? ' occupied' : ''}${seleccionado ? ' selected' : ''}`;
            div.dataset.num = asiento.num;
            div.dataset.precio = asiento.precio;
            div.setAttribute('role', 'button');
            div.setAttribute('aria-label', `Asiento ${asiento.num}, S/ ${asiento.precio}${ocupado ? ', ocupado' : ''}`);
            div.innerHTML = `${SVG_BUTACA}<span class="seat-num">${asiento.num}</span>`;

            if (!ocupado) {
                div.tabIndex = 0;
                div.addEventListener('click', () => toggleAsiento(asiento, div));
                div.addEventListener('keydown', (e) => {
                    if ((e.key === 'Enter' || e.key === ' ') && !div.classList.contains('dimmed')) {
                        e.preventDefault();
                        toggleAsiento(asiento, div);
                    }
                });
            }
            contenedor.appendChild(div);
        });
    });
}

function toggleAsiento(asiento, el) {
    const indice = asientosSeleccionados.findIndex(a => a.num === asiento.num);
    if (indice >= 0) {
        asientosSeleccionados.splice(indice, 1);
        el.classList.remove('selected');
    } else {
        if (asientosSeleccionados.length >= LIMITE_ASIENTOS) {
            alert(`Solo puedes seleccionar un máximo de ${LIMITE_ASIENTOS} asientos.`);
            return;
        }
        asientosSeleccionados.push({ num: asiento.num, precio: asiento.precio });
        el.classList.add('selected');
    }
    actualizarResumen();
}

function actualizarResumen() {
    const lista = document.getElementById('listaAsientosSeleccionados');
    const total = asientosSeleccionados.reduce((sum, a) => sum + a.precio, 0);

    document.getElementById('contadorAsientos').innerText = `${asientosSeleccionados.length} / ${LIMITE_ASIENTOS}`;
    document.getElementById('precioTotal').innerText = `S/ ${total.toFixed(2)}`;
    document.getElementById('ventaCantidad').value = asientosSeleccionados.length;
    document.getElementById('ventaPrecioTotal').value = total.toFixed(2);
    document.getElementById('btnMontoPagar').innerText = `S/ ${total.toFixed(2)}`;

    if (asientosSeleccionados.length === 0) {
        lista.innerHTML = `<span class="text-muted small fst-italic">Ningún asiento marcado</span>`;
        return;
    }
    lista.innerHTML = asientosSeleccionados
        .map(a => `<span class="badge bg-primary fs-6 px-3 py-2 rounded-pill">Asiento ${a.num} (S/ ${a.precio})</span>`)
        .join('');
}

function filtrarPrecio(tarifa, boton) {
    filtroTarifa = tarifa === 'todos' ? 'todos' : parseInt(tarifa, 10);
    document.querySelectorAll('.filter-btn').forEach(b => b.classList.remove('active'));
    if (boton) boton.classList.add('active');
    aplicarFiltro();
}

function aplicarFiltro() {
    document.querySelectorAll('.seat:not(.occupied)').forEach(seat => {
        const precio = parseInt(seat.dataset.precio, 10);
        seat.classList.toggle('dimmed', filtroTarifa !== 'todos' && precio !== filtroTarifa);
    });
}

// ===================== MÉTODOS DE PAGO (copiado de pago.js) =====================
// Nota: los datos de tarjeta son solo visuales; nunca se envían ni se guardan.
function seleccionarMetodo(metodo, elemento) {
    metodoSeleccionado = metodo;
    document.querySelectorAll('.payment-method-box').forEach(b => b.classList.remove('active'));
    if (elemento) elemento.classList.add('active');

    const contenedor = document.getElementById('contenedorDetallePago');
    if (!contenedor) return;

    if (metodo === 'TARJETA') {
        contenedor.innerHTML = `
            <div class="alert alert-info border-0 rounded-3 small mb-3">
                <i class="bi bi-info-circle-fill"></i> ¡Recuerda habilitar las compras por internet en tu app bancaria!
            </div>
            <div class="row g-3">
                <div class="col-12"><label class="form-label small fw-bold text-muted">Número de la tarjeta</label><input type="text" class="form-control rounded-3" placeholder="•••• •••• •••• ••••" maxlength="19" autocomplete="off"></div>
                <div class="col-6"><label class="form-label small fw-bold text-muted">Fecha MM/AA</label><input type="text" class="form-control rounded-3" placeholder="MM/AA" maxlength="5" autocomplete="off"></div>
                <div class="col-6"><label class="form-label small fw-bold text-muted">CVV</label><input type="password" class="form-control rounded-3" placeholder="•••" maxlength="4" autocomplete="off"></div>
                <div class="col-12"><label class="form-label small fw-bold text-muted">Nombre del titular</label><input type="text" class="form-control rounded-3" placeholder="Como figura en la tarjeta" autocomplete="off"></div>
            </div>`;
    } else if (metodo === 'YAPE') {
        contenedor.innerHTML = `
            <div class="text-center p-3">
                <p class="small text-muted mb-2">Escanea el código QR desde tu app Yape o transfiere al número oficial:</p>
                <div class="bg-white p-3 d-inline-block rounded-3 shadow-sm border mb-2">
                    <i class="bi bi-qr-code" style="font-size: 7rem; color: #720e9e;"></i>
                </div>
                <div class="fw-bold" style="color: #720e9e;">Número: 999 888 777</div>
                <small class="text-muted">Titular: UrbanRide Perú S.A.C.</small>
            </div>`;
    } else if (metodo === 'PAYPAL') {
        contenedor.innerHTML = `
            <div class="text-center py-4">
                <p class="text-muted small">Serás redirigido al portal seguro de PayPal para completar tu pago.</p>
                <button type="button" class="btn btn-outline-primary rounded-pill px-4 fw-bold"><i class="bi bi-paypal"></i> Conectar con PayPal</button>
            </div>`;
    } else {
        contenedor.innerHTML = `
            <div class="p-3 bg-light rounded-3 border">
                <div class="fw-bold small text-dark mb-1"><i class="bi bi-receipt"></i> Código CIP de PagoEfectivo: <strong>8392019</strong></div>
                <p class="text-muted small mb-0">Acércate a un agente autorizado o ingresa a tu banca por internet y selecciona Pago de Servicios &gt; PagoEfectivo.</p>
            </div>`;
    }
}

// ===================== HISTORIAL =====================
async function cargarVentas() {
    const tbody = document.getElementById('tablaVentas');
    try {
        const response = await fetchAdmin(API_VENTAS_URL);
        if (!response.ok) throw new Error('No se pudo obtener el historial.');
        ventasCargadas = await response.json();
        renderizarTabla();
        renderizarAsientos(); // refresca los asientos ya vendidos en la fecha elegida
    } catch (err) {
        if (err.message === 'Sesión expirada') return;
        console.error('Error al cargar ventas:', err);
        tbody.innerHTML = `<tr><td colspan="7" class="text-center text-danger py-4">
            No se pudo conectar con el servidor. Verifica que el backend esté en ejecución.</td></tr>`;
    }
}

function renderizarTabla() {
    const tbody = document.getElementById('tablaVentas');
    tbody.innerHTML = '';

    if (ventasCargadas.length === 0) {
        tbody.innerHTML = `<tr><td colspan="7" class="text-center text-muted py-4">
            Aún no hay ventas registradas. Elige asientos arriba para registrar la primera.</td></tr>`;
    }

    let total = 0;
    ventasCargadas.forEach((v, i) => {
        const precio = Number(v.precioTotal) || 0;
        total += precio;

        const badges = numerosDe(v.asientos).map(n => `<span class="badge bg-dark me-1">${n}</span>`).join('');
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td class="text-center fw-bold">${ventasCargadas.length - i}</td>
            <td>${badges}</td>
            <td class="text-center">${escaparHtml(v.cantidad)}</td>
            <td class="text-center">${escaparHtml(v.metodoPago)}</td>
            <td class="text-center">${formatearFecha(v.fecha)}</td>
            <td class="text-end fw-bold">S/ ${precio.toFixed(2)}</td>
            <td class="text-center">
                <button type="button" class="btn btn-outline-primary btn-sm" title="Ver boleta" data-accion="boleta" data-id="${v.idVentaAdmin}">
                    <i class="bi bi-file-earmark-pdf"></i>
                </button>
                <button type="button" class="btn btn-outline-danger btn-sm" title="Eliminar" data-accion="eliminar" data-id="${v.idVentaAdmin}">
                    <i class="bi bi-trash"></i>
                </button>
            </td>`;
        tbody.appendChild(tr);
    });

    document.getElementById('statCantidadVentas').textContent = ventasCargadas.length;
    document.getElementById('statTotalVendido').textContent = `S/ ${total.toFixed(2)}`;
}

// ===================== REGISTRAR =====================
async function registrarVenta(e) {
    e.preventDefault();

    const fecha = document.getElementById('ventaFecha').value;
    if (asientosSeleccionados.length === 0) return mostrarMensaje('Selecciona al menos un asiento en el bus.', 'danger');
    if (!fecha) return mostrarMensaje('Selecciona la fecha de la venta.', 'danger');

    const asientos = asientosSeleccionados.map(a => a.num).sort((x, y) => x - y).join(',');

    const btn = document.getElementById('btnRegistrarVenta');
    const spinner = document.getElementById('spinnerRegistro');
    btn.disabled = true;
    spinner.classList.remove('d-none');

    try {
        // Solo se envían asientos, fecha y método. Cantidad y total los calcula el servidor.
        const response = await fetchAdmin(API_VENTAS_URL, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ asientos, fecha, metodoPago: metodoSeleccionado })
        });
        const data = await response.json();
        if (!response.ok) throw new Error(data.mensaje || 'No se pudo registrar la venta.');

        mostrarMensaje(data.mensaje, 'success');
        asientosSeleccionados = [];
        await cargarVentas();
    } catch (err) {
        if (err.message === 'Sesión expirada') return;
        console.error('Error al registrar venta:', err);
        mostrarMensaje(err.message, 'danger');
    } finally {
        btn.disabled = false;
        spinner.classList.add('d-none');
    }
}

// ===================== ELIMINAR =====================
async function eliminarVenta(id) {
    if (!confirm('¿Eliminar esta venta del historial? Sus asientos quedarán libres otra vez.')) return;
    try {
        const response = await fetchAdmin(`${API_VENTAS_URL}/${id}`, { method: 'DELETE' });
        const data = await response.json();
        if (!response.ok) throw new Error(data.mensaje || 'No se pudo eliminar.');
        await cargarVentas();
    } catch (err) {
        if (err.message !== 'Sesión expirada') alert(err.message);
    }
}

// ===================== BOLETA (reutiliza boleta.html / boleta.js sin modificarlos) =====================
function verBoleta(id) {
    const v = ventasCargadas.find(x => x.idVentaAdmin === id);
    if (!v) return;

    const sesion = obtenerSesionAdmin();
    const total = Number(v.precioTotal) || 0;

    // Una fila por asiento, con la misma estructura que arma pago.js
    const pasajeros = numerosDe(v.asientos).map(num => ({
        idAsiento: num,
        nombre: 'Venta',
        apellido: 'administrativa',
        tipoDocumento: '-',
        numeroDocumento: '-',
        precio: precioDeAsiento(num)
    }));

    const ventaEmitida = {
        codigoReserva: 'ADM-' + String(v.idVentaAdmin).padStart(6, '0'),
        tipoComprobante: 'BOLETA DE VENTA ELECTRÓNICA',
        serieNumero: 'B001-' + String(v.idVentaAdmin).padStart(5, '0'),
        fechaEmision: formatearFecha(v.fecha),
        metodoPago: v.metodoPago,
        comprador: {
            nombre: 'Venta en mostrador',
            documento: '-',
            tipoDocumento: '-',
            correo: '-',
            telefono: ''
        },
        pasajeros: pasajeros,
        subtotal: total,
        descuento: 0,
        total: total
    };

    localStorage.setItem('ultimaVentaUrbanRide', JSON.stringify(ventaEmitida));
    window.open('boleta.html', '_blank');
}
