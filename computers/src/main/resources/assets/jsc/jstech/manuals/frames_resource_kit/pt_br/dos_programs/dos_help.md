---
section: dos_programs
order: 208
---
# A ajuda, em tela cheia

Digite help no prompt para abrir a ajuda do MC-DOS. Ela ocupa a tela inteira, no lugar do prompt, e mostra os tópicos do manual. Seus tópicos vêm do Kit de Recursos do Frames, o livro que você está lendo.

Digite help e o nome de um comando, como help dir, para abrir a página desse comando. A mesma página abre quando você digita o nome do comando, um espaço e /?.

A tela de ajuda tem quatro partes, de cima para baixo, como mostra a tabela.

Table: As partes da tela de ajuda

| Parte | O que mostra |
| --- | --- |
| Linha superior | Os menus Arquivo e Pesquisar. |
| Segunda linha | Quatro botões: Próximo, Voltar, Conteúdo e Índice. |
| Meio | O tópico, com seus links. |
| Linha inferior | As teclas, uma mensagem ou o prompt de Localizar. |

1. Digite help no prompt e pressione [[Enter]].
2. Pressione [[Tab]] até que um botão ou um link tenha o foco, depois pressione [[Enter]] para segui-lo.
3. Pressione [[Alt]]+[[N]] para o próximo tópico, [[Alt]]+[[B]] para o anterior, [[Alt]]+[[C]] para o Conteúdo e [[Alt]]+[[I]] para o Índice.
4. Pressione [[Alt]]+[[S]] para abrir o menu Pesquisar, escolha Localizar... e pressione [[Enter]].
5. Digite as palavras a localizar, depois pressione [[Enter]].
6. Pressione [[Esc]] para sair da ajuda, ou pressione [[Alt]]+[[F4]].

Table: As chaves da ajuda

| Chave | O que faz |
| --- | --- |
| `Tab` | Escolhe o próximo botão ou link, e Shift com Tab escolhe o anterior. |
| `Enter` | Segue o link ou botão escolhido. |
| `Up and Down` | Rola uma linha. |
| `Page Up and Page Down` | Rola uma página. |
| `Home and End` | Vai ao início e ao fim do tópico. |
| `F10 or Alt+F` | Abre o menu Arquivo, com Sair. |
| `Alt+S` | Abre o menu Pesquisar, com Localizar... e Repetir Última Busca. |
| `Alt+C` | Mostra o Conteúdo. |
| `Alt+N` | Mostra o próximo tópico. |
| `Alt+B` | Mostra o tópico anterior. |
| `Alt+I` | Mostra o Índice. |
| `F3` | Repete a última busca. |
| `Esc` | Fecha um menu aberto ou o prompt de Localizar, ou sai da ajuda quando nada está aberto. |
| `Alt+F4` | Sai da ajuda. |

A linha de baixo mostra somente Alt+C=Conteúdo, Alt+N=Próximo e Alt+B=Voltar. Alt+I e Alt+S também funcionam.

Localizar primeiro procura um comando com exatamente esse nome. Depois procura o próximo verbete do manual que você está lendo cujo título ou texto tenha todas as palavras que você digitou. A busca gira a partir do tópico na tela. Repetir Última Busca, ou [[F3]], procura as mesmas palavras de novo.

> [!DEFINE] Tópico
> Uma página da ajuda: a página de um comando, ou um verbete de um manual.

> [!DEFINE] Link
> Uma palavra em uma página que você escolhe com Tab e segue com Enter.

## Se algo der errado

> [!PROBLEM] A linha de baixo diz Nenhuma correspondência encontrada.
> Nenhum comando e nenhum verbete tem todas as palavras que você digitou. Tente menos palavras, ou o nome de um comando.

> [!PROBLEM] Esc não sai da ajuda.
> Esc primeiro fecha um menu aberto ou o prompt de Localizar. Pressione de novo, ou pressione [[Alt]]+[[F4]].

> [!SEE] jsc:help_on_a_computer, jsc:dos_prompt
