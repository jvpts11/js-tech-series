---
section: system_v_menus
order: 352
---
# PACE, os menus em quadros

O PACE é o sistema de menus do UNIX System V, e só esse sistema o tem. Ele abre um quadro de menus sobre a tela, e cada escolha que você faz abre um quadro próprio sobre o anterior. O nome significa Panelled Access Command Environment, do UNIX System V da Bellwether Labs.

> [!DEFINE] quadro
> Uma janela de menus no PACE. Um novo quadro abre sobre o último, e as teclas movem entre eles.

Digite pace para iniciá-lo. Ele ocupa o terminal inteiro, e o primeiro quadro é o principal, com o título PACE. Escolha Sair do PACE, ou pressione F5 no último quadro, e o terminal volta.

Table: O quadro principal

| Escolha | O que abre |
| --- | --- |
| `Office of player` | O Escritório: o Filecabinet, sua pasta pessoal e as outras pastas. |
| Programas | Os programas instalados. ENTER executa um deles. |
| Administração do Sistema | Informações da Máquina, Sistemas de Arquivos e Instalação de Software. |
| Sistema UNIX | Um shell próprio. Digite exit nele para voltar ao PACE. |
| Sair do PACE | Sai do PACE e devolve o terminal. |

Dentro de um quadro, estas teclas funcionam. Os rótulos na linha de baixo da tela indicam as teclas de função.

Table: As teclas no PACE

| Tecla | O que faz |
| --- | --- |
| `Up, Down, Home, End, Page Up, Page Down` | Move-se dentro do quadro. |
| `Enter or F2` | Abre o item. |
| `F1` | HELP: abre um quadro de ajuda. |
| `F3` | PREV-FRM: vai para o quadro anterior a este. |
| `F4` | NEXT-FRM: vai para o quadro seguinte a este. |
| `F5 or Escape` | CANCEL: fecha o quadro do topo. No último quadro, sai do PACE. |
| `F6` | CMD-MENU: abre o menu de comandos. |
| `A letter` | Move para o próximo item que começa com essa letra. |

Um clique do mouse traz um quadro para a frente e escolhe o item dele. Um segundo clique no mesmo item o abre.

## O Escritório e suas pastas

O Filecabinet abre sua pasta pessoal, /usr/player. Outros Usuários lista as pastas dentro de /usr, exceto bin, lib e player, e diz "(nenhum outro usuário)" quando não há nenhuma. Preferências está em cinza, e diz "Nada para definir nesta máquina."

## Abrindo um arquivo pelo PACE

1. Digite pace e pressione Enter.
2. Escolha Escritório de player e pressione Enter.
3. Escolha Filecabinet e pressione Enter.
4. Mova-se até um arquivo com as setas do teclado e pressione Enter. Um arquivo padrão abre no vi.

## Lendo uma pasta

Um quadro de pasta recebe como título o nome e o caminho dela, e lista o que está dentro. O editor vi é explicado em [](jsc:vi). Uma listagem Sigma é um programa na linguagem Sigma, explicada em [](jsc:sigma_two_languages).

Table: O que ENTER faz em cada tipo de entrada

| Detalhe | O que ENTER faz |
| --- | --- |
| Diretório | Abre a pasta. |
| Listagem Sigma, um arquivo que termina em .sg | Abre o arquivo no vi. F6 o executa. |
| Listagem Sigma#, um arquivo que termina em .sgs | Abre o arquivo no vi. Ele não é executado. |
| Executável, um arquivo em /usr/bin ou /bin | Executa o programa. |
| Arquivo padrão | Abre o arquivo no vi. |

Pressione F6 para abrir o menu de comandos, exceto em uma listagem do Sigma, onde F6 executa o arquivo. Os itens do menu são cancel, exit, help, next-frm, prev-frm, refresh, run e unix-system. Refresh lê a pasta novamente.

Run abre uma linha de comando. Digite um comando UNIX, pressione Enter e leia a saída. Escape ou F5 cancela a linha.

Um comando executado pelo PACE mostra sua saída e depois diz "Pressione ENTER para voltar ao PACE." O item Sistema UNIX não tem essa linha: digite exit no shell dele para voltar.

## Administração do Sistema

Table: O que os itens executam

| Item | Comando que executa |
| --- | --- |
| Informações da Máquina | `uname -a` |
| Sistemas de Arquivos | `df` |
| Instalação de Software | `installpkg` |

```command
id: jsc:command/unix/pace
```

## Se algo der errado

> [!PROBLEM] Preferências está em cinza.
> Não há nada para definir neste sistema. O quadro diz "Nada para definir nesta máquina."

> [!PROBLEM] O quadro Outros Usuários diz "(nenhum outro usuário)".
> Sua pasta pessoal é /usr/player, e o PACE não lista bin, lib nem player em /usr, então nenhum outro usuário aparece.

> [!PROBLEM] ENTER em um arquivo Sigma o abre no vi, e ele não é executado.
> Pressione F6 em uma listagem Sigma, um arquivo que termina em .sg, para executá-la.

> [!SEE] jsc:installpkg, jsc:vi, jsc:cde_on_system_v
