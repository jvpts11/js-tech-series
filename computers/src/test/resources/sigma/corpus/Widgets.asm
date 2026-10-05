.asm 3
.arch jsc:x86
.start Corpus.WidgetsCorpus console

.class Corpus.WidgetsCorpus

.method static void Main() slots 40
    ldstr   "Widgets"
    ldc.i4  320
    ldc.i4  200
    newobj  Window(string, int, int)
    stloc   0
    newobj  Column()
    stloc   1
    newobj  TextArea()
    stloc   2
    ldstr   "notes"
    newobj  TextArea(string)
    stloc   3
    ldloc   3
    ldstr   "more"
    stfld   TextArea.Text
    ldloc   3
    dup
    ldfld   TextArea.OnChange
    ldnull
    ldfn    Corpus.WidgetsCorpus.Heard() -> void
    call    Delegate.Combine(Action, Action) -> Action
    stfld   TextArea.OnChange
    newobj  NumberBox()
    stloc   4
    ldc.i4  1
    ldc.i4  9
    newobj  NumberBox(int, int)
    stloc   5
    ldloc   5
    ldc.i4  3
    stfld   NumberBox.Value
    ldloc   5
    ldc.i4  1
    stfld   NumberBox.Least
    ldloc   5
    ldc.i4  9
    stfld   NumberBox.Most
    ldloc   5
    ldc.i4  1
    stfld   NumberBox.Step
    ldloc   5
    dup
    ldfld   NumberBox.OnChange
    ldnull
    ldfn    Corpus.WidgetsCorpus.Heard() -> void
    call    Delegate.Combine(Action, Action) -> Action
    stfld   NumberBox.OnChange
    newobj  Slider()
    stloc   6
    ldc.i4  0
    ldc.i4  100
    newobj  Slider(int, int)
    stloc   7
    ldloc   7
    ldc.i4  50
    stfld   Slider.Value
    ldloc   7
    ldc.i4  0
    stfld   Slider.Least
    ldloc   7
    ldc.i4  100
    stfld   Slider.Most
    ldloc   7
    ldc.i4  5
    stfld   Slider.Step
    ldloc   7
    dup
    ldfld   Slider.OnChange
    ldnull
    ldfn    Corpus.WidgetsCorpus.Heard() -> void
    call    Delegate.Combine(Action, Action) -> Action
    stfld   Slider.OnChange
    newobj  RadioGroup()
    stloc   8
    ldloc   8
    ldstr   "Fast"
    call    RadioGroup.Add(string) -> void
    ldloc   8
    ldstr   "Slow"
    call    RadioGroup.Add(string) -> void
    ldloc   8
    ldc.i4  1
    stfld   RadioGroup.Selected
    ldloc   8
    ldc.i4  1
    stfld   RadioGroup.Across
    ldloc   8
    dup
    ldfld   RadioGroup.OnSelect
    ldnull
    ldfn    Corpus.WidgetsCorpus.Heard() -> void
    call    Delegate.Combine(Action, Action) -> Action
    stfld   RadioGroup.OnSelect
    newobj  ComboBox()
    stloc   9
    ldloc   9
    ldstr   "Legacy"
    call    ComboBox.Add(string) -> void
    ldloc   9
    ldc.i4  1
    stfld   ComboBox.Selected
    ldloc   9
    dup
    ldfld   ComboBox.OnSelect
    ldnull
    ldfn    Corpus.WidgetsCorpus.Heard() -> void
    call    Delegate.Combine(Action, Action) -> Action
    stfld   ComboBox.OnSelect
    newobj  GroupBox()
    stloc   10
    ldstr   "Backup"
    newobj  GroupBox(string)
    stloc   11
    ldloc   11
    ldstr   "Backup"
    stfld   GroupBox.Text
    ldloc   11
    ldloc   3
    stfld   GroupBox.Content
    newobj  TabView()
    stloc   12
    ldloc   12
    ldstr   "Controls"
    ldloc   11
    call    TabView.Add(string, Widget) -> void
    ldloc   12
    ldc.i4  1
    stfld   TabView.Selected
    ldloc   12
    dup
    ldfld   TabView.OnSelect
    ldnull
    ldfn    Corpus.WidgetsCorpus.Heard() -> void
    call    Delegate.Combine(Action, Action) -> Action
    stfld   TabView.OnSelect
    newobj  ScrollView()
    stloc   13
    ldloc   13
    ldloc   2
    stfld   ScrollView.Content
    newobj  Table()
    stloc   14
    ldloc   14
    ldstr   "Name"
    call    Table.AddColumn(string) -> void
    newobj  List<string>()
    stloc   15
    ldloc   15
    ldstr   "a"
    call    List.Add(string) -> void
    ldloc   14
    ldloc   15
    call    Table.AddRow(List<string>) -> void
    ldloc   14
    ldstr   "a"
    call    Table.AddRow(string) -> void
    ldloc   14
    ldstr   "a"
    ldstr   "b"
    call    Table.AddRow(string, string) -> void
    ldloc   14
    ldstr   "a"
    ldstr   "b"
    ldstr   "c"
    call    Table.AddRow(string, string, string) -> void
    ldloc   14
    ldstr   "a"
    ldstr   "b"
    ldstr   "c"
    ldstr   "d"
    call    Table.AddRow(string, string, string, string) -> void
    ldloc   14
    ldc.i4  1
    stfld   Table.Selected
    ldloc   14
    dup
    ldfld   Table.OnSelect
    ldnull
    ldfn    Corpus.WidgetsCorpus.Heard() -> void
    call    Delegate.Combine(Action, Action) -> Action
    stfld   Table.OnSelect
    ldloc   14
    ldc.i4  1
    ldc.i4  1
    call    Table.Cell(int, int) -> string
    stloc   16
    newobj  TreeView()
    stloc   17
    ldloc   17
    ldstr   "This PC"
    call    TreeView.Add(string) -> int
    stloc   18
    ldloc   17
    ldstr   "C:"
    ldloc   18
    call    TreeView.Add(string, int) -> int
    stloc   19
    ldloc   17
    ldloc   19
    call    TreeView.NodeText(int) -> string
    stloc   20
    ldloc   17
    ldloc   19
    stfld   TreeView.Selected
    ldloc   17
    dup
    ldfld   TreeView.OnSelect
    ldnull
    ldfn    Corpus.WidgetsCorpus.Heard() -> void
    call    Delegate.Combine(Action, Action) -> Action
    stfld   TreeView.OnSelect
    newobj  MenuBar()
    stloc   21
    ldloc   21
    ldstr   "File"
    ldstr   "Open"
    call    MenuBar.Add(string, string) -> void
    ldloc   21
    dup
    ldfld   MenuBar.OnPick
    ldnull
    ldfn    Corpus.WidgetsCorpus.Heard() -> void
    call    Delegate.Combine(Action, Action) -> Action
    stfld   MenuBar.OnPick
    ldloc   14
    newobj  ContextMenu(Widget)
    stloc   22
    ldloc   22
    ldstr   "Rename"
    call    ContextMenu.Add(string) -> void
    ldloc   22
    dup
    ldfld   ContextMenu.OnPick
    ldnull
    ldfn    Corpus.WidgetsCorpus.Heard() -> void
    call    Delegate.Combine(Action, Action) -> Action
    stfld   ContextMenu.OnPick
    newobj  StatusBar()
    stloc   23
    ldstr   "Ready"
    newobj  StatusBar(string)
    stloc   24
    ldloc   24
    ldstr   "Ready"
    stfld   StatusBar.Text
    ldloc   24
    ldstr   "12:30"
    call    StatusBar.Add(string) -> void
    newobj  Image()
    stloc   25
    ldstr   "/photo.pix"
    newobj  Image(string)
    stloc   26
    ldloc   26
    ldstr   "/photo.pix"
    stfld   Image.Path
    newobj  Chart()
    stloc   27
    ldc.i4  0
    ldc.i4  100
    newobj  Chart(int, int)
    stloc   28
    ldloc   28
    ldstr   "CPU"
    stfld   Chart.Text
    ldloc   28
    ldc.i4  0
    stfld   Chart.Least
    ldloc   28
    ldc.i4  100
    stfld   Chart.Most
    ldloc   28
    ldc.r8  61.0
    call    Chart.Add(double) -> void
    newobj  LogView()
    stloc   29
    ldloc   29
    ldstr   "10:14 INFO started"
    call    LogView.Add(string) -> void
    newobj  OpenFileDialog()
    stloc   30
    ldstr   "Open"
    newobj  OpenFileDialog(string)
    stloc   31
    ldloc   31
    ldstr   "Open"
    stfld   OpenFileDialog.Title
    ldloc   31
    ldstr   "/"
    stfld   OpenFileDialog.Path
    ldloc   31
    ldstr   "*.txt"
    stfld   OpenFileDialog.Filter
    ldloc   31
    dup
    ldfld   OpenFileDialog.OnChoose
    ldnull
    ldfn    Corpus.WidgetsCorpus.Heard() -> void
    call    Delegate.Combine(Action, Action) -> Action
    stfld   OpenFileDialog.OnChoose
    ldloc   31
    dup
    ldfld   OpenFileDialog.OnCancel
    ldnull
    ldfn    Corpus.WidgetsCorpus.Heard() -> void
    call    Delegate.Combine(Action, Action) -> Action
    stfld   OpenFileDialog.OnCancel
    newobj  SaveFileDialog()
    stloc   32
    ldstr   "Save"
    newobj  SaveFileDialog(string)
    stloc   33
    ldloc   33
    ldstr   "Save"
    stfld   SaveFileDialog.Title
    ldloc   33
    ldstr   "/"
    stfld   SaveFileDialog.Path
    ldloc   33
    ldstr   "*.txt"
    stfld   SaveFileDialog.Filter
    ldloc   33
    ldstr   "a.txt"
    stfld   SaveFileDialog.Name
    ldloc   33
    dup
    ldfld   SaveFileDialog.OnChoose
    ldnull
    ldfn    Corpus.WidgetsCorpus.Heard() -> void
    call    Delegate.Combine(Action, Action) -> Action
    stfld   SaveFileDialog.OnChoose
    ldloc   33
    dup
    ldfld   SaveFileDialog.OnCancel
    ldnull
    ldfn    Corpus.WidgetsCorpus.Heard() -> void
    call    Delegate.Combine(Action, Action) -> Action
    stfld   SaveFileDialog.OnCancel
    newobj  ItemSlot()
    stloc   34
    ldstr   "minecraft:diamond"
    newobj  ItemSlot(string)
    stloc   35
    ldstr   "minecraft:diamond"
    ldc.i4  64
    newobj  ItemSlot(string, int)
    stloc   36
    ldloc   36
    ldstr   "minecraft:diamond"
    stfld   ItemSlot.Item
    ldloc   36
    ldc.i4  64
    stfld   ItemSlot.Amount
    ldloc   36
    dup
    ldfld   ItemSlot.OnClick
    ldnull
    ldfn    Corpus.WidgetsCorpus.Heard() -> void
    call    Delegate.Combine(Action, Action) -> Action
    stfld   ItemSlot.OnClick
    newobj  ItemPicker()
    stloc   37
    ldloc   37
    ldstr   "minecraft:diamond"
    call    ItemPicker.Add(string) -> void
    ldloc   37
    ldc.i4  1
    stfld   ItemPicker.Selected
    ldloc   37
    dup
    ldfld   ItemPicker.OnSelect
    ldnull
    ldfn    Corpus.WidgetsCorpus.Heard() -> void
    call    Delegate.Combine(Action, Action) -> Action
    stfld   ItemPicker.OnSelect
    newobj  OperationView()
    stloc   38
    ldloc   38
    ldstr   "SELECT 64 diamond"
    stfld   OperationView.Text
    ldloc   38
    ldstr   "PROCESSING"
    stfld   OperationView.State
    ldloc   38
    ldc.i4  3
    conv.i8
    stfld   OperationView.Done
    ldloc   38
    ldc.i4  5
    conv.i8
    stfld   OperationView.Total
    ldloc   38
    ldstr   "Mainframe"
    stfld   OperationView.Computer
    ldloc   38
    ldstr   "op"
    call    Operations.Get(string) -> OperationInfo
    call    OperationView.Show(OperationInfo) -> void
    ldstr   "corpus:dial"
    newobj  GenericComponent(string)
    stloc   39
    ldloc   39
    ldc.i4  5
    stfld   GenericComponent.Data
    ldloc   39
    ldnull
    ldfn    Corpus.WidgetsCorpus.Acted(ComponentAction) -> void
    call    GenericComponent.OnAction(Action<ComponentAction>) -> void
    ldloc   1
    ldloc   21
    call    Column.Add(Widget) -> void
    ldloc   1
    ldloc   12
    ldc.i4  1
    call    Column.Add(Widget, int) -> void
    ldloc   1
    ldloc   13
    call    Column.Add(Widget) -> void
    ldloc   1
    ldloc   5
    call    Column.Add(Widget) -> void
    ldloc   1
    ldloc   7
    call    Column.Add(Widget) -> void
    ldloc   1
    ldloc   8
    call    Column.Add(Widget) -> void
    ldloc   1
    ldloc   9
    call    Column.Add(Widget) -> void
    ldloc   1
    ldloc   14
    call    Column.Add(Widget) -> void
    ldloc   1
    ldloc   17
    call    Column.Add(Widget) -> void
    ldloc   1
    ldloc   26
    call    Column.Add(Widget) -> void
    ldloc   1
    ldloc   28
    call    Column.Add(Widget) -> void
    ldloc   1
    ldloc   29
    call    Column.Add(Widget) -> void
    ldloc   1
    ldloc   36
    call    Column.Add(Widget) -> void
    ldloc   1
    ldloc   37
    call    Column.Add(Widget) -> void
    ldloc   1
    ldloc   38
    call    Column.Add(Widget) -> void
    ldloc   1
    ldloc   39
    call    Column.Add(Widget) -> void
    ldloc   1
    ldloc   24
    call    Column.Add(Widget) -> void
    ldloc   0
    ldloc   1
    stfld   Window.Content
    ldloc   0
    call    Window.Show() -> void
    ldloc   31
    ldloc   0
    call    OpenFileDialog.Show(Window) -> void
    ldloc   33
    ldloc   0
    call    SaveFileDialog.Show(Window) -> void
    ldloc   16
    ldloc   20
    call    string.Concat(string, string) -> string
    ldloc   8
    ldfld   RadioGroup.Count
    call    string.Concat(string, int) -> string
    ldloc   9
    ldfld   ComboBox.Count
    call    string.Concat(string, int) -> string
    ldloc   12
    ldfld   TabView.Count
    call    string.Concat(string, int) -> string
    ldloc   14
    ldfld   Table.Count
    call    string.Concat(string, int) -> string
    ldloc   17
    ldfld   TreeView.Count
    call    string.Concat(string, int) -> string
    ldloc   28
    ldfld   Chart.Count
    call    string.Concat(string, int) -> string
    ldloc   29
    ldfld   LogView.Count
    call    string.Concat(string, int) -> string
    ldloc   37
    ldfld   ItemPicker.Count
    call    string.Concat(string, int) -> string
    ldloc   37
    ldfld   ItemPicker.Picked
    call    string.Concat(string, string) -> string
    ldloc   21
    ldfld   MenuBar.Picked
    call    string.Concat(string, string) -> string
    ldloc   21
    ldfld   MenuBar.PickedMenu
    call    string.Concat(string, string) -> string
    ldloc   22
    ldfld   ContextMenu.Picked
    call    string.Concat(string, string) -> string
    ldloc   39
    ldfld   GenericComponent.Kind
    call    string.Concat(string, string) -> string
    ldloc   39
    ldfld   GenericComponent.Data
    call    string.Concat(string, object) -> string
    ldloc   38
    ldfld   OperationView.Done
    call    string.Concat(string, long) -> string
    ldloc   4
    ldfld   NumberBox.Value
    call    string.Concat(string, int) -> string
    ldloc   6
    ldfld   Slider.Value
    call    string.Concat(string, int) -> string
    ldloc   10
    ldfld   GroupBox.Text
    call    string.Concat(string, string) -> string
    ldloc   23
    ldfld   StatusBar.Text
    call    string.Concat(string, string) -> string
    ldloc   25
    ldfld   Image.Path
    call    string.Concat(string, string) -> string
    ldloc   27
    ldfld   Chart.Count
    call    string.Concat(string, int) -> string
    ldloc   30
    ldfld   OpenFileDialog.Path
    call    string.Concat(string, string) -> string
    ldloc   32
    ldfld   SaveFileDialog.Name
    call    string.Concat(string, string) -> string
    ldloc   34
    ldfld   ItemSlot.Amount
    call    string.Concat(string, int) -> string
    call    Console.PrintLine(string) -> void
    ldloc   8
    call    RadioGroup.Clear() -> void
    ldloc   9
    call    ComboBox.Clear() -> void
    ldloc   12
    call    TabView.Clear() -> void
    ldloc   14
    call    Table.Clear() -> void
    ldloc   17
    call    TreeView.Clear() -> void
    ldloc   21
    call    MenuBar.Clear() -> void
    ldloc   22
    call    ContextMenu.Clear() -> void
    ldloc   24
    call    StatusBar.Clear() -> void
    ldloc   28
    call    Chart.Clear() -> void
    ldloc   29
    call    LogView.Clear() -> void
    ldloc   37
    call    ItemPicker.Clear() -> void
    ret

.method static void Heard() slots 0
    ret

.method static void Acted(ComponentAction) slots 1
    ldloc   0
    ldfld   ComponentAction.Name
    ldloc   0
    ldfld   ComponentAction.Value
    call    string.Concat(string, object) -> string
    call    Console.PrintLine(string) -> void
    ret
