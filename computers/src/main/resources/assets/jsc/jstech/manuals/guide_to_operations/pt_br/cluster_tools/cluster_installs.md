---
section: cluster_tools
order: 157
icon: jsc:cluster_management_computer
---
# Instalando em muitas máquinas

Uma instalação em massa grava um sistema ou um programa em todos os nós de um cluster de uma vez, ou nos nós que você escolher, em vez de uma máquina por vez. Você a inicia pelo Gerenciador de Cluster ([](jsc:cluster_manager)) ou com o comando cluster ([](jsc:cluster_command)).

O disco vem de uma unidade vinculada ao Computador de Gerenciamento de Cluster. Conecte a unidade ao computador com um cabo periférico ([](jsc:peripheral_cables)) e coloque o disco na unidade ([](jsc:drives)).

1. Instale uma Placa de Interface de Cluster no Computador de Gerenciamento de Cluster.
2. Conecte uma unidade ao computador com um cabo, e coloque o disco nela.
3. Abra o Gerenciador de Cluster e pressione SISTEMA EM TODOS ou PROGRAMA EM TODOS. Ou digite cluster install, depois o nome do cluster e, em seguida, system ou program.
4. Acompanhe a instalação na janela, ou digite cluster status.

## As placas e o alcance delas

A placa decide quais máquinas uma instalação em massa alcança, e quantos nós ela grava ao mesmo tempo.

Table: Placas de Interface de Cluster

| Placa | Era | Alcança | Nós ao mesmo tempo |
| --- | --- | --- | --- |
| Placa de Console Serial | Vintage | Somente servidores de datacenter | 1 |
| NIC de Gerenciamento | Legacy | Datacenters e supercomputadores | 2 |
| Adaptador de Host Fabric | Standard | Datacenters, supercomputadores e clusters de IA | 4 |
| Fabric DPU | Advanced | Datacenters, supercomputadores e clusters de IA | 8 |

> [!DEFINE] Faixa
> Um nó que está sendo gravado durante uma instalação em massa. A placa decide quantas faixas rodam ao mesmo tempo.

Cada nó que é gravado ao mesmo tempo ocupa uma faixa. Uma placa colocada num slot mais antigo do que aquele para o qual ela foi feita roda menos faixas, porque esse slot transmite dados mais devagar.

Gravar um nó leva 14 segundos numa máquina com velocidade de clock de 2.000 megahertz (MHz). Uma máquina mais rápida leva menos tempo, até 3 segundos. Uma mais lenta leva mais, até 60 segundos.

## Recusas ao iniciar

Table: Mensagens ao iniciar

| Mensagem | O que fazer |
| --- | --- |
| nenhuma placa de interface de cluster | Instale uma Placa de Interface de Cluster no computador. |
| um job já está em execução | Espere terminar, ou cancele com CANCELAR JOB ou cluster cancel. |
| esta placa não alcança esse cluster | Use uma placa que o alcance, da tabela acima. |
| nenhum disco de sistema em um leitor vinculado | Coloque um disco de sistema em uma unidade vinculada ao computador. |
| nenhum disco de programa em um leitor vinculado | Coloque um disco de programa em uma unidade vinculada ao computador. |
| esse sistema se instala manualmente pelo próprio shell | Instale esse sistema manualmente em cada nó. Uma instalação em massa não consegue gravá-lo. |
| esse disco não traz nada para instalar | Use um disco de programa que traga um programa. |
| nenhum nó nesse cluster | Coloque máquinas nos racks do cluster primeiro. |

## Nós que a instalação ignora

Um nó que não pode receber a instalação é ignorado, e o job segue com os outros nós. O nome do nó vem seguido do motivo entre colchetes.

Table: Motivos de ignorar

| Motivo | O que significa e o que fazer |
| --- | --- |
| compartimento desligado | O nó está desligado. Ligue-o com LIGAR. |
| já instalado | O nó já roda esse sistema, ou já tem esse programa. |
| programa desconhecido | A instalação não reconhece o programa do disco. |
| sem sistema | O nó não tem sistema, ou não tem console para o programa. Instale um sistema com SYSTEM antes. |
| não atende aos requisitos do programa | O nó não tem a velocidade do clock, a memória de vídeo ou o espaço livre no disco do sistema que o programa precisa. |
| antigo demais para o programa | O nó é de uma era mais antiga do que o programa precisa. |
| rack ausente | O rack não contém mais o nó. Inicie a instalação de novo. |
| mudou durante a gravação | O nó mudou enquanto a faixa dele o gravava. |

## Como o job termina

Uma faixa de sistema instala o sistema e faz o nó reiniciar nele. Uma faixa de programa instala o programa no nó. Quando o job termina, ele deixa uma linha de resumo.

Table: Linhas de resumo

| Linha | Quando aparece |
| --- | --- |
| `done: <what> on <nodes>` | A instalação terminou. |
| `cancelled: <what> on <nodes>` | Você cancelou o job. |
| `, <n> skipped` | Acrescentada ao fim da linha acima quando alguns nós foram ignorados. |

Cancelar interrompe o job depois dos nós que estão sendo gravados. Os nós que ainda estão na fila ficam como estavam. Os botões LIGAR TUDO e DESLIGAR TUDO, e o comando power, só ligam ou desligam a energia. Eles nunca iniciam uma instalação.

## Se algo der errado

> [!PROBLEM] A instalação não começa e diz "um job já está em execução".
> Espere o job terminar, ou cancele-o com CANCELAR JOB.

> [!PROBLEM] Todos os nós são ignorados.
> Leia o motivo entre colchetes depois de cada nó, e corrija esse motivo. A tabela acima os lista.

> [!PROBLEM] A instalação desaparece depois que o mundo é carregado de novo.
> Inicie a instalação de novo. Um job não sobrevive a um recarregamento.

> [!SEE] jsc:cluster_manager, jsc:cluster_command, jsc:cluster_management_computers, jsc:server_racks
