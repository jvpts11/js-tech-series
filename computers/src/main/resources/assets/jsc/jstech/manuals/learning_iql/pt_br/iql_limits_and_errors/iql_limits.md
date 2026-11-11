---
section: iql_limits_and_errors
order: 381
---
# Limites

A rede define limites para o que uma instrução pode pegar. Alguns limites encurtam um resultado.

Outros recusam a instrução, ou a param no meio do caminho, e mostram uma mensagem.

A tabela mostra cada limite e o que acontece quando você passa dele.

Table: Os limites do IQL

| Limite | Valor | O que acontece quando você passa dele |
| --- | --- | --- |
| Linhas que um QUERY lê, por padrão | 4.096 linhas | Um LIMIT abaixo desse número encurta a lista de linhas. |
| Tipos de item que um \* cobre numa instrução | 256 tipos de item | Um LIMIT acima disso é limitado, e a resposta diz quantos foram mostrados. |
| Níveis de NOT e de parênteses numa condição | 64 níveis | A instrução é recusada. |
| Comparações numa só condição | 256 comparações | A instrução é recusada. |
| Tamanho do nome de uma instrução salva | 64 caracteres | A instrução é recusada. |
| Tamanho do novo nome que uma bigorna dá | 50 caracteres | A instrução é recusada. |
| Ofertas de encantamento, numeradas | 1 a 3 | Qualquer outro número é recusado. |
| Procedimentos que rodam procedimentos | 32 níveis | A rede para com uma mensagem de recursão. |
| Instruções num só pedido | 4.096 instruções | A rede para e diz quantas executou. |
| Linhas que a consulta de um job lê | 64 linhas | O job lê no máximo 64 linhas. |
| Com que frequência a rede confere os jobs | a cada 10 ticks | Um intervalo é exato até cerca de 10 ticks. |
| Potência de redstone | 0 a 15 | Qualquer outro número é recusado. |
| Hora do dia no SET BUS | 0 a 23 | Qualquer outra hora é recusada. |
| Quantidade de um item | Um número inteiro igual ou maior que zero, ou ALL | Uma porcentagem ou um número negativo é recusado. Para uma ação, nenhum número e ALL fazem o mesmo. |
| Quantidade de linhas depois de LIMIT | Um número inteiro | Uma porcentagem é recusada. Um LIMIT de 0 significa sem limite. |

Alguns limites protegem a rede, não a sua instrução. Se um QUERY chegar a 4.096 linhas, use um WHERE para restringir as linhas que você lê, para que as linhas de que precisa não sejam cortadas.

> [!SEE] jsc:iql_errors, jsc:query_and_select
