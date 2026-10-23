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
            <version>1.2.5</version>
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
    <version>1.2.5</version>
</dependency>
```

## Actualización a 1.2.5

- El workflow `Security Scan` usa Grype 0.121.0 sobre el SBOM CycloneDX agregado. La base de vulnerabilidades se actualiza desde Anchore y debe tener menos de 48 horas.
- Bouncy Castle se actualiza a 1.85 para corregir avisos de seguridad detectados en las dependencias criptográficas y PKIX.
- Las firmas públicas del SDK no cambian. Consulta [SECURITY.md](SECURITY.md) para conocer la política de bloqueo y sus límites.

## Actualización a 1.2.4

- Los DTO conservan copias inmutables de sus listas. Para editar sus datos, utiliza una lista propia y construye un nuevo DTO.
- Las autorizaciones y los informes de comprobantes no se reintentan automáticamente. Ante un timeout, consulta el estado antes de reenviar. Las consultas seguras conservan hasta tres intentos totales.
- Los lotes interrumpidos por el hilo conservan una entrada por posición, incluidos duplicados. Los errores distinguen solicitudes no enviadas de operaciones con resultado incierto. La estrategia fail-fast conserva el prefijo procesado.
- El caché persistente se separa por ambiente y CUIT; los archivos planos antiguos no se migran. Coordina la actualización con la expiración del ticket vigente para evitar el rechazo de WSAA por un ticket ya activo.
- El directorio de caché debe permitir escritura, bloqueo de archivos y reemplazo atómico. En Windows, restringe sus permisos a la cuenta de la aplicación.
- Los importes CAE y CAEA deben ser finitos y las cotizaciones, finitas y positivas.
- Un timeout de cero desactiva el límite correspondiente. Las respuestas WSAA se limitan a 1 MiB.
- Los logs del transporte SOAP contienen únicamente dirección y tipo de mensaje. El hardening TLS es optativo y global para la JVM; no sustituye los parches del proveedor de Java.

Consulta [SECURITY.md](SECURITY.md) para los detalles de seguridad y transición. Estos cambios conservan las firmas públicas; las integraciones que mutaban listas de DTO deben adaptarse.

## Verificación del SDK

Ejecuta desde la raíz, con Java 21 o 25 y Python 3.9 o superior:

```bash
python -B -m unittest discover -s scripts -p 'test_*.py'
python -B scripts/check_compatibility.py
```

El segundo comando construye el tag `v1.2.3` en un directorio temporal, verifica su commit y ejecuta pruebas, cobertura y comparación de API pública. También compila consumidores Java y Spring de referencia contra ambas versiones y ejecuta su bytecode contra el candidato. Requiere disponer del tag local; no modifica ramas ni instala la versión anterior en el repositorio Maven local.

La comparación de firmas no detecta por sí sola cambios de comportamiento. Los consumidores de referencia cubren construcción de DTO, edición mediante copias de listas y autoconfiguración Spring. No sustituyen las pruebas de una aplicación externa.

La suite local y la comparación de compatibilidad no requieren credenciales de ARCA. Las pruebas reales de homologación son optativas; cuando no se dispone de un entorno para ejecutarlas, la integración remota queda sin verificar.

Para consultas reales de homologación, configura externamente `ARCA_TEST_CERT_PATH`, `ARCA_TEST_CERT_PASSWORD` y `ARCA_TEST_CUIT`:

```bash
mvn test -Phomologation -pl arca-sdk-client -am -Dtest=RealHomologationIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false
```

La autorización real es una prueba separada: `RealHomologationAuthorizationIntegrationTest`. Solo se activa con `ARCA_TEST_WRITE_ENABLED=true` y requiere estos datos revisados por el operador:

| Variable | Valor requerido |
|---|---|
| `ARCA_TEST_SALES_POINT` | Punto de venta reservado para homologación, sin otros emisores concurrentes |
| `ARCA_TEST_VOUCHER_TYPE` | Código del tipo de comprobante |
| `ARCA_TEST_VOUCHER_NUMBER` | Número explícito; no se incrementa automáticamente al repetir la prueba |
| `ARCA_TEST_RECEIVER_CUIT` | CUIT del receptor de prueba |
| `ARCA_TEST_VOUCHER_DATE` | Fecha revisada en formato `AAAA-MM-DD` |
| `ARCA_TEST_NET_AMOUNT` | Importe neto positivo, con punto decimal |
| `ARCA_TEST_VAT_AMOUNT` | IVA no negativo, con punto decimal |
| `ARCA_TEST_VAT_TYPE` | Código de alícuota, obligatorio cuando el IVA es mayor que cero |

Esta prueba utiliza productos, moneda PES y un único desglose de IVA. Consulta la secuencia antes de emitir y luego consulta la misma identidad. Si el número ya existe, únicamente lo concilia. Ante un fallo de transporte, consulta y no reenvía. Nunca apunta a producción. Una prueba omitida o una conciliación de un comprobante anterior no demuestra una nueva autorización en esa ejecución.

El workflow `Security Scan` usa Grype 0.121.0 en cada PR hacia `main`, en cambios a `main` y diariamente. Descarga una base preconstruida de Anchore, sin claves de API ni consultas directas a NVD. Si la caché está vacía, descarga la base en esa misma ejecución; no requiere un bootstrap previo en `main`. La base validada se guarda antes del escaneo y su fecha de construcción no puede superar las 48 horas. Una descarga o validación fallida bloquea el check.

Para ejecutar el mismo análisis localmente, instala Grype 0.121.0 desde su release oficial y verifica el checksum del binario. El perfil Maven genera un CycloneDX agregado con dependencias de compilación, ejecución, provided y system; excluye las dependencias exclusivamente de test. El validador compara sus coordenadas Maven con el árbol resuelto de todos los módulos, incluidas las transitivas:

El perfil `security-scan` genera el SBOM; la política de vulnerabilidades se aplica mediante Grype y el validador Python de los comandos siguientes.

```bash
mvn package dependency:tree -Psecurity-scan -DskipTests -DoutputType=json -DoutputFile=target/security-dependencies.json
python -B scripts/check_grype_report.py --sbom target/bom.json --dependency-root .
grype --config .grype.yaml db update
grype --config .grype.yaml db status -o json > target/grype-db-status.json
python -B scripts/check_grype_report.py --database target/grype-db-status.json
grype --config .grype.yaml sbom:target/bom.json -o json --file target/grype-report.json
python -B scripts/check_grype_report.py --sbom target/bom.json --report target/grype-report.json
```

El informe queda en `target/grype-report.json`; el SBOM, en `target/bom.json` y `target/bom.xml`. Se bloquea cualquier CVSS de 7 o superior del aviso o de sus avisos relacionados, aunque no exista parche. Sin puntuación numérica, High/Critical bloquean; una severidad desconocida falla como análisis inconcluso. No se configuran supresiones globales.

Grype utiliza principalmente GitHub Security Advisories para Maven y otras fuentes según el ecosistema. Los resultados pueden diferir de Dependency-Check. La caché acelera ejecuciones posteriores, pero sigue existiendo una descarga inicial y dependencia de la disponibilidad de Anchore. En PowerShell anterior a 7.4, guarda la salida JSON de `db status` explícitamente en UTF-8; la redirección de Windows PowerShell puede producir UTF-16.

La migración actualiza Bouncy Castle de 1.78.1 a 1.85 para corregir los avisos detectados en el proveedor criptográfico y PKIX, incluidos CVE-2026-8763 y CVE-2026-13506. La versión del SDK y sus firmas públicas se mantienen; se requiere volver a validar el classpath si la aplicación consumidora fuerza otra versión de Bouncy Castle.

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

El starter `arca-sdk-spring-boot-starter` autoconfigura `ArcaClient` y los subclientes funcionales en Spring Boot 4.0.x. Las propiedades del bloque `arca` pueden definirse en `application.yml` o resolverse desde variables de entorno.

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
* Spring Boot 4.0.x para el starter.

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
