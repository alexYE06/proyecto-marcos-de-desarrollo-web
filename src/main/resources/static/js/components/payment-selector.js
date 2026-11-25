// Componente reutilizable de Métodos de Pago (Web Cliente + POS Ventanilla con calculadora de vuelto)

export function inicializarSelectorPago({
    contenedorOpciones,
    contenedorDetalle,
    modoVentanilla = false,
    metodoInicial = 'TARJETA',
    obtenerMontoActual = () => 0,
    onCambioMetodo = () => {}
}) {
    let metodoActual = metodoInicial;

    function renderizarDetalle(metodo) {
        if (!contenedorDetalle) return;

        if (metodo === 'TARJETA') {
            contenedorDetalle.innerHTML = modoVentanilla
                ? `<div class="p-3 bg-light rounded-3 border small">
                        <div class="fw-bold text-dark mb-2"><i class="bi bi-credit-card-2-front text-primary"></i> Cobro con POS físico (Visa / Mastercard)</div>
                        <div class="row g-2">
                            <div class="col-md-6">
                                <label class="form-label small text-muted mb-1">N° Voucher / Operación POS (opcional)</label>
                                <input type="text" class="form-control form-control-sm rounded-3" placeholder="Ej. 004819" maxlength="20">
                            </div>
                            <div class="col-md-6">
                                <label class="form-label small text-muted mb-1">Últimos 4 dígitos</label>
                                <input type="text" class="form-control form-control-sm rounded-3" placeholder="4242" maxlength="4">
                            </div>
                        </div>
                   </div>`
                : `<div class="alert alert-info border-0 rounded-3 small mb-3">
                        <i class="bi bi-info-circle-fill"></i> ¡Recuerda habilitar las compras por internet en tu app bancaria!
                   </div>
                   <div class="row g-3">
                        <div class="col-12">
                            <label class="form-label small fw-bold text-muted">Número de la tarjeta</label>
                            <input type="text" class="form-control rounded-3" id="tarjetaNum" placeholder="•••• •••• •••• ••••" maxlength="19" autocomplete="off">
                        </div>
                        <div class="col-6">
                            <label class="form-label small fw-bold text-muted">Fecha MM/AA</label>
                            <input type="text" class="form-control rounded-3" id="tarjetaExp" placeholder="MM/AA" maxlength="5" autocomplete="off">
                        </div>
                        <div class="col-6">
                            <label class="form-label small fw-bold text-muted">CVV</label>
                            <input type="password" class="form-control rounded-3" id="tarjetaCvv" placeholder="•••" maxlength="4" autocomplete="off">
                        </div>
                        <div class="col-12">
                            <label class="form-label small fw-bold text-muted">Nombre del titular</label>
                            <input type="text" class="form-control rounded-3" id="tarjetaTitular" placeholder="Como figura en la tarjeta" autocomplete="off">
                        </div>
                   </div>`;
        } else if (metodo === 'YAPE') {
            contenedorDetalle.innerHTML = `
                <div class="text-center p-3 bg-light rounded-3 border">
                    <p class="small text-muted mb-2">Escanea el QR oficial de Yape Empresas o verifica la operación en caja:</p>
                    <div class="bg-white p-2 d-inline-block rounded-3 shadow-sm border mb-2">
                        <i class="bi bi-qr-code" style="font-size: 5rem; color: #720e9e;"></i>
                    </div>
                    <div class="fw-bold" style="color: #720e9e;">Número oficial: 999 888 777</div>
                    <small class="text-muted">Titular: UrbanRide Perú S.A.C.</small>
                </div>`;
        } else if (metodo === 'PAYPAL') {
            contenedorDetalle.innerHTML = `
                <div class="text-center py-3 bg-light rounded-3 border">
                    <p class="text-muted small mb-2">Cobro internacional respaldado por PayPal Checkout.</p>
                    <span class="badge bg-primary-subtle text-primary px-3 py-2 rounded-pill"><i class="bi bi-paypal"></i> Cuenta verificada: pagos@urbanride.pe</span>
                </div>`;
        } else if (metodo === 'EFECTIVO') {
            if (modoVentanilla) {
                const total = Number(obtenerMontoActual()) || 0;
                contenedorDetalle.innerHTML = `
                    <div class="p-3 bg-success bg-opacity-10 rounded-3 border border-success border-opacity-25">
                        <div class="fw-bold small text-success mb-2"><i class="bi bi-cash-coin"></i> Cobro en Efectivo — Calculadora de Caja</div>
                        <div class="row g-2 align-items-end">
                            <div class="col-6">
                                <label class="form-label small fw-bold text-muted mb-1">Efectivo recibido (S/)</label>
                                <input type="number" step="0.50" min="0" class="form-control form-control-sm rounded-3" id="inputEfectivoRecibido" placeholder="${total.toFixed(2)}">
                            </div>
                            <div class="col-6">
                                <div class="small text-muted">Vuelto a entregar:</div>
                                <div class="fs-5 fw-bold text-success" id="textoVueltoCaja">S/ 0.00</div>
                            </div>
                        </div>
                    </div>`;
                const inputEf = contenedorDetalle.querySelector('#inputEfectivoRecibido');
                const txtVuelto = contenedorDetalle.querySelector('#textoVueltoCaja');
                if (inputEf && txtVuelto) {
                    inputEf.addEventListener('input', () => {
                        const recibido = parseFloat(inputEf.value) || 0;
                        const montoActual = Number(obtenerMontoActual()) || 0;
                        const diff = recibido - montoActual;
                        txtVuelto.textContent = diff >= 0 ? `S/ ${diff.toFixed(2)}` : `Faltan S/ ${Math.abs(diff).toFixed(2)}`;
                        txtVuelto.className = diff >= 0 ? 'fs-5 fw-bold text-success' : 'fs-6 fw-bold text-danger';
                    });
                }
            } else {
                contenedorDetalle.innerHTML = `
                    <div class="p-3 bg-light rounded-3 border">
                        <div class="fw-bold small text-dark mb-1"><i class="bi bi-receipt"></i> PagoEfectivo — Banca Móvil y Agentes</div>
                        <p class="text-muted small mb-0">Al finalizar se confirmará tu orden con código CIP autorizado en agentes BCP, BBVA, Interbank y KasNet.</p>
                    </div>`;
            }
        }
    }

    function seleccionar(metodo) {
        metodoActual = metodo;
        if (contenedorOpciones) {
            contenedorOpciones.querySelectorAll('[data-metodo]').forEach(box => {
                box.classList.toggle('active', box.dataset.metodo === metodo);
            });
        }
        renderizarDetalle(metodo);
        onCambioMetodo(metodo);
    }

    if (contenedorOpciones) {
        contenedorOpciones.querySelectorAll('[data-metodo]').forEach(box => {
            box.addEventListener('click', () => seleccionar(box.dataset.metodo));
        });
    }

    seleccionar(metodoInicial);

    return {
        obtenerMetodo: () => metodoActual,
        refrescarMonto: () => {
            if (metodoActual === 'EFECTIVO' && modoVentanilla) {
                renderizarDetalle('EFECTIVO');
            }
        }
    };
}
