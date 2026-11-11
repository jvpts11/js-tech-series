---
section: before_the_reboot
order: 329
---
# As recusas e as suas soluções

Uma recusa é o comando reboot avisando que não reiniciou a máquina. Nada se perde quando ele recusa: sua sessão ativa continua aberta. Digite a etapa que falta, depois rode reboot de novo.

As linhas aparecem no shell ativo depois que você digita reboot, cada uma exatamente como mostrada na tabela. A tabela diz o que cada linha significa e o que você faz a respeito.

Table: As linhas que o comando reboot imprime

| A linha | O que significa | A solução |
| --- | --- | --- |
| `reboot: you are inside the new system. exit first.` | Você ainda está dentro do novo sistema, entrou com chroot. | Digite exit para sair do novo sistema, depois rode reboot. |
| `The new system will not boot:` | Ainda faltam itens da lista de verificação, um para cada linha abaixo dela. | Faça a etapa que cada linha aponta ([](jsc:reboot_checklist)), depois rode reboot de novo. |
| `Rebooting into the new system ...` | Nada está faltando. A instalação é gravada no disco, e a máquina reinicia. | Nada: a máquina reinicia. |
| `The installation could not be written to the disk (no space or no disk).` | Nenhum disco tem espaço para o sistema, ou não há disco para gravar. | Libere espaço no disco, ou coloque um disco no computador, depois rode reboot de novo. |

## O reboot não verifica as montagens

O comando reboot não verifica se o disco do novo sistema ainda está montado. Só a lista de verificação e a gravação no disco decidem. Os guias de Gentoo e Arch ainda mandam desmontar antes com umount -R, então faça isso antes de rodar reboot, como diz o guia.

Um erro de chroot sobre uma pasta raiz que não existe vem do comando chroot. Não é uma recusa do reboot.

## Se algo der errado

> [!PROBLEM] O reboot diz que você está dentro do novo sistema.
> Digite exit, que sai do novo sistema, depois rode reboot.

> [!PROBLEM] A mensagem do disco aparece, sem espaço ou sem disco.
> Libere espaço no disco, ou coloque um disco no computador. Depois rode reboot de novo.

> [!PROBLEM] A mesma lista aparece de novo depois que você fez os passos dela.
> Rode essa etapa de novo, leia o que ela imprime, depois digite reboot de novo.
