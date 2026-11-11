---
section: arch_boot
order: 315
icon: jsc:cd_rom
---
# Escrevendo a lista de inicialização (grub-mkconfig)

O gerenciador de inicialização precisa de uma lista do que iniciar. Essa lista é um arquivo, e a ferramenta grub-mkconfig o escreve. Ela encontra o kernel e as imagens de inicialização em /boot, e escreve uma entrada de menu para eles.

> [!DEFINE] grub.cfg
> O arquivo que o gerenciador de inicialização lê ao iniciar. Ele diz qual kernel iniciar, e como.

O arquivo é sempre gravado em /boot/grub/grub.cfg, seja o que for que você digitar depois de -o.

1. Confirme que você está dentro do novo sistema.
2. Digite grub-mkconfig -o /boot/grub/grub.cfg e pressione [[Enter]].
3. Aguarde o done no fim da saída.

```session
{"lines": [{"prompt": "[root@archiso /]# ", "text": "grub-mkconfig -o /boot/grub/grub.cfg", "typed": true}, {"text": "Generating grub configuration file ..."}, {"text": "Found linux image: /boot/vmlinuz-linux"}, {"text": "Found initrd image: /boot/initramfs-linux.img"}, {"text": "Warning: os-prober will not be executed to detect other bootable"}, {"text": "partitions. Systems on them will not be added to the GRUB boot"}, {"text": "configuration. Check GRUB_DISABLE_OS_PROBER documentation entry."}, {"text": "done"}], "look": "jsc:guide/terminal/zsh", "title": "Arch Linux"}
```

A primeira linha da saída diz que o arquivo de configuração está sendo gerado. Em seguida aparece um aviso, e ele é esperado. O os-prober é uma ferramenta que procura outros sistemas em outros discos, e aqui ele não é executado, então nenhum outro sistema recebe uma entrada. Em UEFI, a saída também diz que adiciona uma entrada no menu de inicialização para as configurações do firmware.

O arquivo tem uma entrada de menu, chamada Arch Linux. Ela carrega o kernel e as imagens de inicialização, e informa ao kernel qual partição é a root, pelo seu UUID.

## Se algo der errado

> [!PROBLEM] A tela mostra "/usr/bin/grub-mkconfig: /boot/grub: Arquivo ou diretório inexistente".
> O gerenciador de inicialização ainda não está instalado, então a pasta dele não existe. Instale-o primeiro ([](jsc:arch_bootloader)).

> [!PROBLEM] A tela mostra "Uso: grub-mkconfig -o /boot/grub/grub.cfg".
> O comando não tem -o. Digite grub-mkconfig -o /boot/grub/grub.cfg.

> [!PROBLEM] A tela mostra "grub-mkconfig: comando não encontrado (é do sistema novo: entre nele primeiro)".
> Você está na mídia ao vivo. Entre primeiro com arch-chroot /mnt ([](jsc:arch_chroot)).

> [!SEE] jsc:arch_reboot
