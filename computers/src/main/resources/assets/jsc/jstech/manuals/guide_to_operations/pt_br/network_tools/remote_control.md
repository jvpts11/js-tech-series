---
section: network_tools
order: 87
icon: jsc:monitor
---
# Controle Remoto

O Controle Remoto assume o controle de outra máquina da rede e põe a tela dela neste monitor. É a versão com cliques do [ssh](jsc:remote_access), o shell seguro. Ele é mais útil para um servidor em um rack, que não tem tela própria ([](jsc:server_racks)).

A janela lista as máquinas desta rede sob o cabeçalho Máquinas nesta rede. Cada linha mostra o tipo da máquina e o seu sistema, e se ela está ligada ou desligada. Selecione uma máquina e pressione Assumir controle. Assumir o controle põe a sessão da máquina neste monitor: o seu autoteste de inicialização, o seu firmware, o seu terminal ou a sua área de trabalho completa ([](jsc:monitors)).

Table: Partes do Controle Remoto

| Parte | O que mostra |
| --- | --- |
| Cabeçalho | Máquinas nesta rede. |
| Linhas | O tipo da máquina e o seu sistema, e se ela está ligada ou desligada. |
| Botão | Assumir controle, ou Selecionar uma máquina quando nenhuma linha estiver escolhida. |

## Se algo der errado

> [!PROBLEM] Uma máquina aparece como desligada.
> Ligue a máquina antes de poder controlá-la.

> [!PROBLEM] A lista diz que nenhuma outra máquina é alcançável.
> Confira se este computador está na rede cujas máquinas você quer alcançar.

> [!SEE] jsc:mainframes, jsc:remote_access, jsc:monitors
