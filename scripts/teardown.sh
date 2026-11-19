#!/usr/bin/env bash
# Limpieza: borra lo que creó el hands-on para no generar costos.
# Uso:  PROJECT_ID=mi-proyecto ./scripts/teardown.sh
set -euo pipefail
: "${PROJECT_ID:?Define PROJECT_ID}"
REGION="${REGION:-us-central1}"
gcloud config set project "$PROJECT_ID"

gcloud run services delete pokedex-api --region "$REGION" --quiet || true
gcloud run services delete pokemon-of-the-day --region "$REGION" --quiet || true
gcloud artifacts repositories delete pokedex --location "$REGION" --quiet || true
# Creados automáticamente por "gcloud run deploy --source" (compartidos por todos los deploys desde código fuente del proyecto)
gcloud artifacts repositories delete cloud-run-source-deploy --location "$REGION" --quiet || true
gcloud storage rm -r "gs://run-sources-${PROJECT_ID}-${REGION}" --quiet || true
gcloud iam service-accounts delete "github-deployer@${PROJECT_ID}.iam.gserviceaccount.com" --quiet || true
gcloud iam workload-identity-pools delete github --location=global --quiet || true
echo "✅ Recursos eliminados. Si el proyecto era solo para la clase, también puedes borrarlo:"
echo "   gcloud projects delete ${PROJECT_ID}"
