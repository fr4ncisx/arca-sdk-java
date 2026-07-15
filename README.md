# ARCA SDK Java

[![Java](https://img.shields.io/badge/Java-21-orange)](https://jdk.java.net/21/)
[![version](https://img.shields.io/github/v/tag/fr4ncisx/arca-sdk-java?style=flat&label=version&color=green)]()
[![status](https://img.shields.io/badge/status-early--release-green)]()
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
| `arca-sdk-wsfexv1` | API de facturación electrónica de exportación WSFEXv1, mappers, modelos y casos de uso |
| `arca-sdk-wsmtxca` | API de facturación electrónica matricial con detalle de artículos WSMTXCA, mappers, modelos y casos de uso |
| `arca-sdk-wscdc` | API de constatación de comprobantes WSCDC, mappers, modelos y casos de uso |
| `arca-sdk-registry` | Consultas tributarias y registrales (ws_sr_padron_a4) |
| `arca-sdk-client` | Fachada unificada y punto de entrada raíz del SDK |
| `arca-sdk-test-support` | Fixtures XML, utilidades de test y soporte para mocks (WireMock) |
| `arca-sdk-bom` | Bill of Materials (BOM) para centralizar versiones |
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

## Diseño y Alcance de la Versión Final (Objetivo)

La versión final del SDK unificará todos los submódulos bajo una API fluida, altamente automatizada y auto-configurable para entornos empresariales.

### 1. Fachada Raíz Unificada (`ArcaClient`)
Toda la interacción con el SDK se canalizará a través de una única clase fachada (`ArcaClient`), construida mediante un Builder fluido que valida la presencia de la configuración básica y el origen de los certificados (sin realizar conexiones de red prematuras):
```java
ArcaConfig config = new ArcaConfig(
    Cuit.parse("20-33333333-9"),
    ArcaEnvironment.HOMOLOGACION,
    Duration.ofSeconds(10),
    Duration.ofSeconds(30)
);

CertificateSource source = Pkcs12CertificateSource.fromPath(
    Paths.get("certificado.p12"),
    "password".toCharArray()
);

ArcaClient arca = ArcaClient.builder()
    .config(config)
    .certificate(source)
    .build();
```

A partir de esta fachada, se podrá acceder a los diferentes servicios web de ARCA integrados:
* `WsfeClient wsfe = arca.wsfev1();` — Para operaciones de facturación electrónica.
* `RegistryClient registry = arca.registry();` — Para consultas tributarias de contribuyentes.

### 2. Autenticación y Renovación Transparente de Tokens (WSAA)
El consumidor no gestionará firmas ni almacenamiento de credenciales. Al invocar cualquier servicio de negocio, el cliente raíz:
* Generará el XML de ticket de requerimiento de acceso (TRA).
* Firmará criptográficamente el payload utilizando CMS (PKCS#7) con la clave privada provista.
* Obtendrá el ticket de acceso (TA) desde WSAA.
* Administrará la caché local (en memoria y serializada de forma segura en disco) y renovará el token de forma automática y asíncrona antes de su vencimiento para evitar demoras en las transacciones de facturación.

### 3. Consulta de Padrones de Contribuyentes (`ws_sr_padron_a4`)
El módulo `arca-sdk-registry` expondrá una API para consultar el padrón de ARCA utilizando el CUIT del receptor, devolviendo de forma estructurada su condición tributaria (IVA, monotributo, exento), razón social y domicilio fiscal, facilitando la validación automática previa a la emisión de facturas A o B.

### 4. Procesamiento por Lotes y Concurrencia
Orquestación avanzada para el envío masivo de comprobantes fiscales, resolviendo automáticamente las restricciones de la API SOAP de ARCA:
* Control de concurrencia y secuenciación estricta de números de comprobantes.
* Agrupamiento automático en lotes según los límites del web service.
* Estrategia de reintentos seguros (idempotentes) ante timeouts o caídas temporales del servidor de ARCA.

### 5. Integración Nativa con Spring Boot (`arca-sdk-spring-boot-starter`)
Un starter autoconfigurable que detectará las propiedades tipadas en `application.yml` y registrará automáticamente en el contexto de Spring:
* Beans inyectables de tipo `ArcaClient`, `WsfeClient` y `RegistryClient`.
* Soporte nativo para perfiles (homologación/producción) y configuración de múltiples contribuyentes de forma declarativa.

## Análisis de Calidad con SonarQube

El proyecto incluye soporte integrado para análisis estático de código mediante **SonarQube** y un entorno local en Docker.

### 1. Iniciar SonarQube localmente (Docker)
Levanta la instancia oficial LTS de SonarQube en el puerto `9000`:

```bash
docker compose up -d
```

### 2. Ejecutar análisis estático
Una vez que SonarQube esté disponible en `http://localhost:9000` y hayas generado tu token de acceso en el panel de administración, ejecuta:

```bash
mvn clean compile sonar:sonar -Dsonar.host.url=http://localhost:9000 -Dsonar.token=tu_token_de_acceso
```

*(Las exclusiones del código JAXB autogenerado por el WSDL ya se encuentran preconfiguradas en el POM padre para evitar ruidos de análisis).*

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
            <version>1.0.0</version>
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

    <!-- Fachada del cliente unificado -->
    <dependency>
        <groupId>io.github.fr4ncisx</groupId>
        <artifactId>arca-sdk-client</artifactId>
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
        <version>1.0.0</version>
    </dependency>
</dependencies>
```

## Uso

### Inicialización del Cliente

El punto de entrada principal y unificado del SDK es `ArcaClient`. Se inicializa usando un constructor fluido (Builder) que encapsula internamente la firma CMS, la comunicación con WSAA, el caché automático de tickets y el mapeo de sub-clientes:

```java
import io.github.fr4ncisx.arca.client.spi.ArcaClient;
import io.github.fr4ncisx.arca.core.config.ArcaConfig;
import io.github.fr4ncisx.arca.core.config.ArcaEnvironment;
import io.github.fr4ncisx.arca.core.model.Cuit;
import io.github.fr4ncisx.arca.core.security.CertificateSource;
import io.github.fr4ncisx.arca.core.security.Pkcs12CertificateSource;
import io.github.fr4ncisx.arca.wsfev1.spi.WsfeClient;
import java.nio.file.Paths;
import java.time.Duration;

// 1. Configuración básica
ArcaConfig config = new ArcaConfig(
    Cuit.parse("20-33333333-9"),
    ArcaEnvironment.HOMOLOGACION,
    Duration.ofSeconds(10), // Timeout de conexión
    Duration.ofSeconds(30)  // Timeout de lectura
);

// 2. Origen del certificado (archivo PKCS#12 .p12 o .pfx)
CertificateSource source = Pkcs12CertificateSource.fromPath(
    Paths.get("certificado.p12"),
    "password".toCharArray()
);

// 3. Construcción del cliente raíz
ArcaClient arca = ArcaClient.builder()
    .config(config)
    .certificate(source)
    .build();

// 4. Obtención de sub-cliente de facturación (WSFEv1)
WsfeClient wsfe = arca.wsfev1();
```

### 1. Facturación Electrónica Nacional (WSFEv1)

Permite consultar el último comprobante autorizado e iniciar solicitudes de CAE (Código de Autorización Electrónico) para facturación local.

```java
import io.github.fr4ncisx.arca.wsfev1.model.common.VoucherType;
import io.github.fr4ncisx.arca.wsfev1.model.lastvoucher.LastVoucherRequest;
import io.github.fr4ncisx.arca.wsfev1.model.lastvoucher.LastVoucherResponse;

WsfeClient wsfe = arca.wsfev1();

// Consultar último comprobante autorizado (Punto de venta 1, Factura A)
LastVoucherResponse last = wsfe.getLastVoucher(
    new LastVoucherRequest(1, VoucherType.INVOICE_A)
);
System.out.printf("Último comprobante autorizado: %d%n", last.voucherNumber());
```

Ejemplo de solicitud de CAE:
```java
import io.github.fr4ncisx.arca.wsfev1.model.cae.*;
import io.github.fr4ncisx.arca.wsfev1.model.common.*;
import java.time.LocalDate;
import java.util.List;

List<CaeVatLine> vatLines = List.of(
    new CaeVatLine(VatType.VAT_21, 1000.00, 210.00) // Base imponible: 1000, IVA 21%: 210
);

CaeRequest request = new CaeRequest(
    VoucherType.INVOICE_A,
    1,                           // Punto de venta
    123456L,                     // Número de comprobante a autorizar
    ConceptType.PRODUCTS,
    Cuit.parse("20-44444444-9"), // CUIT del comprador
    1000.00,                     // Neto gravado
    0.0,                         // Exento
    0.0,                         // Tributos
    210.00,                      // Importe IVA
    1210.00,                     // Importe total
    LocalDate.now(),             // Fecha comprobante
    vatLines
);

CaeResponse response = wsfe.requestCae(request);
if (response.success()) {
    System.out.println("CAE asignado: " + response.cae().orElse(""));
    System.out.println("Vencimiento: " + response.expirationDate().orElse(""));
} else {
    response.errors().forEach(e ->
        System.err.printf("Error %d: %s%n", e.code(), e.message()));
}
```

### 2. Consulta de Padrones de Contribuyentes (Registry / ws_sr_padron_a4)

Permite consultar en tiempo real los datos tributarios, fiscales y regímenes especiales de cualquier contribuyente utilizando su CUIT.

```java
import io.github.fr4ncisx.arca.registry.spi.RegistryClient;
import io.github.fr4ncisx.arca.registry.model.TaxpayerData;

RegistryClient registry = arca.registry();

TaxpayerData taxpayer = registry.getTaxpayer(Cuit.parse("20-44444444-9"));
System.out.println("Razón Social: " + taxpayer.businessName());
System.out.println("Estado de la Clave: " + taxpayer.keyState());
System.out.println("Tipo de Persona: " + taxpayer.personType()); // "FISICA" o "JURIDICA"
```

### 3. Facturación de Exportación (WSFEXv1)

Permite emitir comprobantes tipo E para transacciones internacionales y operaciones de exportación.

```java
import io.github.fr4ncisx.arca.wsfexv1.spi.WsfexClient;
import io.github.fr4ncisx.arca.wsfexv1.model.LastExportVoucherRequest;
import io.github.fr4ncisx.arca.wsfexv1.model.LastExportVoucherResponse;

WsfexClient wsfex = arca.wsfexv1();

// Consultar último comprobante de exportación autorizado
LastExportVoucherResponse lastExport = wsfex.getLastVoucher(
    new LastExportVoucherRequest(VoucherType.EXPORT_INVOICE_E, 2) // Tipo E, Punto de venta 2
);
System.out.println("Última Factura E autorizada: " + lastExport.voucherNumber());
```

### 4. Facturación con Detalle de Artículos (WSMTXCA)

Permite gestionar la emisión de comprobantes que requieren la declaración mandatoria de códigos y detalles a nivel de ítem/artículo.

```java
import io.github.fr4ncisx.arca.wsmtxca.spi.WsmtxcaClient;
import io.github.fr4ncisx.arca.wsmtxca.model.WsmtxcaLastVoucherRequest;
import io.github.fr4ncisx.arca.wsmtxca.model.WsmtxcaLastVoucherResponse;

WsmtxcaClient wsmtxca = arca.wsmtxca();

WsmtxcaLastVoucherResponse lastMtx = wsmtxca.getLastVoucher(
    new WsmtxcaLastVoucherRequest(1, VoucherType.INVOICE_A)
);
System.out.println("Último Comprobante MTXCA: " + lastMtx.voucherNumber());
```

### 5. Constatación de Comprobantes (WSCDC)

Permite validar la autenticidad y vigencia de cualquier comprobante fiscal electrónico emitido (comprobante emitido con CAE o CAEA).

```java
import io.github.fr4ncisx.arca.wscdc.spi.WscdcClient;
import io.github.fr4ncisx.arca.wscdc.model.WscdcDummyResponse;

WscdcClient wscdc = arca.wscdc();

// Health check del servicio de constatación
WscdcDummyResponse dummy = wscdc.dummy();
System.out.println("Estado Servidor: " + dummy.appServer());
System.out.println("Estado Base Datos: " + dummy.dbServer());
```

---

## Configuración avanzada

### Endpoints personalizados (Mocks / Entornos Locales)

Si necesitas redirigir las llamadas SOAP a un servidor local o mock, puedes inyectar las URLs directamente en el builder:

```java
ArcaClient arca = ArcaClient.builder()
    .config(config)
    .certificate(source)
    .wsaaUrl("http://localhost:8080/wsaa")
    .wsfeUrl("http://localhost:8080/wsfev1")
    .build();
```

### Manejo de errores tipados

El SDK atrapa las fallas de red, excepciones SOAP y rechazos de ARCA, devolviendo excepciones jerárquicas tipadas:

```java
try {
    wsfe.requestCae(request);
} catch (ArcaAuthException e) {
    // Error en WSAA (credenciales, expiración del token)
    System.err.println("Fallo de autenticación: " + e.getMessage());
} catch (ArcaSoapException e) {
    // Error de red, DNS o SOAP Fault del servidor de ARCA
    System.err.println("Error SOAP: " + e.getMessage());
} catch (ArcaValidationException e) {
    // Error de validación local previa (ej. CUIT inválido, montos inconsistentes)
    System.err.println("Datos locales inválidos: " + e.getMessage());
}
```

---

## Obtener un Certificado de Homologación (Paso a Paso)

Para interactuar con el entorno de pruebas de ARCA, necesitas un certificado digital provisto por ellos:

1. **Generar la Clave Privada**:
   ```bash
   openssl genrsa -out privada.key 2048
   ```
2. **Generar el archivo de solicitud de firma (CSR)**:
   *(Reemplaza el CUIT y nombre de la app)*:
   ```bash
   openssl req -new -key privada.key -subj "/C=AR/O=Empresa/CN=MiApp/serialNumber=CUIT 20333333339" -out pedido.csr
   ```
3. **Subir el CSR a ARCA**:
   * Ingresa con Clave Fiscal al portal de AFIP/ARCA.
   * Agrega el servicio **Administración de Relaciones de Clave Fiscal**.
   * Crea un nuevo Alias/Servicio y sube el archivo `pedido.csr`.
   * Descarga el certificado firmado (`.crt` o `.pem`) provisto por ARCA.
4. **Exportar a PKCS#12 (.p12)**:
   Empaqueta la clave privada y el certificado firmado en un único almacén seguro compatible con el SDK:
   ```bash
   openssl pkcs12 -export -inkey privada.key -in certificado.crt -out certificado.p12 -name "arca-cert"
   ```
   *(Este archivo `certificado.p12` y su contraseña son los que consume `Pkcs12CertificateSource`)*.


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
