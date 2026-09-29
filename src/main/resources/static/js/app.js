// URL base del backend en Spring Boot
const API_BASE_URL = 'http://127.0.0.1:8080/api/usuarios';

// 1. EVENTO QUE SE EJECUTA AL CARGAR LA PÁGINA
document.addEventListener("DOMContentLoaded", function () {
    const selectOrigen = document.getElementById('selectOrigen');
    const selectDestino = document.getElementById('selectDestino');

    // 10 departamentos disponibles para ida y vuelta
    const departamentos = [
        'Lima',
        'Ica',
        'Huancayo',
        'Arequipa',
        'Trujillo',
        'Chimbote',
        'Chiclayo',
        'Piura',
        'Tacna',
        'Cusco'
    ];

    if (selectOrigen && selectDestino) {
        selectOrigen.innerHTML = '<option value="" selected disabled>Selecciona Origen</option>';
        selectDestino.innerHTML = '<option value="" selected disabled>Selecciona Destino</option>';

        departamentos.forEach(dep => {
            selectOrigen.innerHTML += `<option value="${dep}">${dep}</option>`;
            selectDestino.innerHTML += `<option value="${dep}">${dep}</option>`;
        });
    }

    // Verificar si ya hay sesión iniciada para mostrar el nombre en el navbar
    comprobarEstadoSesionNav();

    // Inicializar listeners de formularios
    inicializarAuth();
});

// 2. FUNCIÓN PARA COMPROBAR SESIÓN EN LA BARRA DE NAVEGACIÓN
function comprobarEstadoSesionNav() {
    const contenedorNav = document.getElementById('contenedorUsuarioNav');
    const usuarioRaw = localStorage.getItem('usuarioUrbanRide');

    if (contenedorNav && usuarioRaw) {
        const usuario = JSON.parse(usuarioRaw);
        contenedorNav.innerHTML = `
            <div class="dropdown">
                <button class="btn btn-warning btn-sm px-3 rounded-pill fw-bold dropdown-toggle text-dark" type="button" data-bs-toggle="dropdown" aria-expanded="false">
                    <i class="bi bi-person-circle"></i> ${usuario.nombre}
                </button>
                <ul class="dropdown-menu dropdown-menu-end shadow-sm">
                    <li><a class="dropdown-item small" href="#" onclick="cerrarSesionUsuario()"><i class="bi bi-box-arrow-right text-danger"></i> Cerrar sesión</a></li>
                </ul>
            </div>
        `;
    }
}

// 3. CERRAR SESIÓN
function cerrarSesionUsuario() {
    localStorage.removeItem('usuarioUrbanRide');
    sessionStorage.removeItem('viajeSeleccionadoId');
    window.location.reload();
}

// 4. FUNCIÓN PARA LOS BOTONES DE LAS TARJETAS (Ica, Huancayo, Arequipa)
function seleccionarDestinoRapido(ciudadDestino) {
    const selectOrigen = document.getElementById('selectOrigen');
    const selectDestino = document.getElementById('selectDestino');
    const fechaViaje = document.getElementById('fechaViaje');

    const ciudadBuscada = ciudadDestino.toLowerCase();

    for (let i = 0; i < selectOrigen.options.length; i++) {
        if (selectOrigen.options[i].text.toLowerCase().includes('lima')) {
            selectOrigen.selectedIndex = i;
            break;
        }
    }

    for (let i = 0; i < selectDestino.options.length; i++) {
        if (selectDestino.options[i].text.toLowerCase().includes(ciudadBuscada)) {
            selectDestino.selectedIndex = i;
            break;
        }
    }

    if (!fechaViaje.value) {
        const hoy = new Date();
        const offset = hoy.getTimezoneOffset();
        const fechaLocal = new Date(hoy.getTime() - (offset * 60 * 1000));
        fechaViaje.value = fechaLocal.toISOString().split('T')[0];
    }

    window.scrollTo({ top: 0, behavior: 'smooth' });
}

// 5. BÚSQUEDA Y RENDERIZADO DE RESULTADOS DE VIAJE
const formBusquedaViaje = document.getElementById('formBusquedaViaje');
if (formBusquedaViaje) {
    formBusquedaViaje.addEventListener('submit', function (e) {
        e.preventDefault();

        const origen = document.getElementById('selectOrigen').value;
        const destino = document.getElementById('selectDestino').value;
        const fecha = document.getElementById('fechaViaje').value;

        if (origen === destino) {
            alert("El origen y el destino no pueden ser el mismo.");
            return;
        }

        const seccionResultados = document.getElementById('seccionResultados');
        const listaResultados = document.getElementById('listaResultados');

        // Mostrar sección y un spinner de carga
        seccionResultados.classList.remove('d-none');
        listaResultados.innerHTML = `
            <div class="col-12 text-center py-5">
                <div class="spinner-border text-warning" role="status"></div>
                <p class="mt-2 text-muted fw-bold">Buscando salidas disponibles...</p>
            </div>
        `;

        seccionResultados.scrollIntoView({ behavior: 'smooth' });

        // Simulación de viajes disponibles
        setTimeout(() => {
            const salidas = [
                { id: 1, servicio: "Suite 160°", salida: "07:45", llegada: "12:15", duracion: "4 hrs 30 mins", precio: 45.00, terminalOrigen: origen, terminalDestino: destino },
                { id: 2, servicio: "Confort 180°", salida: "13:30", llegada: "18:00", duracion: "4 hrs 30 mins", precio: 55.00, terminalOrigen: origen, terminalDestino: destino },
                { id: 3, servicio: "Suite 160°", salida: "21:30", llegada: "02:00", duracion: "4 hrs 30 mins", precio: 45.00, terminalOrigen: origen, terminalDestino: destino }
            ];

            let html = `
                <div class="col-12 mb-3">
                    <h4 class="fw-bold mb-1">Seleccionar horario de ida <span class="text-muted fs-6">(${fecha})</span></h4>
                    <p class="text-muted small mb-0">Precios por persona en servicio seleccionado</p>
                </div>
            `;

            salidas.forEach(v => {
                html += `
                <div class="col-12 mb-3">
                    <div class="card border-0 shadow-sm rounded-4 p-4">
                        <div class="row align-items-center text-center text-md-start gy-3">
                            <div class="col-12 col-md-2">
                                <span class="fw-bold text-uppercase d-block text-primary">${v.servicio}</span>
                                <small class="text-muted"><i class="bi bi-wifi"></i> <i class="bi bi-plug"></i> USB</small>
                            </div>

                            <div class="col-12 col-md-6">
                                <div class="d-flex align-items-center justify-content-center justify-content-md-start gap-3">
                                    <div>
                                        <div class="fs-4 fw-bold text-dark">${v.salida}</div>
                                        <div class="text-muted small">${v.terminalOrigen}</div>
                                    </div>
                                    <div class="text-center px-2 flex-grow-1" style="max-width: 140px;">
                                        <small class="text-muted d-block">${v.duracion}</small>
                                        <hr class="my-1 border-secondary border-dashed">
                                        <small class="text-success fw-bold">Directo</small>
                                    </div>
                                    <div>
                                        <div class="fs-4 fw-bold text-dark">${v.llegada}</div>
                                        <div class="text-muted small">${v.terminalDestino}</div>
                                    </div>
                                </div>
                            </div>

                            <div class="col-12 col-md-4 text-center text-md-end">
                                <div class="mb-1 text-muted small">Desde: <span class="fs-4 fw-bold text-primary">S/ ${v.precio.toFixed(2)}</span></div>
                                <button class="btn btn-outline-warning text-dark fw-bold rounded-pill px-4 shadow-sm" onclick="accionSeleccionarAsiento(${v.id})">
                                    Ver asientos
                                </button>
                            </div>
                        </div>
                    </div>
                </div>`;
            });

            listaResultados.innerHTML = html;
        }, 500);
    });
}

// 6. ACCIÓN AL PULSAR "VER ASIENTOS" (COMPROBAR AUTENTICACIÓN)
function accionSeleccionarAsiento(idViaje) {
    const usuarioSesion = localStorage.getItem('usuarioUrbanRide');

    // Guardar el viaje en sesión
    sessionStorage.setItem('viajeSeleccionadoId', idViaje);

    if (!usuarioSesion) {
        // No está logueado: abre modal de login/registro
        const modalAuth = new bootstrap.Modal(document.getElementById('loginModal'));
        modalAuth.show();
    } else {
        // Ya está autenticado: redirige a la vista del bus
        window.location.href = 'asientos.html';
    }
}

// 7. INTEGRACIÓN DE AUTENTICACIÓN Y PERSISTENCIA (SPRING BOOT -> POSTGRESQL)
function inicializarAuth() {
    const formRegistro = document.getElementById('formRegistro');
    const formLogin = document.getElementById('formLogin');

    // A. REGISTRO DE USUARIO
    if (formRegistro) {
        formRegistro.addEventListener('submit', async function (e) {
            e.preventDefault();

            const nombre = document.getElementById('reg-nombre').value.trim();
            const apellido = document.getElementById('reg-apellido').value.trim();
            const numeroDocumento = document.getElementById('reg-documento').value.trim();
            const correo = document.getElementById('reg-correo').value.trim();
            const password = document.getElementById('reg-password').value;
            const passwordConfirm = document.getElementById('reg-password-confirm').value;

            if (password !== passwordConfirm) {
                alert("Las contraseñas no coinciden. Por favor, revísalas.");
                return;
            }

            const nuevoUsuario = {
                nombre: nombre,
                apellido: apellido,
                tipoDocumento: "DNI",
                numeroDocumento: numeroDocumento,
                correo: correo,
                contrasenaHash: password,
                rol: "CLIENTE",
                activo: true
            };

            const btnSubmit = document.getElementById('btnSubmitRegistro');
            btnSubmit.disabled = true;
            btnSubmit.innerText = "Registrando...";

            try {
                const response = await fetch(`${API_BASE_URL}/registro`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Accept': 'application/json'
                    },
                    body: JSON.stringify(nuevoUsuario)
                });

                const data = await response.json();

                if (!response.ok) {
                    throw new Error(data.mensaje || 'Error al procesar el registro.');
                }

                alert(data.mensaje || '¡Usuario registrado exitosamente en la base de datos!');
                formRegistro.reset();

                // Cambiar a la pestaña de login
                if (typeof cambiarPestañaModal === 'function') {
                    cambiarPestañaModal('login');
                }

                // Autocompletar el correo en el formulario de login
                const loginCorreoInput = document.getElementById('login-correo');
                if (loginCorreoInput) {
                    loginCorreoInput.value = correo;
                }

            } catch (err) {
                console.error("Error en registro:", err);
                alert(err.message);
            } finally {
                btnSubmit.disabled = false;
                btnSubmit.innerText = "Crear mi cuenta";
            }
        });
    }

    // B. INICIO DE SESIÓN
    if (formLogin) {
        formLogin.addEventListener('submit', async function (e) {
            e.preventDefault();

            const correo = document.getElementById('login-correo').value.trim();
            const contrasena = document.getElementById('login-password').value;

            const credenciales = {
                correo: correo,
                contrasenaHash: contrasena
            };

            const btnSubmit = document.getElementById('btnSubmitLogin');
            btnSubmit.disabled = true;
            btnSubmit.innerText = "Verificando...";

            try {
                const response = await fetch(`${API_BASE_URL}/login`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Accept': 'application/json'
                    },
                    body: JSON.stringify(credenciales)
                });

                const data = await response.json();

                if (!response.ok) {
                    throw new Error(data.mensaje || 'Credenciales incorrectas.');
                }

                // Guardar la sesión del usuario
                localStorage.setItem('usuarioUrbanRide', JSON.stringify({
                    nombre: data.nombre,
                    correo: correo
                }));

                alert(`¡Bienvenido, ${data.nombre || 'Usuario'}!`);
                formLogin.reset();

                // Cerrar modal
                const modalEl = document.getElementById('loginModal');
                const modalInstance = bootstrap.Modal.getInstance(modalEl);
                if (modalInstance) {
                    modalInstance.hide();
                }

                // Si venía de pulsar "Ver asientos", redirigir directamente a la pantalla de asientos
                const idViajePendiente = sessionStorage.getItem('viajeSeleccionadoId');
                if (idViajePendiente) {
                    window.location.href = 'asientos.html';
                } else {
                    comprobarEstadoSesionNav();
                }

            } catch (err) {
                console.error("Error en login:", err);
                alert(err.message);
            } finally {
                btnSubmit.disabled = false;
                btnSubmit.innerText = "Ingresar al Sistema";
            }
        });
    }
}