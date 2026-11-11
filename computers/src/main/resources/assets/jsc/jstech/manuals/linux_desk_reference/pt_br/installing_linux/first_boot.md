---
section: installing_linux
order: 290
---
# A primeira inicialização

Quando o instalador termina e o computador reinicia, o sistema instalado inicia a partir do próprio disco. Você vê uma lista de linhas enquanto o kernel e os serviços iniciam, depois uma linha de login no console de texto, a primeira tela da máquina.

```text
{"text": "A inicialização começa com \"Carregando Linux 6.8-jsc ...\" e \"Carregando ramdisk inicial ...\". As linhas que começam com \"[  OK  ]\" informam que um serviço foi iniciado. O sistema é executado pelo systemd."}
```

> [!DEFINE] Systemd
> O gerenciador de serviços desses sistemas. Ele inicia os serviços na inicialização, em ordem.

```table
{"caption": "Linhas do início", "heads": ["Linha", "Quando aparece"], "rows": [[{"literal": "Loading Linux 6.8-jsc ..."}, {"words": "Sempre, primeiro."}], [{"literal": "Linux version 6.8-jsc (x86_64)"}, {"words": "Sempre. Em uma máquina de 32 bits, aparece i686."}], [{"literal": "CPU: <processor model>"}, {"words": "Sempre: a unidade central de processamento (CPU) da máquina."}], [{"literal": "Memory: <MB> MB available"}, {"words": "Sempre: a memória da máquina."}], [{"literal": "sda: <disk name>"}, {"words": "Uma linha para cada disco, com as letras sda, sdb e assim por diante."}], [{"literal": "[  OK  ] Started Journal Service."}, {"words": "Sempre."}], [{"literal": "[  OK  ] Reached target Local File Systems."}, {"words": "Sempre."}], [{"literal": "[  OK  ] Reached target Network is Online."}, {"words": "Só quando um cabo de dados liga o computador a um Mainframe."}], [{"literal": "[  OK  ] Found package mirror on <Mainframe name>."}, {"words": "Só quando esse Mainframe executa o Mirror."}], [{"literal": "Starting kde plasma Display Manager..."}, {"words": "Só quando uma área de trabalho está instalada, com o nome em minúsculas: kde plasma, gnome ou cinnamon."}]]}
```

Sem área de trabalho não há linha do gerenciador de exibição. A inicialização termina no console de texto: um banner, um login e um shell, e nada mais.

> [!DEFINE] Gerenciador de exibição
> O programa que mostra o login gráfico e inicia a área de trabalho. Aqui ele só inicia quando uma área de trabalho está instalada.

O console de texto imprime uma saudação e depois espera na linha de login. Esta é a saudação do Ubuntu. Debian e Fedora imprimem as mesmas linhas com o próprio nome.

```session
{"lines": [{"text": "Ubuntu <hostname> tty1"}, {"text": ""}, {"text": "<hostname> login: player"}, {"text": "Password:"}, {"text": "Welcome to Ubuntu (Linux 6.8-jsc x86_64)"}, {"text": ""}], "look": "jsc:guide/terminal/bash", "title": "Ubuntu"}
```

O login é uma linha, e nada verifica a senha pedida, porque nada neste mundo faz login com senha ainda. Você é sempre o usuário player.

> [!DEFINE] Login
> A etapa em que o sistema pergunta quem está usando o computador, antes de iniciar uma sessão para esse usuário.

## Se algo der errado

> [!PROBLEM] O sistema inicia em um shell, sem área de trabalho.
> Instale uma área de trabalho com apt install gnome no Ubuntu ou Debian, ou dnf install gnome no Fedora, e depois reinicie. O Cinnamon exige hardware Standard.

> [!PROBLEM] A inicialização não mostra a linha "Found package mirror".
> O computador não está em uma rede cujo Mainframe executa o Mirror. Conecte o cabo de dados dele a esse Mainframe ([](jsc:data_cables)).

> [!SEE] jsc:apt, jsc:dnf, jsc:mirror_and_packages
