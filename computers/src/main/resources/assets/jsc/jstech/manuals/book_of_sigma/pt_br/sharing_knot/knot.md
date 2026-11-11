---
section: sharing_knot
order: 499
---
# Knot

O Knot é um programa de área de trabalho que guarda o código-fonte de um arquivo que você ainda está escrevendo. Cada vez que você envia o arquivo, o Knot salva uma cópia como revisão no repositório da rede. Uma revisão guarda um estado salvo do arquivo, com o seu autor, a sua mensagem e a hora em que foi salvo.

Em seguida, o Knot mostra quem mudou o quê, linha por linha. Ele só funciona onde um KnotHub está em execução na rede ([](jsc:knothub_service)). O Knot não é um gerenciador de pacotes: um pacote é um programa pronto entregue a outros, e o Knot guarda o código-fonte antes disso.

## A janela

Table: Parte da janela

| Parte | O que faz |
| --- | --- |
| Push, Pull e Refresh | Push salva uma nova revisão, Pull grava de volta no disco a revisão escolhida, e Refresh pede ao KnotHub o estado novamente. |
| Arquivo | O caminho completo do arquivo que um Push lê. Um Pull não o usa. |
| Caixa de mensagem | A nota que um envio carrega, de até 128 caracteres. |
| Histórico | As revisões mais recentes, cada uma com o seu número, autor e mensagem. Mostra 4 linhas e rola. |
| Comparação | As mudanças da revisão escolhida em relação à anterior: um sinal de mais para uma linha acrescentada, um sinal de menos para uma linha removida, e linhas simples que não mudaram. |
| Barra de status | À esquerda, a última resposta, ou o nome de host do KnotHub. À direita, o número de revisões e o tamanho do repositório. |

Clique em uma linha do histórico para escolher aquela revisão. O campo Arquivo então mostra o arquivo da revisão, e a comparação mostra as suas mudanças. A revisão mais recente é escolhida quando a janela abre.

## Enviando um arquivo

1. Digite o caminho completo do arquivo no campo Arquivo.
2. Digite uma mensagem na caixa de mensagem, se quiser.
3. Pressione Push.
4. Leia a resposta na barra de status, como "Enviado plant.sgs".

O Knot lê o arquivo do disco do sistema do computador em que você está, e não o texto da janela. Um envio com a mensagem vazia é salvo como "sem mensagem". Um envio com o mesmo texto da revisão mais recente daquele arquivo não muda nada, então o Knot o recusa.

## Puxando uma revisão

1. Clique na revisão no histórico.
2. Pressione Pull.
3. Leia a resposta, como "Gravado r3 em plant.sgs".

Um Pull grava a revisão de volta no arquivo a que ela pertence, no disco do sistema. Ele não grava no caminho do campo Arquivo.

Table: Respostas na barra de status

| Resposta | Quando você vê |
| --- | --- |
| `No Computer` | A janela não está em um computador. |
| `No System Disk` | O computador não tem disco do sistema. |
| `No <file> on this machine` | O Push não encontra o arquivo no disco do sistema. |
| `Nothing changed since the last revision` | O arquivo tem o mesmo texto da sua revisão mais recente, ou é um arquivo novo e o repositório já tem 64 arquivos. |
| `Pushed <file>` | O Push salvou uma revisão. |
| `No such revision` | A revisão escolhida sumiu do histórico, ou o KnotHub foi reiniciado. |
| `Not enough free space` | O Pull não conseguiu gravar: o disco está cheio. |
| `Could not write <file>` | O Pull falhou por outro motivo. |
| `Wrote r<number> to <file>` | O Pull gravou a revisão. |

## Revisões e a comparação

O número de uma revisão conta de forma crescente em todo o repositório, não arquivo por arquivo, e um número nunca é reaproveitado enquanto o repositório existir. A primeira revisão de um arquivo é comparada com um texto vazio, então cada linha dela aparece como acrescentada.

A comparação roda linha por linha, na ordem de leitura. Uma linha que as duas versões têm em comum é mostrada como está. Uma linha só na versão mais antiga é removida, e uma linha só na versão mais nova é acrescentada. Ela não procura o menor conjunto de mudanças, então pode mostrar mais linhas do que isso.

Table: Limites

| Limite | Valor |
| --- | --- |
| Revisões guardadas por arquivo | 64 revisões |
| Arquivos em um repositório | 64 arquivos |
| Linhas de comparação mostradas | 128 linhas |
| Caminho no campo Arquivo | 160 caracteres |
| Mensagem | 128 caracteres |

> [!WARNING] Quando um arquivo já tem 64 revisões, o próximo envio dele descarta a mais antiga, e nada avisa você disso.

> [!DEFINE] Knot
> O programa de área de trabalho que salva as revisões de um arquivo e mostra o que mudou entre elas.

> [!DEFINE] Repositório
> O armazenamento de todos os arquivos e de todas as revisões que um KnotHub guarda.

> [!DEFINE] Revisão
> Um estado salvo de um arquivo no repositório, com o seu autor, a sua mensagem e a hora em que foi salvo.

> [!DEFINE] Diff
> A lista das linhas que mudaram entre uma revisão e a anterior.

## Se algo der errado

> [!PROBLEM] O Push diz "Sem Disco do Sistema".
> Coloque um disco do sistema no computador. O Knot lê os arquivos dele.

> [!PROBLEM] O Push diz "Nenhum <file> nesta máquina".
> Confira o caminho no campo Arquivo. O Knot lê o arquivo do disco do sistema, pelo caminho completo.

> [!PROBLEM] O Push diz "Nada mudou desde a última revisão".
> O arquivo tem o mesmo texto da sua revisão mais recente, então nada novo foi salvo. Altere o arquivo e faça o push de novo. Um arquivo novo também é recusado quando o repositório já tem 64 arquivos, e a janela mostra a mesma mensagem nesse caso.

> [!PROBLEM] O Pull diz "Espaço livre insuficiente".
> O disco do sistema está cheio. Libere algum espaço nele e puxe de novo.

> [!PROBLEM] O Pull diz "Nenhuma revisão desse tipo".
> A revisão foi descartada, ou o KnotHub foi reiniciado. Escolha outra revisão no histórico.

> [!PROBLEM] A janela diz "Nenhum KnotHub nesta rede".
> Nenhum Servidor com KnotHub está em execução. Veja a entrada do KnotHub ([](jsc:knothub_service)).

> [!SEE] jsc:knothub_service, jsc:sigma_projects
