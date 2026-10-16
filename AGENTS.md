# morphe-patches — Juankyyy

## Versionado (semantic-release, NO editar a mano)
- `feat:` = app NUEVA parcheada → bump MINOR (1.x.0).
- `fix:` = mejora/corrección en app YA parcheada → bump PATCH (1.1.x).
- `chore:` = no genera release.
- Nunca editar a mano: `patches-list.json`, `patches-bundle.json`, `CHANGELOG.md`,
  `gradle.properties` (versión). Los genera `release.yml`.
- No borrar releases/tags ni hacer force-push salvo el procedimiento de
  "redo release" del template. Si un commit `feat:` debió ser `fix:`, rehacer
  ANTES de mergear a main (reset + recommits + borrar pre-release/tag + push).

## Flujo de ramas
- Todo el trabajo en `dev`. Commits semánticos → pre-releases automáticos.
- Estable: `git checkout main && git merge --no-ff dev` (nunca squash) + push.
- Tras el release estable, el bot hace backmerge main→dev solo.

## Verificar un parche (obligatorio antes de publicar)
1. APK original en `~/Downloads/*.apkm` (o pedirlo al usuario). Extraer `base.apk`.
2. `./gradlew buildAndroid` debe dar BUILD SUCCESSFUL (requiere Android SDK en
   `C:\Android\Sdk` + `local.properties` con `sdk.dir`, y PAT `read:packages`
   en `~/.gradle/gradle.properties`).
3. Validar fingerprints contra el APK real (androguard instalado en el Python
   del sistema): clase + nombre + descriptor + returnType + parámetros deben
   matchear UN solo método, y el opcode del filtro debe existir en él.
4. Probar en móvil: pasar el `.mpp` de `patches/build/libs/` como fuente local
   en Morphe Manager, o activar pre-release en la fuente.

## Convenciones de código
- Un paquete por app: `patches/src/main/kotlin/app/juankyyy/patches/<app>/`.
- `Compatibility.kt` (packageName, version = versionName EXACTO del APK,
  `APKM` si APKMirror dice BUNDLE), `Fingerprints.kt`, un archivo por parche.
- Preferir fingerprints exactos (clase+nombre+firma) confirmados en jadx/APK
  sobre adivinados. Smali inyectado con registros libres (`v0` + return
  inmediato en métodos static es seguro).
