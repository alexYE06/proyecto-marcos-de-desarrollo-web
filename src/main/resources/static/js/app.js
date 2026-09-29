// 1. EVENTO QUE SE EJECUTA AL CARGAR LA PÁGINA
document.addEventListener("DOMContentLoaded", function () {
    const selectOrigen = document.getElementById('selectOrigen');
    const selectDestino = document.getElementById('selectDestino');

    // Ciudades de prueba (luego las traerás de tu base de datos)
    const ciudades = ['Lima', 'Ica', 'Huancayo', 'Arequipa'];

    // Borramos el texto de "Cargando terminales..."
    selectOrigen.innerHTML = '<option value="" selected disabled>Selecciona Origen</option>';
    selectDestino.innerHTML = '<option value="" selected disabled>Selecciona Destino</option>';

    // Llenamos los desplegables con las ciudades
    ciudades.forEach(ciudad => {
        selectOrigen.innerHTML += `<option value="${ciudad}">${ciudad}</option>`;
        selectDestino.innerHTML += `<option value="${ciudad}">${ciudad}</option>`;
    });
});

// 2. FUNCIÓN PARA LOS BOTONES DE LAS TARJETAS (Ica, Huancayo, Arequipa)
function seleccionarDestinoRapido(ciudadDestino) {
    const selectOrigen = document.getElementById('selectOrigen');
    const selectDestino = document.getElementById('selectDestino');
    const fechaViaje = document.getElementById('fechaViaje');
    const formBusqueda = document.getElementById('formBusquedaViaje');

    const ciudadBuscada = ciudadDestino.toLowerCase();

    // Buscar y seleccionar "Lima" en el origen
    for (let i = 0; i < selectOrigen.options.length; i++) {
        if (selectOrigen.options[i].text.toLowerCase().includes('lima')) {
            selectOrigen.selectedIndex = i;
            break;
        }
    }

    // Buscar y seleccionar la ciudad destino
    for (let i = 0; i < selectDestino.options.length; i++) {
        if (selectDestino.options[i].text.toLowerCase().includes(ciudadBuscada)) {
            selectDestino.selectedIndex = i;
            break;
        }
    }

    // Colocar la fecha actual ajustada a tu zona horaria (Lima/Perú)
    if (!fechaViaje.value) {
        // Ajuste para evitar que marque un día adelantado por la hora UTC
        const hoy = new Date();
        const offset = hoy.getTimezoneOffset();
        const fechaLocal = new Date(hoy.getTime() - (offset * 60 * 1000));
        fechaViaje.value = fechaLocal.toISOString().split('T')[0];
    }

    // Subir al buscador suavemente
    window.scrollTo({ top: 0, behavior: 'smooth' });
}

// 3. SIMULACIÓN DE LA BÚSQUEDA (Para evitar que la página se recargue)
document.getElementById('formBusquedaViaje').addEventListener('submit', function (e) {
    e.preventDefault();
    const origen = document.getElementById('selectOrigen').value;
    const destino = document.getElementById('selectDestino').value;

    if (origen === destino) {
        alert("El origen y el destino no pueden ser el mismo.");
        return;
    }

    alert(`Buscando pasajes de ${origen} a ${destino}...`);
    // Aquí luego irá tu conexión a Spring Boot (fetch a tu base de datos)
});