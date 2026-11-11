---
section: desktop_apps
order: 82
icon: jsc:monitor
---
# Calculadora, Paint e Exceed

Três programas da área de trabalho trabalham com números e imagens: a Calculadora para expressões, o Paint para imagens e o Exceed para uma planilha de células que pode perguntar à rede o que ela guarda.

## A Calculadora

A Calculadora calcula a expressão que você digita, com parênteses, potências, raízes, logaritmos e as funções trigonométricas. Digite com as teclas ou com o teclado, e pressione = ou [[Enter]] para obter a resposta. A expressão aceita no máximo 256 caracteres.

Table: Teclas da calculadora

| Tecla | O que faz |
| --- | --- |
| `DEG` | Alterna sin, cos e tan entre graus e radianos. |
| `C` | Limpa a expressão. |
| `<-` | Apaga um caractere. |
| `=` | Calcula a resposta. Enter faz o mesmo. |
| `sin, cos, tan` | As funções trigonométricas. |
| `^` | Eleva um número a uma potência. |
| `sqrt` | A raiz quadrada. |
| `ln` | O logaritmo natural. |
| `log` | O logaritmo. |
| `!` | O fatorial do número anterior. |
| `pi, e` | As constantes pi e e. |

## Se algo der errado

> [!PROBLEM] A tela mostra Error.
> Confira os parênteses, os sinais e os nomes das funções, depois digite a expressão novamente.

## Paint

O Paint desenha uma imagem em uma área de desenho de até 128 por 128 pixels, em uma única camada, com uma paleta de 256 cores. Uma imagem salva é um arquivo .pix de cerca de 1 KB.

Table: Ferramentas do Paint

| Ferramenta | O que faz |
| --- | --- |
| Lápis | Desenha com a cor escolhida. |
| Borracha | Apaga parte da imagem. |
| Preencher | Preenche uma área com a cor escolhida. |
| Conta-gotas | Pega uma cor da imagem. |
| Linha, Retângulo, Elipse | Desenha uma forma desse tipo. |

Os botões no alto são Novo, Abrir, Salvar, Desfazer, o zoom, Papel de Parede e Imprimir. Desfazer volta até 24 passos. Papel de Parede faz da imagem o fundo da área de trabalho. Imprimir envia a imagem para uma impressora, uma imagem por página ([](jsc:printers)).

## Se algo der errado

> [!PROBLEM] O Paint diz "Imagem grande demais para salvar".
> Deixe a imagem menor e salve de novo.

> [!PROBLEM] O Paint diz "Essa imagem é grande demais para abrir aqui".
> Abra uma imagem menor.

> [!PROBLEM] O Paint recusa um arquivo e diz que não é uma imagem.
> Abra um arquivo que termina em .pix, ou um .map que um scanner leu, que o Paint salva como .pix ([](jsc:scanners)).

## Exceed

O Exceed é uma planilha de células, como uma planilha eletrônica. Tem 26 colunas, de A a Z, e 99 linhas. Cada célula tem um nome formado pela letra da sua coluna e pelo número da sua linha, como B7. Os seus arquivos são texto simples separado por vírgulas (.csv).

> [!DEFINE] Célula
> Uma célula da planilha, identificada pela letra da sua coluna e pelo número da sua linha.

> [!DEFINE] Fórmula
> O texto de uma célula que começa com um sinal de igual e calcula um valor a partir de outras células.

> [!DEFINE] Célula ao vivo
> Uma célula cujo número vem da rede, por isso aparece na sua própria cor.

Uma fórmula usa os quatro sinais aritméticos, parênteses, o sinal de potência, os nomes de outras células e as funções abaixo. Uma célula ao vivo é aquela que usa QTY, FREE ou SERVERS. O Exceed pede de novo à rede depois de uma espera de 3 segundos, assim digitar não envia uma requisição a cada tecla.

Table: Funções do Exceed

| Função | O que devolve |
| --- | --- |
| `SUM` | A soma de um alcance de células. |
| `MIN` | O menor valor de um alcance. |
| `MAX` | O maior valor de um alcance. |
| `AVG` | A média de um alcance. |
| `COUNT` | Quantos valores um alcance tem. |
| `QTY("item_id")` | Quantos de um item a rede guarda, dado pelo id do item entre aspas. |
| `FREE()` | Quanto espaço livre a rede ainda tem. |
| `SERVERS()` | Quantos servidores estão na rede. |

Table: Erros em uma célula

| Exibido | O que significa |
| --- | --- |
| `#ERR` | A fórmula não pode ser calculada: sintaxe errada, referência inválida ou função desconhecida. |
| `#LOOP` | A célula se refere a si mesma, diretamente ou por meio de outras células. |
| `#NONE` | A célula pede à rede algo que a máquina ainda não informou. |

Os botões do Exceed são Abrir, Salvar, Salvar Como e Atualizar.

## Se algo der errado

> [!PROBLEM] O Exceed diz "Planilha muito grande para salvar".
> Remova as células de que não precisa e salve de novo.

> [!PROBLEM] O Exceed pergunta se deve abrir outra planilha e perder as suas alterações.
> Responda não e salve a planilha antes.

> [!PROBLEM] Uma célula mostra #ERR.
> Confira os parênteses da fórmula, os nomes das funções e os nomes das células.

> [!PROBLEM] Uma célula mostra #LOOP.
> Remova a referência que leva de volta à célula.

> [!PROBLEM] Uma célula mostra #NONE.
> Confira o id do item ou aguarde a rede informar.

> [!SEE] jsc:programs, jsc:help_on_a_computer
