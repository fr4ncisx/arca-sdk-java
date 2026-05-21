# ARCA SDK Java

[![Java](https://img.shields.io/badge/Java-21-blue)](https://jdk.java.net/21/)
[![version](https://img.shields.io/github/v/tag/fr4ncisx/arca-sdk-java?style=flat&label=version)]()
[![status](https://img.shields.io/badge/status-milestone-orange)]()
[![build](https://img.shields.io/github/actions/workflow/status/fr4ncisx/arca-sdk-java/ci.yml?branch=main)]()
[![licencia](https://img.shields.io/badge/licencia-Apache%202.0-blue)](LICENSE)

SDK Java para integrarse con los servicios web de **ARCA** (ex AFIP) sin lidiar con SOAP, XML ni stubs generados. Proporciona una API limpia y tipada que abstrae toda la complejidad de la comunicación con los servidores de ARCA, desde la autenticación hasta la emisión de comprobantes fiscales electrónicos.

**¿Para qué sirve?**

- **Emitir comprobantes electrónicos**: Solicitar CAE/CAEA para facturas A, B, C, notas de crédito y débito.
- **Consultar comprobantes**: Verificar el estado de facturas emitidas y obtener el último comprobante autorizado.
- **Gestionar puntos de venta**: Listar y validar los puntos de venta asociados a un CUIT.
- **Validar contribuyentes**: Consultar datos tributarios de un receptor (razón social, condición IVA, estado) antes de emitir.
- **Automatizar procesos fiscales**: Procesamiento por lote de comprobantes, health checks y manejo tipado de errores.

**¿Qué resuelve?**

El SDK elimina la necesidad de interactuar directamente con WSDLs, generar stubs JAXB, construir XMLs TRA, firmar mensajes CMS/PKCS#7, gestionar tickets de acceso, renovar credenciales, parsear SOAPFaults o sanitizar datos sensibles en logs. Todo eso queda encapsulado internamente; el consumidor solo usa records, enums y métodos con nombres semánticos.

## Requisitos

| Herramienta | Versión |
|---|---|
| Java | 21 o superior |
| Maven | 3.9 o superior |

## Módulos

| Módulo | Responsabilidad |
|---|---|
| `arca-sdk-core` | Tipos compartidos, ambientes, errores, reloj, sanitización y utilidades comunes |
| `arca-sdk-soap` | Transporte SOAP común, handlers JAX-WS, timeouts y adaptación de errores |
| `arca-sdk-wsaa` | TRA, firma CMS/PKCS#7, `LoginCms`, tickets y renovación automática |
| `arca-sdk-wsfev1` | API de facturación electrónica WSFEv1, mappers, modelos y casos de uso |
| `arca-sdk-registry` | Consultas tributarias y registrales |
| `arca-sdk-test-support` | Fixtures XML, utilidades de test y soporte para mocks |
| `arca-sdk-bom` | Bill of Materials para centralizar versiones |
| `arca-sdk-bundle` | Dependencia de conveniencia para consumidores externos |

## Arquitectura objetivo

La arquitectura objetivo sigue **arquitectura hexagonal**, Clean Code,
principios SOLID y separación estricta entre dominio, aplicación e
infraestructura. La API pública debe depender de modelos propios del SDK; los
stubs JAXB generados desde WSDL quedan aislados en paquetes internos.

```text
API pública
ArcaClient, WsfeClient, RegistryClient, requests y responses tipados

Capa de aplicación
Casos de uso, puertos, validaciones, reglas de negocio y orquestación

Capa de infraestructura
Clientes SOAP, WSAA, firma CMS, caché, métricas, logging y adapters externos

Generado
Stubs JAXB/JAX-WS aislados, sin exposición en contratos públicos
```

Las reglas de seguridad se alinean con OWASP Top 10: no registrar secretos,
validar entradas, controlar errores, limitar exposición de XML sensible y
mantener dependencias auditables.

## Compilar y probar

```bash
mvn clean verify
```

Compila todos los módulos y ejecuta las pruebas disponibles.

```bash
mvn test -pl arca-sdk-core
```

Ejecuta solo las pruebas de `arca-sdk-core`.

```bash
mvn clean verify -Pgenerate-stubs
```

Regenera clases Java desde los WSDL cuando el perfil esté configurado para el
módulo correspondiente.

```bash
mvn verify -Darca.integration=true
```

Reserva este comando para pruebas de integración con credenciales reales de
ARCA. No debe ejecutarse sin certificado, CUIT y ambiente configurados.

## Instalación

Este SDK **no está publicado en Maven Central**. Instálalo primero desde el código fuente:

```bash
git clone https://github.com/fr4ncisx/arca-sdk-java.git
cd arca-sdk-java
mvn clean install -DskipTests
```

Esto publica los artefactos en tu repositorio local de Maven (`~/.m2/repository`).

Luego, en tu propio proyecto, elige **una** de estas dos opciones:

### Opción A — BOM + módulos individuales

Importa el BOM para gestionar versiones automáticamente:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>io.github.fr4ncisx</groupId>
            <artifactId>arca-sdk-bom</artifactId>
            <version>0.1.0-M2</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

Y agrega solo los módulos que necesites:

```xml
<dependencies>
    <!-- Facturación electrónica WSFEv1 -->
    <dependency>
        <groupId>io.github.fr4ncisx</groupId>
        <artifactId>arca-sdk-wsfev1</artifactId>
    </dependency>

    <!-- Consultas tributarias -->
    <dependency>
        <groupId>io.github.fr4ncisx</groupId>
        <artifactId>arca-sdk-registry</artifactId>
    </dependency>
</dependencies>
```

### Opción B — Bundle (todo incluido)

Sin BOM, una sola dependencia con todos los módulos:

```xml
<dependencies>
    <dependency>
        <groupId>io.github.fr4ncisx</groupId>
        <artifactId>arca-sdk-bundle</artifactId>
        <version>0.1.0-M2</version>
    </dependency>
</dependencies>
```

## Uso

### Cliente principal

```java
ArcaConfig config = ArcaConfig.builder()
    .environment(ArcaEnvironment.HOMOLOGACION)
    .cuit(Cuit.parse("20-33333333-9"))
    .build();

CertificateSource source = Pkcs12CertificateSource.fromPath(
    Paths.get("certificado.p12"),
    "password".toCharArray());

ArcaClient arca = ArcaClient.builder()
    .config(config)
    .certificate(source)
    .build();

WsfeClient wsfe = arca.wsfev1();
```

### Consultar último comprobante autorizado

```java
LastVoucherResponse last = wsfe.getLastAuthorizedVoucher(
    new LastVoucherRequest(1, VoucherType.FACTURA_A));

System.out.printf("Último comprobante: %d - %d%n",
    last.voucherFrom(), last.voucherTo());
```

### Solicitar CAE

```java
CaeRequest request = new CaeRequest(
    VoucherType.FACTURA_A,
    1,                              // salesPoint
    ConceptType.PRODUCTOS_Y_SERVICIOS,
    BigDecimal.valueOf(1210.00),    // importeTotal
    BigDecimal.valueOf(1000.00),    // importeGravado
    BigDecimal.valueOf(210.00),     // importeIva
    LocalDate.now(),                // fechaEmision
    LocalDate.now().plusDays(30),   // fechaVencimiento
    Currency.PES,                   // moneda
    BigDecimal.ONE                  // cotización
);

CaeResponse response = wsfe.requestCae(request);

if (response.success()) {
    System.out.println("CAE: " + response.cae().orElse(""));
    System.out.println("Vence: " + response.expirationDate().orElse(""));
} else {
    response.errors().forEach(e ->
        System.err.printf("Error %d: %s%n", e.code(), e.message()));
}
```

### Health check

```java
boolean available = wsfe.ping();
System.out.println("ARCA disponible: " + available);
```

### Listar puntos de venta

```java
List<SalesPoint> salesPoints = wsfe.getSalesPoints();
for (SalesPoint sp : salesPoints) {
    System.out.printf("Pto %d | Emisión: %s | Bloqueado: %s%n",
        sp.numero(), sp.emisionTipo(),
        sp.blocked() ? "Sí" : "No");
}
```

## Configuración avanzada

### Timeouts personalizados

```java
ArcaConfig config = ArcaConfig.builder()
    .connectTimeout(Duration.ofSeconds(10))
    .readTimeout(Duration.ofSeconds(30))
    .sanitizeLogs(true)
    .build();

CertificateSource source = Pkcs12CertificateSource.fromPath(
    Paths.get("certificado.p12"),
    "password".toCharArray());

ArcaClient arca = ArcaClient.builder()
    .config(config)
    .certificate(source)
    .build();
```

### Reloj fijo para tests

```java
ArcaClock clock = FixedClock.withFixed(Instant.parse("2026-01-15T10:00:00Z"));

ArcaConfig config = ArcaConfig.builder()
    .environment(ArcaEnvironment.HOMOLOGACION)
    .cuit(Cuit.parse("20-33333333-9"))
    .clock(clock)
    .build();

CertificateSource source = Pkcs12CertificateSource.fromPath(
    Paths.get("certificado.p12"),
    "password".toCharArray());

ArcaClient arca = ArcaClient.builder()
    .config(config)
    .certificate(source)
    .build();
```

### Manejo de errores tipados

```java
try {
    wsfe.requestCae(request);
} catch (ArcaAuthException e) {
    switch (e.errorCode()) {
        case TA_EXPIRED -> System.err.println("Ticket expirado, reautenticando...");
        case CERTIFICATE_EXPIRED -> System.err.println("Certificado vencido");
        case AUTH_FAILED -> System.err.println("Fallo de autenticación: " + e.getMessage());
    }
} catch (ArcaSoapException e) {
    switch (e.errorCode()) {
        case SOAP_TIMEOUT -> System.err.println("Timeout en la llamada SOAP");
        case SOAP_FAULT -> System.err.println("SOAP Fault: " + e.getMessage());
    }
} catch (ArcaValidationException e) {
    System.err.println("Datos inválidos: " + e.getMessage());
}
```

### Variables de entorno

| Variable | Descripción |
|---|---|
| `ARCA_CERT_PATH` | Ruta al certificado PKCS#12 (.p12) |
| `ARCA_CERT_PASSWORD` | Contraseña del keystore |
| `ARCA_CUIT` | CUIT del contribuyente (sin guiones) |
| `ARCA_ENVIRONMENT` | `homologacion` o `produccion` (default: homologacion) |

## Seguridad

- No registrar tokens, firmas, claves privadas ni contraseñas de keystore.
- Redactar datos sensibles antes de emitir logs o errores.
- Validar datos de entrada antes de construir requests SOAP.
- Encapsular excepciones externas en errores propios del SDK.
- Mantener tests para sanitización, fixtures y contratos públicos.

## Recursos oficiales

| Recurso | URL |
|---|---|
| Web Services SOAP ARCA | https://www.arca.gob.ar/ws/ |
| Documentación Web Services SOAP | https://www.arca.gob.ar/ws/documentacion/ws-factura-electronica.asp |
| Factura electrónica ARCA | https://www.arca.gob.ar/fe/ |
| Homologación externa ARCA | https://www.arca.gob.ar/ws/documentacion/homologacion-externa.asp |
| Manual WSFEv1 vigente — RG 4291 / Proyecto FE v4.3 | https://www.arca.gob.ar/fe/ayuda/documentos/wsfev1-RG-4291.pdf |
| WSDL WSFEv1 homologación | https://wswhomo.afip.gov.ar/wsfev1/service.asmx?WSDL |
| WSDL WSFEv1 producción | https://servicios1.afip.gov.ar/wsfev1/service.asmx?WSDL |
| WSAA — Autenticación y Autorización | https://www.arca.gob.ar/ws/documentacion/wsaa.asp |
| Certificados / WSASS homologación | https://www.arca.gob.ar/ws/documentacion/certificados.asp |

## Licencia

Apache License 2.0. Ver [LICENSE](LICENSE).
