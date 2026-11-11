---
section: linux_packages
order: 292
---
# dnf, verbo por verbo

dnf é o gerenciador de pacotes do Fedora. Ele funciona como o apt: você digita dnf, um verbo e depois o nome do que ele vai tratar. No Ubuntu e no Debian a palavra não é um comando, e o shell responde "comando não encontrado: dnf".

Os verbos são os mesmos do apt, com uma diferença. No dnf, update é o upgrade: ele atualiza os pacotes instalados. Upgrade faz o mesmo trabalho, então qualquer uma das duas palavras funciona.

Table: Os verbos do dnf

| Comando | O que faz |
| --- | --- |
| `dnf install <package>` | Instala o programa a partir do Mirror. |
| `dnf remove <package>` | Remove o programa. purge e erase fazem o mesmo. |
| `dnf search [term]` | Lista os pacotes correspondentes. |
| `dnf list` | Lista os pacotes instalados. |
| `dnf update` | Atualiza os pacotes instalados. |
| `dnf upgrade` | Atualiza os pacotes instalados, como faz o dnf update; full-upgrade e dist-upgrade são o mesmo. |
| `dnf check-update` | Lista os pacotes instalados de que o Mirror tem uma versão mais nova, cada um com a versão nova. Não instala nada. |

```command
id: jsc:command/linux/dnf
```

Uma instalação no Fedora mostra suas próprias linhas, nesta ordem.

```table
{"caption": "O que o dnf mostra em uma instalação", "heads": ["Linha", "O que significa"], "rows": [[{"literal": "Last metadata expiration check: <time> ago."}, {"words": "Quando as listas de pacotes foram lidas pela última vez."}], [{"literal": "Dependencies resolved."}, {"words": "Os pacotes de que o programa precisa são calculados."}], [{"literal": "Installing:  <package>  x86_64  <version>  mirror  <MB> MB"}, {"words": "O programa, sua versão e seu tamanho."}], [{"literal": "Downloading Packages:"}, {"words": "O download a partir do Mirror começa, com uma linha para sua velocidade e progresso."}], [{"literal": "Running transaction"}, {"words": "O programa é gravado no computador."}], [{"literal": "Installed:"}, {"words": "Seguido do nome do programa instalado."}], [{"literal": "Complete!"}, {"words": "A instalação foi concluída."}]]}
```

```text
{"text": "Uma remoção mostra \"Removendo:  <package>\" no lugar das linhas de instalação. Todas as outras recusas são iguais às do apt e começam com \"E: \"."}
```

## Se algo der errado

> [!PROBLEM] comando não encontrado: dnf
> Este é o Ubuntu ou o Debian, que usam apt. Digite apt no lugar ([](jsc:apt)).

> [!PROBLEM] E: não foi possível resolver mirror:// - conecte este computador a uma rede cujo Mainframe executa o serviço Mirror
> O computador não está em uma rede cujo Mainframe executa o Mirror. Conecte-o, como explica [](jsc:mirror_and_packages).

> [!PROBLEM] E: não foi possível localizar o pacote <name>
> O nome está errado, ou o programa não é oferecido no Linux. Encontre o nome correto com dnf search.

> [!PROBLEM] <name> já é a versão mais recente
> Nada a fazer. dnf update atualiza todos os pacotes instalados.

> [!SEE] jsc:apt, jsc:mirror_and_packages
