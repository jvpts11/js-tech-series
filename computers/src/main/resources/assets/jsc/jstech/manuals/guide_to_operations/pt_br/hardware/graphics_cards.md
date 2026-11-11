---
section: hardware
order: 21
icon: jsc:gpu_vertex_gtx_970
covers_family: jsc:graphics_cards
---
# Placas de vídeo

A placa de vídeo desenha a imagem: ela acende os monitores e desenha as janelas neles. Um computador sem placa de vídeo não tem o que mostrar, a menos que seu processador tenha gráficos integrados.

A placa tem uma memória própria, a memória de vídeo (VRAM). Cada monitor usa parte dela, e um monitor maior usa mais, e cada janela que desenha uma imagem própria também usa um pouco.

> [!DEFINE] Memória de vídeo (VRAM)
> A memória própria da placa de vídeo. Os monitores e as janelas que desenham imagens a usam.

Um processador com gráficos integrados não tem memória de vídeo própria. Ele usa um quarto da RAM do computador para a sua imagem, até 1.792 MB. Um computador com placa de vídeo própria não divide sua RAM dessa forma.

Em um Mainframe, cada placa acrescenta uma fila paralela: uma linha de trabalho que o Mainframe executa ao mesmo tempo que as outras. Um Mainframe com três placas executa quatro filas, uma de seus processadores e uma para cada placa ([Estados, prioridades e filas](jsc:states_and_queues)).

A fila de uma placa nunca roda mais rápido que os processadores do Mainframe. A placa cuja fila roda mais devagar que isso aparece em âmbar.

As placas vão nos slots de expansão da placa-mãe ([Placas de expansão](jsc:expansion_cards)). Uma placa em um slot mais antigo roda mais devagar do que sua dica de ferramenta diz. A tabela mostra quanto da sua velocidade ela mantém.

Table: Uma placa em um slot de expansão mais antigo

| Slot em comparação com a placa | Velocidade que a placa mantém |
| --- | --- |
| A mesma geração ou mais nova | Tudo |
| Uma geração mais antiga | Metade |
| Duas gerações mais antigas | Um quarto |
| Três ou mais gerações mais antigas | Um oitavo, que é o mínimo que ela mantém |

Essa mesma fração reduz a memória de vídeo que o computador conta para a placa, e a fila da placa.

Há 76 placas, da VGA-256 à Envya Vertex RTX 5090. Passe o cursor sobre uma placa e a dica de ferramenta mostra seus núcleos, sua memória de vídeo e sua potência.

## Memória de vídeo para os monitores

Um monitor é contado em blocos da sua tela grande. Seu custo de memória de vídeo é o número de blocos vezes o custo de um bloco na sua era.

Table: Memória de vídeo para um bloco de monitor

| Era | Memória de vídeo para um bloco |
| --- | --- |
| Vintage | 64 KB (monocromático) ou 256 KB (16 cores) |
| Legacy | 4 MB |
| Transition | 16 MB |
| Standard | 64 MB |
| Advanced | 256 MB |
| Exa | 1 GB |
| Singularity | 4 GB |

Uma janela que desenha uma imagem custa um quarto de um bloco de cores da sua era, ou um bloco inteiro enquanto estiver maximizada. Apenas programas gráficos usam essa memória, e uma janela minimizada não usa nenhuma.

## Se algo der errado

> [!PROBLEM] O monitor fica apagado enquanto o computador está desligado.
> Ligue o computador. Os monitores só acendem enquanto o computador estiver funcionando.

> [!PROBLEM] O monitor continua apagado.
> Não há placa de vídeo no computador, e o processador não tem gráficos. Coloque uma placa, ou use um processador com gráficos integrados.

> [!PROBLEM] O monitor diz "Memória de vídeo insuficiente: este monitor precisa de <a>, e há <b> livres".
> Use uma placa com mais memória de vídeo, ou menos monitores, ou monitores menores.

> [!PROBLEM] Uma placa faz menos do que sua dica de ferramenta diz.
> Ela está em um slot de expansão mais antigo. Mova-a para um slot mais novo.

> [!SEE] jsc:monitors, jsc:expansion_cards, jsc:states_and_queues
