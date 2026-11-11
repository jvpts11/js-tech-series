---
section: network_operation
order: 107
icon: jsc:mainframe
---
# Serviços no Mainframe

O Mainframe carrega três tipos de software: um motor que decide o trabalho da rede, o Motor de Automação que executa os trabalhos salvos, e o Mirror ([](jsc:mirror)). Um Mainframe novo sai de fábrica com o Midsoft IQL Server instalado e em execução, na versão da sua era.

Os serviços de um Mainframe estão ligados ou desligados. Eles só funcionam enquanto o Mainframe estiver energizado, e são removidos quando o seu disco é formatado.

Table: Os motores do Mainframe

| Motor | O que faz |
| --- | --- |
| Midsoft IQL Server | Planeja o trabalho da rede e responde em IQL. O ISMS é o seu estúdio. ([](jsc:engines)) |
| NextgreIQL | Mostra como planeja o trabalho da rede, e deixa você participar do planejamento. |
| Prophet YourIQL | Você diz a ele qual estado a rede deve manter, e ele descobre como chegar lá e continuar nele. |

Apenas um motor planeja o trabalho da rede por vez. Um motor que você instala torna-se o ativo quando a rede não tem nenhum. Ativar um motor instalado faz dele o que planeja, e o trabalho que o motor anterior havia planejado continua, pois uma Operação pertence à rede assim que é criada.

Substituir um motor leva algum tempo, e a rede fica sem motor até a substituição terminar. A substituição passa por estes passos, em ordem:

1. Pare de aceitar novos planos.
2. Pare o motor antigo.
3. Inicie o novo motor.
4. Descubra o armazenamento e as máquinas.
5. Construa os índices.
6. Publique as capacidades.
7. Espere até a tela dizer Ready.

O Motor de Automação guarda os trabalhos da rede e os dispara na hora certa. Você escreve e supervisiona esses trabalhos no Gerenciador de Automação ([](jsc:automation_engine)).

## Se algo der errado

> [!PROBLEM] A aba Serviços diz que nenhum Network Operations Engine está em execução.
> Instale um motor, ou inicie o que já está instalado.

## Se algo der errado

> [!PROBLEM] Uma substituição não inicia.
> Outra substituição está em andamento, o motor não está instalado, o Mainframe está desligado, ou o motor já é o que está em execução.
