---
section: gentoo_system
order: 327
---
# Compilando programas a partir do código-fonte

Dentro do sistema novo, o emerge instala programas do mesmo jeito que fez durante a instalação. O Portage compila cada programa a partir do código-fonte na sua máquina, então cada programa leva tempo para ser compilado.

Uma compilação é mais rápida em uma máquina cujo processador tem um clock mais alto. As compilações simultâneas são o menor de dois números: a contagem em MAKEOPTS ([](jsc:make_conf)), e os núcleos da sua máquina. Há sempre pelo menos 1 compilação por vez.

Você nomeia um programa pela categoria e pelo nome, como kde-plasma/plasma-meta, ou só pelo nome, em qualquer caixa de letras. A categoria segue o tipo de programa: aplicativos ficam em app-misc, e áreas de trabalho em x11-wm.

Durante a instalação, o Mirror instala por nome só um programa que roda em Linux, não é um serviço em segundo plano e cabe na máquina e na sua era.

Para instalar uma área de trabalho, digite emerge --ask kde-plasma/plasma-meta. Ele compila estes pacotes nesta ordem:

Table: O que a área de trabalho KDE Plasma traz

| Pacote | Versão | Tamanho do arquivo |
| --- | --- | --- |
| `dev-qt/qtbase` | 6.7.3 | 48.0 MB |
| `kde-frameworks/kconfig` | 6.6.0 | 2.1 MB |
| `kde-frameworks/kcoreaddons` | 6.6.0 | 2.4 MB |
| `kde-plasma/kwin` | 6.1.5 | 8.6 MB |
| `kde-plasma/plasma-workspace` | 6.1.5 | 19.0 MB |
| `kde-plasma/plasma-meta` | apenas listado | nenhum |

As áreas de trabalho GNOME, Cinnamon e CDE (Common Desktop Environment) têm cadeias próprias. Um pacote que só aparece na lista, como o pacote meta da área de trabalho, não leva tempo para compilar.

## emerge no sistema que você instalou

Quando o Gentoo inicia a partir do seu próprio disco, o emerge continua como gerenciador de pacotes, e ainda compila o que instala. Cada uma de suas palavras chega ao Mirror pela rede.

Table: O que o emerge faz no sistema instalado

| Palavras | O que fazem |
| --- | --- |
| `emerge <package>` | Compila e instala o pacote. |
| `emerge --ask <package>` | Lista o que ele compilaria, e pergunta antes de começar. |
| `emerge --search [term]` | Procura no Mirror pacotes cujos nomes contenham o termo. |
| `emerge --sync` | Lê as listas do Mirror de novo. |
| `emerge --update @world` | Atualiza todos os pacotes instalados para a versão que o Mirror serve. |
| `emerge --unmerge <package>` | Remove o pacote. |
| `emerge -pvuD @world` | Lista o que o emerge --update @world compilaria, cada pacote com a versão que você tem, e não compila nada. |

```command
id: jsc:command/linux/emerge
```

## Se algo der errado

> [!PROBLEM] O emerge diz que não há ebuilds para atender ao nome que você digitou.
> Confira a grafia, ou use a categoria e o nome, como kde-plasma/plasma-meta.

> [!PROBLEM] O emerge diz "A árvore do Portage está vazia".
> Rode antes o emerge-webrsync ([](jsc:package_tree_and_world)).

> [!PROBLEM] O emerge diz "Não foi possível resolver mirror://mainframe".
> Nenhum Mainframe rodando o Mirror está nesta rede. Conecte-se a um, depois tente de novo.

> [!SEE] jsc:make_conf, jsc:package_tree_and_world, jsc:gentoo_reboot
