// Acceso de administrador desde index.html
// Usa su propia sesión ("adminUrbanRide"), separada de la sesión de clientes.
const API_ADMIN_AUTH_URL = 'http://127.0.0.1:8080/api/admin/auth';

document.addEventListener('DOMContentLoaded', () => {
    const enlace = document.getElementById('enlaceAdmin');
    const modalEl = document.getElementById('adminLoginModal');
    const form = document.getElementById('formAdminLogin');
    if (!enlace || !modalEl || !form) return;

    enlace.addEventListener('click', (e) => {
        e.preventDefault();
        // Si ya inició sesión como admin, entra directo al panel
        if (localStorage.getItem('adminUrbanRide')) {
            window.location.href = 'admin-ventas.html';
            return;
        }
        bootstrap.Modal.getOrCreateInstance(modalEl).show();
    });

    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        const mensaje = document.getElementById('adminLoginMensaje');
        const btn = document.getElementById('btnSubmitAdminLogin');
        mensaje.textContent = '';
        btn.disabled = true;
        btn.textContent = 'Verificando...';

        try {
            const response = await fetch(`${API_ADMIN_AUTH_URL}/login`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
                body: JSON.stringify({
                    correo: document.getElementById('admin-correo').value.trim(),
                    contrasena: document.getElementById('admin-password').value
                })
            });
            const data = await response.json();
            if (!response.ok) throw new Error(data.mensaje || 'No se pudo iniciar sesión.');

            localStorage.setItem('adminUrbanRide', JSON.stringify({ nombre: data.nombre, token: data.token }));
            form.reset();
            window.location.href = 'admin-ventas.html';
        } catch (err) {
            console.error('Error en login admin:', err);
            mensaje.textContent = err.message;
        } finally {
            btn.disabled = false;
            btn.textContent = 'Entrar al panel';
        }
    });
});
