# Contribuir al ARCA SDK Java

Gracias por tu interés en contribuir. Este SDK simplifica la integración con los Web Services SOAP de ARCA (ex-AFIP): facturación electrónica, consulta de padrón, constatación de comprobantes y más.

## Prerrequisitos

- Java 21 o superior
- Maven 3.9 o superior
- Certificado PKCS#12 de homologación (opcional, para tests de integración)

## Primeros pasos

```bash
git clone https://github.com/fr4ncisx/arca-sdk-java.git
cd arca-sdk-java
mvn clean install -DskipTests
mvn test
```

## Estructura del proyecto

| Módulo | Responsabilidad |
|---|---|
| `arca-sdk-core` | Configuración, validación, excepciones, reloj, sanitización |
| `arca-sdk-soap` | Transporte SOAP, handlers JAX-WS, timeouts |
| `arca-sdk-wsaa` | Autenticación WSAA, TRA, firma CMS, caché de tickets |
| `arca-sdk-wsfev1` | Facturación electrónica nacional (CAE, CAEA) |
| `arca-sdk-wsfexv1` | Facturación de exportación |
| `arca-sdk-wsmtxca` | Facturación con detalle de artículos |
| `arca-sdk-wscdc` | Constatación de comprobantes |
| `arca-sdk-registry` | Consultas al padrón de contribuyentes |
| `arca-sdk-client` | Fachada unificada (punto de entrada principal) |
| `arca-sdk-spring-boot-starter` | Autoconfiguración para Spring Boot |
| `arca-sdk-test-support` | Fixtures XML, utilidades WireMock |

## Cómo desarrollar

Para agregar una nueva operación a un servicio existente:

1. **Modelo** — Crear request/response como `record` inmutable en `model/`.
2. **Caso de uso** — Crear clase en `internal/usecase/`, una por operación.
3. **Mapper** — Convertir entre SOAP (JAXB) y dominio, en `internal/usecase/`.
4. **Interfaz pública** — Agregar método en la interfaz `spi/`.
5. **Tests** — Mockear `ArcaSoapPort` y `AuthProvider`, nunca red real.

### Ejemplo de patrón

```
wsfev1/
├── model/lastvoucher/          ← LastVoucherRequest, LastVoucherResponse (records)
├── spi/WsfeClient.java         ← interfaz pública
├── internal/
│   ├── usecase/lastvoucher/
│   │   ├── GetLastVoucherUseCase.java   ← lógica
│   │   └── LastVoucherMapper.java       ← mapeo SOAP ↔ dominio
│   ├── client/DefaultWsfeClient.java    ← implementación (package-private)
│   └── assembler/WsfeClientAssembler.java ← ensamblado
```

## Convenciones de código

- **Records** para todos los modelos, DTOs y value objects.
- **4 espacios** de indentación.
- **Sin comentarios inline** — el código debe ser autoexplicativo.
- **Javadocs en inglés** para API pública, con `@author` y `@since`.
- **Validación fall-fast** — constructores deben rechazar `null` con `ArcaValidationException`.
- **Paquetes sin null** — cada paquete debe tener `package-info.java` con `@NullMarked`.
- **Collections nunca null** — retornar `List.of()` en lugar de `null`.

## Arquitectura

El proyecto usa arquitectura hexagonal:

- **Dominio** (modelos, use cases, excepciones) — puro, sin dependencias SOAP ni JAXB.
- **Infraestructura** (mappers, stubs generados, adaptadores) — en paquetes `internal.*`.
- **SPI público** — solo interfaces en `spi/`, minimalistas.

Los casos de uso dependen de puertos (`ArcaSoapPort`, `AuthProvider`), nunca de implementaciones concretas.

## Tests

- **Unit tests** — mockear interfaces, validar constructores nulos, verificar mapping.
- **Integración** — usar WireMock, nunca tráfico de red real.
- **Ejecutar**: `mvn clean verify` (compila + tests).
- **Un módulo**: `mvn test -pl arca-sdk-wsfev1`.
- **Integración real**: `mvn verify -Darca.integration=true` (requiere certificados).

## Commits

Usamos Conventional Commits:

```
feat(wsfev1): implement batch authorization
fix(core): reject negative CUIT prefix
test(client): add public API contract tests
refactor(wsaa): decouple CMS cryptography via TraSigner SPI
chore(release): bump version to 1.2.2
```

## Pull Requests

Antes de abrir un PR:

- [ ] `mvn clean verify` pasa sin errores
- [ ] Tests agregados para la nueva funcionalidad
- [ ] Los tests existentes siguen pasando
- [ ] Javadoc actualizado en la API pública
- [ ] No se exponen datos sensibles en logs ni mensajes de error

## Seguridad

Nunca commitear:

- Certificados PKCS#12 o claves privadas
- Contraseñas o tokens de acceso
- Payloads SOAP con datos reales de contribuyentes
- CUITs reales en tests (usar `20-33333333-4`)

Usar siempre variables de entorno para configuración sensible.
