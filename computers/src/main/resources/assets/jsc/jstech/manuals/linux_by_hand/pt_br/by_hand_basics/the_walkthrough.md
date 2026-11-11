---
section: by_hand_basics
order: 302
---
# O passo a passo na mídia

A mídia traz o próprio passo a passo: os passos do guia da distribuição, escritos em ordem, como um arquivo simples. Você não precisa de um guia de fora do jogo para segui-lo.

> [!DEFINE] Ferramenta
> Um comando que leva tempo e segura o terminal até terminar, como o pacstrap, ou o emerge no Gentoo.

O passo a passo fica em /root/install.txt, na mídia e não no seu disco, então ele está lá seja qual for o conteúdo do disco. Para encontrá-lo e lê-lo:

1. Digite help e pressione [[Enter]].
2. Digite less /root/install.txt e pressione [[Enter]].
3. Leia o passo em que você está e depois digite-o no prompt.
4. Confira o que o shell respondeu e só então digite o próximo passo.

O passo a passo lista os comandos em ordem. Alguns passos trazem uma marca, e a tabela mostra o que cada marca significa.

Table: Marcas no passo a passo

| Marca | O que significa |
| --- | --- |
| `*` | Um passo sem o qual o sistema não consegue inicializar. |
| `modern firmware only` | Uma nota sob um passo: o passo serve para um computador Standard ou mais novo. Em firmware mais antigo ele é deixado de fora. |

A mídia o cumprimenta ao iniciar: "Digite 'help' para o passo a passo da instalação." Ela faz login como root sozinha, então você não digita nome nem senha.

## O que o help imprime

```session
{"lines": [{"prompt": "root@archiso ~ # ", "text": "help", "typed": true}, {"text": "This is the Arch Linux installation medium."}, {"text": "Nothing here installs itself: the steps are typed by hand, in"}, {"text": "order, and every tool is the real one."}, {"text": ""}, {"text": "The whole walkthrough is in /root/install.txt"}, {"text": "    less /root/install.txt"}, {"text": "A tool that is running can be stopped with CTRL+C."}], "look": "jsc:guide/terminal/zsh", "title": "Arch Linux"}
```

No Gentoo, a mesma ajuda cita o Gentoo, e o prompt mostra livecd em vez de root@archiso.

## As configurações de cada passo

O servidor pode pedir mais antes de um reboot. Cada distribuição tem uma configuração, desligada por padrão, guardada em jscomputers-server.toml ao lado do mundo. A tabela lista o que cada uma acrescenta.

Table: As configurações de cada passo

| Configuração | Padrão | Quando está ligada, o reboot também pede |
| --- | --- | --- |
| Cada passo do Arch | Desligado | O relógio de hardware definido, e um nome para a máquina. |
| Cada passo do Gentoo | Desligado | As montagens bind, um perfil, a atualização @world, o link do kernel e um nome para a máquina. |

Essas configurações mudam apenas o que o reboot recusa. Cada comando ainda responde do jeito da ferramenta real, então você digita os mesmos comandos de qualquer forma. A lista básica que o reboot sempre verifica está em [Quando o reboot recusa](jsc:when_reboot_refuses).

## Se algo der errado

> [!PROBLEM] O reboot responde: "O sistema novo não vai inicializar:", com uma lista embaixo.
> Cada linha diz o que está faltando e o passo que o fornece, entre colchetes. Faça esse passo e depois digite reboot de novo.

> [!PROBLEM] O reboot responde: "reboot: você está dentro do sistema novo. saia primeiro."
> Digite exit para sair do sistema novo, depois digite reboot.

> [!SEE] jsc:installing, jsc:the_systems
