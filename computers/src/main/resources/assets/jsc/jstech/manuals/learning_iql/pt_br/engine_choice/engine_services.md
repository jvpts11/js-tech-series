---
section: engine_choice
order: 384
icon: jsc:mainframe
---
# Iniciando, parando e substituindo um motor

Você inicia, para e substitui motores na aba Serviços do Gerenciador de Rede. O cartão de cabeçalho da aba mostra o estado do motor em uma palavra: Rodando, Parado ou Substituindo. Os botões abaixo dele dependem dessa palavra.

## Os botões

Table: Os botões da aba Serviços

| Estado | Botões | O que você pode clicar |
| --- | --- | --- |
| Rodando | Configurar..., Parar, Trocar... | Parar e Trocar estão disponíveis; Configurar... fica sempre desativado. |
| Substituindo | Configurar..., Parar, Trocar... | Nada: os três ficam desativados. |
| Parado, ou nenhum escolhido | Iniciar... | Iniciar..., quando qualquer motor estiver instalado. |

Um botão faz o que diz ou nada. Se o servidor recusar um clique, nada acontece e a aba mostra de novo o estado real.

## Parando um motor

Parar não pede confirmação. Ele para o motor ativo, que continua instalado e escolhido. Até você iniciar um motor de novo, a rede não tem motor rodando.

A aba então diz "Nenhum Network Operations Engine rodando". O armazenamento continua, e as Operations em andamento seguem. As transferências ainda funcionam, mas as fabricações e as consultas IQL são recusadas até um motor rodar.

Um motor só roda enquanto o seu Mainframe estiver ligado. Quando o Mainframe é desligado, o motor para junto.

## Iniciando um motor

1. Clique em Iniciar... na aba Serviços.
2. Escolha o motor na lista intitulada "Iniciar um Network Operations Engine".
3. Clique em Iniciar para confirmar.

A lista começa com o primeiro motor parado selecionado. A nota dele diz "O engine inicia na hora e planeja todo pedido novo."

Se você escolher um motor instalado que não é o ativo, a rede troca para ele na hora. As etapas temporizadas de uma substituição não rodam nesse caso.

## Substituindo um motor

Uma substituição muda a rede do motor que está rodando para outro motor instalado. Ela leva tempo, e a aba mostra Substituindo até terminar. O novo motor inicia no fim.

> [!DEFINE] Substituição
> A troca temporizada de um motor instalado para outro, durante a qual nenhum motor planeja.

1. Clique em Trocar... na aba Serviços.
2. Escolha o motor na lista intitulada "Trocar o Network Operations Engine".
3. Leia o que a caixa de diálogo diz e depois clique em Trocar para confirmar.

Na caixa de diálogo, o botão Trocar fica desativado quando o motor escolhido é o que já está ativo.

Table: O que a caixa de diálogo Trocar diz

| Linha da caixa de diálogo | O que ela informa |
| --- | --- |
| Os novos pedidos vão para <new engine>. | Toda requisição a partir de agora é planejada pelo novo motor. |
| As <number> Operations em andamento terminam com os planos com que começaram. | O trabalho já em execução não é planejado de novo. |
| Scripts escritos com as instruções próprias do <old engine> podem parar de funcionar; o núcleo do IQL funciona em todos os motores. | Instruções que usam os extras do motor antigo podem falhar, mas o núcleo do IQL continua funcionando. |
| <Subframe> não receberá trabalho até rodar também o <new engine>. | Um Subframe só recebe trabalho quando roda o mesmo motor que o Mainframe. |
| Nenhum outro motor está instalado neste Mainframe. Instale um a partir do disco dele para trocar este. | Ainda não há para onde trocar. |

> [!DEFINE] Subframe
> Um segundo computador da rede que recebe trabalho, quando roda o mesmo motor que o Mainframe.

## Quanto tempo leva uma substituição

Uma substituição leva 40 ticks, mais um tick para cada quatro tipos de item que a rede armazena, até 2.400 ticks. Um tick é a unidade básica de tempo do jogo: 20 ticks formam um segundo.

> [!DEFINE] Tick
> Um vigésimo de segundo no jogo.

> [!DEFINE] Tipo de item
> Um tipo de item que a rede armazena, como um lingote de ferro.

Table: Quanto tempo leva uma substituição

| Tipos de item armazenados | Tempo |
| --- | --- |
| 0 | 40 ticks, 2 segundos |
| 400 | 140 ticks, 7 segundos |
| 9.440 ou mais | 2.400 ticks, 2 minutos |

A janela de progresso passa pelas etapas da substituição em ordem. O N na etapa Manter é o número de Operations que ainda estão rodando.

Table: As etapas de uma substituição

| Etapa na janela de progresso | Parcela do tempo |
| --- | --- |
| Parar de receber novos planos | 2% |
| Manter N Operations em andamento nos seus planos | 2% |
| Parar <old engine>, ou Nada a parar quando a rede não tinha nenhum | 2% |
| Iniciar <new engine> | 2% |
| Descobrir armazenamento e máquinas | 22% |
| Construir índices | 60% |
| Publicar capacidades | 10% |

Construir índices é a etapa que mais leva tempo, porque a rede indexa cada tipo de item que armazena. Depois a janela diz Pronto.

Enquanto uma substituição roda, nenhum motor planeja. As Operations já em andamento continuam, mas uma requisição nova que precise de planejamento é recusada até o novo motor estar pronto. Quando o tempo acaba, o novo motor vira o motor ativo e inicia.

## Instalando um motor

Um motor vem do seu disco, como todo programa. Você coloca o disco em uma unidade ligada ao Mainframe e depois instala o programa como instala qualquer outro ([](jsc:programs)).

A instalação coloca o motor na lista do Mainframe, na versão da era do Mainframe. Se nenhum motor estiver escolhido ainda, o novo vira o motor ativo e inicia na hora.

Uma instalação é recusada quando o motor já está instalado, ou quando ele não foi feito para a era do Mainframe.

> [!WARNING] Formatar o disco do Mainframe remove todos os motores instalados nele.

Remover o motor em execução deixa a rede sem motor, como acontece com uma parada.

## O comando iqlengine

O Midsoft IQL Server também tem um comando próprio, digitado em um prompt: iqlengine. Ele aceita um de quatro verbos.

Table: Os verbos do iqlengine

| Verbo | O que faz |
| --- | --- |
| `install` | Coloca o Midsoft IQL Server no Mainframe. |
| `start` | Inicia o Midsoft IQL Server e troca a rede para ele na hora, se outro motor estiver ativo. |
| `stop` | Para o Midsoft IQL Server, mas só enquanto ele for o motor ativo e estiver rodando. |
| `status` | Mostra se o Midsoft IQL Server não está instalado, rodando ou parado. |

Em uma rede que roda o NextgreIQL, iqlengine stop responde que o Midsoft IQL Server já está parado, porque o Midsoft não é o motor ativo.

## Se algo der errado

> [!PROBLEM] Você clica em Trocar e nada acontece.
> Trocar fica desativado enquanto uma substituição está rodando, quando o motor escolhido já está rodando, ou quando o Mainframe está desligado. Leia primeiro a palavra de estado.

> [!PROBLEM] A caixa de diálogo Trocar diz que nenhum outro motor está instalado.
> Instale outro motor a partir do disco dele, em uma unidade ligada ao Mainframe.

> [!PROBLEM] A aba Serviços diz que nenhum motor está instalado.
> Coloque o disco do motor em uma unidade ligada ao Mainframe e depois instale-o.

> [!PROBLEM] Uma substituição leva mais tempo do que você esperava.
> O tempo cresce com os tipos de item que a rede armazena, até 2 minutos.

> [!SEE] jsc:engine_problems, jsc:midsoft_iql_server
