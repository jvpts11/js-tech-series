---
section: clusters
order: 60
---
# Subframes

Uma Subframe é um computador que ajuda o Mainframe da sua rede a carregar o trabalho. Quando o seu Mainframe está sozinho, ele faz todo o trabalho por conta própria, com a sua capacidade e as suas filas.

Uma Subframe empresta uma parte da sua capacidade. Por padrão, essa parte é 60%, definida pela opção Subframe share na configuração do mod. Ela também empresta as suas filas, que são as suas placas de vídeo, então uma Subframe sem placas de vídeo empresta só capacidade.

Uma Subframe só trabalha para um Mainframe que roda o mesmo motor que ela ([](jsc:engines)). Uma Subframe sem Mainframe não empresta nada.

Você faz uma Subframe de um Mainframe dando ao motor dele o papel de Subframe: marque Subframe na aba Services do Network Manager dele, ou digite iqlengine role subframe no prompt dele. A partir daí ele não conta para conflito de rede, não guarda índice próprio e empresta ao orquestrador da rede. Marque Orchestrator, ou digite iqlengine role orchestrator, para ele voltar a rodar a rede; se a rede já tem um orquestrador, isso é um conflito de rede.

> [!DEFINE] Orquestrador
> O Mainframe que roda a sua rede: o índice, as Operations e o motor dela.

Quando o orquestrador cai e ninguém roda a rede por 60 ticks, alguém assume a rede. Um Mainframe em failover vai primeiro, se houver; senão, uma Subframe que roda o mesmo motor, depois uma que roda outro, a de posição mais baixa primeiro. O motor da rede passa a ser o motor de quem assumiu, e as Operations que o orquestrador caído carregava se perdem. O log de eventos da rede diz quem parou, quem assumiu, que motor roda agora e quantas Operations se perderam.

A aba Services lista as Subframes com o que cada uma empresta, marca a que roda outro motor como reserva, e diz quem assume a rede se o orquestrador cair.

## Se algo der errado

> [!PROBLEM] A tela diz "Nenhuma Subframe nesta rede."
> A rede não tem Subframe, então o Mainframe faz todo o trabalho sozinho.

> [!PROBLEM] A tela diz que uma Subframe não vai receber trabalho até rodar o mesmo motor também.
> Faça-a rodar o mesmo motor do seu Mainframe ([](jsc:engines)).

> [!SEE] jsc:states_and_queues, jsc:engines
