---
section: hardware
order: 19
icon: jsc:cpu_integra_centro_c5_4690k
covers_family: jsc:processors
---
# Processadores

O processador, ou CPU, faz o cálculo de fato. Cada programa que você executa é o processador seguindo suas instruções, uma após a outra, muito rápido.

Dois números dizem o que um processador pode fazer: seus núcleos e sua velocidade de clock em GHz. Os núcleos do processador são as unidades de trabalho que rodam ao mesmo tempo.

> [!DEFINE] Núcleo
> Uma unidade de trabalho que um processador executa ao mesmo tempo que seus outros núcleos.

> [!DEFINE] Velocidade do clock
> A velocidade de cada núcleo, em GHz (gigahertz): bilhões de ciclos do clock por segundo.

Passe o cursor sobre um processador e a dica mostra sua capacidade, em itens por tick (it/t), e sua potência em watts. Em um Mainframe, a capacidade dos processadores se soma e define quantos itens por tick a rede inteira processa ([Mainframes](jsc:mainframes)).

Um processador também precisa caber na placa-mãe. Seu soquete deve ser o soquete da placa-mãe, e sua era deve ser a era da placa-mãe. Enquanto não houver placa-mãe instalada, um processador entra em qualquer slot, para que você possa organizar suas peças primeiro.

Todo processador pertence a um conjunto de instruções. Um programa feito para um conjunto roda nele e em todos os conjuntos mais novos da sua linha, nunca em um mais antigo e nunca em outra linha. Processadores de dois conjuntos nunca dividem um computador, e um sistema feito para um conjunto não é instalado num processador de outro.

Table: Os conjuntos de instruções

| Conjunto de instruções | Tamanho da palavra | Executa programas feitos para |
| --- | --- | --- |
| IA-16 | 16 bits | IA-16 |
| x86 | 32 bits | x86 e IA-16 |
| x86-64 | 64 bits | x86-64, x86 e IA-16 |
| 68k | 32 bits | 68k |
| PowerPC | 32 bits | PowerPC |
| ARM64 | 64 bits | ARM64 |

Os três últimos são da Quince. O Motoroller 68000, o PowerQ G3 e o G4 e os Quince Q1 a Q3 vão só nas placas lógicas da Quince. Os chips Q trazem a memória no encapsulamento, compartilhada com o vídeo, então uma máquina roda com um deles sem nenhum pente de memória.

Um programa passa de uma linha para outra quando é compilado para as duas: sigma build --isa powerpc,x86-64 faz um build universal, que roda em cada uma ([](jsc:sigma_compile)). Um sistema também pode trazer um tradutor, que o roda mais devagar: o Champollion roda programas PowerPC em processadores x86-64 à metade da velocidade, e o Champollion 2 roda programas x86-64 nos chips da própria Quince a quatro quintos dela.

O tamanho da palavra de um processador depende da sua era, e o mesmo acontece com o tamanho de um item. Você verá os mesmos tamanhos nos discos ([Unidades e ticks](jsc:units_and_ticks)).

Table: Tamanho da palavra e do item por era

| Era | Tamanho da palavra | Tamanho de um item |
| --- | --- | --- |
| Vintage | 16 bits | 1 MB |
| Legacy | 32 bits | 16 MB |
| Transition | 64 bits | 256 MB |
| Standard | 64 bits | 256 MB |
| Advanced | 64 bits | 256 MB |
| Exa | 64 bits | 256 MB |
| Singularity | 64 bits | 256 MB |

Há mais de 170 processadores no mod, dos fabricantes Integra e Velocion e da Quince. Um processador com gráficos integrados indica isso na dica, e ele liga um monitor sem placa de vídeo.

## Se algo der errado

> [!PROBLEM] Ele não encaixa.
> O soquete ou a era dele não são os da placa-mãe. Use um processador com o soquete e a era da placa-mãe.

> [!PROBLEM] A montagem diz que o conjunto de instruções da CPU não corresponde.
> Use processadores de um mesmo conjunto de instruções.

> [!PROBLEM] Um sistema não instala, ou o firmware não acha nada para iniciar num disco.
> O sistema é feito para outro processador. Use um sistema feito para o desta máquina.

> [!PROBLEM] A montagem diz que há CPUs demais.
> Retire processadores, ou use uma placa-mãe com mais slots de CPU.

> [!SEE] jsc:motherboards, jsc:mainframes, jsc:eras
