---
section: prompt_programs_and_network
order: 246
---
# Cuidando da rede pelo prompt

Alguns verbos atuam sobre a própria rede, então o prompt só os oferece no computador que guarda essa parte da rede. Se você digitar um deles em outro lugar, o prompt diz que não conhece o comando.

Table: Onde cada verbo é oferecido

| Verbo | Disponível em |
| --- | --- |
| `iqlengine` | O Mainframe da rede |
| `services` | O Mainframe da rede |
| `analyze` | O Mainframe da rede |
| `reindex` | O Mainframe da rede |
| `vacuum` | O Mainframe da rede |
| `mirror` | O Mainframe da rede |
| `cluster` | Um Computador de Gerenciamento de Cluster |
| `gateway` | Um computador com portas, para os seus Network Gateways |
| `borderlink` | Um Computador Gateway WAN com o Nouvell BorderLink |
| `msgsvc` | Um servidor que roda o Serviço de Mensagens |

Os verbos do Mainframe só existem enquanto ele estiver em uma rede. Um Mainframe que roda MC-DOS ou Frames os oferece no Prompt de Comando dele.

## O motor e os serviços

iqlengine inicia e para o Midsoft IQL Server, o motor que planeja o trabalho da rede ([](jsc:midsoft_iql_server)). Digite iqlengine start, iqlengine stop ou iqlengine status. No prompt do próprio Mainframe, iqlengine role subframe ou iqlengine role orchestrator dá esse papel ao seu motor ([](jsc:subframes)). services lista os serviços da rede, cada um com seu estado. O prompt oferece os dois somente enquanto o Midsoft IQL Server estiver instalado no Mainframe.

```command
id: jsc:command/mc_dos/iqlengine
```

```command
id: jsc:command/mc_dos/services
```

## O índice de armazenamento

O Mainframe guarda o índice de armazenamento: a lista de quais discos guardam quais itens, para que a rede encontre um item sem ler todos os discos. Três verbos cuidam dele.

> [!DEFINE] Índice de armazenamento
> A lista do Mainframe sobre quais discos da rede guardam quais itens.

> [!DEFINE] Entrada fantasma
> Um item que o índice de armazenamento ainda lista, embora nenhum disco o guarde mais.

Table: Cuidando do índice

| Verbo | O que faz | O que diz |
| --- | --- | --- |
| `analyze` | Confere o índice com os discos. | `ANALYZE complete - 12 types reconciled` |
| `reindex` | Lê todos os discos de novo e reconstrói o índice. O novo índice assume um ou dois ticks depois. | `REINDEX started - rebuilding the catalog from disks` |
| `vacuum` | Limpa as entradas fantasmas. | `VACUUM freed 3 ghost entries` |

Quando você tira uma unidade de uma baia em funcionamento que não tem Monitor de Integridade, o índice deixa de confiar nessa baia até você digitar reindex ([](jsc:network_services)). Aprendendo IQL explica os mesmos três trabalhos como instruções ([](jsc:maintenance_analyze_vacuum_reindex)).

```command
id: jsc:command/mc_dos/analyze
```

```command
id: jsc:command/mc_dos/reindex
```

```command
id: jsc:command/mc_dos/vacuum
```

## O Mirror

mirror install coloca o Mirror no Mainframe: o serviço do qual o pckmgr busca os pacotes ([](jsc:mirror)). mirror status diz se ele está servindo. Ele responde servindo, instalado (Mainframe desligado) ou não instalado.

```command
id: jsc:command/mc_dos/mirror
```

## Clusters e gateways

cluster faz no prompt o que o Gerenciador de Cluster faz na janela dele ([](jsc:cluster_management_computers)). Ele lista os supercomputadores e datacenters da rede, mostra os nós de um deles, liga ou desliga, e instala um sistema ou um programa neles.

gateway faz para os Network Gateways das portas deste computador o que o Gerenciador de Gateway faz, a ponte para o ComputerCraft ([](jsc:network_gateway)). Ele os lista, mostra o status, as permissões, os computadores e o log de um deles, e altera as configurações dele.

borderlink faz num Computador Gateway WAN o que o Nouvell BorderLink faz na sua janela ([](jsc:wan_gateways)). Lista as quatro portas, mostra uma delas, define o que a outra ponta pode fazer aqui, e mostra o log do gateway: borderlink port 2 relay on.

```command
id: jsc:command/mc_dos/cluster
```

```command
id: jsc:command/mc_dos/gateway
```

```command
id: jsc:command/mc_dos/borderlink
```

## As conversas do Serviço de Mensagens

msgsvc, no prompt do servidor que roda o Serviço de Mensagens, cuida das conversas que ele guarda ([](jsc:messenger)). msgsvc list as numera, msgsvc delete 2 apaga a segunda, msgsvc delete --all apaga todas, e msgsvc status diz o que o serviço guarda e quanto pesa. Uma conversa apagada sai de todos os Messengers na hora.

```command
id: jsc:command/mc_dos/msgsvc
```

## Se algo der errado

> [!PROBLEM] O prompt diz que não conhece o comando.
> Digite no prompt do computador que a tabela indica para ele.

> [!PROBLEM] iqlengine diz que o Midsoft IQL Server já está rodando.
> Ele já está servindo. Digite iqlengine status para verificar.

> [!PROBLEM] iqlengine diz que o Midsoft IQL Server já está parado.
> Digite iqlengine start para iniciá-lo.

> [!PROBLEM] mirror diz que o Mirror já está instalado.
> Digite mirror status para ver se ele está servindo.

> [!SEE] jsc:mainframes, jsc:engine_services, jsc:mirror
