# Historial de cambios

## 1.3.0

### Compatibilidad y soporte normativo ARCA (RG 5616)

- Incorpora el modelo fuertemente tipado `ReceiverVatCondition` para representar las 11 condiciones de IVA del receptor reguladas por la Resolución General 5616/2024 de ARCA.
- Agrega el soporte de `CondicionIVAReceptorId` en solicitudes de emisión de CAE (`CaeRequest`) y de reporte de comprobantes bajo CAEA (`CaeaReportDetail`).
- Mantiene compatibilidad binaria (ABI) y de código fuente mediante constructores sobrecargados que preservan las signaturas anteriores.
- Implementa inferencia inteligente (fallback) de condición de IVA según la clase del comprobante (`IVA_RESPONSABLE_INSCRIPTO` para comprobantes clase A/M y `CONSUMIDOR_FINAL` para clase B/C) para evitar rechazos en emisiones donde el usuario no explicite el valor.
- Amplía la validación del catálogo de tipos de documento (`DocumentTypeInfo`) admitiendo el código oficial 0 (CI Policía Federal).

### Centralización de Servicios WSAA y Resiliencia Configurable

- Centraliza los identificadores de servicio WSAA en el enum fuertemente tipado `WsaaService` (`WSFE`, `WSFEX`, `WSMTXCA`, `WSCDC`, `REGISTRY_A4`, `REGISTRY_A5`, `REGISTRY_A10`, `REGISTRY_A13`), eliminando strings mágicos en los 25 casos de uso de todos los módulos.
- Incorpora `ResilienceConfig` en `arca-sdk-core` para parametrizar de manera inmutable la política de reintentos (`maxAttempts`), retroceso exponencial (`baseDelay`), tolerancia de fallos (`maxConsecutiveFailures`) y tiempo de recuperación del circuit breaker (`resetTimeout`).
- Expone la configuración de resiliencia en Spring Boot Starter mediante prefijo `arca.resilience.*` (`max-attempts`, `base-delay`, `max-consecutive-failures`, `reset-timeout`) preservando los valores por defecto recomendados para producción.
- Mantiene 100% de compatibilidad binaria (ABI) y de código fuente contra la versión baseline v1.2.3 verificado mediante `japicmp` y consumidores de referencia compilados externamente.

### Dependencias y Seguridad

- Actualiza Bouncy Castle a 1.86 (`bcpkix-jdk18on` y `bcprov-jdk18on`).
- Actualiza JAX-WS Metro a 4.0.5 (`jaxws-rt`) y GlassFish JAXB Runtime a 4.0.9 (`jaxb-runtime`).
- Actualiza SLF4J a 2.0.20 (`slf4j-api`) y JSpecify a 1.0.1 (`jspecify`).
- Actualiza dependencias de testing: Mockito 5.24.0 y XMLUnit 2.14.0.

## 1.2.5

### Seguridad

- Sustituye Dependency-Check y su actualización directa mediante la API de NVD por Grype 0.121.0, que analiza el SBOM CycloneDX agregado. El flujo de trabajo de seguridad valida que la base de vulnerabilidades preconstruida tenga menos de 48 horas y permite iniciar desde una caché vacía.
- Actualiza Bouncy Castle a 1.85 para corregir vulnerabilidades detectadas en las dependencias de criptografía y PKIX.
- Conserva las firmas públicas del SDK y el umbral de bloqueo CVSS 7.

## 1.2.4

### Resiliencia y compatibilidad de API

- Conserva instantáneas inmutables de las listas en los DTO. Para modificar los datos, crea una lista mutable propia y construye un DTO nuevo.
- Desactiva los reintentos automáticos de autorizaciones e informes de comprobantes. Ante un tiempo de espera agotado, consulta el resultado antes de reenviar. Las consultas seguras conservan hasta tres intentos totales.
- Mantiene un resultado por posición de entrada en los lotes, incluso para solicitudes duplicadas. Distingue las solicitudes no enviadas de las operaciones cuyo resultado es incierto. La estrategia de fallo rápido conserva el prefijo procesado.
- Aísla las cachés persistentes de tickets por ambiente y CUIT. No migra los archivos planos heredados. Coordina el reemplazo de la caché con la expiración del ticket para evitar el rechazo de un ticket WSAA aún activo.
- Requiere que el directorio de caché permita escritura, bloqueo de archivos y reemplazo atómico. En Windows, restringe sus permisos a la cuenta de la aplicación.
- Rechaza importes CAE y CAEA no finitos y exige cotizaciones finitas y positivas.
- Interpreta un tiempo de espera de cero como ilimitado y limita a 1 MiB el cuerpo de las respuestas WSAA.

### Seguridad

- Limita los registros del transporte SOAP a la dirección del servicio y al tipo de mensaje. El endurecimiento de la revocación TLS sigue siendo optativo y global para la JVM; no reemplaza las actualizaciones del proveedor de Java.
