---
section: disks_and_partitions
order: 31
icon: jsc:disk_vaultis_trench_20m
---
# Dois sistemas num disco

Cada instalador pergunta a você onde o sistema vai, do jeito que o de verdade perguntava, e pode pô-lo ao lado de outro sistema no mesmo disco.

Table: Onde cada instalador põe o seu sistema

| Instalador | O que pergunta |
| --- | --- |
| MC-DOS, UNIX System V, Frames 95 | Nada: ele toma o disco e o formata. |
| Frames XP | Uma lista das partições e do espaço livre, em modo texto; depois, como formatar a que você escolher. |
| Frames 7 ao 11 | Uma tabela das partições, com Novo, Excluir, Formatar e Estender por baixo. |
| Ubuntu, Linux Mint | Instalar ao lado do outro sistema: um divisor encolhe a sua partição, e as setas o movem. |
| Debian, Fedora | Cada disco com as suas partições, e onde o sistema vai ficar. |
| FreeBSD | Auto (ZFS), com um espelho num segundo disco se você quiser, ou Auto (UFS). |

Numa máquina UEFI o instalador cria primeiro a partição EFI, quando o disco não tem uma. No Linux, nos BSD e no UNIX ele cria também uma partição de swap do tamanho da memória, quando há espaço para ela ([](jsc:swap)).

## Escolher o que arranca

Com dois sistemas numa máquina, alguma coisa tem de perguntar qual arranca. Do Standard em diante, o menu GRUB de um sistema Linux lista todos os outros sistemas, e o Frames Boot Manager da partição EFI; o Midsoft Boot Manager lista cada Frames com a sua partição. O menu de arranque do firmware, aberto com F12 quando a máquina liga, tem uma linha para cada sistema em cada partição.

Na Vintage, o disco arranca a sua partição ativa, e nada pergunta. Para arrancar o outro sistema, torne a partição dele a ativa: a opção 2 do FDISK no MC-DOS, ou do fdisk no UNIX.

1. Instale o primeiro sistema e deixe espaço livre no disco, ou deixe o segundo instalador encolher o primeiro.
2. Inicie o segundo instalador e escolha o espaço livre, ou Instalar ao lado.
3. Depois da cópia, reinicie. Escolha o sistema no menu de arranque, ou defina a partição ativa na Vintage.

## Se algo der errado

> [!PROBLEM] O instalador diz que não há espaço.
> Libere espaço no disco primeiro, diminuindo um volume com uma ferramenta de disco.

> [!PROBLEM] Um disco MBR recusa uma nova partição.
> Ele já tem quatro. Exclua uma, ou use um disco GPT.

> [!SEE] jsc:disk_tools_of_each_system, jsc:partitions
