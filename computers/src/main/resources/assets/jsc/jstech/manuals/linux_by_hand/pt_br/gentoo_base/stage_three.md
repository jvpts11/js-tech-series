---
section: gentoo_base
order: 319
---
# O arquivo do estágio 3

Um estágio 3 é um arquivo pronto de um sistema Gentoo base: os arquivos básicos, as ferramentas e as bibliotecas, antes do kernel e antes de qualquer programa que você escolheu. Você o baixa e descompacta no disco do novo sistema, assim não precisa compilar a base a partir do código-fonte por conta própria.

> [!DEFINE] estágio 3
> Um arquivo tar de um sistema Gentoo base, pronto para ser descompactado no lugar de ser construído.

> [!DEFINE] make.conf
> O arquivo de configurações de compilação que o Portage lê antes de compilar um programa.

O nome diz o que ele contém. Em stage3-vel64-openrc.tar.xz, vel64 é a arquitetura do processador, e openrc é o sistema de inicialização (init), o programa que inicia os serviços no boot.

O disco precisa estar montado antes ([](jsc:mounts_and_chroot)). O download salva o arquivo na pasta em que você está, e a mídia ao vivo começa você em /root, então vá para /mnt/gentoo antes de baixar.

## Baixando e descompactando

1. Digite cd /mnt/gentoo e pressione [[Enter]].
2. Digite wget mirror://mainframe/gentoo/stage3-vel64-openrc.tar.xz e pressione [[Enter]].
3. Digite tar xpvf stage3-\*.tar.xz --xattrs-include='\*.\*' e pressione [[Enter]].
4. Aguarde o prompt voltar. A descompactação demora mais em disco lento.

```session
{"lines": [{"prompt": "livecd /mnt/gentoo # ", "text": "wget mirror://mainframe/gentoo/stage3-vel64-openrc.tar.xz", "typed": true}, {"text": "--<date>--  mirror://mainframe/gentoo/stage3-vel64-openrc.tar.xz"}, {"text": "Resolving mainframe... done."}, {"text": "Connecting to mainframe... connected."}, {"text": "Mirror request sent, awaiting response... 200 OK"}], "look": "jsc:guide/terminal/bash", "title": "Gentoo"}
```

A primeira linha da resposta traz a data. Em seguida, uma barra de progresso se preenche enquanto o arquivo é baixado, e a tela avisa quando o arquivo é salvo.

As letras depois de tar importam. x descompacta o arquivo, p mantém as permissões de arquivo, e f indica o arquivo compactado que vem a seguir. A letra v imprime cada caminho conforme ele é descompactado. Sem v, a tela fica quieta até o fim da descompactação. --xattrs-include mantém as informações extras de arquivo de que alguns programas precisam.

Quando a descompactação termina, o novo sistema tem as suas pastas, entre elas /etc, /boot, /usr e /var. Ele também tem dois arquivos iniciais: /etc/portage/make.conf, e um /etc/fstab que contém apenas um cabeçalho. Descompactar o arquivo de novo desfaz todos os passos seguintes, por isso a lista de verificação os pede de novo.

## Se algo der errado

> [!PROBLEM] O download diz "Nome ou serviço desconhecido".
> Nenhum Mainframe executando o Mirror está nesta rede, ou o endereço não começa com mirror://mainframe.

> [!PROBLEM] O download diz "ERROR 404: Not Found."
> O endereço deve terminar em /gentoo/stage3-vel64-openrc.tar.xz, escrito exatamente assim.

> [!PROBLEM] O tar diz "Recusando covardemente a criar um arquivo vazio".
> O comando não cita nenhum arquivo do estágio 3. Digite o comando inteiro, com stage3 no nome do arquivo.

> [!PROBLEM] O tar diz "Não é possível abrir: Arquivo ou diretório inexistente".
> O arquivo está em /root, a pasta em que você começou. Digite cd /mnt/gentoo, e depois baixe o estágio 3 de novo ali.

> [!PROBLEM] O tar diz "recusando descompactar um sistema inteiro sobre a mídia ao vivo".
> O disco não está montado em /mnt/gentoo, ou você não está nessa pasta. Monte-o ([](jsc:mounts_and_chroot)), e depois digite cd /mnt/gentoo.

> [!SEE] jsc:mounts_and_chroot, jsc:make_conf, jsc:installing
