---
section: freebsd_software
order: 336
---
# pkg, verbo por verbo

O pkg é o gerenciador de pacotes do FreeBSD. Um pacote é um programa compilado por outra pessoa e baixado pronto para instalar, então você não precisa compilá-lo. Os pacotes vêm do Mirror da rede, e você digita pkg, um verbo e o nome sobre o qual ele age.

> [!DEFINE] Pacote da comunidade
> Um pacote que outro jogador publicou no Mirror.

> [!DEFINE] Desatualizado
> Um pacote instalado cuja versão é diferente da que o Mirror oferece agora.

Table: Os verbos do pkg

| Comando | O que faz |
| --- | --- |
| `pkg install <package>` | Busca um pacote no Mirror e o instala. |
| `pkg delete <package>` | Remove um pacote instalado. |
| `pkg search [term]` | Lista os pacotes que correspondem ao termo, uma linha para cada um. |
| `pkg info` | Lista todos os pacotes instalados, com a descrição. |
| `pkg update` | Atualiza a lista de pacotes do Mirror sem alterar nenhum pacote instalado. |
| `pkg upgrade` | Instala versões mais novas dos pacotes que você tem instalados. |
| `pkg version -vRL=` | Lista os pacotes instalados que precisam de atualização, cada um com a versão que o Mirror tem. |

Três palavras mais curtas também funcionam: add equivale a install, remove equivale a delete, e query equivale a info.

pkg search marca um pacote com [installed] quando ele está no seu computador, e com [community] quando outro jogador o publicou. pkg upgrade instala a versão mais nova de cada pacote Desatualizado.

```command
id: jsc:command/freebsd/pkg
```

## Instalando um pacote

1. Digite pkg install e o nome do pacote.
2. Leia as linhas que nomeiam os pacotes e o tamanho do download.
3. Aguarde enquanto o pacote é configurado. A configuração roda como uma tarefa que você pode acompanhar.

```listing
{"lines": ["Updating Mirror repository catalogue...", "Mirror repository is up to date.", "The following 1 package(s) will be affected (of 0 checked):", "New packages to be INSTALLED:", "        <name>: <version>", "Number of packages to be installed: 1", "<n> MiB to be downloaded.", "[1/1] Fetching <name>-<version>.pkg from mirror://mainframe"], "name": "pkg install"}
```

Uma remoção lista os pacotes que ela retira e depois diz quanto espaço libera, como "A operação liberará <n> MiB."

## Mensagens do pkg

Table: Mensagens do pkg

| Mensagem | O que significa |
| --- | --- |
| `pkg: could not resolve mirror://` | Este computador não está em uma rede cujo Mainframe rode o serviço Mirror. |
| `pkg: unable to locate package <name>` | O Mirror não oferece um pacote com esse nome. |
| `pkg: unmet requirements (hardware or free disk space)` | O computador não tem o hardware ou o espaço livre em disco de que o pacote precisa. |
| `pkg: <name> is not installed.` | Você tentou remover um pacote que não está instalado. |
| `pkg: <name> comes with every system.` | O programa já está lá, então não há nada para instalar. |
| `pkg: <name> is already installed.` | O pacote já está no computador. |
| `pkg: <name> needs <era> hardware or later` | O computador é de uma era mais antiga do que a que o pacote exige. |
| `pkg: <name> only installs on the Mainframe` | O pacote roda apenas nesse tipo de computador: o Mainframe, um servidor em rack, um Computador de Gerenciamento de Cluster ou um Computador Pessoal. |
| `pkg: this computer cannot store installed programs` | O computador não tem disco onde guardar programas. |
| `no packages installed` | pkg info não tem nada para listar. |
| `no packages match '<term>'` | pkg search não encontrou nenhum pacote com essa palavra. |

## Se algo der errado

> [!PROBLEM] pkg diz que não conseguiu resolver mirror://.
> Conecte este computador a uma rede cujo Mainframe rode o serviço Mirror.

> [!PROBLEM] pkg diz que não consegue localizar o pacote pelo nome.
> Digite pkg search e uma palavra para encontrar o nome real, depois use esse nome.

> [!PROBLEM] pkg diz que os requisitos não foram atendidos.
> Libere algum espaço em disco, ou use um computador com hardware melhor.

> [!PROBLEM] pkg diz que o pacote não está instalado quando você tenta removê-lo.
> Digite pkg info para encontrar o nome exato.

> [!PROBLEM] pkg diz que o programa vem com todos os sistemas.
> Nada a fazer: o pacote já está no computador.

> [!SEE] jsc:programs, jsc:software_commands, jsc:ports_tree
