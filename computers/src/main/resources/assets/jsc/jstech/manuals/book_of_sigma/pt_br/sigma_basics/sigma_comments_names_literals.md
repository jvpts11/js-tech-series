---
section: sigma_basics
order: 412
---
# Comentários, nomes e literais

Um programa é uma lista de instruções. Uma instrução é uma ação, como guardar um número em uma variável ou chamar um método. A maioria das instruções termina com ponto e vírgula. Antes de escrever uma, você precisa das pequenas peças com que elas são feitas: comentários, nomes, palavras-chave e literais.

Um comentário é uma nota para quem lê o programa, e a máquina o ignora. Um comentário de linha começa com duas barras e vai até o fim da linha. Um comentário de bloco começa com uma barra e um asterisco, e termina no primeiro asterisco seguido de barra depois dele. Comentários de bloco não se aninham: você não pode colocar um dentro do outro.

Um nome é uma palavra que você escolhe para uma variável, um método ou um tipo. Ele começa com uma letra ou um sublinhado, e depois contém letras, dígitos ou sublinhados. Qualquer letra conta, então letras acentuadas e gregas são permitidas nos nomes. As palavras-chave abaixo são reservadas: você não pode usá-las como nomes.

Table: Palavras-chave, por grupo

| Grupo | Palavras-chave |
| --- | --- |
| Tipos | `int long float double bool char string object void var` |
| Declarações | `namespace using class interface enum struct record delegate event` |
| Modificadores e ações | `new dispose this base static virtual override abstract public private protected internal readonly` |
| Instruções | `if else while for foreach do switch case default break continue return lock in out` |
| Valores e testes | `true false null is as` |
| Próprias da versão 3, guardadas pelas versões anteriores | `try catch finally throw goto const comefrom abstainfrom reinstate` |
| Palavras só onde a versão 3 as lê | `sealed params nameof operator unsafe stackalloc sizeof byte ignore remember` |

O Sigma recusa algumas destas palavras, mesmo que a linguagem as conheça. A tabela "O que o Sigma tem, e o que o Sigma# acrescenta" mostra que recursos cada linguagem tem. Uma compilação presa à versão 2 recusa as palavras da versão 3, e nenhuma das guardadas pode ser um nome. As palavras da última linha só são palavras onde a linguagem as lê, então um programa ainda pode usá-las como nomes.

Um literal é um valor escrito no programa, como o número 42, o número 0,5, true ou um texto entre aspas duplas. Cada literal tem um tipo, e a tabela mostra qual. Números são escritos em dígitos decimais simples. Um número com expoente, como 1e5, ou com sublinhados, como 1_000, não é um número que a linguagem consegue ler.

Table: Literais

| Escrito como | Tipo | O que significa |
| --- | --- | --- |
| `42` | `int` | Um número inteiro, até 2.147.483.647. |
| `42L` | `long` | Um número inteiro com um L depois, para números maiores. |
| `0.5` | `double` | Um número com ponto. Sem sufixo, é um double. |
| `3.14f` | `float` | Um número com um f depois. |
| `2d` | `double` | Um número inteiro com um d depois, escrito como double. |
| `true, false` | `bool` | Os dois valores lógicos. |
| `'A'` | `char` | Exatamente um caractere, entre aspas simples. |
| `"text"` | `string` | Qualquer texto entre aspas duplas, em uma só linha. |
| `null` | none | Nada. Serve para qualquer tipo que guarde objetos. |

Dentro de um texto ou de um caractere, uma barra invertida inicia uma sequência de escape. A letra depois da barra invertida diz o que entra no texto, como mostra a tabela.

Table: Sequências de escape

| Letra depois de uma barra invertida | Insere |
| --- | --- |
| `n` | Uma nova linha. |
| `t` | Uma tabulação. |
| `r` | Um retorno de carro. |
| `0` | O caractere zero. |
| `\` | Uma barra invertida. |
| `"` | Aspas duplas. |
| `'` | Aspas simples. |

Um texto precisa fechar na linha em que começa. Um texto também pode ter lacunas preenchidas com valores, escritas com um cifrão antes das aspas, e essa forma pertence somente ao Sigma#.

Table: O que o Sigma tem, e o que o Sigma# acrescenta

| Recurso | Sigma | Sigma# |
| --- | --- | --- |
| Tipos básicos e literais: int, long, float, double, bool, char, string | Sim | Sim |
| Arrays | Sim | Sim |
| if, while, do, for, switch, break, continue e return | Sim | Sim |
| dispose | Sim | Sim |
| Testes de tipo com is e as, e conversões (casts) | Sim | Sim |
| class, struct e enum | Sim | Sim |
| virtual, override e a classe base Script | Sim | Sim |
| classes e métodos abstratos | Não | Sim |
| interface e record | Não | Sim |
| delegate, event, propriedade e lambda | Não | Sim |
| var, que lê o tipo a partir do valor | Não | Sim |
| foreach e lock | Não | Sim |
| List, Map e tipos escritos com colchetes angulares | Não | Sim |
| Textos com lacunas, escritos com cifrão | Não | Sim |
| using System e a biblioteca System | Não | Sim |
| Nomes antigos, como printf | Sim | Sim |
| Versão 3: const, goto, as palavras do INTERCAL, #if e ponteiros | Sim | Sim |
| Versão 3: exceções, tuplas, unsafe e as formas pequenas do C# | Não | Sim |

Table: Erros em nomes e literais

| Erro | Mensagem | O que fazer |
| --- | --- | --- |
| `S1001` | o texto nunca foi fechado antes do fim da linha | Feche o texto com aspas duplas na mesma linha. |
| `S1002` | o comentário nunca foi fechado antes do fim do arquivo | Acrescente o asterisco e a barra que fecham o comentário de bloco. |
| `S1003` | '#' não inicia nada que a linguagem conheça | Remova o caractere, ou coloque-o dentro de um texto. |
| `S1004` | '1e5' não é um número que a linguagem consegue ler | Escreva o número em dígitos decimais simples. |
| `S1005` | um literal de caractere contém exatamente um caractere | Mantenha um caractere entre as aspas simples. |
| `S1006` | '\\x' não é uma sequência de escape que a linguagem conhece | Use uma das sequências de escape da tabela acima. |

> [!DEFINE] Instrução
> Uma instrução do programa, como uma atribuição ou uma chamada. A maioria das instruções termina com ponto e vírgula.

> [!DEFINE] Literal
> Um valor escrito no próprio programa, como 42 ou um texto entre aspas.

> [!DEFINE] Palavra-chave
> Uma palavra que a linguagem reserva para o seu próprio uso, como if ou int. Uma palavra-chave não pode ser usada como nome.

> [!DEFINE] Tipo
> O tipo de um valor: números inteiros, números decimais, valores lógicos, caracteres, textos ou objetos. O tipo define o que uma variável pode guardar.

> [!DEFINE] Variável
> Um espaço nomeado que guarda um valor, que o programa pode ler e alterar.

> [!DEFINE] Método
> Um trecho de código nomeado que o programa executa cada vez que você o chama pelo nome.

## Se algo der errado

> [!PROBLEM] O compilador informa que o texto nunca foi fechado antes do fim da linha.
> Coloque as aspas duplas de fechamento na mesma linha das de abertura.

> [!PROBLEM] O compilador informa que '1e5' não é um número que a linguagem consegue ler.
> Escreva o número em dígitos decimais simples, como 100000 em vez de 1e5.

> [!SEE] jsc:sigma_examples, jsc:sigma_syntax_errors
