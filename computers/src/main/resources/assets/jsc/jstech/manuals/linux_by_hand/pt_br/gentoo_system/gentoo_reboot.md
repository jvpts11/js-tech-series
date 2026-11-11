---
section: gentoo_system
order: 326
---
# Reiniciando no Gentoo

O reboot é o último passo. Antes de iniciar o novo sistema, a máquina verifica se o sistema tem tudo o que precisa para inicializar. Se você deixou algo para trás, ela lista os itens faltantes e se recusa a iniciar ([](jsc:reboot_checklist)).

1. Digite exit e pressione [[Enter]].
2. Digite cd e pressione [[Enter]].
3. Digite umount -R /mnt/gentoo e pressione [[Enter]]. O comando não imprime nada.
4. Digite reboot e pressione [[Enter]].

A máquina sempre verifica os itens da primeira tabela. Cada item faltante é impresso com sua correção entre colchetes.

Table: Sempre verificado

| Item faltando | O que a máquina diz |
| --- | --- |
| Sem base | nenhum sistema base instalado (wget do estágio 3, depois tar) |
| Sem root no fstab | nenhum sistema de arquivos raiz em /etc/fstab (blkid, depois escreva a linha) |
| Sem kernel | nenhum kernel (genkernel all, ou make em /usr/src/linux) |
| Sem bootloader | nenhum bootloader (grub-install) |
| Sem menu do GRUB | nada para o bootloader iniciar (grub-mkconfig -o /boot/grub/grub.cfg) |

Com install_by_hand.gentoo_every_step definido como true, a máquina também verifica os itens da segunda tabela.

Table: Verificado quando todos os passos são pedidos

| Item faltando | O que a máquina diz |
| --- | --- |
| Vínculos | /proc, /sys, /dev e /run não foram vinculados ao novo sistema (mount --rbind, --types proc) |
| Perfil | nenhum perfil escolhido (eselect profile list, depois set) |
| World | o sistema não foi atualizado (emerge --ask --verbose --update --deep --newuse @world) |
| Link do kernel | /usr/src/linux não aponta para lugar nenhum (eselect kernel set 1) |
| Nome | a máquina não tem nome (echo <name> > /etc/hostname) |

O nome é definido dentro do novo sistema, com echo <name> > /etc/hostname ou com hostname <name>. Um nome começa com uma letra ou dígito, tem até 15 caracteres e pode conter hífens.

Quando todos os itens estiverem no lugar, a reinicialização faz o disco escolhido ser o primeiro a inicializar. A máquina responde "Reiniciando para o novo sistema".

## Se algo der errado

> [!PROBLEM] O reboot diz "você está dentro do novo sistema. Saia primeiro com exit."
> Digite exit para sair do novo sistema, e depois digite reboot de novo.

> [!PROBLEM] A máquina diz "O novo sistema não vai inicializar" e lista os itens.
> Cada item indica sua correção entre colchetes. Faça esse passo e digite reboot de novo.

> [!PROBLEM] O hostname diz "o nome de host especificado é inválido".
> Use até 15 letras, dígitos e hífens, e comece com uma letra ou dígito.

> [!PROBLEM] O hostname diz "você não pode alterar o nome de host da mídia ao vivo".
> Entre primeiro no novo sistema com chroot /mnt/gentoo /bin/bash, e depois defina o nome.

> [!SEE] jsc:reboot_checklist, jsc:will_not_boot_messages, jsc:after_reboot_checks
