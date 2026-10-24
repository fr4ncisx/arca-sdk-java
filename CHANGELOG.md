# Historial de cambios

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
