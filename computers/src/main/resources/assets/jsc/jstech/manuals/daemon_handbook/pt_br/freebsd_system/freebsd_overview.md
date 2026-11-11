---
section: freebsd_system
order: 332
---
# FreeBSD neste computador

FreeBSD é um sistema semelhante ao UNIX, e o Daemon Handbook trata dele e do UNIX System V. No seu computador ele funciona como os outros sistemas: você o instala a partir de uma mídia de instalação, o inicializa e trabalha no seu prompt. Ele inicia em um prompt de texto, e uma área de trabalho é um pacote extra que você pode adicionar depois.

> [!DEFINE] Sistema de terminal
> Um sistema que inicia em um prompt de texto. Uma área de trabalho é um pacote opcional.

O FreeBSD precisa de hardware Legacy ou mais novo, a primeira era de hardware em que ele roda. Um computador da era Vintage não consegue instalá-lo. O sistema instalado ocupa 2.048 MB de espaço em disco, e 16 MB de memória enquanto roda.

> [!DEFINE] Era de hardware
> A geração do hardware de um computador, de Vintage a Singularity. Legacy é a primeira em que o FreeBSD roda.

```table
{"caption": "O FreeBSD em resumo", "rows": [[{"words": "Era mínima de hardware"}, {"words": "Legacy"}], [{"words": "Espaço em disco do sistema instalado"}, {"amount": 2048, "unit": "MB"}], [{"words": "Memória durante a execução"}, {"amount": 16, "unit": "MB"}], [{"words": "Kernel"}, {"words": "FreeBSD 14.1-RELEASE GENERIC"}], [{"words": "Shell"}, {"words": "sh"}], [{"words": "Gerenciador de pacotes"}, {"words": "pkg"}], [{"words": "Console"}, {"words": "ttyv0"}]]}
```

## O que você vê ao iniciar

Quando um prompt abre, o FreeBSD dá as boas-vindas com as linhas abaixo. A primeira linha indica a arquitetura: IA-32, a arquitetura Intel de 32 bits, em um computador de 32 bits, e vel64 em um de 64 bits.

```session
{"lines": [{"text": "FreeBSD/vel64 (<hostname>) (ttyv0)"}, {"text": ""}, {"text": "login: <your name>"}, {"text": "FreeBSD 14.1-RELEASE (GENERIC)"}, {"text": ""}, {"text": "Welcome to FreeBSD, <your name>."}, {"text": "  Find a command by what it does:"}, {"text": "    apropos <word>"}, {"text": "  Learn the system:"}, {"text": "    man intro"}, {"text": "  See every command here:"}, {"text": "    Tab, twice"}, {"text": ""}, {"text": "Tip: pkg info lists every package installed here."}], "look": "jsc:guide/terminal/sh", "title": "FreeBSD"}
```

A saudação indica três formas de se orientar. A última seção deste capítulo explica cada uma delas ([](jsc:freebsd_help)).

## Áreas de trabalho no FreeBSD

Você adiciona uma área de trabalho depois, ou escolhe uma durante a instalação. A área de trabalho só é oferecida quando o Mirror da rede responde e o hardware do seu computador é novo o bastante para ela.

> [!DEFINE] Mirror
> O servidor de pacotes da rede, executado por um Mainframe que roda o serviço Mirror.

Table: Áreas de trabalho no FreeBSD

| Área de trabalho | Comando | Precisa de no mínimo |
| --- | --- | --- |
| KDE Plasma | `kde-plasma` | Era Legacy, 256 MB de disco, 224 MB de memória |
| GNOME | `gnome` | Era Legacy, 192 MB de disco, 256 MB de memória |
| CDE | `cde` | Era Legacy, 32 MB de disco, 16 MB de memória |
| Cinnamon | `cinnamon` | Era Standard, 160 MB de disco, 160 MB de memória |

As áreas de trabalho de todos os sistemas são descritas em [Áreas de trabalho](jsc:desktops).

## Se algo der errado

> [!PROBLEM] O instalador diz "O FreeBSD precisa de hardware da era Legacy ou mais novo; esta máquina é da era Vintage."
> Instale o FreeBSD em um computador Legacy, ou em um mais novo.

> [!PROBLEM] O instalador diz "Nenhum disco está instalado para colocar FreeBSD nele."
> Coloque um disco em um dos slots do computador.

> [!PROBLEM] O instalador diz "Nenhum disco desta máquina tem espaço para ele."
> Use um disco maior, ou apague um disco na página de Particionamento ([](jsc:bsdinstall)).

> [!SEE] jsc:the_systems, jsc:installing, jsc:bsdinstall
