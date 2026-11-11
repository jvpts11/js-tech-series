---
section: errors_listing
order: 491
---
# Erros de listagem

Uma listagem é o arquivo de texto que o compilador escreve para um programa. A máquina lê a listagem quando o programa começa, e um problema de listagem é uma linha que ela não consegue ler. Um problema de listagem impede o programa de começar, e a máquina mostra só o primeiro problema.

A máquina mostra o nome do programa, dois-pontos e depois a linha com o problema. Por exemplo: “<program>: (4,1): erro A4003: 'nonsense' não é uma instrução”. Um problema de listagem sempre mostra a coluna 1.

Problemas de listagem vêm de três causas: uma listagem editada à mão, uma listagem feita por outra versão do compilador e uma máquina que não roda o conjunto de instruções para o qual o programa foi compilado. Uma listagem que o editor compila sem erros não mostra nenhum problema de listagem.

O runtime lê as listagens nas versões 2, 3 e 4. Uma listagem sem linha de versão dá A4001, e o leitor para ali. Uma linha de versão sem número é lida como a versão mais nova. Depois de um problema de versão, o leitor não lê mais nada, então esse problema é o único mostrado.

> [!DEFINE] rótulo
> Um nome escrito antes de uma linha de um método, para o qual um salto pode apontar.

> [!DEFINE] diretiva
> Uma linha que começa com ponto, como .arch, .field ou .method, e nomeia uma parte do programa.

> [!DEFINE] operando
> O que vem escrito depois do nome de uma instrução, como um número ou um nome.

> [!DEFINE] posição
> Uma posição numerada em um método, onde ele guarda um valor enquanto roda.

> [!DEFINE] linha de posições
> A linha que diz quantas posições um método tem.

Table: Problemas no texto da listagem

| Código | Mensagem | O que significa e a solução |
| --- | --- | --- |
| `A4001` | a listagem precisa começar com uma linha de versão | A primeira linha não é uma linha de versão. Compile o código-fonte de novo. |
| `A4002` | este runtime lê a versão de montagem <version>, e esta listagem é a versão <version> | A listagem veio de um compilador mais novo do que este runtime lê, então ela precisa de um runtime que leia a sua versão. |
| `A4003` | '<name>' não é uma instrução | O nome não é uma instrução do formato da listagem. Verifique a grafia ou compile o código-fonte de novo. |
| `A4004` | '<name>' precisa de algo depois dele | Uma instrução precisa de um operando e não tem nenhum. Escreva o operando ou compile o código-fonte de novo. |
| `A4005` | '<name>' não aceita nada depois dele | Uma instrução tem um operando que não aceita. Remova o operando. |
| `A4006` | '<text>' não é o que '<name>' aceita | O operando é o tipo de valor errado para aquela instrução ou diretiva. |
| `A4007` | '<name>' não é uma linha que este formato tem | Uma linha começa com ponto e não nomeia nenhuma linha que o formato tenha. Verifique a grafia. |
| `A4008` | uma instrução precisa estar dentro de um método | Uma linha de instrução fica fora de qualquer método. Coloque-a dentro de um método. |
| `A4009` | '<name>' precisa estar dentro de um tipo | Uma diretiva fica fora de qualquer tipo. Coloque-a dentro de um tipo. |
| `A4010` | nada neste método está marcado com '<name>' | Um salto aponta para um rótulo que o método não tem. Verifique a grafia ou acrescente o rótulo. |
| `A4012` | esta listagem é a versão de montagem <version> e este runtime lê a versão <version> em diante: compile o código-fonte dela de novo | A listagem é mais antiga do que o runtime lê. Compile o código-fonte de novo com o compilador atual. |
| `A4016` | este método não tem linha de posições, então não tem corpo | O método não diz quantas posições tem. Compile o código-fonte de novo. |
| `A4017` | a posição <place> não existe: este método tem <count> | Uma instrução usa uma posição que o método não tem. Use uma posição dentro da contagem que a mensagem dá. |
| `A4018` | '<name>' marca mais de uma linha deste método | Duas linhas de um método têm o mesmo rótulo. Mantenha um rótulo para cada linha. |
| `A4019` | '<nome>' veio na versão <versão> do formato, e esta listagem está escrita na versão <versão> | Uma listagem tem uma instrução ou uma linha de um formato mais novo do que a sua linha de versão diz. Compile o código-fonte de novo, o que escreve a versão de que ele precisa. |

Table: Problemas com o programa e a máquina

| Código | Mensagem | O que significa e a solução |
| --- | --- | --- |
| `A4013` | nada responde a '<name>' | Uma chamada, um novo objeto ou um campo nomeia algo que a máquina não tem. Verifique a grafia do nome. |
| `A4014` | '<name>' pode ser lido mas não escrito | O programa escreve em uma propriedade que só pode ser lida. Leia o seu valor e não escreva nela. |
| `A4015` | compilado para <instruction set>; esta máquina é <instruction set> | O programa foi compilado para outro conjunto de instruções do que esta máquina roda, então compile-o para o conjunto de instruções desta máquina, ou execute-o em uma máquina que roda esse conjunto. |

O problema com A4015 é outro. A listagem está certa, e a máquina é a errada. Os nomes a que A4013 se refere são os descritos em [a biblioteca](jsc:sigma_library).

## Se algo der errado

> [!PROBLEM] A máquina mostra um problema A4001, uma linha de versão ausente.
> Uma listagem editada à mão pode perder a linha de versão. Compile o código-fonte de novo.

> [!PROBLEM] A máquina mostra um problema A4002, uma listagem mais nova do que o runtime lê.
> A listagem precisa de um runtime que leia a sua versão. Use um runtime que a leia.

> [!PROBLEM] A máquina mostra um problema A4012, uma listagem mais antiga do que o runtime lê.
> Compile o código-fonte de novo com o compilador atual.

> [!PROBLEM] A máquina mostra um problema A4015, que diz que o programa foi compilado para outro conjunto de instruções.
> Compile o programa para o conjunto de instruções desta máquina, ou rode-o em uma máquina que o execute.

> [!SEE] jsc:sigma_runtime_stops
