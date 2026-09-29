let metodoSeleccionado = 'TARJETA';
let descuentoAplicado = 0;
let subtotalGeneral = 0;
let cronometroInterval = null;

document.addEventListener("DOMContentLoaded", () => {
    // 1. Validar sesión del usuario
    const usuarioRaw = localStorage.getItem('usuarioUrbanRide');
    if (!usuarioRaw) {
        alert("Debes iniciar sesión para realizar el pago.");
        window.location.href = "index.html";
        return;
    }
    const usuario = JSON.parse(usuarioRaw);
    const usuarioHeader = document.getElementById('usuarioHeader');
    if (usuarioHeader) {
        usuarioHeader.innerText = usuario.nombre;
    }

    // 2. Obtener datos de pasajeros seleccionados
    const pasajerosRaw = sessionStorage.getItem('datosPasajerosComprando');
    if (!pasajerosRaw) {
        alert("No se encontraron datos de pasajeros. Redirigiendo a selección de asientos...");
        window.location.href = "asientos.html";
        return;
    }

    const pasajeros = JSON.parse(pasajerosRaw);

    // 3. Autocompletar datos del comprador con el primer pasajero
    autocompletarComprador(pasajeros[0]);

    // 4. Renderizar desglose y totales
    calcularTotales(pasajeros);

    // 5. Iniciar o mantener el temporizador de 15 minutos sincronizado
    iniciarTemporizadorSincronizado();

    // 6. Configurar listeners de la pantalla
    const checkMismoPasajero = document.getElementById('checkMismoPasajero');
    if (checkMismoPasajero) {
        checkMismoPasajero.addEventListener('change', (e) => {
            if (e.target.checked) autocompletarComprador(pasajeros[0]);
        });
    }

    const checkFactura = document.getElementById('checkFactura');
    if (checkFactura) {
        checkFactura.addEventListener('change', (e) => {
            const divRuc = document.getElementById('seccionRuc');
            if (divRuc) {
                divRuc.classList.toggle('d-none', !e.target.checked);
            }
        });
    }

    const btnAplicarPromo = document.getElementById('btnAplicarPromo');
    if (btnAplicarPromo) {
        btnAplicarPromo.addEventListener('click', aplicarPromo);
    }

    const btnFinalizarCompra = document.getElementById('btnFinalizarCompra');
    if (btnFinalizarCompra) {
        btnFinalizarCompra.addEventListener('click', procesarPagoFinal);
    }
});

// Autocompletar datos del comprador
function autocompletarComprador(primerPasajero) {
    if (!primerPasajero) return;
    const setVal = (id, val) => {
        const el = document.getElementById(id);
        if (el) el.value = val || '';
    };

    setVal('comp-nombre', primerPasajero.nombre);
    setVal('comp-apePaterno', primerPasajero.apellido);
    setVal('comp-telefono', primerPasajero.telefono);
    setVal('comp-correo', primerPasajero.correo);
}

// Calcular precios y renderizar lista lateral
function calcularTotales(pasajeros) {
    subtotalGeneral = pasajeros.reduce((sum, p) => sum + (p.precio || 0), 0);

    const contenedorDetalles = document.getElementById('desglosePasajerosSide');
    let html = '';
    let nombresTexto = [];

    pasajeros.forEach(p => {
        nombresTexto.push(`${p.nombre} ${p.apellido} (Asiento ${p.idAsiento})`);
        html += `
            <div class="d-flex justify-content-between text-muted small mb-2">
                <span>${p.nombre} ${p.apellido}</span>
                <span class="fw-bold text-dark">S/ ${(p.precio || 0).toFixed(2)}</span>
            </div>
        `;
    });

    if (contenedorDetalles) {
        contenedorDetalles.innerHTML = html;
    }

    const resumenNombres = document.getElementById('resumenNombresPasajeros');
    if (resumenNombres) {
        resumenNombres.innerText = nombresTexto.join(', ');
    }

    actualizarTotalEnPantalla();
}

function actualizarTotalEnPantalla() {
    const totalPagar = Math.max(0, subtotalGeneral - descuentoAplicado);
    const totalLateral = document.getElementById('totalLateralPagar');
    const btnMonto = document.getElementById('btnMontoPagar');

    if (totalLateral) totalLateral.innerText = `S/ ${totalPagar.toFixed(2)}`;
    if (btnMonto) btnMonto.innerText = `S/ ${totalPagar.toFixed(2)}`;
}

// Cambiar método de pago
function seleccionarMetodo(metodo, elemento) {
    metodoSeleccionado = metodo;
    document.querySelectorAll('.payment-method-box').forEach(b => b.classList.remove('active'));
    elemento.classList.add('active');

    const contenedor = document.getElementById('contenedorDetallePago');
    if (!contenedor) return;

    if (metodo === 'TARJETA') {
        contenedor.innerHTML = `
            <div class="alert alert-info border-0 rounded-3 small mb-3">
                <i class="bi bi-info-circle-fill"></i> ¡Recuerda habilitar las compras por internet en tu app bancaria!
            </div>
            <div class="row g-3">
                <div class="col-12"><label class="form-label small fw-bold text-muted">Número de la tarjeta</label><input type="text" class="form-control rounded-3" placeholder="•••• •••• •••• ••••" maxlength="19"></div>
                <div class="col-6"><label class="form-label small fw-bold text-muted">Fecha MM/AA</label><input type="text" class="form-control rounded-3" placeholder="MM/AA" maxlength="5"></div>
                <div class="col-6"><label class="form-label small fw-bold text-muted">CVV</label><input type="password" class="form-control rounded-3" placeholder="•••" maxlength="4"></div>
                <div class="col-12"><label class="form-label small fw-bold text-muted">Nombre del titular</label><input type="text" class="form-control rounded-3" placeholder="Como figura en la tarjeta"></div>
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
                <p class="text-muted small mb-0">Acércate a un agente autorizado o ingresa a tu banca por internet y selecciona Pago de Servicios > PagoEfectivo.</p>
            </div>`;
    }
}

// Cupón de descuento
function aplicarPromo() {
    const inputCodigo = document.getElementById('inputCodigoPromo');
    const mensaje = document.getElementById('mensajePromo');
    const filaDescuento = document.getElementById('filaDescuento');
    const montoDescuentoEl = document.getElementById('montoDescuento');

    if (!inputCodigo) return;
    const codigo = inputCodigo.value.trim().toUpperCase();

    if (codigo === 'PROMO10' || codigo === 'URBAN2026') {
        descuentoAplicado = 15.00;
        mensaje.className = "small mt-2 text-success fw-bold";
        mensaje.innerText = "¡Cupón aplicado exitosamente! (- S/ 15.00)";
        filaDescuento.classList.remove('d-none');
        montoDescuentoEl.innerText = `- S/ ${descuentoAplicado.toFixed(2)}`;
    } else {
        descuentoAplicado = 0;
        mensaje.className = "small mt-2 text-danger";
        mensaje.innerText = "Código promocional no válido o expirado.";
        filaDescuento.classList.add('d-none');
    }

    actualizarTotalEnPantalla();
}

// Temporizador de 15 minutos sincronizado con sessionStorage
function iniciarTemporizadorSincronizado() {
    let expiracion = sessionStorage.getItem('limiteTiempoCompra');
    if (!expiracion) {
        expiracion = Date.now() + 15 * 60 * 1000;
        sessionStorage.setItem('limiteTiempoCompra', expiracion.toString());
    } else {
        expiracion = parseInt(expiracion);
    }

    const reloj = document.getElementById('relojTemporizador');

    function tick() {
        const ahora = Date.now();
        const restante = expiracion - ahora;

        if (restante <= 0) {
            clearInterval(cronometroInterval);
            if (reloj) reloj.innerText = "00:00";
            const modal = new bootstrap.Modal(document.getElementById('modalExpirado'));
            modal.show();
            return;
        }

        const m = Math.floor(restante / (1000 * 60));
        const s = Math.floor((restante % (1000 * 60)) / 1000);
        if (reloj) {
            reloj.innerText = `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
        }
    }

    tick();
    cronometroInterval = setInterval(tick, 1000);
}

// Redirigir al inicio limpiando variables de compra
function redirigirInicio() {
    sessionStorage.removeItem('asientosComprando');
    sessionStorage.removeItem('limiteTiempoCompra');
    sessionStorage.removeItem('viajeSeleccionadoId');
    sessionStorage.removeItem('datosPasajerosComprando');
    window.location.href = "index.html";
}

// Procesar el pago y guardar datos para la boleta
function procesarPagoFinal() {
    const btn = document.getElementById('btnFinalizarCompra');
    btn.disabled = true;
    btn.innerHTML = `<span class="spinner-border spinner-border-sm"></span> Procesando pago seguro...`;

    const totalPagar = Math.max(0, subtotalGeneral - descuentoAplicado);
    const pasajeros = JSON.parse(sessionStorage.getItem('datosPasajerosComprando')) || [];
    const esFactura = document.getElementById('checkFactura')?.checked || false;
    const correoComprador = document.getElementById('comp-correo')?.value || 'usuario@correo.com';
    const codigoVenta = 'UB-' + Math.floor(100000 + Math.random() * 900000);

    setTimeout(() => {
        btn.disabled = false;
        btn.innerHTML = `Finalizar compra por S/ ${totalPagar.toFixed(2)} <i class="bi bi-chevron-double-right"></i>`;

        // Datos completos que leerá boleta.html
        const ventaEmitida = {
            codigoReserva: codigoVenta,
            tipoComprobante: esFactura ? 'FACTURA ELECTRÓNICA' : 'BOLETA DE VENTA ELECTRÓNICA',
            serieNumero: (esFactura ? 'F001-' : 'B001-') + Math.floor(10000 + Math.random() * 90000),
            fechaEmision: new Date().toLocaleString('es-PE'),
            metodoPago: metodoSeleccionado,
            comprador: {
                nombre: (document.getElementById('comp-nombre')?.value || '') + ' ' + (document.getElementById('comp-apePaterno')?.value || ''),
                documento: pasajeros[0]?.numeroDocumento || '00000000',
                tipoDocumento: pasajeros[0]?.tipoDocumento || 'DNI',
                correo: correoComprador,
                telefono: document.getElementById('comp-telefono')?.value || ''
            },
            pasajeros: pasajeros,
            subtotal: subtotalGeneral,
            descuento: descuentoAplicado,
            total: totalPagar
        };

        // Guardar para el visualizador PDF de la boleta
        localStorage.setItem('ultimaVentaUrbanRide', JSON.stringify(ventaEmitida));

        // Resumen dentro del modal de éxito
        const voucherEl = document.getElementById('voucherResumen');
        if (voucherEl) {
            voucherEl.innerHTML = `
                <div class="mb-1"><strong>Código de Reserva:</strong> ${codigoVenta}</div>
                <div class="mb-1"><strong>Comprobante:</strong> ${ventaEmitida.tipoComprobante}</div>
                <div class="mb-1"><strong>Método de pago:</strong> ${metodoSeleccionado}</div>
                <div class="mb-1"><strong>Pasajeros:</strong> ${pasajeros.length}</div>
                <div class="mb-1"><strong>Monto Total Pagado:</strong> S/ ${totalPagar.toFixed(2)}</div>
                <div class="mt-2 text-muted fst-italic">Se ha enviado un correo con los boletos electrónicos en PDF a ${correoComprador}.</div>
            `;
        }

        clearInterval(cronometroInterval);
        const modal = new bootstrap.Modal(document.getElementById('modalExito'));
        modal.show();
    }, 1500);
}

// Abrir boleta en una nueva pestaña
function abrirBoletaPDF() {
    window.open('boleta.html', '_blank');
}