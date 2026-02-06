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
