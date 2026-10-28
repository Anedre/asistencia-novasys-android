# Nova Asistencia — Android

App nativa (Kotlin + Jetpack Compose) del sistema de asistencia. Habla con la
misma API que la web (`asistencia-novasys-v2`) y con el mismo pool de Cognito.
iOS sigue usando la PWA instalable de la web.

Diseño *liquid glass*: superficies de vidrio con blur real del fondo (Haze),
paleta que va del blanco al negro azabache con transición animada entre modo
claro y oscuro, e intro, cargas y transiciones animadas.

## Publicar una versión

La app se distribuye fuera de Play Store por ahora y **se actualiza sola desde
los Releases de GitHub**: al abrirse consulta el último release, descarga el
APK y abre el instalador del sistema. Android solo acepta la actualización si
el APK está firmado con la misma llave que la app instalada.

**Una sola vez** — crea la llave de firma y carga los secrets en GitHub:

```powershell
pwsh scripts/setup-release-signing.ps1
```

> ⚠️ Respalda el keystore (`~/.nova-signing/nova-release.jks`) y su contraseña
> fuera de esta PC. Si se pierden, las apps instaladas no podrán actualizarse
> más: habría que desinstalar y volver a instalar en cada teléfono.

**Cada versión** — crea un tag y GitHub Actions compila, firma y publica:

```bash
git tag v0.3.0
git push origin v0.3.0
```

El workflow (`.github/workflows/release.yml`) usa el tag como `versionName`
y el número de ejecución como `versionCode`, verifica la firma y adjunta
`nova-asistencia-0.3.0.apk` al release. La primera instalación se hace
descargando ese APK desde la página del release.

## Cómo correrla

Requisitos: Android Studio, JDK 17+ y el SDK con la plataforma 37 (Gradle la
descarga sola si la licencia del SDK ya está aceptada).

```bash
./gradlew assembleDebug        # APK debug en app/build/outputs/apk/debug/
./gradlew installDebug         # instala en el emulador o teléfono conectado
```

O abre la carpeta en Android Studio y dale a Run.

### Contra qué backend apunta

| Build   | URL por defecto                       | Dónde cambiarla                       |
|---------|---------------------------------------|---------------------------------------|
| debug   | `http://10.0.2.2:3000` (tu `next dev`) | `novaApiBaseUrlDebug` en `gradle.properties` |
| release | `https://asistencia.novasys.com.pe`   | `novaApiBaseUrl` en `gradle.properties`      |

`10.0.2.2` es tu PC vista desde el **emulador**. En un teléfono físico usa la IP
de tu PC en la red local:

```bash
./gradlew installDebug -PnovaApiBaseUrlDebug=http://192.168.1.20:3000
```

El backend acepta el token de la app desde `Anedre/asistencia-novasys-v2#3`
(desplegado en producción el 2026-10-27).

## Cómo autentica

1. La app hace `InitiateAuth` (`USER_PASSWORD_AUTH`) **directo contra Cognito**,
   mismo pool y mismo app client que la web. Sin AWS SDK: es un POST JSON.
2. Guarda ID/access/refresh token cifrados con una clave AES-GCM del
   **Android Keystore** (`TokenStore`). Backups desactivados.
3. Cada llamada a la API manda `Authorization: Bearer <IdToken>`. El backend
   lo verifica contra el JWKS del pool y lo resuelve con el mismo resolvedor de
   identidad que usa la sesión web (`employee-session.ts`), así rol y tenant
   nunca difieren entre web y app.
4. El token se refresca solo 2 min antes de vencer; ante un 401 se refresca y
   reintenta una vez. Solo un refresh token rechazado por Cognito cierra la
   sesión; una caída de red nunca te desloguea.

## Estructura

```
app/src/main/java/pe/com/novasys/asistencia/
├── NovasysApp.kt          AppContainer: dependencias cableadas a mano
├── MainActivity.kt        Raíz: login o home según la sesión
├── data/auth/             CognitoClient, TokenStore (Keystore), AuthRepository
├── data/api/              NovaApi (cliente REST) y modelos
├── data/update/           UpdateRepository: GitHub Releases → APK
├── data/settings/         Preferencias (modo de tema)
└── ui/
    ├── theme/             Paletas claro/azabache interpoladas, tipografía, barras
    ├── components/        Glass (aurora, superficies, botones), Motion, glifos
    ├── intro/             Intro animada de arranque
    ├── update/            Banner de actualización
    ├── login/             Pantalla y ViewModel de login
    └── home/              "Mi día": reloj, estado, marcación
```

El acento (`#3FBEFF`) es el mismo de `src/styles/nova-design.css` en la web.
El resto de la paleta glass vive en `ui/theme/Theme.kt` (`LightGlass` /
`DarkGlass`); ambas tienen los mismos campos para poder interpolarse.

## Estado y roadmap

**Hecho (v0.2):** login con Cognito, sesión cifrada con refresh automático,
"Mi día" con entrada / refrigerio / salida contra la API real, diseño liquid
glass claro/azabache con cambio de tema animado, intro, loader orbital,
skeletons, dígitos rodantes, háptica al marcar, y auto-actualización desde
GitHub Releases.

**Empleado — siguientes pantallas**

| Pantalla        | Endpoints                                              |
|-----------------|--------------------------------------------------------|
| Historial       | `GET /api/attendance/history`, `/api/attendance/week`  |
| Solicitudes     | `GET/POST /api/requests`                               |
| Regularización  | `/api/regularization`, `/api/regularization/range`     |
| Perfil          | `GET/PUT /api/profile`, `/api/profile/avatar`          |
| Notificaciones  | `GET /api/notifications`                               |
| Feed / Eventos  | `/api/feed/*`, `/api/events/*`                         |
| Mensajes        | `/api/messages/channels/*`                             |
| Docs RRHH       | `GET /api/hr/documents`                                |

**Admin**

| Pantalla            | Endpoints                                                      |
|---------------------|----------------------------------------------------------------|
| Dashboard           | `GET /api/admin/dashboard`                                     |
| Asistencia del día  | `/api/admin/attendance`, `/api/admin/attendance/open-shifts`   |
| Aprobaciones        | `/api/admin/approvals`, `POST /api/requests/[id]/approve`      |
| Empleados           | `/api/admin/employees`, `/api/admin/invitations`               |
| Regularizar         | `/api/admin/daily-summary/[employeeId]/[workDate]`             |
| Reportes            | `/api/admin/reports/stats`, `/api/admin/reports/export`        |
| Auditoría           | `/api/admin/audit/*`                                           |

> Antes de exponer el panel admin en el teléfono conviene cerrar el IDOR
> multi-tenant pendiente en los endpoints admin (actúan sobre `employeeId`
> enviado por el cliente sin verificar el tenant).

**Capacidades nativas**

- **Push (FCM):** proyecto Firebase + endpoint en el backend para registrar el
  token del dispositivo + envío desde los servicios de notificaciones.
- **GPS en la marcación:** `recordEventSchema` aún no acepta ubicación; hay que
  agregar `lat/lng/accuracy` en el backend primero.
- **Marcación offline:** cola en Room + WorkManager que envía al recuperar red.
- **Desbloqueo biométrico** al abrir la app.
- **Navegación + DI:** Navigation Compose y Hilt cuando haya varias pantallas.

**Publicación en Play Store**

- Cuenta de Play Console **como organización** (Novasys Peru): las cuentas
  personales nuevas deben pasar 14 días de test cerrado con 12 testers.
- Keystore de firma de release (guárdala fuera del repo, `*.jks` está en
  `.gitignore`) y Play App Signing.
- Política de privacidad pública (la app maneja datos personales y, con GPS,
  ubicación).

## Nota sobre Gradle en esta máquina

El proyecto declara versiones directo en los `build.gradle.kts` en vez de un
version catalog (`libs.versions.toml`): en esta PC, Gradle 9.3.1 no generaba
los accesores `libs` (caché en `~/.gradle/caches/9.3.1/kotlin-dsl` bloqueada
por el daemon de Android Studio). Si quieres volver al catálogo, cierra Studio,
borra esa carpeta y prueba de nuevo.
