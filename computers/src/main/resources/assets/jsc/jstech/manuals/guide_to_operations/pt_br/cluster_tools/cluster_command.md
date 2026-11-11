---
section: cluster_tools
order: 156
icon: jsc:cluster_management_computer
---
# O comando cluster

O comando cluster faz o mesmo trabalho que o Gerenciador de Cluster, para quando você preferir o prompt. Digite cluster e uma das palavras abaixo no prompt de um sistema em um Computador de Gerenciamento de Cluster ([](jsc:cluster_management_computers)).

> [!DEFINE] R:U
> O endereço de um nó: o número do rack na ordem do cluster, contado a partir de 1, e depois a linha de unidades, também contada a partir de 1. R2:U5 é a linha de unidades 5 do segundo rack.

O comando precisa de uma placa de interface de cluster no computador. Sem ela, a resposta é "nenhuma placa de interface de cluster instalada". A linha de uso diz: list | nodes <name> | power <name> [R:U] on|off | install <name> system|program | status | cancel.

Table: Os subcomandos

| Comando | O que faz |
| --- | --- |
| `list` | Lista os supercomputadores e as seções de datacenter que o cartão alcança; ls ou nenhuma palavra fazem o mesmo. |
| `nodes <name>` | Lista os nós de um cluster, com o rack, a unidade, o sistema e a energia de cada um. |
| `power <name> on\|off` | Liga ou desliga todos os nós do cluster, sem instalar nada. |
| `power <name> R:U on\|off` | Liga ou desliga um nó. |
| `install <name> system\|program` | Inicia uma instalação em massa de um sistema ou de um programa no cluster ([](jsc:cluster_installs)). |
| `status` | Mostra a instalação em andamento, ou a última. |
| `cancel` | Interrompe a instalação em andamento assim que os nós que estão sendo gravados agora terminarem. |

Os nomes são comparados sem diferenciar maiúsculas de minúsculas. O comando install responde com o job que iniciou, quantos nós ele grava de uma vez, e um lembrete para acompanhá-lo com cluster status. A linha de status lista o job, depois quantos nós estão concluídos, sendo gravados, na fila e ignorados.

Table: As colunas da lista

| Coluna | O que mostra |
| --- | --- |
| `KIND` | Supercomputador ou datacenter. |
| `NAME` | O nome que o Gerenciador de Cluster mostra para o cluster. |
| `STATE` | Online ou offline para um supercomputador; online ou vazio para uma seção. |
| `LOAD` | As fabricações em uso nas fabricações paralelas do supercomputador, ou o número de servidores de uma seção. |

Um cluster que a placa não consegue alcançar termina a sua linha com "(fora do alcance desta placa)".

Table: As colunas dos nós

| Coluna | O que mostra |
| --- | --- |
| `RACK` | O número do rack, contado a partir de 1 na ordem do cluster. |
| `U` | A linha de unidades, contada a partir de 1. |
| `NODE` | O nome do nó. |
| `SYSTEM` | O sistema no nó, ou nenhum. |
| `POWER` | on ou off. |

## Se algo der errado

> [!PROBLEM] Diz "este comando só existe em um Computador de Gerenciamento de Cluster".
> Execute o comando em um Computador de Gerenciamento de Cluster, não em outro computador.

> [!PROBLEM] Diz "nenhuma placa de interface de cluster instalada".
> Instale uma placa de interface de cluster no computador.

> [!PROBLEM] Diz "nenhum cluster chamado" seguido do nome que você digitou.
> Use um nome da lista que o comando mostra.

> [!PROBLEM] Diz "nenhum nó em" seguido de um endereço.
> Confira o endereço R:U na listagem de nós.

> [!PROBLEM] Mostra como usar power.
> Adicione o endereço R:U, depois on ou off.

> [!PROBLEM] Diz "já ligado" ou "já desligado".
> O nó já está nesse estado, ou a placa não o alcança.

> [!PROBLEM] Diz "nenhum job em execução".
> Inicie um job com install primeiro, ou leia o status de novo depois que ele começar.

> [!SEE] jsc:cluster_manager, jsc:cluster_installs
