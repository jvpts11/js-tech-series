---
section: errors_reading
order: 486
---
# Lendo uma linha de erro

Quando você compila um programa Sigma, o compilador confere cada arquivo do programa e lista cada erro que encontra. Uma compilação é uma execução do compilador sobre os seus arquivos, e o resultado é ou uma listagem que a máquina pode executar, ou uma lista de erros. Cada erro ocupa uma linha, e essa linha informa onde ele está, de que tipo é e do que trata.

Table: Partes de uma linha de erro

| Parte | O que ela informa |
| --- | --- |
| O nome do arquivo | O arquivo-fonte que contém o erro, com o nome que você salvou. |
| (12,9) | A linha e a coluna, ambas contadas a partir de 1 no canto superior esquerdo do arquivo. |
| error | O tipo da linha. Toda linha que o compilador lhe dá é um erro. |
| `S2001` | O código do erro. Ele diz o tipo de erro, e é o mesmo em todos os idiomas. |
| A mensagem | Qual é o erro, em palavras, com os nomes que ele encontrou nele. |

Por exemplo, um erro em um arquivo chamado Monitor.sgs pode aparecer assim: "Monitor.sgs(12,9): error S2001: expected ';' but found '}'". O compilador esperava um ponto e vírgula na linha 12, coluna 9, e encontrou uma chave de fechamento.

Um erro em uma listagem tem a mesma forma, mas sem nome de arquivo: "(4,1): error A4003: 'nonsense' não é uma instrução". Leia do mesmo jeito. Os erros de listagem têm página própria: [erros de listagem](jsc:sigma_listing_errors).

Leia a lista de cima para baixo e corrija primeiro a primeira linha. Um erro no início de um arquivo pode causar as linhas seguintes, então o restante da lista pode mudar quando você o corrige. A lista é ordenada por arquivo, depois por linha, e depois por coluna.

O compilador lista no máximo 100 erros por arquivo. Quando um arquivo tem mais, a última linha da lista diz "erros demais; o restante não foi reportado", e você corrige os de cima antes de compilar de novo.

A lista de problemas do editor, veja [os editores](jsc:sigma_editors), agrupa os problemas por arquivo. O arquivo com mais problemas vem primeiro, então a lista começa pelo arquivo que mais precisa de trabalho.

> [!DEFINE] compilar
> Transformar o texto-fonte de um programa em uma listagem que uma máquina pode executar.

> [!DEFINE] listagem
> O arquivo de texto que o compilador escreve a partir de um programa, com os passos que a máquina executa. O nome dele termina em asm.

## Se algo der errado

> [!PROBLEM] A mensagem diz "Monitor.sgs não compila" quando você inicia o programa.
> O arquivo não compilou, então não tem listagem para executar. Abra-o no editor, corrija o primeiro problema e compile de novo.

> [!SEE] jsc:sigma_compile_errors
