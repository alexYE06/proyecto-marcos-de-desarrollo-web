const LIMITE_ASIENTOS = 5;
let asientosSeleccionados = [];

// 1. Distribución del bus de dos pisos (frente arriba).
//    Cada fila = [izq1, izq2, pasillo, der1, der2]
//    's' = asiento | 'tv' | 'esc' (escalera) | 'wc' (baño) | 'volante' (conductor) | 'bodega' (zona de equipaje, ocupa toda la fila) | '_' = vacío
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

// Asientos que ya están vendidos
const ASIENTOS_OCUPADOS = [2, 8, 11, 15, 19, 26, 27, 34, 38, 45, 46, 53, 60];

// Numera los asientos y les asigna tarifa (Piso 1: VIP 160° / Piso 2: Confort)
function construirPiso(disposicion, numInicial, precioDe) {
    let n = numInicial;
    return disposicion.map(fila => fila.map(celda => {
        if (celda !== 's') return celda;
        const num = n++;
        return { num, precio: precioDe(num), ocupado: ASIENTOS_OCUPADOS.includes(num) };
    }));
}

// Piso 1: asientos 1-20 (la primera fila es S/ 125, el resto S/ 115)
const asientosDataPiso1 = construirPiso(DISPOSICION_PISO1, 1, num => (num <= 4 ? 125 : 115));
// Piso 2: asientos 21-64 (S/ 90)
const asientosDataPiso2 = construirPiso(DISPOSICION_PISO2, 21, () => 90);

document.addEventListener("DOMContentLoaded", () => {
    // Validar sesión
    const usuarioRaw = localStorage.getItem('usuarioUrbanRide');
    if (!usuarioRaw) {
        alert("Debes iniciar sesión para seleccionar tus asientos.");
        window.location.href = "index.html";
        return;
    }

    const usuario = JSON.parse(usuarioRaw);
    document.getElementById('usuarioInfo').innerText = `Pasajero: ${usuario.nombre}`;

    // Renderizar ambos pisos
    renderizarPiso('deckPiso1', asientosDataPiso1);
    renderizarPiso('deckPiso2', asientosDataPiso2);
});

// Dibujo de la butaca (el color lo pone el CSS según la tarifa y el estado)
const SVG_BUTACA = `<svg viewBox="0 0 40 44" aria-hidden="true">
    <rect x="1.5" y="17" width="8" height="21" rx="4" fill="currentColor"/>
    <rect x="30.5" y="17" width="8" height="21" rx="4" fill="currentColor"/>
    <rect x="1.5" y="17" width="8" height="21" rx="4" class="sombra"/>
    <rect x="30.5" y="17" width="8" height="21" rx="4" class="sombra"/>
    <rect x="7" y="2" width="26" height="26" rx="10" fill="currentColor"/>
    <rect x="8" y="24" width="24" height="16" rx="6" fill="currentColor"/>
    <rect x="8" y="24" width="24" height="16" rx="6" class="sombra"/>
</svg>`;

// Iconos del bus (baño, TV, escalera, volante)
const ICONOS_BUS = {
    tv: `<svg viewBox="0 0 24 24" aria-label="TV"><rect x="3" y="6" width="18" height="12" rx="2.5" fill="none" stroke="currentColor" stroke-width="1.8"/><path d="M9 2.5l3 3.5 3-3.5M9 21h6" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/></svg>`,
    esc: `<svg viewBox="0 0 24 24" aria-label="Escalera"><path d="M3 20h5v-5h5v-5h5V5h3" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/></svg>`,
    wc: `<svg viewBox="0 0 40 44" aria-label="Baño"><rect x="3" y="5" width="34" height="34" rx="7" fill="none" stroke="currentColor" stroke-width="2"/><text x="20" y="28" text-anchor="middle" font-size="14" font-weight="800" font-family="sans-serif" fill="currentColor">WC</text></svg>`,
    volante: `<svg viewBox="0 0 40 44" aria-label="Conductor"><circle cx="20" cy="22" r="15" fill="none" stroke="currentColor" stroke-width="2.4"/><circle cx="20" cy="22" r="4" fill="currentColor"/><path d="M5.5 20h11.5M23 20h11.5M20 26v11" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round"/></svg>`,
    maleta: `<svg viewBox="0 0 24 24" aria-label="Equipaje"><rect x="4" y="8" width="16" height="12" rx="2.5" fill="none" stroke="currentColor" stroke-width="1.8"/><path d="M9 8V6a1.5 1.5 0 0 1 1.5-1.5h3A1.5 1.5 0 0 1 15 6v2M4 13h16" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/></svg>`
};

// Renderizar las filas del piso (asientos + elementos del bus)
function renderizarPiso(containerId, filas) {
    const contenedor = document.getElementById(containerId);
    contenedor.innerHTML = '';

    filas.forEach(fila => {
        fila.forEach((celda, col) => {
            // Zona de equipaje: una sola franja que ocupa todo el ancho
            if (celda === 'bodega') {
                if (col === 0) {
                    const bodega = document.createElement('div');
                    bodega.className = 'zona-equipaje';
                    bodega.innerHTML = `${ICONOS_BUS.maleta}<span>Equipaje</span>`;
                    contenedor.appendChild(bodega);
                }
                return;
            }

            // Elementos que no son asiento (pasillo, TV, escalera, baño, volante...)
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
            const div = document.createElement('div');
            div.className = `seat ${asiento.ocupado ? 'occupied' : ''}`;
            div.dataset.num = asiento.num;
            div.dataset.precio = asiento.precio;
            div.setAttribute('role', 'button');
            div.setAttribute('aria-label', `Asiento ${asiento.num}, S/ ${asiento.precio}${asiento.ocupado ? ', ocupado' : ''}`);
            div.innerHTML = `${SVG_BUTACA}<span class="seat-num">${asiento.num}</span>`;

            if (!asiento.ocupado) {
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

// Marcar / desmarcar asiento
function toggleAsiento(asiento, el) {
    const indice = asientosSeleccionados.findIndex(a => a.num === asiento.num);

    if (indice >= 0) {
        // Desmarcar
        asientosSeleccionados.splice(indice, 1);
        el.classList.remove('selected');
    } else {
        // Validar límite de 5 asientos
        if (asientosSeleccionados.length >= LIMITE_ASIENTOS) {
            alert(`Solo puedes seleccionar un máximo de ${LIMITE_ASIENTOS} asientos.`);
            return;
        }
        asientosSeleccionados.push(asiento);
        el.classList.add('selected');
    }

    actualizarResumen();
}

// Actualizar panel lateral
function actualizarResumen() {
    const contador = document.getElementById('contadorAsientos');
    const contenedorLista = document.getElementById('listaAsientosSeleccionados');
    const totalEl = document.getElementById('precioTotal');
    const btnPago = document.getElementById('btnContinuarPago');

    contador.innerText = `${asientosSeleccionados.length} / ${LIMITE_ASIENTOS}`;

    if (asientosSeleccionados.length === 0) {
        contenedorLista.innerHTML = `<span class="text-muted small fst-italic">Ningún asiento marcado</span>`;
        totalEl.innerText = `S/ 0.00`;
        btnPago.disabled = true;
        return;
    }

    // Listar badges
    contenedorLista.innerHTML = asientosSeleccionados
        .map(a => `<span class="badge bg-primary fs-6 px-3 py-2 rounded-pill">Asiento ${a.num} (S/ ${a.precio})</span>`)
        .join('');

    const total = asientosSeleccionados.reduce((sum, a) => sum + a.precio, 0);
    totalEl.innerText = `S/ ${total.toFixed(2)}`;
    btnPago.disabled = false;
}

// Filtro superior por tarifas (S/ 90, S/ 115, S/ 125 o Todos)
function filtrarPrecio(tarifa) {
    const todosLosAsientos = document.querySelectorAll('.seat:not(.occupied)');
    const botones = document.querySelectorAll('.filter-btn');

    botones.forEach(btn => btn.classList.remove('active'));

    todosLosAsientos.forEach(seat => {
        const precio = parseInt(seat.dataset.precio);
        if (tarifa === 'todos' || precio === tarifa) {
            seat.classList.remove('dimmed');
        } else {
            seat.classList.add('dimmed');
        }
    });
}

// Redirección al formulario de pasajeros
const btnPago = document.getElementById('btnContinuarPago');
if (btnPago) {
    btnPago.addEventListener('click', () => {
        if (asientosSeleccionados.length === 0) {
            alert("Por favor selecciona al menos un asiento.");
            return;
        }

        // Guardamos los asientos elegidos para usarlos en la siguiente pantalla
        sessionStorage.setItem('asientosComprando', JSON.stringify(asientosSeleccionados));
        
        // Inicializar la marca de tiempo para los 15 minutos (si no existe ya)
        const tiempoExpiracion = Date.now() + 15 * 60 * 1000;
        sessionStorage.setItem('limiteTiempoCompra', tiempoExpiracion.toString());

        // Redirigir a la pantalla de pasajeros
        window.location.href = 'pasajeros.html';
    });
}

// Ayudante: cerrar el globo con la X (solo por esta visita a la página)
document.addEventListener("DOMContentLoaded", () => {
    const ayudante = document.getElementById("ayudante");
    const cerrar = document.getElementById("ayudanteCerrar");
    if (ayudante && cerrar) {
        cerrar.addEventListener("click", () => ayudante.classList.add("oculto"));
    }
});
