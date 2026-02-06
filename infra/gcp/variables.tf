variable "project_id" {
  type = string
}

variable "region" {
  type    = string
  default = "us-central1"
}

variable "service_name" {
  type    = string
  default = "event-relay"
}

variable "image" {
  type = string
}

variable "db_name" {
  type    = string
  default = "eventrelay"
}

variable "db_user" {
  type    = string
  default = "eventrelay"
}

variable "db_password" {
  type      = string
  sensitive = true
}

variable "api_key" {
  type      = string
  sensitive = true
}

variable "db_tier" {
  type    = string
  default = "db-f1-micro"
}

variable "subnet_cidr" {
  type    = string
  default = "10.10.0.0/24"
}

variable "connector_cidr" {
  type    = string
  default = "10.8.0.0/28"
}

variable "public_service" {
  type    = bool
  default = true
}

variable "min_instances" {
  type    = number
  default = 0
}

variable "max_instances" {
  type    = number
  default = 5
}
