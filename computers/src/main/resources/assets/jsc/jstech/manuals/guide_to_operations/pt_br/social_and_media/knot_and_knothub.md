---
section: social_and_media
order: 91
icon: jsc:cd_rom
---
# Knot e KnotHub

O Knot mantém o histórico de um arquivo em que os jogadores de uma rede ainda estão trabalhando. Você envia o arquivo ao repositório da rede, e o Knot o guarda como uma revisão: um estado salvo do arquivo, numerado r1, r2 e assim por diante.

> [!DEFINE] Revisão
> Um estado salvo de um arquivo no Knot, numerado r1, r2 e assim por diante.

> [!DEFINE] Push
> Enviar o estado atual do arquivo ao repositório como uma nova revisão.

> [!DEFINE] Pull
> Buscar a cópia da rede de um arquivo.

O Knot guarda o código-fonte que ainda está sendo trabalhado, antes de ele virar um pacote pronto ([](jsc:programs)). O Push lê o arquivo do disco deste computador, e não do texto digitado na janela, então a rede guarda o que a máquina realmente tem. O histórico corre no alto da janela, e as mudanças ficam embaixo.

Um arquivo guarda no máximo 64 revisões, e as mais antigas saem primeiro. Um repositório guarda no máximo 64 arquivos. Cada revisão é guardada inteira, e não como uma diferença em relação à anterior, então o computador que guarda o repositório precisa do espaço em disco para todas elas.

O KnotHub guarda o repositório da rede, e ele roda em um servidor em um rack da rede ([](jsc:server_racks)). O [Livro do Sigma](jsc:knot) explica o Knot para programas.

## Se algo der errado

> [!PROBLEM] A janela diz "Nenhum KnotHub nesta rede".
> Instale o KnotHub em um servidor de um rack desta rede, e deixe-o em execução.

> [!PROBLEM] O Push responde "Sem alteração em r", seguido de um número.
> O arquivo não mudou desde a sua última revisão. Mude o arquivo no seu disco e envie de novo.

> [!SEE] jsc:knot, jsc:server_racks, jsc:programs
