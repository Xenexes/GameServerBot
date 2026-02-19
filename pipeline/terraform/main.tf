terraform {
  required_version = ">= 1.0.0"
  required_providers {
    proxmox = {
      source  = "bpg/proxmox"
      version = "~> 0.97.1"
    }
  }
}

provider "proxmox" {
  endpoint  = var.proxmox_api_url
  api_token = "${var.proxmox_api_token_id}=${var.proxmox_api_token_secret}"
  insecure  = var.proxmox_tls_insecure
}

resource "proxmox_virtual_environment_container" "game_server_bot" {
  node_name     = var.target_node
  vm_id         = var.vmid
  unprivileged  = true
  start_on_boot = true
  started       = true

  operating_system {
    template_file_id = var.template
    type             = "debian"
  }

  initialization {
    hostname = var.hostname

    ip_config {
      ipv4 {
        address = var.ip_address
        gateway = var.gateway
      }
    }

    user_account {
      keys     = [var.ssh_public_key]
      password = var.root_password
    }
  }

  network_interface {
    name   = "eth0"
    bridge = var.bridge
  }

  disk {
    datastore_id = var.storage
    size         = var.disk_size
  }

  features {
    nesting = true
  }

  cpu {
    cores = var.cores
  }

  memory {
    dedicated = var.memory
  }

  connection {
    type        = "ssh"
    host        = split("/", var.ip_address)[0]
    user        = "root"
    private_key = file(pathexpand(var.ssh_private_key_path))
    timeout     = "3m"
  }

  provisioner "remote-exec" {
    inline = [
      "apt-get update -qq",
      "apt-get install -y -qq python3",
    ]
  }
}
