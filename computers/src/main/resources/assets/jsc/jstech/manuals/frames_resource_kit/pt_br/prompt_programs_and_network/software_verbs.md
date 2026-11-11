---
section: prompt_programs_and_network
order: 244
---
# Instalando e removendo programas

Um programa chega ao computador de duas formas: a partir de uma mídia, como um disco em uma unidade vinculada ([](jsc:installing)), ou a partir do Mirror, o serviço de pacotes da rede. programs lista os programas instalados aqui. store lista os que você pode instalar; install e uninstall os alteram; e nas edições Frames o pckmgr busca pacotes no Mirror.

> [!DEFINE] Mirror
> O serviço de pacotes da rede, que oferece programas que você pode instalar.

> [!DEFINE] Pacote
> Um programa que o Mirror oferece, pronto para ser baixado e configurado.

Table: Verbos para programas

| Verbo | Palavras depois dele, e o que faz |
| --- | --- |
| `programs (apps)` | Sem palavras. Lista os programas instalados neste computador, cada um com o seu id. |
| `install` | O id de um programa. Instala o programa a partir da sua mídia. |
| `store (available)` | Sem palavras. Lista os programas que você pode instalar neste computador. |
| `uninstall` | O id de um programa. Remove um programa instalado. |
| `pckmgr` | install, remove, search, list ou update, e um nome. Funciona a partir do Mirror, somente nas edições Frames. |

## Instalando a partir de uma mídia

1. Digite programs para ver os programas já instalados.
2. Digite store para ver os programas que você pode instalar.
3. Coloque o disco do programa em uma unidade vinculada, se o programa precisar de uma.
4. Digite install e o id do programa.
5. Espere as linhas que dizem Configurando. A última linha diz Instalação concluída. Reiniciando no novo sistema.

Quando um programa precisa de um disco que não está em uma unidade vinculada, install diz que o programa precisa do disco de instalação em uma unidade vinculada.

```command
id: jsc:command/mc_dos/programs
```

```command
id: jsc:command/mc_dos/install
```

```command
id: jsc:command/mc_dos/store
```

```command
id: jsc:command/mc_dos/uninstall
```

## pckmgr nas edições Frames

O pckmgr busca pacotes do Mirror nas edições Frames. O MC-NET chama o mesmo gerenciador de netgetter. O MC-DOS não tem pckmgr: ele instala programas apenas a partir de mídias.

Table: Verbos do pckmgr

| Verbo | O que faz |
| --- | --- |
| `install NAME` | Baixa e configura um pacote. |
| `remove NAME` | Desinstala um pacote. |
| `search [text]` | Encontra os pacotes que o Mirror oferece. Sem texto, lista o que é oferecido. |
| `list [--available]` | Lista os pacotes instalados, ou tudo o que é oferecido com --available. |
| `update` | Atualiza os pacotes instalados para a compilação atual. |

```command
id: jsc:command/frames/pckmgr
```

## screenfetch nas edições Frames

screenfetch é um pequeno programa para as edições Frames. Depois de instalado, digitar screenfetch no Prompt de Comando mostra o logo do Frames, com um resumo da sua máquina ao lado: o sistema, o seu kernel, o seu tempo de atividade, os seus pacotes e o seu shell.

```command
id: jsc:command/frames/screenfetch
```

## Se algo der errado

> [!PROBLEM] programa inexistente: NAME
> Digite programs ou store para ver os ids, e confira a grafia.

> [!PROBLEM] NAME precisa do disco de instalação em uma unidade vinculada
> Vincule uma unidade e coloque o disco nela ([](jsc:drives)).

> [!PROBLEM] não foi possível resolver mirror:// - nenhuma fonte de pacotes alcançável
> O computador não está em uma rede com um Mirror. Conecte-o a uma ([](jsc:mirror)).

> [!PROBLEM] Nenhum pacote corresponde a NAME.
> Digite search sem texto para listar os pacotes que o Mirror oferece.

> [!PROBLEM] este verbo precisa de um nome de pacote
> Digite um nome de pacote depois de install ou remove.

> [!PROBLEM] A instalação não pôde ser gravada no disco (sem espaço ou sem disco).
> Libere espaço no disco, ou use um disco maior.

> [!SEE] jsc:programs, jsc:mirror
