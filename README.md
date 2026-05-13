# ARCA SDK Java

[![Java](https://img.shields.io/badge/Java-21-blue)](https://jdk.java.net/21/)
[![version](https://img.shields.io/github/v/tag/fr4ncisx/arca-sdk-java?style=flat&label=version&sort=semver)]()
[![build](https://img.shields.io/github/actions/workflow/status/fr4ncisx/arca-sdk-java/ci.yml?branch=main)]()
[![licencia](https://img.shields.io/badge/licencia-Apache%202.0-blue)](LICENSE)

SDK Java puro para integrarse con los servicios web de **ARCA** (ex AFIP).
Cubre el flujo completo de autenticación WSAA, transporte SOAP, modelos tipados
para facturación electrónica (WSFEv1) y manejo seguro de datos sensibles en
registros de auditoría.

Está diseñado para ser **agnóstico de framework**: funciona en aplicaciones Java
puras, Spring Boot, herramientas CLI o cualquier runtime que ejecute Java 21+.

## Requisitos

| Herramienta | Versión |
|---|---|
| Java | 21 o superior |
| Maven | 3.9 o superior |

## Módulos

**`arca-sdk-bundle` es el módulo recomendado para consumidores externos.**
Agrupa todos los módulos runtime en una sola dependencia. Los módulos
individuales existen para desarrollo interno y mantenibilidad del SDK.

| Módulo | Responsabilidad |
|---|---|
| `arca-sdk-core` | Configuración, enumeraciones de ambiente, jerarquía de excepciones sellada, modelos compartidos y sanitización de logs |
| `arca-sdk-soap` | Cliente SOAP genérico, handlers JAX-WS, configuración de timeouts y adaptadores de transporte |
| `arca-sdk-wsaa` | Autenticación contra WSAA: generación del TRA, firma CMS/PKCS#7, invocación a LoginCms y gestión de tickets con caché |
| `arca-sdk-wsfev1` | Integración con facturación electrónica WSFEv1: consulta de último comprobante, solicitud de CAE y modelos de negocio tipados |
| `arca-sdk-test-support` | Cliente mock basado en WireMock, fixtures XML de ejemplo y utilidades para escribir tests sin conexión a ARCA |
| `arca-sdk-bom` | Bill of Materials que centraliza las versiones de todos los módulos y sus dependencias externas |
| `arca-sdk-bundle` | Artefacto de conveniencia (packaging POM) que agrupa los módulos runtime en una sola dependencia |

## Arquitectura

El SDK sigue una arquitectura hexagonal con tres capas bien definidas:

```
┌─────────────────────────────────────────────────┐
│                   API Pública                     │
│  ArcaClient, WsfeClient, ArcaEnvironment,        │
│  LastVoucherRequest, CaeRequest, CaeResponse     │
├─────────────────────────────────────────────────┤
│                Capa de Aplicación                 │
│  DefaultAuthProvider, GetLastVoucherUseCase,      │
│  RequestCaeUseCase, BatchProcessUseCase          │
├─────────────────────────────────────────────────┤
│              Capa de Infraestructura              │
│  ArcaSoapClient (Metro/JAX-WS), LoginCmsClient,  │
│  CmsSigner, TicketCache, MetricsSoapPort         │
├─────────────────────────────────────────────────┤
│              Generado (aislado)                   │
│  Stubs JAXB desde WSDL oficial de ARCA           │
└─────────────────────────────────────────────────┘
```

Puntos clave de la arquitectura:

- **La API pública no expone tipos generados**: los stubs JAXB del WSDL están
  encapsulados en el paquete `internal.generated` y nunca atraviesan la frontera
  pública.
- **Los puertos (interfaces) están en la capa de aplicación**: `AuthProvider`,
  `TicketCache`, `ArcaSoapPort` son interfaces definidas en el dominio, no en la
  infraestructura.
- **Las excepciones son selladas**: `ArcaException` es una jerarquía sellada con
  `ArcaAuthException`, `ArcaSoapException` y `ArcaValidationException`, lo que
  permite a los consumidores hacer `switch` exhaustivo sobre el tipo de error.
- **Las métricas son neutrales**: `ArcaMetrics` se define en `core` sin depender
  de SOAP ni de ninguna implementación concreta.

## Versionado

Cada milestone se libera cuando todas las tareas del backlog asignadas a esa
versión están completas y superan los criterios definidos en
`milestoneDefinitions`. La progresión es:

1. **Batches de desarrollo incremental** (`0.1.0-M1`, `0.1.0-M2`, etc.)
2. **Release candidate** (`0.1.0-rc1`) para validación final
3. **Release oficial** (`0.1.0`) cuando el RC está verificado
4. **Hotfix** (`0.1.1`) si aparece un bug después del release oficial

## Compilar

```bash
mvn clean verify
```

Para regenerar las clases Java a partir del WSDL oficial de ARCA:

```bash
mvn clean verify -Pgenerate-stubs
```

Para ejecutar los tests de integración contra credenciales reales de ARCA
(requiere certificado digital configurado):

```bash
mvn verify -Darca.integration=true
```

Para compilar y probar un módulo específico:

```bash
mvn test -pl arca-sdk-core
```

## Cómo usar el SDK

### Configuración básica

```java
ArcaClient arca = ArcaClient.builder()
    .environment(ArcaEnvironment.HOMOLOGACION)
    .cuit(20333333339L)
    .certificate(Paths.get("certificado.p12"), "password".toCharArray())
    .connectTimeout(Duration.ofSeconds(10))
    .build();
```

### Consultar el último comprobante

```java
private static final System.Logger LOG = System.getLogger("WsfeClient");

WsfeClient wsfe = arca.wsfev1();
LastVoucherResponse response = wsfe.getLastAuthorizedVoucher(
    new LastVoucherRequest(1, VoucherType.FACTURA_A));
LOG.log(System.Logger.Level.INFO, "Último comprobante: {0}", response.cbteNro());
```

### Solicitar un CAE

```java
CaeResponse cae = wsfe.requestCae(new CaeRequest(
    1, VoucherType.FACTURA_A, ConceptType.PRODUCTO,
    new MonetaryAmount(10000.00), LocalDate.now()));
if (cae.success()) {
    LOG.log(System.Logger.Level.INFO, "CAE asignado: {0}", cae.cae().orElseThrow());
} else {
    LOG.log(System.Logger.Level.ERROR, "Errores: {0}", cae.errores());
}
```

## Seguridad

El SDK está diseñado para no exponer datos sensibles en registros de auditoría
ni en mensajes de error:

- El token y la firma del ticket de acceso siempre se redactan en logs
- Las claves privadas nunca se registran
- Las contraseñas del keystore nunca se registran
- Los mensajes CMS firmados completos nunca se registran
- Los XML SOAP de producción nunca se registran en texto plano
- Las excepciones no incluyen tokens base64 ni firmas en sus mensajes

## Recursos útiles

| Recurso | URL |
|---|---|
| Portal Web Services ARCA | https://www.afip.gob.ar/ws/documentacion/ |
| WSAA - Documentación oficial | https://www.afip.gob.ar/ws/documentacion/wsaa.asp |
| WSAA - Especificación técnica (PDF) | https://www.afip.gob.ar/ws/wsaa/especificacion_tecnica_wsaa_1.2.2.pdf |
| WSAA - Manual del desarrollador (PDF) | https://www.afip.gob.ar/ws/WSAA/WSAAmanualDev.pdf |
| WSAA - WSDL (homologación) | https://wsaahomo.afip.gov.ar/ws/services/LoginCms?wsdl |
| WSFEv1 - Documentación oficial | https://www.afip.gob.ar/ws/documentacion/ws-factura-electronica.asp |
| WSFEv1 - Referencia de operaciones | https://servicios1.afip.gov.ar/wsfev1/service.asmx |
| WSFEv1 - Manual del desarrollador (PDF) | https://www.afip.gob.ar/fe/ayuda/documentos/Manual-desarrollador-V.2.21.pdf |
| WSASS - Autoservicio de certificados | https://auth.afip.gob.ar/contribuyente_/login.xhtml |
| JDK 21 | https://jdk.java.net/21/ |
| Maven | https://maven.apache.org/ |

## Licencia

Apache License 2.0. Ver [LICENSE](LICENSE).
