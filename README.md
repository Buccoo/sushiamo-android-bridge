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

## Firma APK (importante)

Per evitare errore Android `App non installata`, l'APK deve essere firmato sempre con la stessa chiave.

### Locale

1. Copia `keystore.properties.example` in `keystore.properties`
2. Inserisci percorso `.jks` + password + alias
3. Esegui build release

Se `keystore.properties` manca, il progetto fa fallback a firma debug (installabile, ma non adatta a distribuzione stabile/update a lungo termine).

### GitHub Actions (release firmata stabile)

Aggiungi in `Settings > Secrets and variables > Actions` della repo:

- `ANDROID_KEYSTORE_BASE64` → contenuto base64 del file `.jks`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

Con questi secrets, il workflow crea `keystore.properties` al volo e genera release APK firmata correttamente.

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
