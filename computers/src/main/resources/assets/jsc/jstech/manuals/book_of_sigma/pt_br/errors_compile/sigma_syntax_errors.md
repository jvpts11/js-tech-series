---
section: errors_compile
order: 488
---
# Erros de sintaxe

Um erro de sintaxe é um erro na forma do texto. O compilador não consegue ler a linha como uma instrução, uma declaração ou um valor, porque uma peça está faltando, sobrando ou no lugar errado. Os códigos abaixo são os erros que você vê na linha que eles indicam.

O compilador primeiro corta o texto em tokens. Um token é um pedaço de texto: uma palavra, um número, uma string ou um símbolo. Uma mensagem mostra um token entre aspas, como ';'. Um token sem grafia única é mostrado pelo seu tipo: um inteiro, um inteiro longo, um float, um double, uma string, um caractere, um nome, ou o fim do arquivo.

> [!DEFINE] token
> Um pedaço de texto que o compilador lê como uma unidade só: uma palavra, um número, uma string ou um símbolo.

Table: Códigos ao ler o texto

| Código | Mensagem | O que significa e a correção |
| --- | --- | --- |
| `S1001` | a string não foi fechada antes do fim da linha | Uma string não tem aspas de fechamento na sua linha. Feche-a na mesma linha. |
| `S1002` | o comentário não foi fechado antes do fim do arquivo | Um comentário não tem marca de fechamento. Adicione a marca de fechamento. |
| `S1003` | '<character>' não inicia nada que a linguagem conheça | O caractere não faz parte da linguagem. Remova-o ou troque-o. |
| `S1004` | '<text>' não é um número que a linguagem consegue ler | Um número tem letras ou um segundo ponto grudados nele, ou um valor grande demais para o seu tipo. Escreva o número de novo. |
| `S1005` | um literal de caractere tem exatamente um caractere | Duas aspas simples não têm caractere, ou têm mais de um. Mantenha exatamente um caractere entre elas. |
| `S1006` | '\\<character>' não é uma sequência de escape que a linguagem conhece | Uma barra invertida é seguida de um caractere que não é uma sequência de escape. Use uma sequência de escape que a linguagem tenha, ou remova a barra invertida. |
| `S1007` | <texto> | Uma linha #error para a compilação com o seu texto, e uma linha #warning o informa. Leia o texto: o autor do arquivo o escreveu para você. |
| `S1008` | '#<palavra>' não é uma diretiva que a linguagem lê | Uma linha começa com # e uma palavra que não é diretiva. Use #if, #elif, #else, #endif, #define, #undef, #error ou #warning. |
| `S1009` | '<texto>' não é uma condição que #<palavra> consegue ler | A condição tem algo além de símbolos, true, false, !, &&, \|\|, ==, != e parênteses. Escreva-a com esses. |
| `S1010` | \#<palavra> não tem um #if aberto antes dele | Um #elif, #else ou #endif está sem um #if antes dele. Acrescente o #if, ou remova a linha. |
| `S1011` | este #if nunca é fechado com um #endif | Acrescente o #endif onde termina a parte que o #if escolhe. |
| `S1012` | este #if já teve o seu #else, então #<palavra> vem tarde demais | Um #elif ou um segundo #else vem depois do #else. Mova-o para antes do #else. |

Table: Códigos ao ler a gramática

| Código | Mensagem | O que significa e a correção |
| --- | --- | --- |
| `S2001` | esperado <token>, mas encontrado <token> | A gramática precisa de uma peça e encontra outra. Coloque a peça que falta onde a mensagem indica. |
| `S2002` | esperado um tipo, mas encontrado <token> | Aqui precisa estar um nome de tipo. Escreva o nome de um tipo. |
| `S2003` | esperada uma expressão, mas encontrado <token> | Aqui precisa estar um valor, por exemplo entre as chaves vazias de uma string interpolada. Escreva o valor. |
| `S2004` | esperado um campo, um método, uma propriedade ou um evento, mas encontrado <token> | Uma linha dentro de um tipo não é nenhuma destas quatro. Escreva uma delas, ou remova a linha. |
| `S2005` | esperado uma classe, uma struct, um record, uma interface, um enum ou um delegate, mas encontrado <token> | Onde um tipo deveria começar, a linha começa com outra coisa. Comece o tipo com uma destas palavras. |
| `S2006` | só uma chamada, uma atribuição, um incremento, um decremento ou um novo objeto pode ser usado como um comando | Uma linha tem um valor que não faz nada, como a + 1. Atribua-o, exiba-o ou apague-o. |
| `S2007` | '<word>' foi dado duas vezes | O mesmo modificador aparece duas vezes, como public public. Mantenha apenas um deles. |
| `S2008` | o lado esquerdo de uma atribuição precisa ser uma variável, um campo, uma propriedade ou um elemento | O lado esquerdo do sinal = precisa ser uma variável, um campo, uma propriedade ou um elemento. Coloque um desses ali. |
| `S2009` | um using precisa vir antes do namespace e dos tipos | Uma linha using está baixa demais no arquivo. Mova-a para acima do namespace. |
| `S2010` | um arquivo declara um namespace sozinho na sua linha, antes dos seus tipos; coloque mais deles em blocos | O arquivo tem mais de uma linha de namespace. Mantenha uma sozinha na linha e escreva as outras em blocos. |
| `S2011` | todo tipo está em um namespace: coloque 'namespace Nome;' no início do arquivo | Um tipo aparece sem namespace. Escreva uma linha de namespace no início do arquivo. |
| `S2013` | isto está aninhado a mais de 128 níveis de profundidade, o que é mais do que o compilador lê; o resto do arquivo não foi lido | Há blocos demais dentro de blocos. Divida a parte mais funda em partes menores. |
| `S2014` | '<word>' fica guardada para uma versão futura da linguagem; por enquanto, <alternative> | O arquivo escreve try, catch, finally, throw, goto, const, comefrom, abstainfrom ou reinstate numa compilação presa à versão 2 ou anterior. A versão 3 os traz, e as versões anteriores já os guardavam, então nenhum nome pode ser um deles. Prenda o projeto à versão 3, ou escreva o que a mensagem diz. O Sigma nunca tem try, catch, finally nem throw. |
| `S2015` | um rótulo marca uma instrução, e <token> não é uma | Um rótulo está no fim de um bloco, ou antes de algo que não é uma instrução. Ponha o rótulo na frente de uma instrução. |

## Se algo der errado

> [!PROBLEM] Diz "esperado ';', mas encontrado '}'".
> A linha antes da chave não termina com ponto e vírgula. Adicione o ponto e vírgula no fim dela.

> [!PROBLEM] Diz "só uma chamada, uma atribuição, um incremento, um decremento ou um novo objeto pode ser usado como um comando".
> A linha tem um valor que não faz nada. Atribua-o, exiba-o ou apague-o.

> [!PROBLEM] Diz que todo tipo está em um namespace.
> Escreva uma linha de namespace no início do arquivo, com um nome que você escolher.

> [!PROBLEM] Diz que isto está aninhado a mais de 128 níveis de profundidade.
> Divida a parte mais funda em partes menores. O resto do arquivo só é lido depois disso.

> [!SEE] jsc:sigma_compile_errors
