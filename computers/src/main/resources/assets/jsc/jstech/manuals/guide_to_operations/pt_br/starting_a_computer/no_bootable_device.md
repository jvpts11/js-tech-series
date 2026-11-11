---
section: starting_a_computer
order: 9
icon: jsc:floppy_disk
---
# Quando nada inicializa

Às vezes o firmware não encontra nada que possa iniciar. Então ele diz isso na tela e espera por você. As palavras dependem da era da placa-mãe.

Table: O que a tela diz

| Era | O que a tela diz |
| --- | --- |
| Vintage | "Disco sem sistema ou erro de disco", depois "Substitua e pressione qualquer tecla quando estiver pronto". |
| Legacy e Transition | "FALHA NA INICIALIZAÇÃO DO DISCO, INSIRA O DISCO DE SISTEMA E PRESSIONE ENTER". |
| Standard e Advanced | "Nenhum dispositivo inicializável encontrado", depois "Pressione qualquer tecla para entrar na Configuração". |

Quando um disco tem um sistema cujo gerenciador de inicialização foi apagado, a própria queixa desse sistema substitui o texto do firmware. A tela então acrescenta "Insira uma mídia de instalação e instale por cima para reparar."

A tela lista cada disco e unidade que ela conhece. Quando há mais do que ela consegue mostrar, a última linha diz quantos a mais há. Uma lista vazia diz "Nenhum disco e nenhuma unidade estão conectados a este computador."

A dica diz "Insira a mídia de instalação e pressione Enter, ou DEL para a Configuração". Pressione qualquer outra tecla, e a tela diz Entrando na Configuração e abre a Configuração ([](jsc:setup_utility)).

1. Coloque uma mídia de instalação numa unidade conectada ao computador.
2. Pressione Enter nesta tela para iniciar a partir dela, ou pressione DEL para ir à Configuração.
3. Siga o instalador ([](jsc:installing)).

Table: Qual unidade lê qual mídia

| Mídia | Unidade que a lê |
| --- | --- |
| Disquete | Unidade de Disquete. |
| CD-ROM | Unidade de CD, Unidade de DVD ou Unidade de Blu-ray. |
| DVD-ROM | Unidade de DVD ou Unidade de Blu-ray. |
| Pen Drive | Estação de Dock, ou a porta USB na frente do gabinete a partir da Transition. |

## Se algo der errado

> [!PROBLEM] A tela diz que nenhum dispositivo inicializável foi encontrado.
> Ainda não há nenhum disco com sistema, e nenhuma mídia de instalação está numa unidade. Coloque uma ([](jsc:installing)).

> [!PROBLEM] Uma mídia está numa unidade, mas a tela não a lista.
> A mídia está no tipo errado de unidade, ou a unidade não está conectada ao computador. Veja a tabela acima ([](jsc:drives)).

> [!PROBLEM] A tela diz "Nenhum disco e nenhuma unidade estão conectados a este computador."
> Conecte uma unidade, ou um disco, ao computador ([](jsc:drives)).

> [!SEE] jsc:installing, jsc:drives, jsc:boot_menu
