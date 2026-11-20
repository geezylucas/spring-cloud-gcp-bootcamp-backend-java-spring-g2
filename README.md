# Hands-on Módulo 10 · CI/CD y despliegue en Google Cloud (Cloud Run + Cloud Run functions)

Bootcamp Backend con Java y Spring · Código Facilito · Java 25 · Spring Boot 4.1

Una **Pokédex API** (Spring Boot) que consume la API pública [PokéAPI](https://pokeapi.co), se prueba en
**GitHub Actions** (CI), se despliega sola a **Cloud Run** (CD) y consulta a una **Cloud Run function** (FaaS)
que elige el "Pokémon del día".

```
 git push ─► GitHub Actions ──► Artifact Registry ──► Cloud Run: pokedex-api ──► PokéAPI (pública)
            (tests + imagen)      (imagen :SHA)            │
                                                           └──► Cloud Run function: pokemon-of-the-day
```

## Estructura

| Carpeta | Qué es |
|---|---|
| `pokedex-api/` | API Spring Boot: `GET /api/pokemon/{nombre}`, `GET /api/pokemon/daily`, `/actuator/health`. Incluye `Dockerfile` multi-stage |
| `functions/pokemon-of-the-day/` | Función HTTP (Functions Framework para Java) → `{"id":25,"date":"2026-11-16"}` |
| `.github/workflows/ci.yml` | **CI**: build + tests en cada PR / push a una rama |
| `.github/workflows/deploy.yml` | **CD**: tests → imagen → Artifact Registry → Cloud Run (al hacer push a `main`) |
| `scripts/setup-gcp.sh` | Prepara GCP: APIs, Artifact Registry, service account y Workload Identity Federation |
| `scripts/teardown.sh` | Limpieza para no generar costos |

## Requisitos

- JDK **25**, Maven 3.9+, Docker, `git`
- Cuenta de GitHub y [`gcloud` CLI](https://cloud.google.com/sdk/docs/install)
- Un proyecto de Google Cloud **con facturación activa** (Cloud Run tiene capa gratuita, pero la cuenta pide tarjeta)
- *Si no tienes cuenta de GCP*: puedes seguir los pasos 1, 2 y 5 (local + CI) y ver los demás en la demo

## Paso a paso

### 1 · Corre la API en local
```bash
cd pokedex-api
mvn verify                      # tests (no usan internet: MockRestServiceServer)
mvn spring-boot:run
curl localhost:8080/api/pokemon/pikachu
curl localhost:8080/api/pokemon/daily      # "source":"local" (aún no hay función)
```

### 2 · Contenedor local
```bash
docker build -t pokedex-api pokedex-api
docker run --rm -p 8080:9090 -e PORT=9090 pokedex-api     # 12 factores: el puerto viene del entorno
curl localhost:8080/actuator/health
```

### 3 · Prepara Google Cloud
```bash
gcloud auth login
export PROJECT_ID=mi-proyecto-m10  GITHUB_REPO=tu-usuario/pokedex-cloud
./scripts/setup-gcp.sh
```
Al terminar imprime las **variables** que debes crear en GitHub (Settings → Secrets and variables → Actions → *Variables*).

### 4 · Primer despliegue a mano (Cloud Run)
```bash
gcloud run deploy pokedex-api --source pokedex-api --region us-central1 \
  --allow-unauthenticated --max-instances 2
# imprime la URL del servicio:  https://pokedex-api-xxxxx.a.run.app
curl "$URL/api/pokemon/pikachu"
```
`--source` usa Cloud Build + tu `Dockerfile`. `--max-instances` pone un tope de gasto.

### 5 · CI con GitHub Actions
Sube el repositorio a GitHub, crea una rama y abre un Pull Request: corre `ci.yml` (build + tests de la API y de la función).

### 6 · CD: desplegar al hacer push a `main`
Con las variables ya creadas, un *merge* a `main` ejecuta `deploy.yml`: prueba → construye la imagen
`…-docker.pkg.dev/<proyecto>/pokedex/pokedex-api:<sha>` → la sube → despliega → prueba de humo a `/actuator/health`.
La autenticación es **sin llaves** (Workload Identity Federation): GitHub entrega un token OIDC de corta vida y
solo **tu** repositorio puede usar la service account.

### 7 · Revisiones, tráfico y rollback
```bash
gcloud run revisions list --service pokedex-api --region us-central1
# Canary: 10 % a la revisión nueva
gcloud run services update-traffic pokedex-api --region us-central1 \
  --to-revisions REVISION_NUEVA=10,REVISION_ANTERIOR=90
# Rollback inmediato
gcloud run services update-traffic pokedex-api --region us-central1 --to-revisions REVISION_ANTERIOR=100
```

### 8 · La función (FaaS)
```bash
cd functions/pokemon-of-the-day
mvn function:run                          # prueba local en :8080  →  curl localhost:8080
gcloud run deploy pokemon-of-the-day --source . \
  --function com.codigofacilito.cloud.PokemonOfTheDay \
  --base-image java25 --region us-central1 --allow-unauthenticated
curl "$FUNCTION_URL"                      # {"id":…,"date":"…"}
```

### 9 · Conecta la API con la función
Guarda la URL en la variable `FUNCTION_URL` de GitHub (el workflow la inyecta) o, para probar rápido:
```bash
gcloud run services update pokedex-api --region us-central1 --set-env-vars FUNCTION_URL=$FUNCTION_URL
curl "$URL/api/pokemon/daily"             # "source":"function"
```
Prueba la resiliencia: pon una URL inválida en `FUNCTION_URL` y verás `"source":"local"` (la API sigue viva).

### 10 · Logs y métricas
Consola de Google Cloud → *Cloud Run* → servicio → pestañas **Logs** y **Metrics** (solicitudes, latencia, instancias).
O desde la terminal: `gcloud run services logs read pokedex-api --region us-central1 --limit 20`.

### 11 · Costos y limpieza
```bash
PROJECT_ID=mi-proyecto-m10 ./scripts/teardown.sh
```
Crea además una **alerta de presupuesto** en Billing. La capa gratuita de Cloud Run (servicios con facturación por
solicitud) incluye cada mes 2 millones de solicitudes, 180 000 vCPU-segundos y 360 000 GiB-segundos; **Artifact Registry
y Cloud Build tienen precios aparte**. Verifica los valores vigentes en la página de precios de Cloud Run.

## Ideas para practicar
- Añade `Cloud SQL` o `Secret Manager` y lee un secreto como variable de entorno.
- Protege la función con IAM (quita `--allow-unauthenticated`) y llama con un token de identidad.
- Agrega un *circuit breaker* (Spring Cloud Circuit Breaker + Resilience4j) a `DailyPickClient`.

## Solución de problemas: `setup-gcp.sh`

- **"Service account … does not exist" justo después de crearla:** IAM tarda unos segundos en propagar; el script ya espera y reintenta. Si lo corres otra vez, detecta que existe y sigue.
- **`ALREADY_EXISTS` en el pool pero luego `NOT_FOUND` / "Identity Pool does not exist":** el pool `github` está en estado *DELETED* (los pools borrados se conservan 30 días y bloquean el nombre). El script ahora lo restaura solo; a mano: `gcloud iam workload-identity-pools undelete github --location=global`. Alternativa: usa otro nombre editando `POOL` en el script.
- El script se puede ejecutar varias veces sin romper nada.
