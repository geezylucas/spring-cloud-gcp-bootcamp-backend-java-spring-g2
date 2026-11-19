#!/usr/bin/env bash
# Prepara Google Cloud para el hands-on del Módulo 10.
# Uso:  PROJECT_ID=mi-proyecto GITHUB_REPO=usuario/pokedex-cloud ./scripts/setup-gcp.sh
# Requiere: gcloud autenticado (gcloud auth login) y un proyecto con facturación activa.
set -euo pipefail

: "${PROJECT_ID:?Define PROJECT_ID}"
: "${GITHUB_REPO:?Define GITHUB_REPO (usuario/repositorio)}"
REGION="${REGION:-us-central1}"
REPOSITORY="${REPOSITORY:-pokedex}"
SA_NAME="github-deployer"
POOL="github"
PROVIDER="github-provider"

gcloud config set project "$PROJECT_ID"
PROJECT_NUMBER="$(gcloud projects describe "$PROJECT_ID" --format='value(projectNumber)')"
SA_EMAIL="${SA_NAME}@${PROJECT_ID}.iam.gserviceaccount.com"

echo "▶ 1/5 Habilitando APIs"
gcloud services enable \
  run.googleapis.com artifactregistry.googleapis.com cloudbuild.googleapis.com \
  iamcredentials.googleapis.com sts.googleapis.com

echo "▶ 2/5 Repositorio de imágenes (Artifact Registry)"
gcloud artifacts repositories describe "$REPOSITORY" --location="$REGION" >/dev/null 2>&1 \
  && echo "   (ya existe)" \
  || gcloud artifacts repositories create "$REPOSITORY" \
       --repository-format=docker --location="$REGION" \
       --description="Imágenes del hands-on M10"

echo "▶ 3/5 Service account que desplegará desde GitHub"
if gcloud iam service-accounts describe "$SA_EMAIL" >/dev/null 2>&1; then
  echo "   (ya existe)"
else
  gcloud iam service-accounts create "$SA_NAME" --display-name="GitHub Actions deployer"
  # IAM es eventualmente consistente: esperar a que la cuenta sea visible
  for i in $(seq 1 15); do
    gcloud iam service-accounts describe "$SA_EMAIL" >/dev/null 2>&1 && break
    sleep 2
  done
fi
for ROLE in roles/run.admin roles/artifactregistry.writer roles/iam.serviceAccountUser; do
  for i in 1 2 3 4 5; do
    gcloud projects add-iam-policy-binding "$PROJECT_ID" \
      --member="serviceAccount:${SA_EMAIL}" --role="$ROLE" --condition=None >/dev/null && break
    echo "   reintentando ($i)..."; sleep 4
  done
done

echo "▶ 4/5 Workload Identity Federation (sin llaves)"
POOL_STATE="$(gcloud iam workload-identity-pools describe "$POOL" --location=global --format='value(state)' 2>/dev/null || true)"
if [ -z "$POOL_STATE" ]; then
  gcloud iam workload-identity-pools create "$POOL" --location=global --display-name="GitHub"
elif [ "$POOL_STATE" = "DELETED" ]; then
  echo "   El pool estaba borrado (se conserva 30 días): restaurándolo"
  gcloud iam workload-identity-pools undelete "$POOL" --location=global
else
  echo "   (pool ya existe)"
fi
PROVIDER_STATE="$(gcloud iam workload-identity-pools providers describe "$PROVIDER" --location=global --workload-identity-pool="$POOL" --format='value(state)' 2>/dev/null || true)"
if [ "$PROVIDER_STATE" = "DELETED" ]; then
  gcloud iam workload-identity-pools providers undelete "$PROVIDER" --location=global --workload-identity-pool="$POOL"
fi
# create-oidc si no existe; update-oidc si ya existe (así la condición siempre apunta a TU repo)
if [ -z "$PROVIDER_STATE" ]; then
  CMD=create-oidc
else
  CMD=update-oidc
fi
gcloud iam workload-identity-pools providers "$CMD" "$PROVIDER" \
  --location=global --workload-identity-pool="$POOL" \
  --issuer-uri="https://token.actions.githubusercontent.com" \
  --attribute-mapping="google.subject=assertion.sub,attribute.repository=assertion.repository" \
  --attribute-condition="assertion.repository=='${GITHUB_REPO}'"

echo "▶ 5/5 Solo ESTE repositorio puede usar la service account"
gcloud iam service-accounts add-iam-policy-binding "$SA_EMAIL" \
  --role="roles/iam.workloadIdentityUser" \
  --member="principalSet://iam.googleapis.com/projects/${PROJECT_NUMBER}/locations/global/workloadIdentityPools/${POOL}/attribute.repository/${GITHUB_REPO}" >/dev/null

cat <<EOF

✅ Listo. Crea estas VARIABLES en GitHub (Settings → Secrets and variables → Actions → Variables):

  GCP_PROJECT_ID       = ${PROJECT_ID}
  GCP_REGION           = ${REGION}
  WIF_PROVIDER         = projects/${PROJECT_NUMBER}/locations/global/workloadIdentityPools/${POOL}/providers/${PROVIDER}
  WIF_SERVICE_ACCOUNT  = ${SA_EMAIL}
  FUNCTION_URL         = (la URL de la función, cuando la despliegues; puede ir vacía al inicio)
EOF
