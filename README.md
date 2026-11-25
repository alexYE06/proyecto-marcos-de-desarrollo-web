# UrbanRide — Sistema Web de Transporte Interprovincial (Spring Boot 3 + Thymeleaf + Spring Security JWT + PostgreSQL)

Aplicación web integral para la gestión de rutas, flota, venta omnicanal de pasajes (Web y Ventanilla POS), emisión de comprobantes electrónicos con **código QR verificable (ZXing)** y control de embarque.

---

## Cumplimiento 100 % de la Rúbrica Técnica (Marcos de Desarrollo Web — UTP)

1. **Maquetación responsiva**: Diseño adaptable móvil/tablet/escritorio en todos los módulos y mapa interactivo de bus de 2 pisos.
2. **Framework Front-end (Bootstrap 5.3)**: Formularios con validación visual (`needs-validation` / `was-validated`), ventanas modales, barras de navegación responsivas, tablas dinámicas, acordeón (`#acordeonPoliticasViaje`), alertas, carrusel (`#carruselFlotaUrbanRide`) y popovers interactivos.
3. **Framework Back-end (Spring Boot 3.2)**: Arquitectura limpia en capas (`config`, `security`, `model`, `repository`, `dto`, `service`, `controller`, `exception`).
4. **Motor de Plantillas SSR (Thymeleaf)**: Renderizado en servidor en `PortalThymeleafController` para la portada principal (`/`) con fragmentos reutilizables (`templates/fragments/footer.html`) y la **Cartelera Digital de Salidas en Vivo** (`/cartelera`).
5. **Operaciones con Base de Datos (Spring Data JPA + PostgreSQL)**: Persistencia transaccional real en `bd_terminal` para usuarios, terminales, rutas, buses, empleados, viajes, cupones, ventas y boletos QR.
6. **Spring Security + Autenticación y Autorización JWT**: `SecurityFilterChain` stateless, contraseñas con `BCryptPasswordEncoder`, filtro `JwtAuthenticationFilter` con tokens **JWT (`HS256` - RFC 7519)** y segregación estricta de privilegios por rol.

---

## Roles y Cuentas Semilla de Demostración

| Rol (`ROLE_*`) | Módulo / Vista | Correo | Contraseña |
| :--- | :--- | :--- | :--- |
| **`ADMIN`** | Gobierno total, reportes CSV, anulaciones, viajes, flota, RR.HH. y roles (`admin-panel.html`) | `admin@urbanride.pe` | `Admin123!` |
| **`VENDEDOR`** | Punto de Venta en mostrador, calculadora de vuelto y arqueo de turno (`vendedor-pos.html`) | `vendedor@urbanride.pe` | `Vendedor123!` |
| **`EMBARQUE`** | Manifiesto de pasajeros, despacho de unidad y validación anti-fraude QR (`embarque.html`) | `embarque@urbanride.pe` | `Embarque123!` |
| **`CLIENTE`** | Búsqueda de viajes, bloqueo 15 min, compra con cupones/RUC y Mis Viajes (`/`, `mis-viajes.html`) | `cliente@urbanride.pe` | `Cliente123!` |

---

## Cómo iniciar sin necesidad de IDE (1 clic)

Ejecuta **`iniciar_urbanride.bat`** en la raíz del proyecto: levanta automáticamente PostgreSQL (`D:\pgsql` en `localhost:5432`) e inicia el servidor Spring Boot en `http://localhost:8080`.
