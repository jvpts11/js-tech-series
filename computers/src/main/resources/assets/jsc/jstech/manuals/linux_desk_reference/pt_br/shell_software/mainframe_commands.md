---
section: shell_software
order: 260
---
# Cuidando da rede pelo prompt

Alguns comandos atuam sobre a própria rede, então o prompt os oferece apenas no computador que mantém essa parte da rede. Digite um deles em outro lugar, e o prompt diz que não conhece o comando.

Table: Onde cada comando é oferecido

| Comando | Oferecido em |
| --- | --- |
| `iqlengine` | O Mainframe da rede |
| `services` | O Mainframe da rede |
| `analyze` | O Mainframe da rede |
| `reindex` | O Mainframe da rede |
| `vacuum` | O Mainframe da rede |
| `mirror` | O Mainframe da rede |
| `cluster` | Um Computador de Gerenciamento de Cluster |
| `gateway` | Um computador com portas, para os seus Gateways de Rede |
| `borderlink` | Um Computador Gateway WAN com o Nouvell BorderLink |
| `msgsvc` | Um servidor que roda o Serviço de Mensagens |

O Mainframe é o computador que coordena uma rede ([](jsc:mainframes)). Seus comandos existem apenas enquanto ele estiver em uma rede.

## O motor e os serviços

iqlengine inicia e para o Midsoft IQL Server, o motor que planeja o trabalho da rede ([](jsc:midsoft_iql_server)). Digite iqlengine start, iqlengine stop ou iqlengine status. No prompt do próprio Mainframe, iqlengine role subframe ou iqlengine role orchestrator dá esse papel ao seu motor ([](jsc:subframes)). services lista os serviços da rede, cada um com seu estado. O prompt oferece os dois somente enquanto o Midsoft IQL Server estiver instalado no Mainframe.

```command
id: jsc:command/linux/iqlengine
```

```command
id: jsc:command/linux/services
```

## O índice de armazenamento

O Mainframe guarda o índice de armazenamento: sua lista de quais discos guardam quais itens, para que a rede encontre um item sem ler todos os discos. Três comandos cuidam dele.

> [!DEFINE] Índice de armazenamento
> A lista do Mainframe de quais discos da rede guardam quais itens.

> [!DEFINE] Entrada fantasma
> Um item que o índice de armazenamento ainda lista, embora nenhum disco o guarde mais.

Table: Cuidando do índice

| Comando | O que faz | O que diz |
| --- | --- | --- |
| `analyze` | Confere o índice com os discos. | `ANALYZE complete - 12 types reconciled` |
| `reindex` | Lê todos os discos de novo e reconstrói o índice. O novo índice assume um ou dois ticks depois. | `REINDEX started - rebuilding the catalog from disks` |
| `vacuum` | Limpa as entradas fantasma. | `VACUUM freed 3 ghost entries` |

Quando você tira uma unidade de uma baia em execução que não tem Monitor de Integridade, o índice deixa de confiar nessa baia até que você digite reindex ([](jsc:network_services)). Aprendendo IQL explica os mesmos três trabalhos como instruções ([](jsc:maintenance_analyze_vacuum_reindex)).

```command
id: jsc:command/linux/analyze
```

```command
id: jsc:command/linux/reindex
```

```command
id: jsc:command/linux/vacuum
```

## O Mirror

mirror install coloca o Mirror no Mainframe: o serviço de onde o seu gerenciador de pacotes busca pacotes ([](jsc:mirror)). mirror status diz se ele está servindo. Ele responde serving, installed (Mainframe off) ou not installed.

```command
id: jsc:command/linux/mirror
```

## Clusters e gateways

cluster faz no prompt o que o Gerenciador de Cluster faz na sua janela ([](jsc:cluster_management_computers)). Ele lista os supercomputadores e datacenters da rede, mostra os nós de um deles, liga ou desliga, e instala um sistema ou um programa neles.

gateway faz o que o Gerenciador de Gateway faz pelos Gateways de Rede nas portas deste computador, a ponte para o ComputerCraft ([](jsc:network_gateway)). Ele os lista, mostra o status, as permissões, os computadores e o log de um deles, e altera suas configurações.

borderlink faz num Computador Gateway WAN o que o Nouvell BorderLink faz na sua janela ([](jsc:wan_gateways)). Lista as quatro portas, mostra uma delas, define o que a outra ponta pode fazer aqui, e mostra o log do gateway: borderlink port 2 relay on.

```command
id: jsc:command/linux/cluster
```

```command
id: jsc:command/linux/gateway
```

```command
id: jsc:command/linux/borderlink
```

## As conversas do Serviço de Mensagens

msgsvc, no prompt do servidor que roda o Serviço de Mensagens, cuida das conversas que ele guarda ([](jsc:messenger)). msgsvc list as numera, msgsvc delete 2 apaga a segunda, msgsvc delete --all apaga todas, e msgsvc status diz o que o serviço guarda e quanto pesa. Uma conversa apagada sai de todos os Messengers na hora.

```command
id: jsc:command/linux/msgsvc
```

## Se algo der errado

> [!PROBLEM] O prompt diz que não conhece o comando.
> Digite no prompt do computador que a tabela indica para ele.

> [!PROBLEM] iqlengine informa que o Midsoft IQL Server já está em execução.
> Ele já está servindo. Digite iqlengine status para verificar.

> [!PROBLEM] iqlengine informa que o Midsoft IQL Server já está parado.
> Digite iqlengine start para iniciá-lo.

> [!PROBLEM] mirror informa que o Mirror já está instalado.
> Digite mirror status para ver se ele está servindo.

> [!SEE] jsc:mainframes, jsc:engine_services, jsc:mirror
