variable "proxmox_api_url" {
  type        = string
  description = "Proxmox API URL (e.g. https://pve.example.com:8006/api2/json)"
}

variable "proxmox_api_token_id" {
  type        = string
  sensitive   = true
  description = "Proxmox API token ID (e.g. root@pam!terraform)"
}

variable "proxmox_api_token_secret" {
  type        = string
  sensitive   = true
  description = "Proxmox API token secret"
}

variable "proxmox_tls_insecure" {
  type        = bool
  default     = true
  description = "Skip TLS verification for Proxmox API (use true for self-signed certs)"
}

variable "target_node" {
  type        = string
  default     = "pve"
  description = "Proxmox node name to create the LXC on"
}

variable "vmid" {
  type        = number
  default     = 200
  description = "LXC container ID"
}

variable "hostname" {
  type        = string
  default     = "game-server-bot"
  description = "LXC container hostname"
}

variable "template" {
  type        = string
  description = "Proxmox LXC template path (e.g. local:vztmpl/debian-13-standard_13.1-2_amd64.tar.zst)"
}

variable "ip_address" {
  type        = string
  description = "Static IP in CIDR notation (e.g. 192.168.1.50/24)"
}

variable "gateway" {
  type        = string
  description = "Network gateway IP"
}

variable "bridge" {
  type        = string
  default     = "vmbr0"
  description = "Proxmox network bridge"
}

variable "storage" {
  type        = string
  default     = "local-lvm"
  description = "Proxmox storage pool for the rootfs"
}

variable "disk_size" {
  type        = number
  default     = 8
  description = "Root filesystem size in GB"
}

variable "cores" {
  type        = number
  default     = 2
  description = "Number of CPU cores"
}

variable "memory" {
  type        = number
  default     = 1024
  description = "Memory in MB"
}

variable "root_password" {
  type        = string
  sensitive   = true
  description = "Root password for console access via Proxmox UI"
}

variable "ssh_public_key" {
  type        = string
  sensitive   = true
  description = "SSH public key content to inject into the LXC"
}

variable "ssh_private_key_path" {
  type        = string
  default     = "~/.ssh/id_ed25519"
  description = "Path to SSH private key for bootstrap connection (~ is expanded)"
}
