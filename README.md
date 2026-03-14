# SushiAMO Android Bridge

Android app dedicata per fare da bridge stampa sempre-attivo senza Desktop App.

## Cosa fa

- Login con credenziali reali Supabase (`email/password`) dell'account ristorante
- Risoluzione automatica del ristorante associato (owner → `user_roles` admin/manager/staff)
- Foreground service persistente (`BridgeService`) con polling periodico
- Claim/ack reale della coda stampa tramite RPC (`print_claim_jobs`, `print_complete_job`)
- Invio ticket verso stampante di rete TCP/RAW (host/porta da route job)
- Notifica di stato sempre visibile e metriche locali (`claimed/printed/failed`)

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

## Configurazione bridge (prima apertura app)

Il cliente NON inserisce URL/chiavi Supabase.

Nell'app Android il cliente vede solo:

- `Email admin` (o manager/staff con ristorante associato)
- `Password`

`Supabase URL` e `anon key` vengono iniettati in build release tramite secrets CI.

Secrets richiesti in GitHub Actions:

- `BRIDGE_SUPABASE_URL`
- `BRIDGE_SUPABASE_ANON_KEY`

Poi premi `Login`: il bridge salva sessione, risolve il ristorante e abilita `Avvia bridge`.

Nota sicurezza: non usare `service_role` nel client Android.

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
