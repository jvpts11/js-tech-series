---
section: starting_a_computer
order: 6
icon: jsc:floppy_disk
---
# O Menu de Inicialização e o gerenciador de inicialização

O Menu de Inicialização inicia um disco ou mídia só para esta inicialização. Pressione [[F12]] enquanto o computador inicia ([](jsc:turning_on)). Você escolhe com as chaves abaixo.

Table: As chaves do Menu de Inicialização

| Chave | O que faz |
| --- | --- |
| `Up and Down` | Selecione uma entrada na lista. |
| `Enter` | Inicialize a entrada selecionada. |

```text
{"text": "A tela tem o título Menu de Inicialização. Em uma máquina com BIOS azul, ela mostra \"Menu de Inicialização  (somente nesta inicialização)\", e a lista dela tem o cabeçalho \"Menu de Inicialização:\"."}
```

O computador não salva a escolha, então a próxima inicialização segue a ordem de inicialização ([](jsc:firmware)) de novo.

## Gerenciadores de inicialização

O gerenciador de inicialização é um menu diferente do Menu de Inicialização. Alguns sistemas mostram o seu próprio menu de início antes de iniciar: o GRUB faz isso no Linux, o Gerenciador de Inicialização Midsoft faz no Frames, e o FreeBSD tem o seu próprio carregador. O gerenciador de inicialização deixa você escolher entre os sistemas de um disco ([](jsc:two_systems)).

Se você não fizer nada, o gerenciador de inicialização faz a contagem regressiva e depois inicia sozinho a entrada destacada, após 5 segundos. Pressione Enter para iniciar a entrada destacada imediatamente.

A ajuda dele diz: "Use as teclas Para cima e Para baixo para escolher qual entrada fica destacada. Pressione Enter para inicializar a entrada selecionada."

Em um servidor, a configuração boot.show_boot_menu decide se a máquina para no gerenciador de inicialização toda vez que liga. Ela vem ligada por padrão. Desligue-a, e o sistema escolhido inicia de imediato. A configuração fica no arquivo de configuração do jscomputers-server.

> [!DEFINE] Gerenciador de inicialização
> O menu de início próprio do sistema, exibido antes de o sistema começar. Ele deixa você escolher um dos sistemas de um disco.

## Se algo der errado

> [!PROBLEM] F12 não faz nada.
> O Menu de Inicialização só abre quando um disco ou mídia pode iniciar o computador ([](jsc:no_bootable_device)).

> [!PROBLEM] O gerenciador de inicialização inicia o sistema errado.
> Pressione Para cima ou Para baixo para destacar o sistema que você quer, depois pressione Enter antes que a contagem termine.

> [!PROBLEM] O computador para no gerenciador de inicialização toda vez que liga.
> Desligue boot.show_boot_menu no arquivo de configuração do jscomputers-server.

> [!SEE] jsc:two_systems, jsc:help_on_a_computer, jsc:firmware
