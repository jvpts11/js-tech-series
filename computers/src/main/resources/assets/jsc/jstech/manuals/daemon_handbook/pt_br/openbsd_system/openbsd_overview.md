---
section: openbsd_system
order: 342
---
# O OpenBSD neste computador

O OpenBSD é um sistema tipo UNIX feito pela Blowfish Project, e é seguro por padrão: inicia no terminal só com o que precisa ligado, e todo o resto espera você ligar. A sua marca é o Puffy, o baiacu.

> [!DEFINE] Seguro por padrão
> Um sistema que sobe com o mínimo rodando, para que nada rode sem você pedir.

O OpenBSD precisa de hardware Legacy ou mais novo. Ocupa 1.024 MB de disco, menos quando você deixa sets de fora no instalador, e 12 MB de memória enquanto roda.

```table
{"caption": "O OpenBSD num relance", "rows": [[{"words": "Era mínima de hardware"}, {"words": "Legacy"}], [{"words": "Espaço em disco do sistema instalado"}, {"amount": 1024, "unit": "MB"}], [{"words": "Memória enquanto roda"}, {"amount": 12, "unit": "MB"}], [{"words": "Kernel"}, {"words": "OpenBSD 7.5 GENERIC#79"}], [{"words": "Shell"}, {"words": "ksh"}], [{"words": "Gerenciador de pacotes"}, {"words": "pkg_add"}], [{"words": "Console"}, {"words": "ttyC0"}]]}
```

## O que você vê quando ele inicia

O seu prompt é o nome do computador e uma cerquilha, porque você é root. A saudação diz os caminhos para se achar: o apropos e a página afterboot só aparecem quando o set man está instalado.

```session
{"lines": [{"text": "OpenBSD/IA-32 (<hostname>.my.domain) (ttyC0)"}, {"text": ""}, {"text": "login: player"}, {"text": "OpenBSD 7.5 (GENERIC) #79"}, {"text": ""}, {"text": "Welcome to OpenBSD: The proactively secure Unix-like operating system."}, {"text": "  Find a command by what it does:"}, {"text": "    apropos <word>"}, {"text": "  Learn the system:"}, {"text": "    man afterboot"}, {"text": ""}, {"text": "Tip: rcctl ls on lists every service switched on here."}], "look": "jsc:guide/terminal/sh", "title": "OpenBSD"}
```

## Uma área de trabalho no OpenBSD

O OpenBSD aceita as mesmas áreas de trabalho que o FreeBSD. Uma área de trabalho instalada ainda sobe no terminal: o seu gerenciador de exibição, o xenodm, só a inicia depois que você digita rcctl enable xenodm ([](jsc:openbsd_daemons)). Toda área de trabalho pendura a mesma imagem do mar.

## Se algo der errado

> [!PROBLEM] Há uma área de trabalho instalada e o computador ainda inicia no terminal.
> O xenodm está desligado, que é como o OpenBSD vem. Digite rcctl enable xenodm, e rcctl start xenodm para subi-la agora.

> [!PROBLEM] O prompt diz ksh: man: not found.
> O set man foi deixado de fora no instalador. Instale o sistema de novo com ele.

> [!SEE] jsc:the_systems, jsc:openbsd_installer, jsc:openbsd_daemons
