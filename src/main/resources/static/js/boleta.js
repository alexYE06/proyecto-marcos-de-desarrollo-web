import { apiGet } from './core/api.js';
import { escaparHtml, moneda } from './core/formatters.js';

document.addEventListener('DOMContentLoaded', async () => {
    document.getElementById('btnImprimirPdf')?.addEventListener('click', () => window.print());

    const params = new URLSearchParams(window.location.search);
    const idVenta = params.get('idVenta');
    let venta = null;

    if (idVenta) {
        try {
            venta = await apiGet(`/cliente/comprobantes/${encodeURIComponent(idVenta)}`, { silenciar401: true });
        } catch (err) {
            console.warn('Usando respaldo local del comprobante:', err.message);
        }
    }

    if (!venta) {
        const localRaw = localStorage.getItem('ultimaVentaUrbanRide');
        if (localRaw) {
            venta = JSON.parse(localRaw);
        }
    }

    if (!venta) {
        alert('No se encontró información del comprobante.');
        window.close();
        return;
    }

    renderizarComprobante(venta);
});

function renderizarComprobante(venta) {
    if (venta.estadoPago === 'ANULADO') {
        const alerta = document.getElementById('alertaEstadoAnulado');
        alerta.classList.remove('d-none');
        alerta.textContent = `COMPROBANTE ANULADO · Motivo: ${venta.motivoAnulacion || 'Anulación administrativa'}`;
    }

    document.getElementById('tipoDocTitulo').textContent = venta.tipoComprobante || 'BOLETA DE VENTA ELECTRÓNICA';
    document.getElementById('numSerieComp').textContent = venta.serieNumero || '-';
    document.getElementById('compFecha').textContent = venta.fechaVenta || venta.fechaEmision || '-';
    document.getElementById('compCanal').textContent = `${venta.canalVenta || 'WEB'} (${venta.vendedorNombre || 'Autoservicio'})`;
    document.getElementById('compPago').textContent = venta.metodoPago || '-';
    document.getElementById('compReserva').textContent = venta.codigoReserva || '-';

    document.getElementById('clienteNombre').textContent = venta.razonSocialFactura || venta.compradorNombre || '-';
    document.getElementById('clienteDoc').textContent = venta.rucFactura
        ? `RUC: ${venta.rucFactura}`
        : `${venta.compradorTipoDoc || 'DNI'}: ${venta.compradorDocumento || '-'}`;
    document.getElementById('clienteCorreo').textContent = venta.compradorCorreo || '-';
    document.getElementById('compItinerario').textContent =
        `${venta.origenCiudad || ''} → ${venta.destinoCiudad || ''} · Salida: ${venta.fechaHoraSalida || '-'} · Bus: ${venta.placaBus || '-'}`;

    const boletos = venta.boletos || [];
    const tbody = document.getElementById('tablaItemsBoleta');
    tbody.innerHTML = boletos.map(b => `
        <tr>
            <td class="text-center fw-bold font-monospace">${escaparHtml(b.codigoBoleto)}</td>
            <td>${escaparHtml(venta.servicioViaje || 'Doble Piso VIP')} · <strong>Asiento ${b.numeroAsiento}</strong> (Piso ${b.piso})</td>
            <td>${escaparHtml(b.pasajeroNombreCompleto || (b.pasajeroNombre + ' ' + b.pasajeroApellido))}</td>
            <td class="text-center">${escaparHtml(b.pasajeroTipoDoc)} ${escaparHtml(b.pasajeroDocumento)}</td>
            <td class="text-end fw-bold">${moneda(b.precio)}</td>
        </tr>`).join('');

    if (boletos[0]?.codigoQr) {
        document.getElementById('contenedorQrPrincipal').innerHTML =
            `<img src="${boletos[0].codigoQr}" alt="QR Boleto" width="96" height="96">`;
    }

    const listaPases = document.getElementById('listaPasesQr');
    listaPases.innerHTML = boletos.map(b => `
        <div class="col-12 col-md-6">
            <div class="border rounded-3 p-2 d-flex align-items-center gap-3 bg-light">
                ${b.codigoQr
                    ? `<img src="${b.codigoQr}" alt="QR ${escaparHtml(b.codigoBoleto)}" width="84" height="84" class="bg-white p-1 border rounded">`
                    : '<i class="bi bi-qr-code fs-1"></i>'}
                <div class="small">
                    <div class="fw-bold text-primary font-monospace">${escaparHtml(b.codigoBoleto)}</div>
                    <div class="fw-semibold">${escaparHtml(b.pasajeroNombreCompleto)}</div>
                    <div class="text-muted">${escaparHtml(b.pasajeroTipoDoc)}: ${escaparHtml(b.pasajeroDocumento)}</div>
                    <div class="badge bg-dark mt-1">Asiento ${b.numeroAsiento} · Piso ${b.piso}</div>
                    <span class="badge ${b.estadoAbordaje === 'ABORDADO' ? 'bg-success' : 'bg-secondary'} mt-1">${escaparHtml(b.estadoAbordaje)}</span>
                </div>
            </div>
        </div>`).join('');

    const total = Number(venta.montoTotal ?? venta.total) || 0;
    const baseImponible = total / 1.18;
    const igv = total - baseImponible;

    document.getElementById('totalSubtotal').textContent = moneda(baseImponible);
    document.getElementById('totalIgv').textContent = moneda(igv);
    document.getElementById('totalFinal').textContent = moneda(total);

    const desc = Number(venta.descuento) || 0;
    if (desc > 0) {
        document.getElementById('filaDescuento').classList.remove('d-none');
        document.getElementById('totalDescuento').textContent = `- ${moneda(desc)}`;
    }
}
