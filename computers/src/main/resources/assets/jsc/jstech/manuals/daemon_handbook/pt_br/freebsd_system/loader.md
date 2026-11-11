---
section: freebsd_system
order: 334
---
# O carregador e as linhas de inicialização

Depois do firmware, o computador executa o carregador. O carregador é o programa no início do disco que carrega o kernel, que é o núcleo do sistema. Ele é o gerenciador de inicialização do FreeBSD, e pode mostrar um menu curto por cinco segundos antes de iniciar o sistema.

> [!DEFINE] Carregador
> O programa no início do disco que carrega o kernel.

> [!DEFINE] Kernel
> O núcleo do sistema, e a primeira coisa que as linhas de inicialização citam.

> [!DEFINE] Linhas de inicialização
> O texto que o sistema imprime no caminho para cima, quando inicia, e no caminho para baixo, quando para.

O carregador não escolhe entre sistemas. Ele inicia o FreeBSD a partir do disco de sistema e não lista nenhum outro sistema. Se você quiser outro sistema no mesmo computador, use a ordem de inicialização do firmware ou o Menu de Inicialização dele ([](jsc:firmware), [](jsc:boot_menu)).

## O menu do carregador

Table: As entradas do menu do carregador

| Entrada | O que faz |
| --- | --- |
| Boot | Inicia o sistema em execução. É o padrão: Enter o inicia. |
| Reboot | Inicia a máquina de novo. |
| Firmware settings | Abre as configurações do firmware, mostradas apenas no hardware Standard e posteriores, que usa UEFI (a Unified Extensible Firmware Interface). |

O menu faz a contagem regressiva a partir de 5 segundos. A linha de cima diz "Bem-vindo a FreeBSD", e a linha abaixo diz "Inicialização automática em 5 segundos. [Espaço] para pausar".

Qualquer tecla interrompe a contagem, embora a linha cite a barra de espaço. Depois que a contagem para, pressione [[Enter]] para iniciar a entrada padrão, ou um número de 1 a 9 para iniciar essa entrada na hora. Teclas além das entradas não fazem nada.

A configuração do servidor boot.show_boot_menu liga ou desliga o menu. Ela vem ligada por padrão. Quando está desligada, o computador inicia o sistema na hora.

## As linhas de inicialização na subida

Na subida, o FreeBSD imprime um conjunto fixo de linhas. Cada linha aparece somente quando sua condição é atendida, como indica a última coluna.

```table
{"caption": "As linhas de inicialização na subida", "heads": ["Linha", "Quando aparece"], "rows": [[{"literal": "Loading kernel..., then Booting..."}, {"words": "No início."}], [{"literal": "FreeBSD 14.1-RELEASE GENERIC <architecture>"}, {"words": "Sempre."}], [{"literal": "CPU: <processor name>"}, {"words": "Sempre: o processador do computador."}], [{"literal": "real memory  = <n> MB"}, {"words": "Sempre: a memória que o computador tem."}], [{"literal": "ada0: <<disk name>>"}, {"words": "Uma linha para cada disco, depois ada1 e assim por diante."}], [{"literal": "Trying to mount root from ufs:/dev/ada0p2 [rw]..."}, {"words": "Sempre: o disco próprio do sistema."}], [{"literal": "Setting hostname: <hostname>."}, {"words": "Sempre."}], [{"literal": "Starting Network: lo0 em0."}, {"words": "Só quando um cabo de dados chega ao computador."}], [{"literal": "pkg: repository Mirror found on <mirror host>."}, {"words": "Só quando um Mirror responde."}], [{"literal": "Starting devd."}, {"words": "Sempre."}], [{"literal": "Starting syslogd."}, {"words": "Sempre."}], [{"literal": "Starting cron."}, {"words": "Só quando o cron está ligado."}], [{"literal": "Starting sshd."}, {"words": "Só quando o acesso remoto é permitido."}], [{"literal": "Starting <desktop> display manager."}, {"words": "Só quando uma área de trabalho está instalada."}]]}
```

## As linhas de inicialização na descida

Quando você para ou reinicia o computador, o FreeBSD imprime o caminho para baixo. Ele para cada serviço em execução e sincroniza os discos. A última linha diz "Reiniciando..." quando o computador reinicia, e "O sistema operacional foi parado." quando para.

## Quando o arquivo do sistema está faltando

O carregador inicia o arquivo boot/kernel, na pasta boot do disco de sistema. Sem esse arquivo, o FreeBSD não consegue iniciar. Para repará-lo, instale por cima a partir de uma mídia.

## Se algo der errado

> [!PROBLEM] O computador espera no menu do carregador, e nada inicia.
> Pressione Enter para iniciar o Boot, ou espere 5 segundos para que ele inicie sozinho.

> [!PROBLEM] O computador inicia um sistema que não é o FreeBSD.
> O carregador inicia apenas o FreeBSD. Mude a ordem de inicialização do firmware, ou use o Menu de Inicialização dele ([](jsc:firmware)).

> [!PROBLEM] O menu do carregador não aparece.
> A configuração do servidor boot.show_boot_menu está desligada. Ligue-a para mostrar o menu de novo.

> [!PROBLEM] O arquivo do sistema boot/kernel está faltando no disco do sistema.
> Inicialize uma mídia de instalação e instale por cima para reparar.

> [!SEE] jsc:firmware, jsc:boot_menu, jsc:bsdinstall
