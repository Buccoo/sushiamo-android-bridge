# SushiAMO Android Bridge

Android app dedicata per fare da bridge stampa sempre-attivo senza Desktop App.

## Cosa fa (MVP)

- Esegue un foreground service persistente (`BridgeService`)
- Polling periodico coda job (placeholder pronto per integrazione Supabase)
- Notifica di stato sempre visibile
- Avvio/arresto manuale dal pannello principale

## Requisiti

- Android 8+ (consigliato Android 10+)
- Device dedicato, alimentazione continua, Wi-Fi stabile
- Ottimizzazioni batteria disabilitate per l'app

## Build locale

1. Apri la cartella in Android Studio
2. Configura Android SDK (API 34)
3. Esegui build release:

```bash
gradle :app:assembleRelease
```

APK output:

- `app/build/outputs/apk/release/app-release.apk`

## Configurazione bridge

Imposta endpoint/token nel file:

- `app/src/main/java/com/sushiamo/bridge/BridgeConfig.kt`

Campi principali:

- `SUPABASE_URL`
- `SUPABASE_SERVICE_ROLE_TOKEN` (non usare in client pubblico; per prod usa token scoped per bridge)
- `RESTAURANT_ID`

## Release GitHub

Workflow incluso:

- `.github/workflows/android-release.yml`

Trigger:

- `workflow_dispatch`
- push di tag `v*` (es. `v1.0.0`)

Il workflow:

1. Builda `:app:assembleRelease`
2. Carica artifact APK
3. Se è un tag, crea GitHub Release con APK allegato

## Hardening consigliato prima di produzione

- Device binding + registration handshake
- Token rotanti a scope ridotto
- Ack idempotente job
- Retry esponenziale + dead-letter
- Watchdog servizio + auto-restart boot
- Cifratura locale config sensibile
