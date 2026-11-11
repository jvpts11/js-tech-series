---
section: sigma_version_three_section
order: 428
---
# O que a versão 3 traz

Um projeto fica preso a uma versão da sua linguagem, que você escolhe nas configurações dele. A versão 3 é a mais nova. Ela traz as palavras e as formas da tabela, e uma compilação presa à versão 2 recusa cada uma delas onde está escrita, com o código S3057 ou S2014.

Table: O que a versão 3 traz

| O quê | Sigma | Sigma# |
| --- | --- | --- |
| const, rótulos e goto | Sim | Sim |
| comefrom, abstainfrom, reinstate, ignore e remember | Sim | Sim |
| \#if e as outras diretivas | Sim | Sim |
| Ponteiros, sizeof e as chamadas da memória crua | Sim | Sim, em código unsafe |
| unsafe e stackalloc | Não | Sim |
| try, catch, finally e throw | Não | Sim |
| sealed, classes estáticas, params, funções locais e nameof | Não | Sim |
| Tuplas | Não | Sim |
| Métodos de extensão, operadores e indexadores | Não | Sim |

Um programa escrito para a versão 2 ainda compila na versão 3, e na mesmíssima listagem. As palavras novas só são palavras onde a linguagem as lê, então um programa que chama uma variável de unsafe ou um método de sizeof continua funcionando. As palavras try, catch, finally, throw, goto, const, comefrom, abstainfrom e reinstate já eram guardadas pelas versões anteriores, então nenhum programa podia usá-las como nomes.

Uma listagem que usa uma instrução da versão 3, como throw ou uma leitura através de um ponteiro, é escrita na versão 4 do formato de listagem, que um runtime mais antigo recusa. Um programa que não usa nenhuma delas é escrito no formato de sempre.

> [!DEFINE] Palavra contextual
> Uma palavra que só significa algo onde a linguagem a espera, e é um nome comum em todo o resto.

## Se algo der errado

> [!PROBLEM] O compilador diz que uma palavra precisa da versão 3 da linguagem.
> Prenda o projeto à versão 3 nas configurações dele, ou escreva o programa sem essa palavra.

> [!SEE] jsc:sigma_constants_goto, jsc:sigma_exceptions, jsc:sigma_pointers
