---
section: peripherals
order: 73
icon: jsc:printer
covers_family: jsc:printers
---
# Impressoras

Uma impressora coloca um documento no papel. Cada era tem sua própria impressora, e cada uma desenha a imagem no papel do seu jeito, como as impressoras reais daquela época faziam. Conecte uma impressora a um computador, coloque papel comum na bandeja e imprima a partir de um programa.

Uma imagem também pode sair no papel. Uma folha com imagem pode ser pendurada em um quadro de item, onde preenche a face do quadro. Clique com o botão direito em uma folha de Papel Impresso para lê-la.

Table: As impressoras

| Impressora | Era | Tinta | Cor |
| --- | --- | --- | --- |
| Epsilon FX-80 | Vintage | Matriz de pontos | Preto e branco |
| Pakard DeskJot 940 | Legacy | Jato de tinta, em pontos grossos | Colorida |
| Pakard FotoSmart C4280 | Transition | Jato de tinta, em pontos finos | Colorida |
| Pakard LaserJot 1102 | Standard | Laser, com toner | Preto e branco |
| Epsilon EcoTonk ET-2720 | Advanced | Tanque de tinta, a imagem como ela é | Colorida |

A dica da impressora e a caixa de diálogo de impressão informam quais impressoras imprimem em preto e branco: "Imprime em preto e branco" ou "Imprime em cores". A caixa de diálogo também mostra a palavra Mono ou Color ao lado do nome da impressora.

A FX-80 desenha em pontos pretos, uma passagem a cada oito linhas. A DeskJot deixa faixas de cor, e a FotoSmart imprime as mesmas tintas em pontos mais finos. A LaserJot usa toner, e a EcoTonk imprime a imagem exatamente como ela é.

## Quanto tempo uma página leva

Table: Ticks para uma página

| Impressora | Ticks por página |
| --- | --- |
| Epsilon FX-80 | 144 ticks |
| Pakard DeskJot 940 | 120 ticks |
| Pakard FotoSmart C4280 | 100 ticks |
| Pakard LaserJot 1102 | 70 ticks |
| Epsilon EcoTonk ET-2720 | 110 ticks |

Um tick é um vigésimo de segundo, então 20 ticks formam um segundo. Quanto menos ticks uma página leva, mais rápida é a impressora.

## Papel e a saída

A bandeja aceita papel comum, e mapas vazios para copiar um mapa. Clique com o botão direito na impressora segurando papel para encher a bandeja. Cada página usa uma folha da bandeja. Quando a bandeja está vazia, a tarefa espera com o status "sem papel".

A impressora coloca as páginas prontas em seus três compartimentos de saída. Quando estiverem cheios, o status diz "retire as folhas". Retire as folhas pela janela da impressora: shift-clique em uma folha para movê-la para o seu inventário.

## A fila

A fila é a lista de tarefas esperando para imprimir, e a primeira tarefa é a que está sendo impressa. A impressora guarda 16 tarefas, e uma tarefa pode pedir de 1 a 99 cópias. Uma tarefa enviada para uma fila cheia é recusada. Na janela da impressora, PAUSE para a impressão, RESUME a retoma e CANCELAR JOB cancela a tarefa.

A caixa de diálogo de impressão mostra o estado da impressora: "Pronta", "Imprimindo", "Pausada", "Sem papel" ou "Bandeja de saída cheia".

## A janela da impressora

Table: Parte da janela

| Parte | O que mostra |
| --- | --- |
| QUEUE | As tarefas esperando para imprimir. |
| NOW | A tarefa que está sendo impressa. |
| SHEET | O progresso da tarefa, folha por folha. |
| OUT | Os compartimentos de saída, e as folhas em cada um. |
| PAPER | As folhas que restam na bandeja, com a nota "folhas da bandeja, uma por página". |
| PAUSE, RESUME, CANCELAR JOB | Botões: param a impressão, retomam a impressão ou cancelam a tarefa. |

## Imprimindo pela linha de comando

Na linha de comando, um arquivo vai para uma impressora com um comando. O prompt do MC-DOS usa PRINT, e os shells de Linux, FreeBSD e UNIX System V usam lp. Os comandos são explicados por completo no livro do MC-DOS ([](jsc:dos_printing)) e nos livros de Linux ([](jsc:printing)).

Table: Os comandos de impressão

| Comando | Sistema | O que faz |
| --- | --- | --- |
| `PRINT` | MC-DOS | Imprime arquivos de texto em segundo plano. |
| `lp` | Linux, FreeBSD e UNIX System V | Envia arquivos para uma impressora. Sua opção -d indica a impressora, e -n, o número de cópias. |
| `lpstat` | Linux, FreeBSD e UNIX System V | Lista a fila de impressão e se cada impressora está ociosa. |

No lp, cada impressora tem um nome. Informe-o com -d. O nome também é o início do id da requisição que o lp responde: o nome, um traço e um número.

Table: Impressora e seu nome para lp

| Impressora | Nome para lp |
| --- | --- |
| Epsilon FX-80 | `epsilon` |
| Pakard DeskJot 940 | `deskjot` |
| Pakard FotoSmart C4280 | `fotosmart` |
| Pakard LaserJot 1102 | `laserjot` |
| Epsilon EcoTonk ET-2720 | `ecotonk` |

## Copiar um mapa, e os livros

Um arquivo .map, que um scanner lê de um mapa ([](jsc:scanners)), imprime num mapa vazio na bandeja: imprima-o do computador, ou mande-o com PRINT ou lp, e a impressora devolve o mesmo mapa, desenhado. Sem mapa vazio na bandeja, o trabalho espera com o status "sem mapa vazio". Um .map só imprime no mundo cujo scanner o leu.

Livros são impressos só pela Impressora de Grande Escala, uma máquina à parte ([](jsc:large_scale_printer)).

## Se algo der errado

> [!PROBLEM] Nada sai.
> Leia o status. "sem papel" significa que a bandeja está vazia, então coloque papel comum. "retire as folhas" significa que a saída está cheia, então retire as folhas.

> [!PROBLEM] Nenhuma impressora está ligada a este computador.
> Conecte a impressora ao computador com um cabo periférico ([](jsc:peripheral_cables)).

> [!PROBLEM] A impressora diz que não pode aceitar mais documentos: a fila dela está cheia.
> Espere a fila esvaziar, ou cancele a tarefa com CANCELAR JOB.

> [!PROBLEM] Uma imagem em cores saiu em preto e branco.
> Imprima em uma impressora que imprime em cores: a impressora Legacy, Transition ou Advanced.

> [!PROBLEM] O comando lp diz que o destino é "inexistente".
> Use o nome de uma impressora conectada ao computador. O lpstat os mostra.

> [!PROBLEM] O arquivo não é um mapa que um scanner deste mundo leu.
> Um .map só imprime como foi escaneado neste mundo; um que foi alterado, ou vem de outro mundo, não imprime.

> [!SEE] jsc:peripheral_cables, jsc:dos_printing, jsc:printing
