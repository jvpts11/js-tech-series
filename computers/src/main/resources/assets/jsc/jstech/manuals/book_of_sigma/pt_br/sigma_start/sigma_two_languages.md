---
section: sigma_start
order: 405
---
# Duas linguagens, uma família

Sigma e Sigma# são as duas linguagens de programação dos computadores daqui. Elas formam uma só família: tudo o que é escrito em Sigma também é um programa válido em Sigma#. Você aprende uma e consegue ler a outra.

> [!DEFINE] Sigma
> A menor das duas linguagens de programação. Seus arquivos-fonte terminam em .sg, e seu compilador é scc.

> [!DEFINE] Sigma#
> A linguagem de programação completa, que aceita tudo o que o Sigma aceita. Seus arquivos-fonte terminam em .sgs, e seu compilador é sgsc.

Table: As duas linguagens

| Linguagem | Compilador | Terminação do arquivo-fonte |
| --- | --- | --- |
| Sigma | `scc` | `.sg` |
| Sigma# | `sgsc` | `.sgs` |

O Sigma mantém [classe](jsc:sigma_classes), struct, array, laço e chamada, e nada além disso do tipo avançado. O Sigma# acrescenta o que o Sigma deixa de fora, como threads, coleções, interfaces e genéricos. Um programa escrito em Sigma compila sem mudanças em Sigma#.

Qual delas escolher? Primeiro, o seu computador decide. Um computador Vintage só comporta o compilador Sigma, então o Sigma é a sua única linguagem. Um computador Legacy ou superior comporta toda a cadeia de ferramentas, e aí o Sigma# lhe dá a biblioteca completa.

Um compilador transforma um arquivo-fonte em uma listagem: o texto em assembly que um computador executa, em um arquivo que termina em .asm. Cada listagem é feita para um conjunto de instruções.

> [!DEFINE] compilador
> Um programa que transforma o texto de um arquivo-fonte em uma listagem. Quando o arquivo-fonte tem um erro, ele mostra os erros e não grava nada.

> [!DEFINE] listagem
> O texto em assembly que um compilador grava, em um arquivo que termina em .asm.

> [!DEFINE] arquivo-fonte
> Um arquivo de texto que contém um programa em Sigma ou Sigma#, escrito antes de ser compilado.

> [!DEFINE] projeto
> Uma pasta que guarda os arquivos-fonte de um programa e as configurações da sua compilação.

O runtime executa as listagens. Ele é um pacote próprio, e, depois de instalado, dá ao computador o comando sigma ([](jsc:sigma_installing)).

> [!DEFINE] runtime
> O programa que executa listagens em um computador, e que dá ao computador o comando sigma.

Cada computador executa um conjunto de instruções (ISA): as operações básicas que o seu processador conhece. Uma listagem é feita para um conjunto, e um computador só a executa se o seu processador conhecer esse conjunto. Este mod tem três: IA-16, x86 e x86-64.

> [!DEFINE] Conjunto de instruções (ISA)
> As operações básicas que um processador conhece. Uma listagem é feita para um conjunto, e um computador só a executa se o seu processador conhecer esse conjunto.

Table: Os três conjuntos de instruções

| Nome | Largura | Executa listagens compiladas para |
| --- | --- | --- |
| `IA-16` | 16 bits | Somente IA-16 |
| `x86` | 32 bits | x86 e IA-16 |
| `x86-64` | 64 bits | x86-64, x86 e IA-16 |

Um programa feito para o conjunto de instruções mais antigo continua funcionando em todos os mais novos. Por isso as listagens IA-16 rodam em qualquer lugar, e as listagens x86-64 rodam só em máquinas x86-64.

Todo computador também tem uma era, a sua geração de hardware: Vintage, Legacy, Transition, Standard, Advanced, Exa ou Singularity. A era define qual conjunto de instruções o computador tem e quais softwares ele pode instalar. As eras são explicadas em [](jsc:eras).

> [!DEFINE] era
> A geração de hardware de um computador. A era define o seu conjunto de instruções e quais softwares ele pode instalar.

Table: O conjunto de instruções de cada era

| Era | Conjunto de instruções |
| --- | --- |
| Vintage | `IA-16` |
| Legacy | `x86` |
| Transition, Standard, Advanced, Exa e Singularity | `x86-64` |

Quando você não escolhe um conjunto de instruções, a compilação usa o mais antigo em que a linguagem roda: IA-16 para o Sigma, e x86 para o Sigma#. Assim, um programa Sigma# compilado por padrão roda de um computador Legacy para cima, e nunca em um Vintage.

O Sigma tem uma biblioteca menor que a do Sigma#. Suas classes ficam no namespace Standard, com Console, File, Math e Convert entre elas. O capítulo [](jsc:sigma_library) lista o que cada uma faz.

Table: O que o Sigma deixa de fora

| Ausente no Sigma | O que o compilador sugere escrever |
| --- | --- |
| Interfaces | Uma classe cujos métodos são virtuais, e construa sobre ela em vez disso. |
| Records | Uma struct com os mesmos campos. |
| Classes abstratas | Uma classe com métodos virtuais que não fazem nada, e construa sobre ela. |
| Métodos abstratos | Dê a ele um corpo que não faz nada, e marque-o como virtual. |
| Delegates | Chame o método pelo nome, ou guarde o objeto e chame por ele. |
| Eventos | Guarde o objeto que precisa ser avisado, e chame um método nele. |
| Propriedades | Um campo, ou um método que devolve o valor. |
| Genéricos, como List<T> | Um array, ou uma classe que guarda exatamente o que você precisa. |
| Lista | Um array, e um contador seu ao lado dele. |
| Mapa | Dois arrays, ou um array pesquisado pela chave. |

## Se algo der errado

> [!PROBLEM] A compilação para com a mensagem "IA-16 roda apenas Σ; escreva em Σ, em um projeto Σ".
> Escreva o programa em Sigma, em um projeto Sigma. Ou defina o alvo da plataforma como x86 ou x86-64.

> [!PROBLEM] Uma listagem é recusada: "compilado para x86; esta máquina é IA-16".
> O computador é um Vintage, que executa apenas listagens IA-16. Compile com scc, que compila para IA-16, ou execute a listagem em um computador Legacy ou superior.

> [!SEE] jsc:sigma_language
