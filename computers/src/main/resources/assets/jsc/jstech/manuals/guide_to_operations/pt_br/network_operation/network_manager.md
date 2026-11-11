---
section: network_operation
order: 104
icon: jsc:monitor
---
# O Gerenciador de Rede

O Gerenciador de Rede é a sala de controle da rede. É um programa de área de trabalho, e só o Mainframe o roda: você o abre no Mainframe, e nenhum outro computador consegue.

Suas sete abas mostram a rede por sete lados. Cada uma é uma forma de perguntar à rede o que ela guarda, o que está fazendo e quão bem está fazendo.

Table: As sete abas

| Aba | O que mostra |
| --- | --- |
| Dispositivos | Todo nó que a rede conhece, com o seu tipo, se está online ou offline e há quanto tempo está ligado. |
| Processos | As Operações em andamento, uma linha cada. Um clique abre uma, com os botões Fechar e Cancelar. |
| Hardware | A capacidade de orquestração, as filas paralelas, o buffer de RAM e o armazenamento da rede, e quantos computadores de cada tipo existem. |
| Mapa | Os nós e as suas linhas, desenhados como um mapa. Arraste um nó para movê-lo. |
| Log | Duas vistas no topo. Operations: as Operations registradas, filtradas por tipo, por quem pediu e por período. Eventos: o que aconteceu com cada máquina da rede, das mais recentes para as mais antigas. |
| Estatísticas | Quantas Operations de cada tipo terminaram na última hora, no último dia ou nos últimos 7 dias, quanto tempo esperaram e correram, o pico do dia e a capacidade usada. |
| Serviços | Os motores e serviços do Mainframe, e os passos para trocar um motor ([](jsc:mainframe_services)). |

No Mapa, arraste um nó com o mouse para movê-lo, arraste com o botão do meio para deslocar a visão, e gire a roda para dar zoom. A legenda lista as linhas em uso: acesso, backbone, longa distância e alta computação.

A aba Processos lista o trabalho que a rede está fazendo agora. Cada Operação tem a sua própria prioridade e o seu próprio registro de quanto já andou ([](jsc:what_operations_are)).

## Os eventos da rede

A vista Eventos da aba Log é o log de eventos da rede, à parte das suas Operações. Cada linha tem a hora, um nível, a máquina e o que aconteceu. INFO é algo que aconteceu, como uma máquina ligada ou desligada. WARN é algo para olhar, como um evento do mundo previsto ou em curso. ERROR é algo que deu errado, como dois Mainframes numa mesma rede.

Três listas acima dela filtram o que ela mostra: o nível, a máquina (ou a própria rede) e até quando atrás, da última hora a tudo o que ela guarda. Imprimir manda o que ela mostra para a impressora do computador. Exportar grava tudo num arquivo de texto num disco do Mainframe, um evento por linha.

A rede guarda os seus últimos sete dias, e no máximo 2.000 eventos. Os mais antigos dão lugar aos novos.

## As Operations e as estatísticas

A vista Operations da aba Log lista as Operations que o Mainframe registrou, com a hora em que cada uma terminou, o seu id, o que era, o seu estado e quem a pediu. Três listas acima dela filtram o que mostra: o tipo (com CRAFT, é o histórico das suas fabricações), quem pediu e até quando no passado. Imprimir manda o que ela mostra para a impressora do computador.

A aba Stats cobre o período que você escolhe: a última hora, o último dia ou os últimos 7 dias do jogo. Cada tipo de Operation tem a sua contagem por hora, quanto tempo esperou e correu em média e quantas falharam. Abaixo da tabela ficam o máximo de Operations que correram ao mesmo tempo hoje, com a hora do dia, e um gráfico da capacidade de orquestração usada nas últimas 2 horas. A aba Dispositivos dá o tempo ligado de cada máquina em dias e horas do jogo.

> [!DEFINE] Nó
> Qualquer bloco que a rede conhece: um Mainframe, um servidor, um Subframe ou um computador.

> [!DEFINE] Serviço
> Um programa instalado em um servidor, ou no Mainframe, que dá à máquina a sua função. Você escolhe os serviços de cada máquina.

## Se algo der errado

> [!PROBLEM] O Gerenciador de Rede não está nos seus programas.
> Ele só roda no Mainframe. Abra o Mainframe, não um Computador Pessoal.

## Se algo der errado

> [!PROBLEM] Um nó aparece como offline na aba Dispositivos.
> Ligue o nó e verifique se o trecho de cabos dele chega ao Mainframe ([](jsc:data_cables)).
