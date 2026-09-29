document.addEventListener("DOMContentLoaded", () => {
    // 1. Obtener los datos de la última venta registrada
    const ventaRaw = localStorage.getItem('ultimaVentaUrbanRide');
    if (!ventaRaw) {
        alert("No se encontró información de la compra.");
        window.close();
        return;
    }

    const venta = JSON.parse(ventaRaw);

    // 2. Poblar cabecera y datos del comprobante
    const tipoDocTitulo = document.getElementById('tipoDocTitulo');
    const numSerieComp = document.getElementById('numSerieComp');
    const compFecha = document.getElementById('compFecha');
    const compPago = document.getElementById('compPago');
    const compReserva = document.getElementById('compReserva');
    const codigoHash = document.getElementById('codigoHash');

    if (tipoDocTitulo) tipoDocTitulo.innerText = venta.tipoComprobante;
    if (numSerieComp) numSerieComp.innerText = venta.serieNumero;
    if (compFecha) compFecha.innerText = venta.fechaEmision;
    if (compPago) compPago.innerText = venta.metodoPago;
    if (compReserva) compReserva.innerText = venta.codigoReserva;
    if (codigoHash) codigoHash.innerText = (Math.random().toString(36).substring(2, 15) + Math.random().toString(36).substring(2, 15));

    // 3. Poblar datos del cliente
    const clienteNombre = document.getElementById('clienteNombre');
    const clienteDoc = document.getElementById('clienteDoc');
    const clienteCorreo = document.getElementById('clienteCorreo');
    const clienteTelefono = document.getElementById('clienteTelefono');

    if (clienteNombre) clienteNombre.innerText = venta.comprador.nombre;
    if (clienteDoc) clienteDoc.innerText = `${venta.comprador.tipoDocumento}: ${venta.comprador.documento}`;
    if (clienteCorreo) clienteCorreo.innerText = venta.comprador.correo;
    if (clienteTelefono) clienteTelefono.innerText = venta.comprador.telefono || '-';

    // 4. Poblar tabla con los boletos/asientos comprados
    const tabla = document.getElementById('tablaItemsBoleta');
    if (tabla && venta.pasajeros) {
        tabla.innerHTML = '';
        venta.pasajeros.forEach(p => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td class="text-center fw-bold">1</td>
                <td>Servicio Interprovincial - <strong>Asiento ${p.idAsiento}</strong></td>
                <td>${p.nombre} ${p.apellido}</td>
                <td class="text-center">${p.tipoDocumento} ${p.numeroDocumento}</td>
                <td class="text-end fw-bold">S/ ${p.precio.toFixed(2)}</td>
            `;
            tabla.appendChild(tr);
        });
    }

    // 5. Cálculos tributarios (Operación Gravada + IGV 18%)
    const baseImponible = venta.total / 1.18;
    const igv = venta.total - baseImponible;

    const totalSubtotal = document.getElementById('totalSubtotal');
    const totalIgv = document.getElementById('totalIgv');
    const totalFinal = document.getElementById('totalFinal');

    if (totalSubtotal) totalSubtotal.innerText = `S/ ${baseImponible.toFixed(2)}`;
    if (totalIgv) totalIgv.innerText = `S/ ${igv.toFixed(2)}`;
    if (totalFinal) totalFinal.innerText = `S/ ${venta.total.toFixed(2)}`;

    // Manejo visual del descuento si aplicó cupón
    if (venta.descuento && venta.descuento > 0) {
        const filaDescuento = document.getElementById('filaDescuento');
        const totalDescuento = document.getElementById('totalDescuento');
        if (filaDescuento && totalDescuento) {
            filaDescuento.classList.remove('d-none');
            totalDescuento.innerText = `- S/ ${venta.descuento.toFixed(2)}`;
        }
    }
});

// Función para imprimir o generar PDF
function imprimirBoletaPDF() {
    window.print();
}