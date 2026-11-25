// Componente reutilizable del Bus de Dos Cubiertas (64 asientos)
// Usado en: Selección Cliente (asientos.html) y Punto de Venta Vendedor (vendedor-pos.html)

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

export function infoTarifaAsiento(num, tarifas = { panoramico: 125, vip: 115, piso2: 90 }) {
    if (num <= 4) {
        return { num, piso: 1, nivel: 'panoramico', categoria: 'VIP Panorámico 180°', precio: Number(tarifas.panoramico) };
    }
    if (num <= 20) {
        return { num, piso: 1, nivel: 'vip', categoria: 'VIP Ejecutivo 160°', precio: Number(tarifas.vip) };
    }
    return { num, piso: 2, nivel: 'piso2', categoria: 'Confort 140°', precio: Number(tarifas.piso2) };
}

export function crearControladorMapaBus({
    elPiso1,
    elPiso2,
    limiteAsientos = 5,
    soloLectura = false,
    onSeleccionCambio = () => {}
}) {
    let ocupadosSet = new Set();
    let seleccionados = [];
    let filtroNivel = 'todos';
    let tarifasActuales = { panoramico: 125, vip: 115, piso2: 90 };

    function construirFilas(disposicion, inicial) {
        let n = inicial;
        return disposicion.map(fila => fila.map(celda => {
            if (celda !== 's') return celda;
            return infoTarifaAsiento(n++, tarifasActuales);
        }));
    }

    function renderizarPiso(contenedor, filas) {
        if (!contenedor) return;
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
                    deco.className = col === 2 ? (celda === '_' ? 'corredor' : 'corredor celda-pasillo') : 'celda';
                    deco.innerHTML = ICONOS_BUS[celda] || '';
                    contenedor.appendChild(deco);
                    return;
                }

                const asiento = celda;
                const ocupado = ocupadosSet.has(asiento.num);
                const marcado = seleccionados.some(a => a.num === asiento.num);
                const atenuado = !ocupado && filtroNivel !== 'todos' && asiento.nivel !== filtroNivel;

                const div = document.createElement('div');
                div.className = `seat${ocupado ? ' occupied' : ''}${marcado ? ' selected' : ''}${atenuado ? ' dimmed' : ''}`;
                div.dataset.num = asiento.num;
                div.dataset.nivel = asiento.nivel;
                div.dataset.precio = asiento.precio;
                div.setAttribute('role', 'button');
                div.setAttribute('title', `Asiento ${asiento.num} · ${asiento.categoria} · S/ ${asiento.precio.toFixed(2)}`);
                div.setAttribute('aria-label', `Asiento ${asiento.num}, S/ ${asiento.precio}${ocupado ? ', ocupado' : ''}`);
                div.innerHTML = `${SVG_BUTACA}<span class="seat-num">${asiento.num}</span>`;

                if (!ocupado && !soloLectura) {
                    div.tabIndex = 0;
                    div.addEventListener('click', () => alternarAsiento(asiento, div));
                    div.addEventListener('keydown', (e) => {
                        if ((e.key === 'Enter' || e.key === ' ') && !div.classList.contains('dimmed')) {
                            e.preventDefault();
                            alternarAsiento(asiento, div);
                        }
                    });
                }

                contenedor.appendChild(div);
            });
        });
    }

    function alternarAsiento(asiento, elemento) {
        const idx = seleccionados.findIndex(a => a.num === asiento.num);
        if (idx >= 0) {
            seleccionados.splice(idx, 1);
            elemento.classList.remove('selected');
        } else {
            if (seleccionados.length >= limiteAsientos) {
                alert(`Solo puedes seleccionar un máximo de ${limiteAsientos} asientos por operación.`);
                return;
            }
            seleccionados.push(asiento);
            seleccionados.sort((a, b) => a.num - b.num);
            elemento.classList.add('selected');
        }
        onSeleccionCambio([...seleccionados]);
    }

    function renderizar() {
        renderizarPiso(elPiso1, construirFilas(DISPOSICION_PISO1, 1));
        renderizarPiso(elPiso2, construirFilas(DISPOSICION_PISO2, 21));
    }

    return {
        cargarDatosMapa({ ocupados = [], preseleccionados = [], tarifas }) {
            if (tarifas) {
                tarifasActuales = tarifas;
            }
            ocupadosSet = new Set(ocupados.map(Number));
            seleccionados = seleccionados
                .filter(a => !ocupadosSet.has(a.num))
                .map(a => infoTarifaAsiento(a.num, tarifasActuales));

            if (preseleccionados && preseleccionados.length > 0 && seleccionados.length === 0) {
                seleccionados = preseleccionados
                    .map(Number)
                    .filter(n => !ocupadosSet.has(n))
                    .slice(0, limiteAsientos)
                    .map(n => infoTarifaAsiento(n, tarifasActuales));
            }
            renderizar();
            onSeleccionCambio([...seleccionados]);
        },
        filtrarPorNivel(nivel) {
            filtroNivel = nivel || 'todos';
            renderizar();
        },
        limpiarSeleccion() {
            seleccionados = [];
            renderizar();
            onSeleccionCambio([]);
        },
        obtenerSeleccionados() {
            return [...seleccionados];
        }
    };
}
