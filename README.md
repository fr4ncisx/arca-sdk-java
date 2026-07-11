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
            <version>0.5.0-M1</version>
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
        <version>0.5.0-M1</version>
    </dependency>
</dependencies>
```

## Uso

> [!NOTE]
> En esta versión del SDK, la fachada raíz `ArcaClient` y su builder están planificados para la siguiente fase. Para interactuar con los servicios, el cliente `WsfeClient` se inicializa y ensambla de forma manual inyectando los componentes de WSAA e infraestructura.

### Inicialización del Cliente

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

AuthProvider authProvider = new DefaultAuthProvider(
    new PersistentTicketCache(Paths.get("tickets-cache")),
    new TraGenerator(SystemClock.INSTANCE),
    new CmsSigner(source),
    new LoginCmsClient(config)
);

WsfeClient wsfe = WsfeClientAssembler.assemble(config, authProvider);
```

### Consultar último comprobante autorizado

```java
LastVoucherResponse last = wsfe.getLastVoucher(
    new LastVoucherRequest(1, VoucherType.INVOICE_A)
);

System.out.printf("Último comprobante autorizado: %d%n", last.voucherNumber());
```

### Solicitar CAE

```java
List<CaeVatLine> vatLines = List.of(
    new CaeVatLine(VatType.VAT_21, 1000.00, 210.00)
);

CaeRequest request = new CaeRequest(
    VoucherType.INVOICE_A,
    1,
    123456L,
    ConceptType.PRODUCTS,
    Cuit.parse("20-44444444-9"),
    1000.00,
    0.0,
    0.0,
    210.00,
    1210.00,
    LocalDate.now(),
    vatLines
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

## Configuración avanzada

### Endpoints personalizados (Mocks / WireMock)

Si necesitas redirigir las llamadas de SOAP a un servidor local o mock en tus entornos de pruebas, puedes pasar la URL del endpoint al ensamblador:

```java
String customWsfeUrl = "http://localhost:8080/wsfev1";
WsfeClient wsfe = WsfeClientAssembler.assemble(config, authProvider, customWsfeUrl);
```

### Manejo de errores tipados

El SDK mapea las excepciones de transporte SOAP, fallos de autenticación de WSAA y validaciones a excepciones de dominio del SDK con códigos estructurados de la enumeración `ArcaErrorCode`:

```java
try {
    wsfe.requestCae(request);
} catch (ArcaAuthException e) {
    switch (e.errorCode()) {
        case TAEXPIRED -> System.err.println("Ticket expirado");
        case CERTIFICATEEXPIRED -> System.err.println("Certificado vencido o inválido");
        case AUTHFAILED -> System.err.println("Fallo de autenticación: " + e.getMessage());
    }
} catch (ArcaSoapException e) {
    switch (e.errorCode()) {
        case SOAPTIMEOUT -> System.err.println("Timeout en la llamada SOAP");
        case SOAPFAULT -> System.err.println("SOAP Fault retornado por ARCA: " + e.getMessage());
    }
} catch (ArcaValidationException e) {
    System.err.println("Datos inválidos locales: " + e.getMessage());
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
