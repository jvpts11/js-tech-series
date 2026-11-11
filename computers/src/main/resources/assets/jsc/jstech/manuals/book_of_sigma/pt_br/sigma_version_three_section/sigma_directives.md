---
section: sigma_version_three_section
order: 431
---
# \#if e os símbolos

Uma diretiva é uma linha que começa com # e fala com o compilador, não com a máquina. Com #if, uma parte de um arquivo só é compilada quando um símbolo está definido. As linhas deixadas de fora não são lidas, então podem conter qualquer coisa.

Table: As diretivas

| Escrito como | O que faz |
| --- | --- |
| `#if SYMBOL` | Compila as linhas até o próximo #elif, #else ou #endif quando o símbolo está definido. Uma condição junta símbolos, true e false com &&, \|\|, !, == e !=, e parênteses. |
| `#elif SYMBOL` | Outra condição, perguntada quando as anteriores foram falsas. |
| `#else` | As linhas compiladas quando nenhuma condição foi verdadeira. |
| `#endif` | Termina o #if. |
| `#define SYMBOL, #undef SYMBOL` | Define um símbolo, ou tira um, daquela linha até o fim do arquivo. |
| `#error text, #warning text` | Para a compilação com esse texto, ou o informa e continua. |

Table: Os símbolos que o compilador define

| Símbolo | Quando |
| --- | --- |
| O conjunto de instruções, em maiúsculas, como X86 para jsc:x86 | Sempre: a máquina para a qual a listagem é construída. |
| A era, em maiúsculas, como VINTAGE | Quando a máquina que compila conhece a sua era. |
| `SIGMA_3` | Em toda compilação da versão 3. |

O seu projeto também pode definir os seus próprios símbolos, com uma linha define: no seu arquivo de projeto. Cada diretiva fica numa linha só dela.

> [!DEFINE] Diretiva
> Uma linha que começa com #, que diz ao compilador que linhas ler.

> [!DEFINE] Símbolo
> Um nome sobre o qual uma diretiva pergunta, que está definido ou não está.

## Se algo der errado

> [!PROBLEM] O compilador diz que um #if nunca é fechado com um #endif.
> Acrescente o #endif onde termina a parte que depende do símbolo.

> [!PROBLEM] O compilador diz que uma palavra não é uma diretiva que a linguagem lê.
> Use uma das diretivas da tabela.

> [!SEE] jsc:sigma_version_three
