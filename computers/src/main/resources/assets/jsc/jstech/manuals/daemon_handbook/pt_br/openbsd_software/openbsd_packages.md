---
section: openbsd_software
order: 345
---
# Pacotes e a árvore de ports

O gerenciador de pacotes do OpenBSD são três comandos. O pkg_add busca um pacote no Mirror da rede e o instala, o pkg_info lista o que está instalado, e o pkg_delete retira um pacote.

Table: As três ferramentas

| Comando | O que faz |
| --- | --- |
| `pkg_add <package>` | Instala um pacote do Mirror. |
| `pkg_add -u` | Leva todo pacote instalado à versão do Mirror. |
| `pkg_info` | Lista os pacotes instalados. |
| `pkg_info -Q <term>` | Busca no Mirror. |
| `pkg_delete <package>` | Retira um pacote. |
| `pkg_add -un` | Lista o que o pkg_add -u atualizaria, cada pacote com as suas duas versões, e não instala nada. |

```command
id: jsc:command/openbsd/pkg_add
```

```command
id: jsc:command/openbsd/pkg_info
```

```command
id: jsc:command/openbsd/pkg_delete
```

## A árvore de ports

A árvore de ports é a receita de todo programa, para compilar no seu próprio computador. No OpenBSD você a busca com o cvs, e compila um port com o make, que precisa do set comp.

1. Digite cvs -q checkout -P ports para organizar a árvore em /usr/ports.
2. Vá até a pasta de um port, como cd /usr/ports/sysutils/screenfetch.
3. Digite make install clean para compilá-lo, instalá-lo e limpar depois.
4. Mais tarde, digite cvs -q update -Pd em /usr/ports para atualizar a árvore.

```command
id: jsc:command/openbsd/cvs
```

> [!SEE] jsc:openbsd_patches, jsc:openbsd_overview
