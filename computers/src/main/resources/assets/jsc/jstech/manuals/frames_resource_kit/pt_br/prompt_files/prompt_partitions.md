---
section: prompt_files
order: 238
---
# Partições: FDISK e diskpart

Um disco pode ter várias partições, cada uma uma unidade à parte ([](jsc:partitions)). O MC-DOS divide um disco com o FDISK, e o Frames com o diskpart; do Frames XP em diante, o Gerenciamento de Disco faz o mesmo numa janela.

> [!DEFINE] Gerenciamento de Disco
> A janela do Frames XP ao 11 que cria, formata, redimensiona e exclui os volumes de cada disco.

O FDISK abre um menu. A opção 1 dele faz uma partição do tamanho que você digita, a opção 2 define a ativa, a partição que a máquina inicia, a opção 3 apaga uma e a opção 4 as mostra. Uma escolha vazia sai dele.

```command
id: jsc:command/mc_dos/fdisk
```

O diskpart recebe os comandos no seu próprio prompt, DISKPART>: "list disk", depois "select disk 1", depois "create partition primary", "format fs=ntfs quick" e "exit" para sair.

```session
{"lines": [{"prompt": "DISKPART> ", "text": "select disk 1", "typed": true}, {"text": "Disk 1 is now the selected disk."}, {"prompt": "DISKPART> ", "text": "create partition primary", "typed": true}, {"text": "DiskPart succeeded in creating the specified partition."}], "look": "jsc:guide/terminal/dos", "title": "Frames"}
```

```command
id: jsc:command/frames/diskpart
```

## Se algo der errado

> [!PROBLEM] O diskpart diz que não há disco selecionado.
> Digite primeiro select disk e o número que o list disk mostra.

> [!PROBLEM] A partição de onde o sistema roda é recusada.
> Nenhuma das ferramentas a exclui nem a formata. Mude outro disco.

> [!SEE] jsc:format_a_drive, jsc:disk_tools_of_each_system
