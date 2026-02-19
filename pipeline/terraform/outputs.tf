output "container_ip_address" {
  value       = split("/", var.ip_address)[0]
  description = "IP address of the deployed LXC container"
}

output "container_id" {
  value       = proxmox_virtual_environment_container.game_server_bot.vm_id
  description = "Proxmox VM ID of the LXC container"
}

output "ssh_connection_command" {
  value       = "ssh root@${split("/", var.ip_address)[0]}"
  description = "SSH command to connect to the LXC container"
}

output "hawser_url" {
  value       = "http://${split("/", var.ip_address)[0]}:2376"
  description = "Hawser (Dockhand agent) URL for remote container management"
}
