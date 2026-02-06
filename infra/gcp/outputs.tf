output "service_url" {
  value = google_cloud_run_v2_service.main.uri
}

output "db_private_ip" {
  value = google_sql_database_instance.main.private_ip_address
}

output "api_key_secret" {
  value = google_secret_manager_secret.api_key.secret_id
}
