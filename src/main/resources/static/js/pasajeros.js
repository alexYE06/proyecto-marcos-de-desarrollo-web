let intervalId = null;

document.addEventListener("DOMContentLoaded", () => {
    // 1. Validar sesión
    const usuarioRaw = localStorage.getItem('usuarioUrbanRide');
    if (!usuarioRaw) {
        alert("Sesión no iniciada. Redirigiendo...");
        window.location.href = "index.html";
        return;
    }
    const usuario = JSON.parse(usuarioRaw);
    document.getElementById('usuarioHeader').innerText = usuario.nombre;

    // 2. Obtener asientos seleccionados
    const asientosRaw = sessionStorage.getItem('asientosComprando');
    if (!asientosRaw) {
        alert("No has seleccionado asientos.");
        window.location.href = "asientos.html";
        return;
    }

    const asientos = JSON.parse(asientosRaw);
    document.getElementById('totalPasajerosBadge').innerText = `${asientos.length} a bordo`;

    // 3. Renderizar formulario por cada asiento y desglose de precios
    renderizarFormularios(asientos, usuario);
    renderizarDesglose(asientos);

    // 4. Iniciar o reanudar el cronómetro de 15 minutos
    iniciarTemporizador();

    // 5. Listener para guardar datos e ir a pago
    const form = document.getElementById('formPasajeros');
    form.addEventListener('submit', procesarDatosPasajeros);
});

// Renderizar un bloque de formulario por cada asiento
function renderizarFormularios(asientos, usuario) {
    const contenedor = document.getElementById('contenedorFormulariosPasajeros');
    contenedor.innerHTML = '';

    asientos.forEach((asiento, index) => {
        const esPrimerPasajero = (index === 0);
        
        // Autocompletar el primer pasajero con el usuario autenticado si aplica
        const nombresDefault = esPrimerPasajero ? usuario.nombre : '';

        const tarjeta = document.createElement('div');
        tarjeta.className = 'card-pasajero p-4 mb-4';
        tarjeta.innerHTML = `
            <div class="d-flex align-items-center justify-content-between mb-3 border-bottom pb-2">
                <div class="d-flex align-items-center gap-2">
                    <span class="badge-asiento">Asiento ${asiento.num}</span>
                    <span class="fw-bold text-dark">Pasajero ${index + 1}</span>
                </div>
                <span class="text-muted small">Tarifa: <strong>S/ ${asiento.precio.toFixed(2)}</strong></span>
            </div>

            <div class="row g-3">
                <div class="col-12 col-md-4">
                    <label class="form-label small fw-bold text-muted">Tipo Documento</label>
                    <select class="form-select bg-white rounded-3" name="tipoDoc_${asiento.num}" required>
                        <option value="DNI" selected>DNI</option>
                        <option value="PASAPORTE">Pasaporte</option>
                        <option value="CE">Carnet de Extranjería</option>
                    </select>
                </div>
                <div class="col-12 col-md-8">
                    <label class="form-label small fw-bold text-muted">Número de Documento</label>
                    <input type="text" class="form-control bg-white rounded-3" name="numDoc_${asiento.num}" placeholder="Ej. 72481923" maxlength="20" required>
                </div>

                <div class="col-12 col-md-6">
                    <label class="form-label small fw-bold text-muted">Nombres</label>
                    <input type="text" class="form-control bg-white rounded-3" name="nombres_${asiento.num}" value="${nombresDefault}" placeholder="Nombres completos" required>
                </div>
                <div class="col-12 col-md-6">
                    <label class="form-label small fw-bold text-muted">Apellidos</label>
                    <input type="text" class="form-control bg-white rounded-3" name="apellidos_${asiento.num}" placeholder="Apellidos completos" required>
                </div>

                <div class="col-12 col-md-6">
                    <label class="form-label small fw-bold text-muted">Correo de contacto / Envío de boleto</label>
                    <input type="email" class="form-control bg-white rounded-3" name="correo_${asiento.num}" value="${esPrimerPasajero ? (usuario.correo || '') : ''}" placeholder="ejemplo@correo.com" required>
                </div>
                <div class="col-12 col-md-6">
                    <label class="form-label small fw-bold text-muted">Teléfono / WhatsApp</label>
                    <input type="tel" class="form-control bg-white rounded-3" name="telefono_${asiento.num}" placeholder="999888777" maxlength="15">
                </div>
            </div>
        `;
        contenedor.appendChild(tarjeta);
    });
}

// Renderizar resumen de tarifas en la columna derecha
function renderizarDesglose(asientos) {
    const desglose = document.getElementById('desglosePrecios');
    const totalEl = document.getElementById('montoTotalPasajeros');

    let total = 0;
    let html = '';

    asientos.forEach((a, i) => {
        total += a.precio;
        html += `
            <div class="d-flex justify-content-between small text-muted mb-2">
                <span>Pasajero ${i + 1} (Asiento ${a.num})</span>
                <span class="fw-bold text-dark">S/ ${a.precio.toFixed(2)}</span>
            </div>
        `;
    });

    desglose.innerHTML = html;
    totalEl.innerText = `S/ ${total.toFixed(2)}`;
}

// Temporizador de 15 minutos (900 segundos)
function iniciarTemporizador() {
    let expiracion = sessionStorage.getItem('limiteTiempoCompra');

    if (!expiracion) {
        expiracion = Date.now() + 15 * 60 * 1000;
        sessionStorage.setItem('limiteTiempoCompra', expiracion.toString());
    } else {
        expiracion = parseInt(expiracion);
    }

    const reloj = document.getElementById('relojTemporizador');

    function actualizarReloj() {
        const ahora = Date.now();
        const restanteMs = expiracion - ahora;

        if (restanteMs <= 0) {
            clearInterval(intervalId);
            reloj.innerText = "00:00";
            
            // Disparar modal de expiración
            const modal = new bootstrap.Modal(document.getElementById('modalExpirado'));
            modal.show();
            return;
        }

        const minutos = Math.floor(restanteMs / (1000 * 60));
        const segundos = Math.floor((restanteMs % (1000 * 60)) / 1000);

        reloj.innerText = `${minutos.toString().padStart(2, '0')}:${segundos.toString().padStart(2, '0')}`;
    }

    actualizarReloj();
    intervalId = setInterval(actualizarReloj, 1000);
}

// Botón de salida si expira la sesión
function redirigirInicio() {
    sessionStorage.removeItem('asientosComprando');
    sessionStorage.removeItem('limiteTiempoCompra');
    sessionStorage.removeItem('viajeSeleccionadoId');
    window.location.href = "index.html";
}

// Procesar formulario y pasar al Paso 3 (Pago)
function procesarDatosPasajeros(e) {
    e.preventDefault();

    const asientos = JSON.parse(sessionStorage.getItem('asientosComprando'));
    const listaPasajeros = [];

    asientos.forEach(a => {
        const tipoDoc = document.querySelector(`[name="tipoDoc_${a.num}"]`).value;
        const numDoc = document.querySelector(`[name="numDoc_${a.num}"]`).value.trim();
        const nombres = document.querySelector(`[name="nombres_${a.num}"]`).value.trim();
        const apellidos = document.querySelector(`[name="apellidos_${a.num}"]`).value.trim();
        const correo = document.querySelector(`[name="correo_${a.num}"]`).value.trim();
        const telefono = document.querySelector(`[name="telefono_${a.num}"]`).value.trim();

        listaPasajeros.push({
            idAsiento: a.num,
            precio: a.precio,
            tipoDocumento: tipoDoc,
            numeroDocumento: numDoc,
            nombre: nombres,
            apellido: apellidos,
            correo: correo,
            telefono: telefono
        });
    });

    // Guardar para el módulo final de pago
    sessionStorage.setItem('datosPasajerosComprando', JSON.stringify(listaPasajeros));
    
    alert("¡Datos guardados con éxito! Pasando a la pasarela de pago...");
    window.location.href = 'pago.html'; // Próximo paso
}