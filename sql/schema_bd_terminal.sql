-- =============================================================================
-- UrbanRide - Esquema Unificado PostgreSQL (bd_terminal)
-- Compatible con tablas previas y con migración automática Hibernate (update)
-- =============================================================================

-- 1. USUARIOS Y ROLES DEL SISTEMA (ADMIN, VENDEDOR, EMBARQUE, CLIENTE)
CREATE TABLE IF NOT EXISTS usuario (
    id_usuario SERIAL PRIMARY KEY,
    nombre VARCHAR(60) NOT NULL,
    apellido VARCHAR(60) NOT NULL,
    tipo_documento VARCHAR(10) NOT NULL DEFAULT 'DNI',
    numero_documento VARCHAR(20) NOT NULL UNIQUE,
    correo VARCHAR(120) NOT NULL UNIQUE,
    telefono VARCHAR(15),
    contrasena_hash VARCHAR(255) NOT NULL,
    rol VARCHAR(20) NOT NULL DEFAULT 'CLIENTE',
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. TERMINALES TERRESTRES
CREATE TABLE IF NOT EXISTS terminal (
    id_terminal SERIAL PRIMARY KEY,
    nombre VARCHAR(120) NOT NULL,
    ciudad VARCHAR(80) NOT NULL,
    direccion VARCHAR(200),
    activo BOOLEAN DEFAULT TRUE
);
ALTER TABLE terminal ADD COLUMN IF NOT EXISTS activo BOOLEAN DEFAULT TRUE;

-- 3. RUTAS INTERPROVINCIALES
CREATE TABLE IF NOT EXISTS ruta (
    id_ruta SERIAL PRIMARY KEY,
    id_terminal_origen INTEGER NOT NULL REFERENCES terminal(id_terminal),
    id_terminal_destino INTEGER NOT NULL REFERENCES terminal(id_terminal),
    precio_base DOUBLE PRECISION NOT NULL,
    horas_estimadas INTEGER NOT NULL,
    distancia_km INTEGER,
    activo BOOLEAN DEFAULT TRUE
);
ALTER TABLE ruta ADD COLUMN IF NOT EXISTS distancia_km INTEGER;
ALTER TABLE ruta ADD COLUMN IF NOT EXISTS activo BOOLEAN DEFAULT TRUE;

-- 4. FLOTA DE BUSES
CREATE TABLE IF NOT EXISTS bus (
    id_bus SERIAL PRIMARY KEY,
    placa VARCHAR(15) NOT NULL UNIQUE,
    numero_interno VARCHAR(20),
    marca VARCHAR(60),
    modelo VARCHAR(80),
    pisos INTEGER NOT NULL DEFAULT 2,
    capacidad_asientos INTEGER NOT NULL DEFAULT 64,
    estado VARCHAR(25) NOT NULL DEFAULT 'Activo'
);

-- 5. EMPLEADOS / TRIPULACIÓN OPERATIVA (Conductor, Copiloto, Terramoza)
CREATE TABLE IF NOT EXISTS empleado (
    id_empleado SERIAL PRIMARY KEY,
    dni_empleado VARCHAR(15) NOT NULL UNIQUE,
    nombre VARCHAR(60) NOT NULL,
    apellido VARCHAR(60) NOT NULL,
    cargo VARCHAR(30) NOT NULL,
    licencia VARCHAR(25),
    telefono VARCHAR(15),
    sueldo NUMERIC(10, 2),
    activo BOOLEAN DEFAULT TRUE
);
ALTER TABLE empleado ADD COLUMN IF NOT EXISTS licencia VARCHAR(25);
ALTER TABLE empleado ADD COLUMN IF NOT EXISTS activo BOOLEAN DEFAULT TRUE;

-- 6. PROGRAMACIÓN DE VIAJES / SALIDAS
CREATE TABLE IF NOT EXISTS viaje (
    id_viaje SERIAL PRIMARY KEY,
    id_ruta INTEGER NOT NULL REFERENCES ruta(id_ruta),
    id_bus INTEGER REFERENCES bus(id_bus),
    id_conductor INTEGER REFERENCES empleado(id_empleado),
    id_copiloto INTEGER REFERENCES empleado(id_empleado),
    fecha_hora_salida TIMESTAMP NOT NULL,
    precio DOUBLE PRECISION NOT NULL,
    asientos_disponibles INTEGER NOT NULL DEFAULT 64,
    servicio VARCHAR(50) DEFAULT 'Doble Piso VIP',
    estado VARCHAR(25) DEFAULT 'PROGRAMADO'
);
ALTER TABLE viaje ADD COLUMN IF NOT EXISTS id_bus INTEGER REFERENCES bus(id_bus);
ALTER TABLE viaje ADD COLUMN IF NOT EXISTS id_conductor INTEGER REFERENCES empleado(id_empleado);
ALTER TABLE viaje ADD COLUMN IF NOT EXISTS id_copiloto INTEGER REFERENCES empleado(id_empleado);
ALTER TABLE viaje ADD COLUMN IF NOT EXISTS servicio VARCHAR(50) DEFAULT 'Doble Piso VIP';
ALTER TABLE viaje ADD COLUMN IF NOT EXISTS estado VARCHAR(25) DEFAULT 'PROGRAMADO';

-- 7. CUPONES PROMOCIONALES
CREATE TABLE IF NOT EXISTS cupon_descuento (
    id_cupon SERIAL PRIMARY KEY,
    codigo VARCHAR(30) NOT NULL UNIQUE,
    descripcion VARCHAR(150),
    monto_descuento NUMERIC(10, 2) NOT NULL,
    usos_maximos INTEGER NOT NULL DEFAULT 100,
    usos_actuales INTEGER NOT NULL DEFAULT 0,
    fecha_expiracion DATE,
    activo BOOLEAN NOT NULL DEFAULT TRUE
);

-- 8. OPERACIONES DE VENTA UNIFICADAS (WEB + VENTANILLA)
CREATE TABLE IF NOT EXISTS operacion_venta (
    id_venta SERIAL PRIMARY KEY,
    codigo_reserva VARCHAR(25) NOT NULL UNIQUE,
    tipo_comprobante VARCHAR(20) NOT NULL DEFAULT 'BOLETA',
    serie_numero VARCHAR(25) NOT NULL UNIQUE,
    ruc_factura VARCHAR(11),
    razon_social_factura VARCHAR(150),
    canal_venta VARCHAR(20) NOT NULL,
    id_viaje INTEGER NOT NULL REFERENCES viaje(id_viaje),
    id_cliente INTEGER REFERENCES usuario(id_usuario),
    id_vendedor INTEGER REFERENCES usuario(id_usuario),
    comprador_nombre VARCHAR(120) NOT NULL,
    comprador_tipo_doc VARCHAR(15) NOT NULL DEFAULT 'DNI',
    comprador_documento VARCHAR(20) NOT NULL,
    comprador_correo VARCHAR(120),
    comprador_telefono VARCHAR(20),
    metodo_pago VARCHAR(20) NOT NULL,
    codigo_cupon VARCHAR(30),
    subtotal NUMERIC(10, 2) NOT NULL,
    descuento NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    monto_total NUMERIC(10, 2) NOT NULL,
    estado_pago VARCHAR(30) NOT NULL,
    expira_en TIMESTAMP,
    fecha_venta TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    motivo_anulacion VARCHAR(250),
    id_solicitante_anulacion INTEGER REFERENCES usuario(id_usuario),
    id_autorizador_anulacion INTEGER REFERENCES usuario(id_usuario),
    fecha_anulacion TIMESTAMP
);

-- 9. BOLETOS EMITIDOS CON QR VERIFICABLE Y CONTROL DE ABORDAJE
CREATE TABLE IF NOT EXISTS boleto_emitido (
    id_boleto SERIAL PRIMARY KEY,
    codigo_boleto VARCHAR(30) NOT NULL UNIQUE,
    id_venta INTEGER NOT NULL REFERENCES operacion_venta(id_venta) ON DELETE CASCADE,
    id_viaje INTEGER NOT NULL REFERENCES viaje(id_viaje),
    numero_asiento INTEGER NOT NULL,
    piso INTEGER NOT NULL,
    categoria_asiento VARCHAR(30) NOT NULL,
    pasajero_tipo_doc VARCHAR(15) NOT NULL DEFAULT 'DNI',
    pasajero_documento VARCHAR(20) NOT NULL,
    pasajero_nombre VARCHAR(80) NOT NULL,
    pasajero_apellido VARCHAR(80) NOT NULL,
    pasajero_correo VARCHAR(120),
    pasajero_telefono VARCHAR(20),
    precio NUMERIC(10, 2) NOT NULL,
    codigo_qr TEXT,
    estado_abordaje VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    fecha_abordaje TIMESTAMP,
    id_inspector_abordaje INTEGER REFERENCES usuario(id_usuario)
);

CREATE INDEX IF NOT EXISTS idx_viaje_salida ON viaje(fecha_hora_salida);
CREATE INDEX IF NOT EXISTS idx_boleto_viaje_asiento ON boleto_emitido(id_viaje, numero_asiento);
CREATE INDEX IF NOT EXISTS idx_venta_cliente ON operacion_venta(id_cliente);
CREATE INDEX IF NOT EXISTS idx_venta_vendedor ON operacion_venta(id_vendedor);

-- 10. BITÁCORA INMUTABLE DE AUDITORÍA (QUIÉN, QUÉ, CUÁNDO Y POR QUÉ)
CREATE TABLE IF NOT EXISTS auditoria_log (
    id_auditoria SERIAL PRIMARY KEY,
    fecha_hora TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_usuario_actor INTEGER,
    actor_nombre VARCHAR(120) NOT NULL,
    actor_rol VARCHAR(25) NOT NULL,
    modulo VARCHAR(40) NOT NULL,
    accion VARCHAR(45) NOT NULL,
    entidad VARCHAR(40) NOT NULL,
    id_entidad VARCHAR(40),
    descripcion VARCHAR(350) NOT NULL,
    motivo VARCHAR(250)
);
CREATE INDEX IF NOT EXISTS idx_auditoria_fecha ON auditoria_log(fecha_hora DESC);
CREATE INDEX IF NOT EXISTS idx_auditoria_modulo ON auditoria_log(modulo, accion);
