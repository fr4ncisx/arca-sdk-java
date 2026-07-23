# ARCA SDK Java

[![Java](https://img.shields.io/badge/Java-21-orange)](https://jdk.java.net/21/)
[![version](https://img.shields.io/github/v/tag/fr4ncisx/arca-sdk-java?style=flat&label=version&color=green)]()
[![status](https://img.shields.io/badge/status-beta--release-green)]()
[![build](https://img.shields.io/github/actions/workflow/status/fr4ncisx/arca-sdk-java/ci.yml?branch=main)]()
[![licencia](https://img.shields.io/badge/licencia-Apache%202.0-blue)](LICENSE)

SDK Java para consumir los Web Services de **ARCA** (ex AFIP) mediante una API tipada, estable y orientada a uso productivo. El objetivo es encapsular la complejidad de SOAP, XML, WSAA y firma criptográfica detrás de una fachada limpia.

**Características principales**

- **Facturación electrónica**: solicitudes de CAE y CAEA para comprobantes de uso habitual.
- **Consulta de comprobantes**: acceso al último comprobante autorizado y al estado de emisión.
- **Validación tributaria**: consulta de datos de contribuyentes mediante el padrón oficial.
- **Servicios complementarios**: facturación de exportación, detalle de artículos y constatación de comprobantes.
- **Autenticación y transporte encapsulados**: gestión de WSAA, tickets, firma CMS, persistencia de sesión y errores tipados.

**Alcance del SDK**

El consumidor trabaja con modelos propios del proyecto y evita interactuar directamente con WSDLs, stubs JAXB, XML de autenticación, manejo manual de tickets o sanitización artesanal de logs y errores.

## Requisitos

| Herramienta | Versión |
|---|---|
| Java | 21 o superior |
| Maven | 3.9 o superior |

## Configuración por variables de entorno

El SDK puede configurarse mediante variables de entorno o propiedades externas. En Spring Boot, el bloque `arca` puede resolverse a partir del binding estándar del framework.

| Variable | Descripción |
|---|---|
| `ARCA_CUIT` | CUIT del contribuyente |
| `ARCA_ENVIRONMENT` | `HOMOLOGACION` o `PRODUCCION` |
| `ARCA_CERTIFICATE_LOCATION` | Ubicación del certificado PKCS#12 (`classpath:`, `file:`, etc.) |
| `ARCA_CERTIFICATE_PASSWORD` | Contraseña del certificado |
| `ARCA_CONNECT_TIMEOUT` | Timeout de conexión, por ejemplo `10s` |
| `ARCA_READ_TIMEOUT` | Timeout de lectura, por ejemplo `30s` |
| `ARCA_RESILIENCE_ENABLED` | Activa o desactiva la resiliencia del transporte |
| `ARCA_TICKET_CACHE_DIR` | Directorio para persistir tickets de WSAA entre reinicios (opcional) |

Ejemplo de configuración en Windows:

```bash
setx ARCA_CUIT 20333333339
setx ARCA_ENVIRONMENT HOMOLOGACION
setx ARCA_CERTIFICATE_LOCATION classpath:cert.p12
setx ARCA_CERTIFICATE_PASSWORD tu_clave
setx ARCA_TICKET_CACHE_DIR "%USERPROFILE%\.arca-sdk\ticket-cache"
```

## Módulos

| Módulo | Responsabilidad |
|---|---|
| `arca-sdk-core` | Tipos compartidos, ambientes, errores, reloj, sanitización y utilidades comunes |
| `arca-sdk-soap` | Transporte SOAP común, SPI `ArcaSoapPort`, resiliencia y adaptación de errores |
| `arca-sdk-wsaa` | TRA, firma CMS/PKCS#7, `LoginCms`, tickets y renovación automática |
| `arca-sdk-wsfev1` | API de facturación electrónica WSFEv1, mappers, modelos y casos de uso |
| `arca-sdk-wsfexv1` | API de facturación electrónica de exportación WSFEXv1, mappers, modelos y casos de uso |
| `arca-sdk-wsmtxca` | API de facturación electrónica matricial con detalle de artículos WSMTXCA, mappers, modelos y casos de uso |
| `arca-sdk-wscdc` | API de constatación de comprobantes WSCDC, mappers, modelos y casos de uso |
| `arca-sdk-registry` | Consultas tributarias y registrales (ws_sr_padron_a4) |
| `arca-sdk-client` | Fachada unificada y punto de entrada raíz del SDK |
| `arca-sdk-spring-boot-starter` | Autoconfiguración de Spring Boot para exponer `ArcaClient`, `WsfeClient` y `RegistryClient` |
| `arca-sdk-test-support` | Fixtures XML, utilidades de test y soporte para mocks (WireMock) |
| `arca-sdk-bom` | Bill of Materials (BOM) para centralizar versiones |
| `arca-sdk-bundle` | Dependencia de conveniencia para consumidores externos |

## Instalación

Este SDK no está publicado en Maven Central. Primero instálalo desde el código fuente:

```bash
git clone https://github.com/fr4ncisx/arca-sdk-java.git
cd arca-sdk-java
mvn clean install -DskipTests
```

Luego, en tu proyecto, elige una de estas opciones:

### Opción A — BOM + módulos individuales

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>io.github.fr4ncisx</groupId>
            <artifactId>arca-sdk-bom</artifactId>
            <version>1.2.1</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### Opción B — Bundle

```xml
<dependency>
    <groupId>io.github.fr4ncisx</groupId>
    <artifactId>arca-sdk-bundle</artifactId>
    <version>1.2.1</version>
</dependency>
```

## Uso

### Inicialización del cliente

El punto de entrada principal del SDK es `ArcaClient`. La forma recomendada de inicializarlo es creando un `ArcaConfig` con los valores necesarios y construyendo el cliente mediante el builder.

```java
import io.github.fr4ncisx.arca.client.spi.ArcaClient;
import io.github.fr4ncisx.arca.core.config.ArcaConfig;
import io.github.fr4ncisx.arca.core.config.ArcaEnvironment;
import io.github.fr4ncisx.arca.core.tax.Cuit;
import io.github.fr4ncisx.arca.wsaa.spi.Pkcs12CertificateSource;
import io.github.fr4ncisx.arca.wsfev1.model.common.VoucherType;
import io.github.fr4ncisx.arca.wsfev1.model.lastvoucher.LastVoucherRequest;
import io.github.fr4ncisx.arca.wsfev1.model.lastvoucher.LastVoucherResponse;

import java.nio.file.Path;
import java.time.Duration;

public class ArcaExample {

    public static void main(String[] args) {
        ArcaConfig config = new ArcaConfig(
                Cuit.parse("20-33333333-4"),
                ArcaEnvironment.HOMOLOGACION,
                Duration.ofSeconds(10),
                Duration.ofSeconds(30)
        ).withTicketCacheDir(Path.of(
                System.getProperty("user.home"), ".arca-sdk", "ticket-cache"));

        ArcaClient client = ArcaClient.builder()
                .config(config)
                .certificate(Pkcs12CertificateSource.fromPath(
                        Path.of("/path/to/cert.p12"),
                        "password".toCharArray()))
                .build();

        LastVoucherResponse last = client.wsfev1().getLastVoucher(
                new LastVoucherRequest(1, VoucherType.INVOICE_A));

        System.out.printf("Último comprobante autorizado: %d%n", last.lastNumber());
    }
}
```

### Configuración avanzada

El constructor de `ArcaConfig` acepta parámetros opcionales para personalizar el comportamiento del SDK:

```java
ArcaConfig config = new ArcaConfig(
        Cuit.parse("20-33333333-4"),
        ArcaEnvironment.HOMOLOGACION,
        Duration.ofSeconds(10),
        Duration.ofSeconds(30)
)
.withResilienceEnabled(false)
.withTicketCacheDir(Path.of("/tmp/arca-cache"));
```

### Manejo de errores

El SDK define tres excepciones principales para diferentes escenarios de fallo:

```java
try {
    ArcaClient client = ArcaClient.builder()
            .config(config)
            .certificate(certificate)
            .build();

    LastVoucherResponse last = client.wsfev1().getLastVoucher(
            new LastVoucherRequest(1, VoucherType.INVOICE_A));
} catch (ArcaAuthException e) {
    System.err.println("Error de autenticación: " + e.getMessage());
} catch (ArcaSoapException e) {
    System.err.println("Error de transporte: " + e.getMessage());
} catch (ArcaValidationException e) {
    System.err.println("Configuración inválida: " + e.getMessage());
}
```

| Excepción | Escenario |
|---|---|
| `ArcaAuthException` | WSAA rechazó la autenticación (certificado inválido, TA expirado, etc.) |
| `ArcaSoapException` | Error de transporte SOAP (timeout, red, servidor no disponible) |
| `ArcaValidationException` | Configuración inválida (CUIT mal formado, timeouts negativos, etc.) |

### Servicios incluidos

La fachada raíz expone los clientes funcionales del SDK:

* `wsfev1()` para facturación electrónica nacional.
* `registry()` para consultas al padrón de contribuyentes.
* `wsfexv1()` para facturación de exportación.
* `wsmtxca()` para facturación con detalle de artículos.
* `wscdc()` para constatación de comprobantes.

### Verificación de salud

El SDK permite verificar la disponibilidad de los servicios ARCA antes de realizar operaciones:

```java
boolean online = client.ping();
System.out.println("Servicios ARCA: " + (online ? "disponibles" : "no disponibles"));

boolean wsfeOnline = client.wsfev1().ping();
```

### Integración con Spring Boot

El starter `arca-sdk-spring-boot-starter` autoconfigura `ArcaClient` y los subclientes funcionales en Spring Boot 3.5.x. Las propiedades del bloque `arca` pueden definirse en `application.yml` o resolverse desde variables de entorno.

Cuando la aplicación se despliega con configuración externa, Spring Boot puede poblar el bloque `arca` a partir de variables de entorno con el prefijo `ARCA_`.

| Variable de entorno | Propiedad Spring Boot |
|---|---|
| `ARCA_CUIT` | `arca.cuit` |
| `ARCA_ENVIRONMENT` | `arca.environment` |
| `ARCA_CONNECT_TIMEOUT` | `arca.connect-timeout` |
| `ARCA_READ_TIMEOUT` | `arca.read-timeout` |
| `ARCA_RESILIENCE_ENABLED` | `arca.resilience-enabled` |
| `ARCA_CERTIFICATE_LOCATION` | `arca.certificate-location` |
| `ARCA_CERTIFICATE_PASSWORD` | `arca.certificate-password` |
| `ARCA_TICKET_CACHE_DIR` | `arca.ticket-cache-dir` |

```xml
<dependency>
    <groupId>io.github.fr4ncisx</groupId>
    <artifactId>arca-sdk-spring-boot-starter</artifactId>
</dependency>
```

```yaml
arca:
  cuit: "20-33333333-4"
  environment: HOMOLOGACION
  connect-timeout: 10s
  read-timeout: 30s
  resilience-enabled: true
  certificate-location: classpath:cert.p12
  certificate-password: "ClaveDelCertificado"
  ticket-cache-dir: /home/user/.arca-sdk/ticket-cache
```

```java
import io.github.fr4ncisx.arca.registry.spi.RegistryClient;
import io.github.fr4ncisx.arca.registry.model.TaxpayerData;
import io.github.fr4ncisx.arca.wsfev1.spi.WsfeClient;
import io.github.fr4ncisx.arca.wsfev1.model.common.VoucherType;
import io.github.fr4ncisx.arca.wsfev1.model.lastvoucher.LastVoucherRequest;
import io.github.fr4ncisx.arca.wsfev1.model.lastvoucher.LastVoucherResponse;
import org.springframework.stereotype.Service;

@Service
public class FacturacionService {

    private final WsfeClient wsfeClient;
    private final RegistryClient registryClient;

    public FacturacionService(WsfeClient wsfeClient, RegistryClient registryClient) {
        this.wsfeClient = wsfeClient;
        this.registryClient = registryClient;
    }

    public long obtenerUltimoComprobante(int puntoVenta, VoucherType tipo) {
        LastVoucherResponse response = wsfeClient.getLastVoucher(
                new LastVoucherRequest(puntoVenta, tipo));
        return response.lastNumber();
    }

    public TaxpayerData consultarContribuyente(String cuit) {
        return registryClient.getTaxpayer(Cuit.parse(cuit));
    }
}
```

### Autenticación con HSM/KMS

Para entornos que requieren hardware security modules (HSM) o key management services (KMS), el SDK soporta un signer personalizado mediante la interfaz `TraSigner`:

```java
import io.github.fr4ncisx.arca.wsaa.spi.TraSigner;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class HsmTraSigner implements TraSigner {

    private final HsmClient hsm;

    public HsmTraSigner(HsmClient hsm) {
        this.hsm = hsm;
    }

    @Override
    public String sign(String traXml) throws ArcaAuthException {
        byte[] traBytes = traXml.getBytes(StandardCharsets.UTF_8);
        byte[] signature = hsm.sign(traBytes, "SHA256withRSA");
        return Base64.getEncoder().encodeToString(signature);
    }
}

ArcaClient client = ArcaClient.builder()
        .config(config)
        .signer(new HsmTraSigner(hsmClient))
        .build();
```

> **Nota:** Al usar `.signer()`, no es necesario proporcionar `.certificate()`. El builder requiere exactamente uno de los dos.

## Compatibilidad

* Java 21 o superior.
* Maven 3.9 o superior.
* Spring Boot 3.5.x para el starter.

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

## Problemas comunes

| Error | Causa | Solución |
|---|---|---|
| `El CEE ya posee un TA valido` | WSAA ya tiene un TA activo para este CUIT+servicio | Esperar ~12h a que expire, o usar el TA cacheado con `ticketCacheDir` |
| `Certificate not registered` | Certificado no registrado en portal ARCA | Registrar el certificado en la portal de fiscal de ARCA |
| `WSAA returned empty response` | CMS inválido o certificado corrupto | Verificar certificado `.p12` y contraseña |
| `Connect timeout` | Servidor ARCA no disponible o firewall | Verificar conexión a internet y URLs de homologación |
| `ArcaConfig: cuit must not be null` | CUIT no proporcionado o formato inválido | Usar `Cuit.parse("XX-XXXXXXXX-X")` con formato correcto |

## Contribuir

Ver [CONTRIBUTING.md](CONTRIBUTING.md) para guía de contribución.

## Licencia

Apache License 2.0. Ver [LICENSE](LICENSE).
