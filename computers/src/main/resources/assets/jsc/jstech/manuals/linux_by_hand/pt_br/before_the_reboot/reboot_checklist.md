---
section: before_the_reboot
order: 328
---
# O que o novo sistema precisa antes de reiniciar

Quando o novo sistema estiver instalado, você reinicia a máquina nele com o comando reboot. Antes de reiniciar a máquina, o shell confere uma lista curta do que o novo sistema precisa para iniciar. Se algo da lista estiver faltando, a máquina não reinicia: o shell nomeia os itens que faltam, e sua sessão live continua aberta.

O passo de reinício de cada guia ([](jsc:arch_reboot), [](jsc:gentoo_reboot)) roda essa verificação. A lista tem duas partes. A parte sempre perguntada reúne os passos que toda instalação precisa. A parte opcional reúne o restante do manual, e o shell só a verifica quando uma configuração pede.

## Verificação antes de reiniciar

1. Digite reboot e pressione [[Enter]].
2. Se o shell imprimir "O sistema novo não vai inicializar:", leia as linhas abaixo dessa mensagem.
3. Faça o passo que cada linha indica.
4. Digite reboot de novo.

## A parte sempre perguntada

Cada linha nomeia um item que falta, a linha que o shell imprime para ele e o passo que resolve.

Table: Sempre pedido

| Item ausente | O shell imprime | O passo que resolve |
| --- | --- | --- |
| Sistema base (Gentoo) | `no base system installed (wget the stage 3, then tar)` | Descompacte o arquivo do estágio 3 no disco ([](jsc:stage_three)). |
| Sistema base (Arch) | `no base system installed (pacstrap)` | Instale o sistema base com pacstrap ([](jsc:arch_base_system)). |
| Tabela do que montar (Gentoo) | `no root filesystem in /etc/fstab (blkid, then write the line)` | Escreva a linha da partição raiz ([](jsc:fstab_by_hand)). |
| Tabela do que montar (Arch) | `no filesystem table (genfstab -U /mnt >> /mnt/etc/fstab)` | Escreva a tabela com genfstab ([](jsc:arch_fstab)). |
| Kernel (Gentoo) | `no kernel (genkernel all, or make in /usr/src/linux)` | Compile e instale o kernel ([](jsc:kernel_gentoo)). |
| Carregador de boot (ambos) | `no bootloader (grub-install)` | Instale o GRUB ([](jsc:arch_bootloader) no Arch, [](jsc:gentoo_bootloader) no Gentoo). |
| Lista do carregador de boot (ambos) | `nothing for the bootloader to start (grub-mkconfig -o /boot/grub/grub.cfg)` | Escreva a lista com grub-mkconfig ([](jsc:arch_grub_config)). |

O Gentoo confere sua tabela pelo conteúdo do arquivo, sem rodar uma ferramenta. Uma linha que não começa com cerquilha precisa de pelo menos três campos, e o segundo campo precisa ser uma barra. A dica cita o blkid, a ferramenta que mostra os detalhes de cada partição. O estágio 3 escreve uma tabela só com comentários, então este item continua na lista até você escrever a linha da raiz. No Arch, o item sai assim que o genfstab rodar.

## A parte opcional

O shell só pergunta a parte opcional quando a configuração do mundo para aquele sistema está ligada. As duas configurações vêm desligadas por padrão, então um mundo pergunta apenas o básico. As configurações são install_by_hand.gentoo_every_step e install_by_hand.arch_every_step, na configuração do servidor.

Table: A parte opcional, quando sua configuração está ligada

| Item ausente | O shell imprime | O passo que resolve |
| --- | --- | --- |
| Montagens bind (Gentoo) | `/proc, /sys, /dev and /run were not bound into the new system (mount --rbind, --types proc)` | Faça o bind das quatro pastas no novo sistema ([](jsc:mounting)). |
| Perfil (Gentoo) | `no profile chosen (eselect profile list, then set)` | Escolha um perfil ([](jsc:package_tree_and_world)). |
| Atualização do world (Gentoo) | `the system was not brought up to date (emerge --ask --verbose --update --deep --newuse @world)` | Execute a atualização que a linha indica ([](jsc:package_tree_and_world)). |
| Link do kernel (Gentoo) | `/usr/src/linux points nowhere (eselect kernel set 1)` | Aponte /usr/src/linux para um kernel com eselect kernel set ([](jsc:kernel_gentoo)). |
| Relógio de hardware (Arch) | `the hardware clock was not set (hwclock --systohc)` | Ajuste o relógio de hardware com hwclock ([](jsc:arch_clock_and_name)). |
| Nome da máquina (ambos) | `the machine has no name (echo <name> > /etc/hostname)` | Dê um nome à máquina, como explica a seção abaixo. |

## O nome da máquina

Um nome tem de 1 a 15 caracteres. Ele usa letras, dígitos e hifens, e começa com uma letra ou um dígito. Você o define com o comando hostname, ou escrevendo-o em /etc/hostname dentro do novo sistema. De qualquer forma, a lista o conta.

> [!DEFINE] Lista de verificação da reinicialização
> A lista do que o novo sistema ainda precisa, que o shell live monta antes de reiniciar a máquina.

## Se algo der errado

> [!PROBLEM] hostname: o nome de host especificado é inválido
> Use de 1 a 15 letras, dígitos ou hifens, e comece com uma letra ou um dígito.

> [!PROBLEM] hostname: você não pode alterar o nome de host da mídia live
> Entre no novo sistema primeiro ([](jsc:arch_chroot) no Arch, [](jsc:mounts_and_chroot) no Gentoo), e depois defina o nome ali.
