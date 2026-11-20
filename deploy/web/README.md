# Website on Cloud Run

Serves `docs/` (also published on GitHub Pages) from nginx on Cloud Run.

```sh
PROJECT=present-hellohuman-1120
IMAGE=asia-south1-docker.pkg.dev/$PROJECT/cloud-run-source-deploy/present-web
gcloud builds submit . --project $PROJECT --config deploy/web/cloudbuild.yaml \
  --ignore-file deploy/web/.gcloudignore --substitutions _IMAGE=$IMAGE
gcloud run deploy present-web --image $IMAGE --project $PROJECT --region asia-south1 \
  --allow-unauthenticated --port 8080 --memory 256Mi --max-instances 3
```
