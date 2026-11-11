---
section: library_toolkit
order: 464
---
# Path, Directory e vigiar um arquivo

O Sigma# 3 acrescenta o que o seu programa precisa para se achar nos discos: desmontar caminhos, as pastas, o tamanho de um arquivo e quando ele mudou pela última vez, e ser avisado quando ele muda. Path e Directory ficam no System.IO, ao lado do File.

Table: Membros do Path

| Membro | O que dá |
| --- | --- |
| `Path.Combine(folder, name)` | O nome dentro da pasta, com o separador com que a pasta foi escrita. |
| `Path.FileName(path)` | O nome no fim do caminho. |
| `Path.Extension(path)` | O final do nome com o ponto, como .log, ou texto vazio. |
| `Path.Folder(path)` | A pasta em que o caminho está. |

Table: Membros do Directory e do File

| Membro | O que faz |
| --- | --- |
| `Directory.Exists(path)` | Sim quando a pasta existe. |
| `Directory.Make(path)` | Cria a pasta. |
| `Directory.Delete(path)` | Apaga a pasta. |
| `Directory.Files(path)` | Os nomes dos arquivos nela. |
| `Directory.Folders(path)` | Os nomes das pastas nela. |
| `File.Size(path)` | O tamanho do arquivo em bytes, ou -1 quando ele não está lá. |
| `File.Changed(path)` | O tick do mundo em que o arquivo mudou pela última vez, que você compara com Time.Tick, ou -1 quando ele não está lá. |
| `File.Watch(path, handler)` | Chama o tratador com um FileEvent cada vez que o arquivo muda, aparece ou some. Dá uma Subscription: solte-a para parar. |
| `FileEvent.Path, FileEvent.Changed, FileEvent.Previous` | O caminho vigiado, e o tick em que ele mudou agora e antes, -1 para um arquivo que não está lá. |

Size, Changed e Watch leem os arquivos dos drives da própria máquina. Uma vigia não custa nada para pôr: a máquina olha uma vez por tick, como faz com uma vigia na rede.

> [!DEFINE] Separador
> O caractere entre as pastas de um caminho: uma barra invertida no Frames e no MC-DOS, uma barra nos sistemas Unix.

## Se algo der errado

> [!PROBLEM] File.Size dá -1 para um arquivo no compartilhamento de outro computador.
> Size, Changed e Watch leem só os drives desta máquina. Copie o arquivo para cá antes.

> [!PROBLEM] Directory.Delete dá não.
> Apague antes os arquivos dentro da pasta.

> [!SEE] jsc:files_and_file_handles, jsc:file_tree, jsc:network_watches
