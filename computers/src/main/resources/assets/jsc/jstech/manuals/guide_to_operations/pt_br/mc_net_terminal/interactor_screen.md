---
section: mc_net_terminal
order: 112
icon: jsc:monitor
---
# O Interactor

O Interactor é a janela da rede em tela cheia no MC-NET. Sozinho, ocupa o terminal inteiro. Seus cabeçalhos são a rede, os servidores, o que está em retenção, o trabalho em andamento e o que você favoritou, e uma lista fica abaixo deles.

As setas percorrem a lista. Tab leva ao próximo cabeçalho, e digitar procura algo. Na parte de baixo da tela há dez teclas, numeradas de 1 a 10. Cada tecla corresponde à tecla de função do mesmo número: pressione [[F1]] para a tecla 1, até [[F10]] para a tecla 10.

Table: As teclas do Interactor

| Tecla | O que faz |
| --- | --- |
| `F1` | Ajuda. |
| `F2` | Pegar: pega itens da rede e os coloca nas suas mãos. Primeiro pergunta a quantidade. |
| `F3` | Colocar: coloca na rede o que está nas suas mãos. |
| `F4` | Fabricar: pergunta a quantidade e depois inicia a fabricação do item. |
| `F5` | Bloquear: retém o item selecionado. Ele passa a aparecer na lista de retenção. |
| `F6` | Liberar: desbloqueia o item selecionado. |
| `F7` | Favorito: marca o item selecionado com estrela, para que ele apareça em favoritos. |
| `F8` | Parar: para a operação selecionada no cabeçalho Ops. Em qualquer outro cabeçalho, avisa que só operações podem ser paradas. |
| `F9` | Buscar: procura um item enquanto você digita o nome. |
| `F10` | Sair: devolve o terminal. |

As mesmas palavras funcionam no prompt, e fazem as mesmas coisas. Cada uma é mostrada aqui com o que faz.

Table: Palavras no prompt

| Palavras | O que fazem |
| --- | --- |
| `interac` | Abre a rede inteira em tela cheia. |
| `interac list stone --sort count` | Lista stone, com a maior quantidade primeiro. |
| `interac get 42 cobblestone` | Pega 42 cobblestone para as suas mãos. |
| `interac put hand` | Coloca na rede o que está na sua mão. |
| `interac where diamond` | Informa onde o diamante está guardado. |

A opção --to local deixa o que foi pego neste computador, e não nas suas mãos. A opção --sort count ordena uma listagem pela quantidade, e --sort name ordena pelo nome.

Você pode nomear um item do jeito que falaria: cobblestone, minecraft:cobblestone ou "oak log". Um nome que serve para mais de uma coisa recebe como resposta as coisas que ele serve, para que sua próxima linha possa nomear a que você quis dizer.

Com palavras, o Interactor faz a única coisa que você pediu e devolve o prompt. Uma operação que leva tempo mostra seu número, e o resultado aparece antes do próximo prompt.

## Se algo der errado

> [!PROBLEM] O nome não corresponde a nada na rede.
> Confira a grafia, ou nomeie o item de outro jeito, como minecraft:cobblestone.

> [!PROBLEM] A rede não guarda nada.
> Coloque os itens antes, com put.

> [!PROBLEM] interac não é oferecido no prompt.
> A máquina não tem rede. interac, ssh e mirror não são oferecidos em uma máquina sem rede ([](jsc:network_members)).

> [!PROBLEM] A tecla Parar avisa que somente operações podem ser paradas.
> Parar funciona somente no cabeçalho Ops. Selecione antes uma operação ali.
