# ARCA SDK Java

[![Java](https://img.shields.io/badge/Java-21-blue)](https://jdk.java.net/21/)
[![version](https://img.shields.io/github/v/tag/fr4ncisx/arca-sdk-java?style=flat&label=version&sort=semver)]()
[![build](https://img.shields.io/github/actions/workflow/status/fr4ncisx/arca-sdk-java/ci.yml?branch=main)]()
[![licencia](https://img.shields.io/badge/licencia-Apache%202.0-blue)](LICENSE)

SDK Java puro para integrarse con servicios web de **ARCA** mediante una API
agnóstica de framework. El objetivo del proyecto es cubrir autenticación WSAA,
transporte SOAP, facturación electrónica WSFEv1 y servicios complementarios sin
exponer stubs generados ni datos sensibles.

El repositorio está en desarrollo activo. La versión actual `0.1.1-M2` no está
lista para producción: contiene la base técnica del reactor Maven, parte de
`core`, soporte de fixtures y el WSDL de WSFEv1 versionado. WSAA end to end,
cliente SOAP reusable, clientes WSFEv1 y `registry` siguen en backlog.

## Estado actual

Implementado hoy:

- Reactor Maven multi-módulo con Java 21.
- `arca-sdk-core` con ambiente, reloj configurable y jerarquía base de
  excepciones.
- `arca-sdk-test-support` con carga de fixtures XML y datos de prueba.
- `arca-sdk-wsfev1` con WSDL oficial versionado en `src/main/resources/wsdl`.
- Módulos `wsaa`, `soap`, `registry`, `bom` y `bundle` creados como estructura
  base.

Pendiente antes de uso real:

- Crear el flujo completo de autenticación WSAA, firma CMS y caché de tickets.
- Crear el transporte SOAP reusable con timeouts, handlers y sanitización.
- Crear `WsfeClient`, `ArcaClient` y casos de uso de consulta/emisión.
- Crear validaciones de negocio para comprobantes, puntos de venta, monedas,
  conceptos, IVA, CAE y CAEA.
- Validar integración con certificados reales en homologación de ARCA.

## Requisitos

| Herramienta | Versión |
|---|---|
| Java | 21 o superior |
| Maven | 3.9 o superior |

## Módulos

| Módulo | Estado | Responsabilidad |
|---|---|---|
| `arca-sdk-core` | Parcial | Tipos compartidos, ambientes, errores, reloj, sanitización y utilidades comunes |
| `arca-sdk-soap` | Scaffold | Transporte SOAP común, handlers JAX-WS, timeouts y adaptación de errores |
| `arca-sdk-wsaa` | Scaffold | TRA, firma CMS/PKCS#7, `LoginCms`, tickets y renovación automática |
| `arca-sdk-wsfev1` | Scaffold + WSDL | API de facturación electrónica WSFEv1, mappers, modelos y casos de uso |
| `arca-sdk-registry` | Scaffold | Futuras consultas tributarias y registrales |
| `arca-sdk-test-support` | Parcial | Fixtures XML, utilidades de test y soporte para mocks |
| `arca-sdk-bom` | Scaffold | Bill of Materials para centralizar versiones |
| `arca-sdk-bundle` | Scaffold | Dependencia de conveniencia para consumidores externos |

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

## API pública objetivo

Los siguientes nombres describen la superficie buscada para consumidores del
SDK. Todavía no deben tomarse como API disponible hasta que el backlog los
marque implementados.

```java
ArcaClient arca = ArcaClient.builder()
    .environment(ArcaEnvironment.HOMOLOGACION)
    .cuit(20333333339L)
    .certificate(Paths.get("certificado.p12"), "password".toCharArray())
    .build();

WsfeClient wsfe = arca.wsfev1();
LastVoucherResponse last = wsfe.getLastAuthorizedVoucher(
    new LastVoucherRequest(1, VoucherType.FACTURA_A));
```

## Seguridad

- No registrar tokens, firmas, claves privadas ni contraseñas de keystore.
- Redactar datos sensibles antes de emitir logs o errores.
- Validar datos de entrada antes de construir requests SOAP.
- Encapsular excepciones externas en errores propios del SDK.
- Mantener tests para sanitización, fixtures y contratos públicos.

## Recursos oficiales

| Recurso | URL |
|---|---|
| Web Services SOAP ARCA | https://www.arca.gob.ar/ws/documentacion/ |
| Factura electrónica ARCA | https://www.arca.gob.ar/fe/ |
| Ayuda WSFEv1 | https://www.arca.gob.ar/fe/ayuda/webservice.asp |
| Manual WSFEv1 vigente listado por ARCA | https://www.arca.gob.ar/fe/ayuda/documentos/wsfev1-RG-4291.pdf |
| WSDL WSFEv1 homologación | https://wswhomo.afip.gov.ar/wsfev1/service.asmx?WSDL |
| WSDL WSFEv1 producción | https://servicios1.afip.gov.ar/wsfev1/service.asmx?WSDL |
| WSASS certificados homologación | https://www.arca.gob.ar/ws/documentacion/certificados.asp |

## Licencia

Apache License 2.0. Ver [LICENSE](LICENSE).
