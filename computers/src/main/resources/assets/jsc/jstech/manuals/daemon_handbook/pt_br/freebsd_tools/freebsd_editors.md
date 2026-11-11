---
section: freebsd_tools
order: 339
---
# Editores no FreeBSD

O FreeBSD vem com dois editores para arquivos de texto: o ee, o menor, e o vi, o editor do UNIX. Os dois já estão no sistema, então você nunca precisa instalá-los.

Table: Os dois editores

| Editor | O que ele é no FreeBSD |
| --- | --- |
| `ee <file>` | O editor próprio do FreeBSD. Ele mostra os seus atalhos nas linhas do topo da tela. |
| `vi <file>` | O vi do FreeBSD é o nvi. Ele não tem cor nem régua. |

Cada editor tem as suas próprias páginas, com as teclas e os passos: [ee](jsc:ee) e [vi](jsc:vi), e as duas estão em [Editores no prompt](jsc:editors_overview). Esta página diz apenas o que é específico do FreeBSD.

## As mensagens de cada editor

O ee mostra "<name>" [Novo arquivo] quando o arquivo que você nomeou é novo. Depois de salvar, ele mostra "<name>" com o número de linhas e o número de caracteres.

O vi mostra <name>: arquivo novo: linha 1 para um arquivo novo, e <name>: sem alterações: linha 1 para um arquivo que já existe. Depois de gravar, ele mostra o número de linhas e de caracteres.

Para sair do ee, pressione [[Esc]] para abrir o menu dele, e depois pressione [[Enter]] para escolher sair do editor. A tela mostra a mesma dica: ESC-Enter: sair. No vi, pressione [[Esc]] para voltar ao modo normal, e depois digite :wq para gravar e sair.

## Se algo der errado

> [!PROBLEM] O ee diz Comando desconhecido no prompt de comandos.
> O prompt de comandos aceita write, quit e exit. Digite um deles.

> [!PROBLEM] O vi diz E37: sem gravação desde a última alteração (adicione ! para forçar).
> Digite :w para gravar primeiro, ou :q! para sair sem gravar.

> [!PROBLEM] O vi diz E492: não é um comando do editor.
> Digite um comando da página do vi ([](jsc:vi)).

> [!PROBLEM] O que você digita não aparece como texto no vi.
> Pressione i para começar a inserir texto, e Esc para parar de inserir.

> [!SEE] jsc:ee, jsc:vi, jsc:editors_overview
