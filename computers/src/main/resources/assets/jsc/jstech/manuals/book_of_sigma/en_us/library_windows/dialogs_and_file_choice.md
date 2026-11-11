---
section: library_windows
order: 474
---
# Dialogs: picking a file to open or save

A dialog is a small window that asks the player something. It opens over another window, its owner window, and it closes when the player answers it.

Your program makes the dialog, shows it over a window that is open, and reads the path the player picked.

> [!DEFINE] Dialog
> A small window that asks the player something, and opens over another window.

> [!DEFINE] Owner window
> The window a dialog sits over.

Table: Dialog members

| Member | What it does |
| --- | --- |
| `OpenFileDialog.new(), OpenFileDialog.new(title)` | Makes a dialog that asks the player to open a file. Version 2. |
| `SaveFileDialog.new(), SaveFileDialog.new(title)` | Makes a dialog that asks the player where to save a file. Version 2. |
| `OpenFileDialog.Title, SaveFileDialog.Title` | The title of the dialog. |
| `OpenFileDialog.Path, SaveFileDialog.Path` | The path the player picked. |
| `OpenFileDialog.Filter, SaveFileDialog.Filter` | The filter text of the dialog. |
| `OpenFileDialog.Show(owner), SaveFileDialog.Show(owner)` | Shows the dialog over the owner window. |
| `SaveFileDialog.Name` | The file name the dialog suggests. |
| `OpenFileDialog.OnChoose, SaveFileDialog.OnChoose` | The event that runs when the player chooses a file. |
| `OpenFileDialog.OnCancel, SaveFileDialog.OnCancel` | The event that runs when the player cancels. |
| `MessageBox.Show(title, text)` | A window with a title and one line of text. Sigma# only. |

A window holds one dialog at a time.

## If something goes wrong

> [!PROBLEM] The program stops and says "a dialog opens over a window that is open", because the owner window is not open.
> Show the owner window first, then open the dialog over it.

> [!PROBLEM] The program stops and says this window already has a dialog open over it.
> Close the open dialog first, then open the next one.

> [!SEE] jsc:monitors, jsc:windows_and_layout
