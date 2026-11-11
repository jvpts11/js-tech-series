---
section: gentoo_base
order: 322
---
# A árvore de pacotes, o perfil e o @world

O Portage instala programas a partir de receitas. A árvore de pacotes é a coleção dessas receitas, uma para cada programa.

> [!DEFINE] ebuild
> A receita de compilação de um programa: de onde buscar o código-fonte e como compilá-lo.

A árvore vem de um snapshot datado do repositório. O Portage confere o snapshot com sua soma de verificação e sua assinatura GNU Privacy Guard (GPG). Você obtém a árvore com emerge-webrsync, que baixa o snapshot, ou com emerge --sync, que a copia por rsync. Qualquer um dos dois funciona quando ainda não há árvore.

1. Dentro do novo sistema, digite emerge-webrsync e pressione [[Enter]].
2. Digite eselect profile list e pressione [[Enter]] para ver os perfis.
3. Digite eselect profile set, depois o número que você quer, e pressione [[Enter]].
4. Digite emerge --ask --verbose --update --deep --newuse @world e pressione [[Enter]].

> [!DEFINE] perfil
> Um conjunto nomeado de escolhas padrão de compilação. Ele diz como os programas são compilados, e não instala nada.

O perfil define os padrões que toda compilação segue. Escolha um desta tabela.

Table: Os perfis

| Número | Perfil |
| --- | --- |
| 1 | `default/linux/vel64/23.0` |
| 2 | `default/linux/vel64/23.0/systemd` |
| 3 | `default/linux/vel64/23.0/desktop` |
| 4 | `default/linux/vel64/23.0/desktop/gnome` |
| 5 | `default/linux/vel64/23.0/desktop/plasma` |
| 6 | `default/linux/vel64/23.0/no-multilib` |
| 7 | `default/linux/vel64/23.0/hardened` |

Escolha um perfil com eselect profile set, mesmo que o perfil 1 sirva para você.

> [!DEFINE] @world
> O conjunto de pacotes que o sistema quer. Atualizá-lo traz cada um para a versão mais recente.

O comando de atualização compila todo pacote do @world que esteja desatualizado. Cada pacote tem flags USE, que são opções que ligam ou desligam seus recursos opcionais.

> [!DEFINE] Flag USE
> Uma opção que liga ou desliga um recurso opcional de um programa na hora da compilação.

Table: Os pacotes do @world

| Pacote e versão | Arquivo e tamanho | Flags USE |
| --- | --- | --- |
| `sys-libs/ncurses 6.5_p20250125` | ncurses-6.5.tar.gz, 3,6 MB; substitui 6.4_p20240414 | Ativos: cxx, tinfo. Desativados: debug, doc. |
| `app-shells/bash 5.2_p37` | bash-5.2.tar.gz, 10,7 MB; substitui 5.2_p26 | Ativos: net, nls, readline. Desativados: afs, plugins. |
| `app-editors/vim 9.1.0794` | vim-9.1.0794.tar.gz, 16,8 MB; não substitui nada | Ativos: acl, crypt, nls. Desativados: X, lua, perl. |

Com --ask, o Portage para e pergunta: Você quer fazer o merge destes pacotes? [Yes/No]. Uma resposta que começa com n encerra, e a tela mostra "Saindo". Qualquer outra resposta continua, inclusive Enter.

Em seguida, cada pacote passa pelas suas fases. Ele baixa o arquivo do Mirror, desempacota o código-fonte, o prepara, o configura, o compila, o instala e o faz merge no sistema.

## Se algo der errado

> [!PROBLEM] emerge diz "a árvore do Portage está vazia".
> Execute emerge-webrsync, ou emerge --sync, antes de fazer qualquer merge.

> [!PROBLEM] emerge diz "Não foi possível resolver mirror://mainframe".
> Nenhum Mainframe que execute o [Mirror](jsc:mirror) está nesta rede. Conecte-se a um e tente de novo.

> [!PROBLEM] emerge exibe "comando não encontrado" quando é executado fora do novo sistema, já que o gerenciador de pacotes é o do novo sistema.
> Entre primeiro no novo sistema: chroot /mnt/gentoo /bin/bash.

> [!PROBLEM] emerge diz "Nenhum pacote informado."
> Digite o nome de pelo menos um pacote depois de emerge.

> [!PROBLEM] emerge diz que não há ebuilds que satisfaçam o nome que você digitou.
> Confira a grafia. O nome precisa ser um programa que o Mirror tem ([](jsc:building_from_source)).

> [!PROBLEM] eselect diz que o alvo não parece ser válido.
> Digite um número da lista que eselect profile list mostra.

> [!SEE] jsc:make_conf, jsc:kernel_gentoo, jsc:building_from_source
