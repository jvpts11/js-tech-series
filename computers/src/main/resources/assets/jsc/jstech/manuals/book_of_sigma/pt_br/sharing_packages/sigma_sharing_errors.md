---
section: sharing_packages
order: 498
---
# Erros ao empacotar e publicar

Cada erro começa com um prefixo que indica de onde ele veio. Um erro do sgpack pode começar com sgpack:. Uma linha que começa com package.cpk: aponta para uma linha do seu manifesto. Uma linha que começa com o nome de um arquivo de pacote, como mypkg-1.0.0.cpk:, vem de publish e aponta para esse arquivo.

A tabela mostra cada texto como o jogo o exibe, com o que ele significa e como corrigi-lo.

Table: Erros do comando sgpack

| O texto que você vê | O que significa e a correção |
| --- | --- |
| `usage: sgpack init [name] \| build \| publish [file] \| unpublish <name>` | Você não digitou nenhum verbo. Escolha um dos quatro verbos. |
| `package.cpk is already here; edit it, or delete it to start over` | init encontrou um manifesto. Edite-o, ou apague-o para começar do zero. |
| `no package.cpk here; run 'sgpack init' first` | build não encontrou nenhum manifesto. Execute sgpack init nesta pasta ([](jsc:sigma_package_manifest)). |
| `sgpack: <file> is named in the manifest but not here` | Um arquivo listado está faltando. Copie-o para a pasta, ou remova a linha dele. |
| `package.cpk: <problem>` | O manifesto ou um arquivo não passa em uma verificação. Corrija a linha que o texto indica ([](jsc:sigma_package_manifest)). |
| `name the package to publish, or run this beside a package.cpk` | publish não tem arquivo nem manifesto. Informe o arquivo, ou execute o comando na pasta do manifesto. |
| `could not resolve mirror:// - connect this computer to a network whose Mainframe runs the Mirror service` | Nenhum Mirror atende esta rede. Conecte-se a uma rede cujo Mainframe execute o Mirror ([](jsc:mirror)). |
| `mypkg-1.0.0.cpk: this is not a package (build one with 'sgpack build')` | O arquivo não é um pacote compilado. Execute sgpack build e depois publique o arquivo .cpk. |
| `the Mirror is full (64 packages)` | O Mirror não tem espaço para um nome novo. Tire um pacote do Mirror ([](jsc:sigma_unpublish)). |
| `usage: sgpack unpublish <name>` | unpublish não tem nome. Digite o nome do pacote depois dele. |
| `the Mirror is not serving <name>` | O nome não está no Mirror. Confira a grafia. |
| `could not resolve mirror://` | unpublish não encontra nenhum Mirror que sirva esta rede. Veja [o Mirror](jsc:mirror). |

> [!SEE] jsc:sigma_package_manifest, jsc:sigma_build_package, jsc:sigma_publish_mirror
