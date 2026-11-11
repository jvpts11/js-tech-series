---
section: errors_compile
order: 490
---
# Recusas de compilação

O compilador também pode recusar uma compilação por dois motivos que nada têm a ver com os erros do seu texto. O primeiro é um recurso que o compilador ainda não compila. O segundo é uma máquina alvo que roda apenas o conjunto de instruções mais antigo.

Table: Recusas durante a compilação

| Código | Mensagem | O que significa e a correção |
| --- | --- | --- |
| `S4011` | um texto com buracos que nunca foi reduzido ainda não é compilado | O compilador ainda não compila um texto com valores inseridos nele. Escreva o texto de outro jeito. |
| `S4011` | uma lambda dentro de outra que mantém a sua variável ainda não é compilada | O compilador ainda não compila uma lambda aninhada em outra. Escreva-a sem o aninhamento. |
| `S4011` | uma lambda que mantém uma variável declarada por um laço ainda não é compilada | O compilador ainda não compila uma lambda que mantém a variável de um laço. Reescreva a lambda para que ela não mantenha essa variável. |
| `S4012` | IA-16 roda apenas Σ; escreva em Σ, em um projeto Σ | Um programa Sigma# pediu o conjunto de instruções IA-16, o mais antigo. Escreva o programa em Sigma, em um projeto Sigma. |

A mensagem de S4012 aparece na linha 1, coluna 1 do arquivo.

## Se algo der errado

> [!PROBLEM] Diz que uma lambda ainda não é compilada, ou que um texto ainda não é compilado.
> O compilador ainda não suporta essa forma. Escreva a lambda ou o texto de outro jeito.

> [!PROBLEM] Diz que IA-16 roda apenas Σ.
> Seu programa Sigma# tem como alvo o conjunto de instruções mais antigo, IA-16, que roda apenas Sigma. Escreva-o em Sigma, dentro de um projeto Sigma.

> [!SEE] jsc:sigma_compile_errors
