---
section: arch_system
order: 313
icon: jsc:cd_rom
---
# Reconstruindo as imagens de inicialização (mkinitcpio)

Antes de o sistema real iniciar, o kernel precisa de um pequeno sistema temporário que carregue os drivers dos discos. Esse sistema é o initramfs. O Arch constrói dois deles, chamados imagens de inicialização: uma normal e uma de reserva, para quando a normal não iniciar.

> [!DEFINE] initramfs
> Um pequeno sistema temporário que inicia antes do real, para carregar os drivers dos discos.

A ferramenta que as constrói é o mkinitcpio. Digite mkinitcpio -P para construir as duas imagens de novo. A ferramenta trabalha no novo sistema, então você precisa estar dentro dele.

1. Confirme que você está dentro do novo sistema.
2. Digite mkinitcpio -P e pressione [[Enter]].
3. Aguarde até o prompt voltar.

```session
{"lines": [{"prompt": "[root@archiso /]# ", "text": "mkinitcpio -P", "typed": true}, {"text": "==> Building image from preset: /etc/mkinitcpio.d/linux.preset: 'default'"}, {"text": "==> Using default configuration file: '/etc/mkinitcpio.conf'"}, {"text": "  -> -k /boot/vmlinuz-linux -g /boot/initramfs-linux.img"}, {"text": "==> Starting build: '6.11.5-arch1-1'"}], "look": "jsc:guide/terminal/zsh", "title": "Arch Linux"}
```

Cada imagem é construída em etapas chamadas hooks. São doze: base, udev, autodetect, microcode, modconf, kms, keyboard, keymap, consolefont, block, filesystems e fsck.

> [!DEFINE] hook
> Uma etapa da construção de uma imagem de inicialização.

A imagem de reserva pula a etapa autodetect. O pacstrap já construiu as duas imagens nas últimas etapas, então este comando serve para quando você quiser construí-las de novo. Em um processador lento, leva mais tempo.

## Se algo der errado

> [!PROBLEM] A tela mostra "mkinitcpio: comando não encontrado (é do sistema novo: arch-chroot /mnt primeiro)".
> Você está na mídia ao vivo. Entre primeiro com arch-chroot /mnt ([](jsc:arch_chroot)).
