---
section: errors_compile
order: 487
---
# Como o compilador lê um arquivo

O compilador não verifica um arquivo de uma vez só. Ele passa por algumas etapas, em ordem, e cada etapa pode encontrar erros. Um erro em uma etapa pode esconder os erros das etapas seguintes, então a lista pode mudar conforme você corrige as coisas.

Table: As etapas de uma compilação

| Etapa | O que verifica |
| --- | --- |
| Leitura do texto | Os caracteres e as palavras de cada arquivo, uma parte por vez. |
| Gramática | A forma de cada linha: os seus colchetes, as suas palavras-chave e a sua pontuação. |
| Regras da linguagem | Se todo recurso usado no arquivo existe no nível de linguagem dele. |
| Semântica | O que os nomes e os tipos do arquivo representam. |
| Listagem | A listagem só é gravada quando nenhuma etapa encontrou erro. |

O compilador verifica a semântica somente quando as etapas anteriores não encontraram nenhum erro. Assim, um arquivo com erro de sintaxe mostra primeiro os seus erros de sintaxe, e os erros de semântica aparecem depois que você os corrige. O editor verifica o arquivo enquanto você digita, e ele é mais tolerante: pode relatar erros de semântica enquanto uma linha ainda está quebrada. Considere esses erros menos certos, e verifique de novo quando os erros de sintaxe tiverem sumido.

Table: Os dois níveis da linguagem

| Nível | Marca | O arquivo-fonte termina em | O que tem |
| --- | --- | --- | --- |
| Sigma | Σ | `sg` | O subconjunto menor, que as máquinas mais antigas aceitam. |
| Sigma# | Σ# | `sgs` | Tudo o que a linguagem tem. |

O compilador compila um programa para o conjunto de instruções mais antigo que roda todas as instruções que o programa usa. Se você pedir um conjunto de instruções mais novo, o compilador respeita a sua escolha.

O compilador lê no máximo 128 níveis de aninhamento, ou seja, blocos dentro de blocos dentro de blocos. Acima disso, ele informa o código S2013 e não lê o resto do arquivo.

> [!DEFINE] erro de sintaxe
> Um erro na forma do texto: um colchete que falta, uma palavra onde deveria haver um símbolo ou uma linha que não termina.

> [!DEFINE] compilação
> Uma execução do compilador sobre um programa: ele gera uma listagem, ou as mensagens quando o programa tem erros.

> [!DEFINE] semântica
> O que os nomes e os tipos de um programa representam, verificado depois que a sua forma está correta.

> [!DEFINE] Conjunto de instruções
> As instruções de máquina que o processador de um computador executa. Cada computador tem o seu, e um programa é compilado para um deles.

## Se algo der errado

> [!PROBLEM] A lista mudou depois que você corrigiu uma linha, e novos erros apareceram.
> Isso é esperado: as etapas escondem os erros umas das outras. Corrija a primeira linha da nova lista e compile de novo.

> [!PROBLEM] O compilador lhe dá uma lista de erros e nenhuma listagem.
> A listagem só é gravada quando não resta nenhum erro. Corrija cada linha da lista e depois compile de novo.

> [!SEE] jsc:sigma_syntax_errors, jsc:sigma_semantic_errors, jsc:sigma_build_errors
