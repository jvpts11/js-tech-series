---
section: arch_finish
order: 316
icon: jsc:cd_rom
---
# Verificando e reiniciando

Antes de reiniciar, o instalador verifica se o sistema novo consegue iniciar. Se uma peça estiver faltando, a máquina lista o que falta e se recusa a reiniciar até você adicionar a peça.

Quando a verificação passa, o instalador copia o sistema novo para o disco que contém /mnt. Depois a máquina reinicia nele. A cópia também leva a tabela do sistema de arquivos que você escreveu, o nome que você escolheu e os programas que você pediu com pacman -S.

1. Digite exit e aperte [[Enter]]. O prompt mostra de novo a mídia ao vivo.
2. Digite reboot e aperte [[Enter]].

Table: O que a verificação procura

| Parte faltando | A mensagem que ela exibe |
| --- | --- |
| O sistema base | `no base system installed (pacstrap)` |
| A tabela do sistema de arquivos | `no filesystem table (genfstab -U /mnt >> /mnt/etc/fstab)` |
| O gerenciador de inicialização | `no bootloader (grub-install)` |
| A lista de inicialização | `nothing for the bootloader to start (grub-mkconfig -o /boot/grub/grub.cfg)` |

As imagens de inicialização não estão nesta lista. Se a configuração do servidor Every step of Arch estiver ligada, a lista também verifica o relógio de hardware e o nome da máquina ([](jsc:arch_clock_and_name)).

Se uma peça estiver faltando, a reinicialização para e lista o que falta:

```session
{"lines": [{"prompt": "root@archiso ~ # ", "text": "reboot", "typed": true}, {"text": "The new system will not boot:"}, {"text": "  - no bootloader (grub-install)"}, {"text": "  - nothing for the bootloader to start (grub-mkconfig -o /boot/grub/grub.cfg)"}], "look": "jsc:guide/terminal/zsh", "title": "Arch Linux"}
```

Depois de uma recusa, faça cada passo listado, neste capítulo, e depois reinicie de novo. O firmware inicia primeiro o disco instalado, e a lista de inicialização mostra a entrada Arch Linux.

## Se algo der errado

> [!PROBLEM] A tela mostra "reboot: você está dentro do sistema novo. saia primeiro."
> Você ainda está dentro do sistema novo. Digite exit e depois reboot.

> [!PROBLEM] A tela mostra "O sistema novo não vai inicializar:" e lista as peças que faltam.
> Faça cada passo listado e depois reinicie de novo.

> [!PROBLEM] A tela mostra "A instalação não pôde ser gravada no disco (sem espaço ou sem disco)."
> O disco que contém /mnt não tem espaço para o sistema, ou não está lá. Verifique o disco e depois instale de novo.

> [!SEE] jsc:when_reboot_refuses
