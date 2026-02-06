# GCP Terraform

This folder provisions Cloud Run, Cloud SQL, Secret Manager, and a VPC connector

## Prereqs
- `gcloud auth application-default login`
- Terraform 1.5+

## Apply
```bash
terraform init
terraform apply \
  -var project_id=YOUR_PROJECT_ID \
  -var image=YOUR_IMAGE_URI \
  -var db_password=YOUR_DB_PASSWORD \
  -var api_key=YOUR_API_KEY
```

## Outputs
- Cloud Run URL
- Cloud SQL private IP
- Secret Manager key name

## CI/CD deploy
The GitHub Actions workflow `.github/workflows/deploy-gcp.yml` expects:
- `GCP_WIF_PROVIDER` secret with the Workload Identity Provider resource
- `GCP_SERVICE_ACCOUNT` secret with the deploy service account email

You must also create an Artifact Registry repo and grant the service account:
- `roles/run.admin`
- `roles/artifactregistry.writer`
- `roles/iam.serviceAccountUser`
