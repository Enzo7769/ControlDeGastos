# Control de Gastos

App Android (Jetpack Compose + Material 3) para controlar tarjetas de crédito, cuotas, créditos,
gastos fijos e ingresos, y saber cuánto te queda cada mes. Los datos se guardan en el teléfono y se
sincronizan con tu cuenta en Firebase.

## Capturas

| Inicio | Deudas | Detalle | Proyección |
|---|---|---|---|
| ![Inicio](docs/capturas/01_inicio.png) | ![Deudas](docs/capturas/03_deudas.png) | ![Detalle](docs/capturas/04_detalle_tarjeta.png) | ![Proyección](docs/capturas/07_proyeccion.png) |

| Modo oscuro | Ingresos | Ajustes | Inicio de sesión |
|---|---|---|---|
| ![Oscuro](docs/capturas/02_inicio_oscuro.png) | ![Ingresos](docs/capturas/06_ingresos.png) | ![Ajustes](docs/capturas/08_ajustes.png) | ![Login](docs/capturas/09_login.png) |

## Funciones

- **Inicio**: saldo del mes (ingresos − gastos), pagado / pendiente, alerta de meses con déficit,
  gráfico de distribución y lista de pagos del mes para marcar como pagados.
- **Deudas**: tarjetas, créditos y gastos fijos con buscador y filtros. En cada una: cupo usado y
  disponible, calendario de pagos mes a mes y "Agregar compra en cuotas".
- **Ingresos**: tus sueldos u otras fuentes de ingreso mensual.
- **Proyección**: saldo de los próximos meses en gráfico y en tabla tipo Excel.
- **Ajustes**: tema claro / oscuro / sistema, color principal, bloqueo con huella o PIN, incluir o
  no gastos fijos, recordatorios, compartir un resumen, buscar actualizaciones y cerrar sesión.
- **Recordatorios de pago**: define el día de vencimiento de cada tarjeta o cuenta y recibe un aviso
  un día antes y el mismo día.

## Estructura

```
app/src/main/java/com/example/controldegastos/
├── MainActivity.kt          # Tema, bloqueo biométrico
├── data/                    # Modelos, JSON, repositorio (local + Firestore), ajustes
├── domain/                  # FinanceCalculator: cálculos puros (con pruebas)
├── ui/
│   ├── AppRoot.kt           # Navegación, barra inferior, snackbar global
│   ├── FinanceViewModel.kt
│   ├── components/          # Campo de dinero, selector de mes, gráficos…
│   ├── screens/             # Una pantalla por archivo
│   └── theme/
└── util/                    # Formato CLP, meses, biometría
```

## Compilar

Requiere Android Studio (o JDK 21 + Android SDK).

```bash
./gradlew assembleDebug        # APK de prueba en app/build/outputs/apk/debug/
./gradlew testDebugUnitTest    # pruebas unitarias
./gradlew assembleRelease      # APK firmado y optimizado (requiere la clave de firma)
```

### Firma de release

La clave **nunca se sube a GitHub**. Para compilar el release en tu computador crea
`keystore.properties` en la raíz del proyecto (ya está en `.gitignore`):

```properties
storeFile=/ruta/a/controldegastos-release.jks
storePassword=...
keyAlias=controldegastos
keyPassword=...
```

> Guarda el archivo `.jks` y sus contraseñas en un lugar seguro (por ejemplo, un gestor de
> contraseñas). Si se pierden, no podrás publicar actualizaciones que se instalen encima de la app.

## Publicar una versión (GitHub Actions)

Cada push y pull request compila la app, corre las pruebas y lint, y deja el APK como artefacto.
Para publicar un APK descargable:

1. En GitHub: **Settings → Secrets and variables → Actions → New repository secret** y crea:
   | Secreto | Valor |
   |---|---|
   | `KEYSTORE_BASE64` | contenido del archivo `.jks` en base64 (`base64 -w0 controldegastos-release.jks`) |
   | `SIGNING_STORE_PASSWORD` | contraseña del almacén |
   | `SIGNING_KEY_ALIAS` | `controldegastos` |
   | `SIGNING_KEY_PASSWORD` | contraseña de la clave |
2. Sube `versionCode` y `versionName` en `app/build.gradle.kts`.
3. Crea y sube una etiqueta: `git tag v2.1.0 && git push origin v2.1.0`.
4. El APK firmado aparece en **Releases** del repositorio.

Para que la app avise de la nueva versión, en Firebase Remote Config pon `apkUrl` =
`https://github.com/Enzo7769/ControlDeGastos/releases/latest` y sube `latestVersionCode`.

## Firebase

- **Authentication**: habilitar *Correo/contraseña*.
- **Iniciar sesión con Google** (el botón aparece solo cuando esto está listo):
  1. Authentication → Método de acceso → habilitar **Google**.
  2. Configuración del proyecto → tu app Android → **Agregar huella digital** con los SHA-1 de
     la clave de debug (`./gradlew signingReport`) y de release:
     `0E:6C:4A:6E:20:24:76:E1:E6:13:76:30:A0:F6:E0:C1:85:69:73:08`
  3. Descargar de nuevo `google-services.json` y reemplazar `app/google-services.json`.
- **Firestore**: los datos de cada usuario están en `users/{uid}/data/financial_info`.
  Las reglas de seguridad están en [`firestore.rules`](firestore.rules): cada usuario solo puede
  leer y escribir lo suyo. Para aplicarlas: copia el contenido en Firestore → Reglas, o usa
  `firebase deploy --only firestore:rules`.
- **Remote Config** (actualizaciones): `latestVersionCode`, `latestVersionName`, `apkUrl`, `updateNotes`.

Los datos de la versión 1 (montos por columna) se migran automáticamente al abrir la versión 2.
