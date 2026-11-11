---
section: help_at_the_prompt
order: 281
icon: jsc:guide_to_operations
---
# O comando man

O man mostra a página de manual de um comando. Funciona nos sistemas UNIX: Linux, UNIX System V e FreeBSD. No MC-DOS, MC-NET e Frames, use help.

Quando os manuais da série têm uma página para o comando, o man abre essa página no paginador. Quando não têm nenhuma, o man imprime a própria página do comando no terminal, em maiúsculas, como LS(1).

No UNIX e no FreeBSD, o man também lê as entradas dos manuais, que você pode abrir pelo nome. Uma entrada nomeada pela sua última parte, como graphics-cards, abre no paginador como uma página da seção 7. A entrada sobre placas de vídeo é uma delas ([](jsc:graphics_cards)).

## Usando o man

1. Digite man e o nome de um comando, como man ls no Linux.
2. Leia a página, depois pressione q para voltar ao prompt.
3. No UNIX ou no FreeBSD, digite man graphics-cards para ler a entrada sobre placas de vídeo.

Table: Teclas no paginador

| Tecla | O que faz |
| --- | --- |
| `Space` | Avança uma página. |
| `b` | Volta uma página. |
| `j or Down` | Avança uma linha. |
| `k or Up` | Volta uma linha. |
| `Page Down, Page Up` | Move uma página para um dos dois lados. |
| `g or Home` | Vai para o início da página. |
| `G or End` | Vai para o fim da página. |
| `/` | Inicia uma busca pelo texto que você digitar. |
| `n` | Encontra a próxima ocorrência da busca. |
| `q or Escape` | Devolve o terminal. |

A linha do rodapé mostra o nome da página com (7), a linha em que você está, e "(pressione q para sair)".

Quando você digita uma palavra depois do man, ele procura uma correspondência nesta ordem, e para na primeira que encontrar.

Table: A ordem que o man segue

| Passo | O que o man mostra |
| --- | --- |
| Um comando deste sistema, com uma página nos manuais | Essa página, aberta no paginador. |
| Uma entrada dos manuais, no UNIX e no FreeBSD | A entrada, aberta no paginador. |
| Um comando deste sistema, sem página nos manuais | A própria página do comando, impressa no terminal em maiúsculas, como LS(1). |
| Um tópico como intro, no FreeBSD | A página do tópico, impressa no terminal como INTRO(1). |
| Nenhum destes | "Não há entrada no manual para" e a palavra que você digitou. |

```command
id: jsc:command/linux/man
```

> [!DEFINE] seção
> O número entre parênteses depois do nome de uma página: (1) para um comando, e (7) para uma entrada de manual.

> [!DEFINE] paginador
> Uma tela que mostra um texto longo uma tela por vez, com teclas para se mover por ele.

## Se algo der errado

> [!PROBLEM] A resposta é "Não há entrada no manual para graphics-cards" no Linux.
> No Linux, o man lê apenas comandos. Leia a entrada com info ([](jsc:info_keys)) em vez disso.

> [!PROBLEM] A resposta é "Não há entrada no manual para graphics".
> Digite o nome inteiro, graphics-cards. Uma parte do nome não encontra nada.

> [!PROBLEM] A resposta é "Qual página do manual você quer?".
> Você digitou man sem nome. Digite man e o nome de um comando, como man ls no Linux.

> [!PROBLEM] A dica embaixo sugere "man intro", e isso dá "Não há entrada no manual para intro".
> O tópico intro existe apenas no FreeBSD. No Linux e no UNIX System V, use help ou apropos.

> [!SEE] jsc:whatis_and_apropos, jsc:info_keys, jsc:graphics_cards
