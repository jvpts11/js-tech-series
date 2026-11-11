---
section: quince_systems_section
order: 355
---
# Os sistemas da Quince

A Quince faz os seus próprios computadores e os seus próprios sistemas para eles. Cada sistema roda nos processadores para os quais foi feito, e em nenhum outro: você não consegue instalar um sistema para o PowerPC num processador Integra. Os computadores da Quince estão descritos em [](jsc:quince_computers).

> [!DEFINE] Quince System
> O primeiro sistema da Quince, para o Russet 128K e o seu processador 68k.

> [!DEFINE] Quince OS 9
> O último sistema clássico, para o PowerPC do PowerQ G3 e do G4.

> [!DEFINE] Quince OS X
> O sistema que veio depois, para o PowerPC e para os processadores Integra.

> [!DEFINE] Quince OS X Server
> O Quince OS X para servidores, com o Server Admin para cuidar dos seus serviços.

> [!DEFINE] quinceOS
> O sistema mais novo da Quince, para o Q1, o Q2 e o Q3, os chips da própria Quince.

Table: Os sistemas

| Sistema | Primeira era | Processadores | Disco | Memória |
| --- | --- | --- | --- | --- |
| Quince System | Vintage | 68k | 1 MB | 1 MB |
| Quince OS 9 | Legacy | PowerPC | 400 MB | 32 MB |
| Quince OS X | Legacy | PowerPC, x86-64 | 2.048 MB | 128 MB |
| Quince OS X Server | Transition | x86-64, ARM64 | 4.096 MB | 256 MB |
| quinceOS | Advanced | ARM64 | 16.384 MB | 4.096 MB |

O Quince System e o Quince OS 9 são os sistemas clássicos. Rodam no kernel quince_classic, que deixa um programa rodar até ele ceder a vez ao próximo, e não têm linha de comando nenhuma: tudo se faz com o mouse. O Quince OS X, o seu Server e o quinceOS rodam no kernel wallace, um UNIX por baixo, com um Terminal e o shell zsh. Esse UNIX é a razão de o capítulo da Quince estar neste manual.

> [!DEFINE] Sistema clássico
> O Quince System ou o Quince OS 9: sem linha de comando, e com a memória dada em blocos fixos.

O Quince OS X traz o Champollion, que roda um programa feito para o PowerPC num processador x86-64 à metade da velocidade. O Quince OS X Server e o quinceOS trazem o Champollion 2, que roda programas x86-64 nos chips da Quince a quatro quintos da velocidade ([](jsc:processors)).

Table: Como cada um formata o disco

| Sistema | Sistema de arquivos |
| --- | --- |
| Quince System | HFS |
| Quince OS 9 | HFS+ |
| Quince OS X e o seu Server | HFS+ (Journaled) |
| quinceOS | APFS |

## Se algo der errado

> [!PROBLEM] O instalador não oferece o sistema.
> O processador do computador não é um dos que o sistema foi feito para usar. Instale-o num computador da Quince com o processador certo.

> [!SEE] jsc:the_systems, jsc:quince_installing, jsc:quince_desktop
