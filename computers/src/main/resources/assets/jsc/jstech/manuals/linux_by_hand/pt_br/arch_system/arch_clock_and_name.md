---
section: arch_system
order: 312
icon: jsc:cd_rom
---
# O relógio e o nome da máquina

Duas configurações tornam o novo sistema seu: o relógio e o nome da máquina. O instalador pergunta as duas antes de reiniciar só quando a configuração do servidor Every step of Arch está ativa ([](jsc:installing)). Você pode sempre defini-las com os comandos abaixo.

O comando hwclock --systohc copia o relógio do sistema para o relógio de hardware, para que a hora seja mantida enquanto o computador está desligado.

> [!DEFINE] relógio de hardware
> O relógio da placa-mãe, que mantém a hora enquanto o computador está desligado.

1. Digite hwclock --systohc e pressione [[Enter]].
2. Digite echo jarch > /etc/hostname, com o seu próprio nome no lugar de jarch, e pressione [[Enter]].
3. Digite hostname e pressione [[Enter]]. Ele imprime o nome que você escolheu.

Table: As regras para o nome da máquina

| Regra | O que diz |
| --- | --- |
| Comprimento | No máximo 15 caracteres. |
| Primeiro caractere | Uma letra ou um dígito. |
| Demais caracteres | Letras, dígitos e hífens. |
| Não permitido | Pontos, sublinhados e espaços. |

O nome vai com o novo sistema quando a instalação termina. A máquina instalada o mantém, e a rede o mostra.

## Se algo der errado

> [!PROBLEM] A tela mostra "hostname: você não pode alterar o nome de host da mídia live".
> Você está na mídia ao vivo, não no novo sistema. Entre primeiro com arch-chroot /mnt ([](jsc:arch_chroot)).

> [!PROBLEM] A tela mostra "hostname: o nome de host especificado é inválido".
> O nome quebra uma regra da tabela, ou tem mais de uma palavra. Use letras, dígitos e hífens, no máximo 15 caracteres, começando com uma letra ou um dígito.

> [!SEE] jsc:arch_mkinitcpio
