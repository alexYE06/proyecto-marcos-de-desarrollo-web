import { apiGet, apiPost } from './core/api.js';
import { obtenerSesion, guardarSesion, limpiarSesion, urlPanelPorRol, etiquetaCargo } from './core/session.js';
import { escaparHtml, moneda, formatearFechaISO } from './core/formatters.js';

document.addEventListener('DOMContentLoaded', async () => {
    // Activar todos los Popovers de Bootstrap 5 presentes en la vista
    document.querySelectorAll('[data-bs-toggle="popover"]').forEach(el => new bootstrap.Popover(el));

    renderizarEstadoNav();
    configurarModalAuth();
    configurarBotonesDestinoRapido();
    configurarBuscador();

    // Si Thymeleaf ya renderizó los terminales desde el servidor, no hace falta recargarlos
    const selectOrigen = document.getElementById('selectOrigen');
    if (selectOrigen && selectOrigen.options.length <= 1) {
        await cargarTerminalesReales();
    }
    await cargarSalidas('', '', '');

    if (new URLSearchParams(window.location.search).get('login') === '1' && !obtenerSesion()) {
        bootstrap.Modal.getOrCreateInstance(document.getElementById('loginModal')).show();
    }
});

function renderizarEstadoNav() {
    const contenedorUsuario = document.getElementById('contenedorUsuarioNav');
    const navItemPanel = document.getElementById('navItemPanelRol');
    const sesion = obtenerSesion();

    if (!sesion) {
        if (navItemPanel) navItemPanel.innerHTML = '';
        return;
    }

    const rol = (sesion.rol || 'CLIENTE').toUpperCase();
    const destinoPanel = urlPanelPorRol(rol);

    const etiquetaBotonPanel = {
        ADMIN: '<i class="bi bi-speedometer2"></i> Panel Administrador',
        VENDEDOR: '<i class="bi bi-pc-display-horizontal"></i> Ventanilla POS',
        EMBARQUE: '<i class="bi bi-qr-code-scan"></i> Control Embarque',
        CLIENTE: '<i class="bi bi-ticket-detailed"></i> Mis Viajes'
    }[rol] || 'Mi Panel';

    if (navItemPanel) {
        navItemPanel.innerHTML = `
            <a class="btn btn-outline-warning btn-sm rounded-pill px-3 fw-bold" href="${destinoPanel}">
                ${etiquetaBotonPanel}
            </a>`;
    }

    if (contenedorUsuario) {
        contenedorUsuario.innerHTML = `
            <div class="dropdown">
                <button class="btn btn-warning btn-sm px-3 rounded-pill fw-bold dropdown-toggle text-dark" type="button" data-bs-toggle="dropdown">
                    <span class="badge bg-dark text-warning me-1">${escaparHtml(rol)}</span>
                    ${escaparHtml(sesion.nombre || sesion.nombreCompleto)}
                </button>
                <ul class="dropdown-menu dropdown-menu-end shadow">
                    <li><span class="dropdown-item-text small text-muted">${escaparHtml(etiquetaCargo(rol))}</span></li>
                    <li><hr class="dropdown-divider"></li>
                    <li><a class="dropdown-item small fw-semibold" href="${destinoPanel}"><i class="bi bi-grid-1x2 text-primary"></i> Ir a mi módulo</a></li>
                    <li><a class="dropdown-item small fw-semibold" href="mis-viajes.html"><i class="bi bi-person-gear text-secondary"></i> Mi perfil y compras</a></li>
                    <li><hr class="dropdown-divider"></li>
                    <li><button type="button" class="dropdown-item small text-danger fw-bold" id="btnLogoutNav"><i class="bi bi-box-arrow-right"></i> Cerrar sesión</button></li>
                </ul>
            </div>`;
        document.getElementById('btnLogoutNav')?.addEventListener('click', async () => {
            try { await apiPost('/auth/logout'); } catch {}
            limpiarSesion();
            window.location.href = 'index.html';
        });
    }
}

async function cargarTerminalesReales() {
    const selectOrigen = document.getElementById('selectOrigen');
    const selectDestino = document.getElementById('selectDestino');
    if (!selectOrigen || !selectDestino) return;

    try {
        const terminales = await apiGet('/catalogo/terminales');
        const ciudadesUnicas = [...new Map(terminales.map(t => [t.ciudad, t])).values()];

        selectOrigen.innerHTML = '<option value="" selected disabled>Selecciona Origen</option>';
        selectDestino.innerHTML = '<option value="" selected disabled>Selecciona Destino</option>';

        ciudadesUnicas.forEach(t => {
            const label = `${escaparHtml(t.ciudad)} (${escaparHtml(t.nombre)})`;
            selectOrigen.innerHTML += `<option value="${escaparHtml(t.ciudad)}">${label}</option>`;
            selectDestino.innerHTML += `<option value="${escaparHtml(t.ciudad)}">${label}</option>`;
        });
    } catch (err) {
        console.error('Error cargando terminales:', err);
        selectOrigen.innerHTML = '<option value="">Sin conexión al servidor</option>';
        selectDestino.innerHTML = '<option value="">Sin conexión al servidor</option>';
    }
}

function configurarBuscador() {
    const form = document.getElementById('formBusquedaViaje');
    const btnTodas = document.getElementById('btnVerTodasSalidas');

    form?.addEventListener('submit', async (e) => {
        e.preventDefault();
        form.classList.add('was-validated');
        if (!form.checkValidity()) return;

        const origen = document.getElementById('selectOrigen').value;
        const destino = document.getElementById('selectDestino').value;
        const fecha = document.getElementById('fechaViaje').value;

        if (origen && destino && origen === destino) {
            alert('La ciudad de origen y destino no pueden ser la misma.');
            return;
        }

        const titulo = document.getElementById('tituloResultados');
        if (titulo) {
            titulo.textContent = `Salidas: ${origen} → ${destino}${fecha ? ' (' + formatearFechaISO(fecha) + ')' : ''}`;
        }
        await cargarSalidas(origen, destino, fecha);
        document.getElementById('seccionResultados')?.scrollIntoView({ behavior: 'smooth' });
    });

    btnTodas?.addEventListener('click', async () => {
        document.getElementById('fechaViaje').value = '';
        document.getElementById('tituloResultados').textContent = 'Próximas Salidas Programadas';
        await cargarSalidas('', '', '');
    });
}

async function cargarSalidas(origen, destino, fecha) {
    const lista = document.getElementById('listaResultados');
    if (!lista) return;

    lista.innerHTML = `
        <div class="col-12 text-center py-4">
            <div class="spinner-border text-warning" role="status"></div>
            <p class="mt-2 text-muted small fw-bold">Consultando viajes en PostgreSQL...</p>
        </div>`;

    try {
        const params = new URLSearchParams();
        if (origen) params.set('origen', origen);
        if (destino) params.set('destino', destino);
        if (fecha) params.set('fecha', fecha);

        const viajes = await apiGet(`/catalogo/viajes?${params.toString()}`);

        if (!viajes || viajes.length === 0) {
            lista.innerHTML = `
                <div class="col-12">
                    <div class="alert alert-light border rounded-4 p-4 text-center">
                        <i class="bi bi-calendar-x fs-2 text-muted d-block mb-2"></i>
                        <h6 class="fw-bold mb-1">No hay salidas programadas con esos filtros exactos</h6>
                        <p class="text-muted small mb-0">Prueba quitando la fecha para ver todos los horarios próximos o consulta otra ruta.</p>
                    </div>
                </div>`;
            return;
        }

        lista.innerHTML = viajes.map(v => {
            const origCiu = v.ruta?.origen?.ciudad || '-';
            const origNom = v.ruta?.origen?.nombre || '';
            const destCiu = v.ruta?.destino?.ciudad || '-';
            const destNom = v.ruta?.destino?.nombre || '';
            const horas = v.ruta?.horasEstimadas || 6;

            const fechaHora = v.fechaHoraSalida ? new Date(v.fechaHoraSalida) : null;
            const horaSalida = fechaHora ? fechaHora.toTimeString().slice(0, 5) : '--:--';
            const fechaSalida = v.fechaHoraSalida ? formatearFechaISO(v.fechaHoraSalida) : '-';
            const llegadaDate = fechaHora ? new Date(fechaHora.getTime() + horas * 3600 * 1000) : null;
            const horaLlegada = llegadaDate ? llegadaDate.toTimeString().slice(0, 5) : '--:--';

            const disp = v.asientosDisponibles ?? 64;
            const sinCupo = disp <= 0;
            const placa = v.bus?.placa ? `Bus ${v.bus.placa}` : 'Doble Piso';

            return `
                <div class="col-12">
                    <div class="card ticket-card border-0 shadow-sm rounded-4 p-4">
                        <div class="row align-items-center text-center text-md-start gy-3">
                            <div class="col-12 col-md-3">
                                <span class="badge bg-dark text-warning mb-1">${escaparHtml(fechaSalida)}</span>
                                <span class="fw-bold text-uppercase d-block text-primary">${escaparHtml(v.servicio || 'Doble Piso VIP')}</span>
                                <small class="text-muted"><i class="bi bi-bus-front"></i> ${escaparHtml(placa)} · <i class="bi bi-wifi"></i> USB</small>
                            </div>

                            <div class="col-12 col-md-5">
                                <div class="d-flex align-items-center justify-content-center justify-content-md-start gap-3">
                                    <div>
                                        <div class="fs-4 fw-bold text-dark">${escaparHtml(horaSalida)}</div>
                                        <div class="fw-semibold small">${escaparHtml(origCiu)}</div>
                                        <div class="text-muted" style="font-size: 0.72rem;">${escaparHtml(origNom)}</div>
                                    </div>
                                    <div class="text-center px-2 flex-grow-1" style="max-width: 140px;">
                                        <small class="text-muted d-block">~${horas} hrs</small>
                                        <hr class="my-1 border-secondary border-dashed">
                                        <small class="text-success fw-bold">${disp} asientos libres</small>
                                    </div>
                                    <div>
                                        <div class="fs-4 fw-bold text-dark">${escaparHtml(horaLlegada)}</div>
                                        <div class="fw-semibold small">${escaparHtml(destCiu)}</div>
                                        <div class="text-muted" style="font-size: 0.72rem;">${escaparHtml(destNom)}</div>
                                    </div>
                                </div>
                            </div>

                            <div class="col-12 col-md-4 text-center text-md-end">
                                <div class="mb-2 text-muted small">Desde: <span class="fs-4 fw-bold text-primary">${moneda(v.precio)}</span></div>
                                <button type="button" class="btn ${sinCupo ? 'btn-secondary' : 'btn-warning'} text-dark fw-bold rounded-pill px-4 shadow-sm"
                                    data-seleccionar-viaje="${v.idViaje}" ${sinCupo ? 'disabled' : ''}>
                                    ${sinCupo ? 'Agotado' : 'Elegir asientos'} <i class="bi bi-chevron-right"></i>
                                </button>
                            </div>
                        </div>
                    </div>
                </div>`;
        }).join('');

        lista.querySelectorAll('[data-seleccionar-viaje]').forEach(btn => {
            btn.addEventListener('click', () => seleccionarViajeParaCompra(btn.dataset.seleccionarViaje));
        });
    } catch (err) {
        console.error('Error al buscar viajes:', err);
        lista.innerHTML = `<div class="col-12 text-center text-danger py-4 fw-semibold">No se pudo conectar con el servidor (${escaparHtml(err.message)}).</div>`;
    }
}

function seleccionarViajeParaCompra(idViaje) {
    sessionStorage.setItem('viajeSeleccionadoId', String(idViaje));
    const sesion = obtenerSesion();
    if (!sesion) {
        mostrarFeedbackAuth('Inicia sesión o regístrate gratis para reservar tus asientos.', 'primary');
        bootstrap.Modal.getOrCreateInstance(document.getElementById('loginModal')).show();
        return;
    }
    window.location.href = 'asientos.html';
}

function configurarBotonesDestinoRapido() {
    document.querySelectorAll('[data-destino-rapido]').forEach(btn => {
        btn.addEventListener('click', async () => {
            const ciudadDestino = btn.dataset.destinoRapido;
            const selOrig = document.getElementById('selectOrigen');
            const selDest = document.getElementById('selectDestino');

            if (selOrig) {
                const idxLima = [...selOrig.options].findIndex(o => o.value.toLowerCase() === 'lima');
                if (idxLima >= 0) selOrig.selectedIndex = idxLima;
            }
            if (selDest) {
                const idxDest = [...selDest.options].findIndex(o => o.value.toLowerCase() === ciudadDestino.toLowerCase());
                if (idxDest >= 0) selDest.selectedIndex = idxDest;
            }

            document.getElementById('tituloResultados').textContent = `Salidas: Lima → ${ciudadDestino}`;
            await cargarSalidas('Lima', ciudadDestino, '');
            document.getElementById('seccionResultados')?.scrollIntoView({ behavior: 'smooth' });
        });
    });
}

function configurarModalAuth() {
    const tabLogin = document.getElementById('tabLogin');
    const tabRegistro = document.getElementById('tabRegistro');
    const contLogin = document.getElementById('formLoginContainer');
    const contRegistro = document.getElementById('formRegistroContainer');
    const formLogin = document.getElementById('formLogin');
    const formRegistro = document.getElementById('formRegistro');

    const activarTab = (modo) => {
        mostrarFeedbackAuth('', 'muted');
        const esLogin = modo === 'login';
        contLogin?.classList.toggle('d-none', !esLogin);
        contRegistro?.classList.toggle('d-none', esLogin);
        tabLogin?.classList.toggle('active', esLogin);
        tabRegistro?.classList.toggle('active', !esLogin);
    };

    tabLogin?.addEventListener('click', () => activarTab('login'));
    tabRegistro?.addEventListener('click', () => activarTab('registro'));

    // Botones de autocompletado de cuentas demo por rol
    document.querySelectorAll('[data-demo-user]').forEach(btn => {
        btn.addEventListener('click', () => {
            document.getElementById('login-correo').value = btn.dataset.demoUser;
            document.getElementById('login-password').value = btn.dataset.demoPass;
            mostrarFeedbackAuth(`Credenciales de ${btn.textContent} listas. Pulsa "Ingresar al Sistema".`, 'success');
        });
    });

    formLogin?.addEventListener('submit', async (e) => {
        e.preventDefault();
        formLogin.classList.add('was-validated');
        if (!formLogin.checkValidity()) return;

        const btn = document.getElementById('btnSubmitLogin');
        btn.disabled = true;
        btn.textContent = 'Verificando credenciales...';
        mostrarFeedbackAuth('', 'muted');

        try {
            const data = await apiPost('/auth/login', {
                correo: document.getElementById('login-correo').value.trim(),
                contrasena: document.getElementById('login-password').value
            });

            guardarSesion(data);
            formLogin.reset();
            bootstrap.Modal.getInstance(document.getElementById('loginModal'))?.hide();

            const rol = (data.rol || 'CLIENTE').toUpperCase();
            const viajePendiente = sessionStorage.getItem('viajeSeleccionadoId');

            // Si es personal interno (ADMIN, VENDEDOR, EMBARQUE), va directo a su panel de trabajo
            if (rol !== 'CLIENTE') {
                window.location.href = data.panelUrl || urlPanelPorRol(rol);
                return;
            }

            if (viajePendiente) {
                window.location.href = 'asientos.html';
            } else {
                renderizarEstadoNav();
            }
        } catch (err) {
            mostrarFeedbackAuth(err.message, 'danger');
        } finally {
            btn.disabled = false;
            btn.textContent = 'Ingresar al Sistema';
        }
    });

    formRegistro?.addEventListener('submit', async (e) => {
        e.preventDefault();
        formRegistro.classList.add('was-validated');
        if (!formRegistro.checkValidity()) return;

        const pass = document.getElementById('reg-password').value;
        const passConfirm = document.getElementById('reg-password-confirm').value;

        if (pass !== passConfirm) {
            mostrarFeedbackAuth('Las contraseñas ingresadas no coinciden.', 'danger');
            return;
        }

        const btn = document.getElementById('btnSubmitRegistro');
        btn.disabled = true;
        btn.textContent = 'Creando cuenta...';

        try {
            const data = await apiPost('/auth/registro', {
                nombre: document.getElementById('reg-nombre').value.trim(),
                apellido: document.getElementById('reg-apellido').value.trim(),
                tipoDocumento: document.getElementById('reg-tipo-doc').value,
                numeroDocumento: document.getElementById('reg-documento').value.trim(),
                correo: document.getElementById('reg-correo').value.trim(),
                telefono: document.getElementById('reg-telefono').value.trim(),
                contrasena: pass
            });

            guardarSesion(data);
            formRegistro.reset();
            bootstrap.Modal.getInstance(document.getElementById('loginModal'))?.hide();

            if (sessionStorage.getItem('viajeSeleccionadoId')) {
                window.location.href = 'asientos.html';
            } else {
                renderizarEstadoNav();
            }
        } catch (err) {
            mostrarFeedbackAuth(err.message, 'danger');
        } finally {
            btn.disabled = false;
            btn.textContent = 'Crear mi cuenta y continuar';
        }
    });
}

function mostrarFeedbackAuth(texto, tipo = 'danger') {
    const el = document.getElementById('authMensajeFeedback');
    if (!el) return;
    el.className = `small fw-bold text-center mb-3 text-${tipo}`;
    el.textContent = texto;
}
