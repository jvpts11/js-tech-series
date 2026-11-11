---
section: help_at_the_prompt
order: 283
icon: jsc:guide_to_operations
---
# info e suas teclas

info é a ferramenta do Linux para os manuais. Enquanto man mostra uma página, info mostra os manuais como telas ligadas entre si. Cada capítulo, seção e entrada de um manual é um nó, e os comandos deste computador ficam ao lado deles.

info ocupa o terminal inteiro, como as telas de ajuda. Pressione q para sair e voltar ao prompt.

## Usando o info

1. Digite info e pressione [[Enter]] para abrir o diretório, a lista de todos os manuais e comandos.
2. Digite info, um espaço e um nome, como info graphics-cards, para abrir essa entrada diretamente.
3. Pressione [[Tab]] para escolher um link, depois [[Enter]] para segui-lo.
4. Pressione q para sair do info.

O diretório é a primeira tela. Ele lista todos os manuais, com uma linha de menu para os comandos deste computador. Pressione d a qualquer momento para voltar a ele.

Table: Chaves no info

| Chave | O que faz |
| --- | --- |
| `q` | Sai do info. |
| `n, p` | Vai para o nó seguinte, ou para o anterior. |
| `u` | Vai para o nó acima deste. |
| `t` | Vai para o nó principal do manual. |
| `d` | Vai para o diretório de todos os manuais. |
| `l` | Volta para o nó que você leu por último. |
| `h` | Mostra as teclas. |
| `m` | Pede o nome de um item de menu. Pressione Enter depois. |
| `s or /` | Pede um texto de busca, que Enter executa; Escape ou Ctrl+G cancela. |
| `Space, Page Down` | Desce uma tela. |
| `Backspace, Page Up` | Sobe uma tela. |
| `Up, Down` | Move uma linha. |
| `Tab, Shift+Tab` | Escolhe o link seguinte ou o anterior. |
| `Enter` | Segue o link escolhido. |

```command
id: jsc:command/linux/info
```

> [!DEFINE] nó
> Uma parte de um manual de info, um capítulo, uma seção ou uma entrada, lida como uma única tela.

## Se algo der errado

> [!PROBLEM] A resposta é "comando não encontrado: info" nos outros sistemas, e no FreeBSD "info: não encontrado. Tente: apropos <word>, ou man intro".
> info existe apenas no Linux. Use man <command> no UNIX System V e no FreeBSD, ou help <command> no MC-DOS, MC-NET e Frames.

> [!PROBLEM] A resposta é "No menu item" e um nome, depois que você pressiona m.
> O nome deve corresponder a um item de menu deste nó. Pressione d ou t para ir para outro lugar, ou escolha um link com Tab.

> [!PROBLEM] A resposta é "Não há ponteiro 'Próximo' para este nó.", ou a mesma mensagem para Prev ou Up.
> Este nó não tem vizinho nessa direção. Use n, p, u ou l para ir para outro lugar.

> [!PROBLEM] A resposta é "Não há nós anteriores."
> Você ainda não leu outro nó, então não há para onde voltar. Leia um nó com n primeiro.

> [!PROBLEM] A resposta é "Falha na busca:" seguido do texto que você procurou.
> O texto não está neste manual. Tente uma palavra mais curta.

> [!SEE] jsc:man_pages, jsc:help_command
