---
section: arch_boot
order: 314
icon: jsc:cd_rom
---
# Instalando o gerenciador de inicialização

Quando o firmware inicia um disco, ele inicia o primeiro programa dele: o gerenciador de inicialização. O gerenciador então inicia o kernel com as imagens de inicialização. O Arch usa o GRUB para essa tarefa. O GRUB mostra um menu, ou inicia sua única entrada, e carrega o kernel.

> [!DEFINE] firmware
> O programa na placa-mãe que inicia o computador, antes de qualquer sistema rodar.

> [!DEFINE] gerenciador de inicialização
> O programa que o firmware inicia primeiro, e que então inicia o kernel.

> [!DEFINE] GRUB
> O gerenciador de inicialização que o Arch usa.

O gerenciador de inicialização vem em dois pacotes, e você instala os dois com o pacman. O pacote efibootmgr é a ferramenta que informa ao firmware onde está o gerenciador de inicialização. O comando grub-install se recusa a rodar até que o pacote grub esteja instalado.

Table: Os pacotes do gerenciador de inicialização

| Pacote | Tamanho |
| --- | --- |
| `grub` | 7,8 MiB |
| `efibootmgr` | 0,1 MiB |

1. Confirme que você está dentro do novo sistema.
2. Digite pacman -S grub efibootmgr e pressione [[Enter]].
3. Pressione [[Enter]] ou digite Y na pergunta, e aguarde até a instalação terminar.
4. Digite o comando grub-install do seu firmware, da tabela abaixo, e pressione [[Enter]].

```session
{"lines": [{"prompt": "[root@archiso /]# ", "text": "pacman -S grub efibootmgr", "typed": true}, {"text": ":: Proceed with installation? [Y/n]"}], "look": "jsc:guide/terminal/zsh", "title": "Arch Linux"}
```

Existem dois tipos de firmware: o mais antigo Basic Input/Output System (BIOS) e o mais novo Unified Extensible Firmware Interface (UEFI). A tabela mostra a instalação de cada um.

Table: A instalação para cada firmware

| Firmware | Comando | Plataforma que informa |
| --- | --- | --- |
| UEFI, na era Standard e posteriores | `grub-install --efi-directory=/boot` | `x86_64-efi` |
| BIOS, nas eras Legacy e Transition | `grub-install /dev/sda` | `i386-pc` |

Qual linha é a sua depende do firmware do seu computador ([](jsc:firmware_for_install)). Em firmware mais antigo, o gerenciador de inicialização vai no disco inteiro, não em uma partição. No UEFI, monte a partição de sistema EFI em /mnt/boot antes de entrar ([](jsc:mounting)), para que /boot dentro do novo sistema seja essa partição.

No UEFI a tela mostra "Instalando para a plataforma x86_64-efi.", e no BIOS mostra "Instalando para a plataforma i386-pc.". As duas terminam com "Instalação concluída. Nenhum erro relatado." A instalação não grava a lista de inicialização. Esse é o próximo passo ([](jsc:arch_grub_config)).

## Se algo der errado

> [!PROBLEM] A tela mostra "zsh: comando não encontrado: grub-install", com uma linha que diz que o gerenciador de inicialização é um pacote.
> O pacote GRUB ainda não está instalado. Instale-o primeiro: pacman -S grub efibootmgr.

> [!PROBLEM] A tela mostra "grub-install: comando não encontrado (é do sistema novo: entre nele primeiro)".
> Você está na mídia ao vivo. Entre primeiro com arch-chroot /mnt ([](jsc:arch_chroot)).

> [!PROBLEM] A tela mostra "grub-install: erro: /boot/efi não parece ser uma partição EFI."
> A partição de sistema EFI não está montada onde o comando procura. Monte-a em /mnt/boot antes de entrar ([](jsc:mounting)), depois execute grub-install --efi-directory=/boot.

> [!PROBLEM] A tela mostra "grub-install: erro: o dispositivo de instalação não foi especificado."
> Em firmware mais antigo, informe o disco: grub-install /dev/sda.

> [!PROBLEM] A tela mostra "grub-install: erro: não foi possível encontrar um dispositivo para" e o nome de uma partição, como /dev/sda2.
> O gerenciador de inicialização vai no disco, não em uma partição. Informe o disco: grub-install /dev/sda.

> [!SEE] jsc:arch_grub_config
