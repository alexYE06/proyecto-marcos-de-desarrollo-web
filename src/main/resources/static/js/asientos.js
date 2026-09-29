const LIMITE_ASIENTOS = 5;
let asientosSeleccionados = [];

// 1. Mapeo de asientos según tipo y piso (Piso 1: VIP 160°, Piso 2: Confort)
const asientosDataPiso1 = [
    { num: 1, precio: 125, ocupado: false },
    { num: 2, precio: 125, ocupado: true },
    { num: 3, precio: 125, ocupado: false },
    { num: 4, precio: 115, ocupado: false },
    { num: 5, precio: 115, ocupado: false },
    { num: 6, precio: 115, ocupado: false },
    { num: 7, precio: 115, ocupado: false },
    { num: 8, precio: 115, ocupado: true },
    { num: 9, precio: 115, ocupado: false },
    { num: 10, precio: 115, ocupado: false },
    { num: 11, precio: 115, ocupado: false },
    { num: 12, precio: 115, ocupado: false }
];

const asientosDataPiso2 = [
    { num: 13, precio: 90, ocupado: false },
    { num: 14, precio: 90, ocupado: false },
    { num: 15, precio: 90, ocupado: true },
    { num: 16, precio: 90, ocupado: false },
    { num: 17, precio: 90, ocupado: false },
    { num: 18, precio: 90, ocupado: false },
    { num: 19, precio: 90, ocupado: false },
    { num: 20, precio: 90, ocupado: false },
    { num: 21, precio: 90, ocupado: false },
    { num: 22, precio: 90, ocupado: false },
    { num: 23, precio: 90, ocupado: true },
    { num: 24, precio: 90, ocupado: false },
    { num: 25, precio: 90, ocupado: false },
    { num: 26, precio: 90, ocupado: false },
    { num: 27, precio: 90, ocupado: false },
    { num: 28, precio: 90, ocupado: false }
];

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

// Renderizar las butacas
function renderizarPiso(containerId, lista) {
    const contenedor = document.getElementById(containerId);
    contenedor.innerHTML = '';

    lista.forEach(asiento => {
        const div = document.createElement('div');
        div.className = `seat ${asiento.ocupado ? 'occupied' : ''}`;
        div.dataset.num = asiento.num;
        div.dataset.precio = asiento.precio;
        div.innerText = asiento.num;

        if (!asiento.ocupado) {
            div.addEventListener('click', () => toggleAsiento(asiento, div));
        }

        contenedor.appendChild(div);
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