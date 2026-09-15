.asm 2
.start Corpus.UiCorpus console

.class Corpus.UiCorpus

.method static void Main() slots 17
    ldstr   "Corpus"
    ldc.i4  240
    ldc.i4  150
    newobj  Window(string, int, int)
    stloc   0
    ldloc   0
    ldstr   "Corpus"
    stfld   Window.Title
    newobj  Row()
    stloc   1
    newobj  Column()
    stloc   2
    ldloc   1
    ldc.i4  2
    stfld   Row.Spacing
    ldloc   2
    ldc.i4  2
    stfld   Column.Spacing
    newobj  Label()
    stloc   3
    ldstr   "name"
    newobj  Label(string)
    stloc   4
    ldloc   4
    ldstr   "name"
    stfld   Label.Text
    ldloc   4
    ldc.i4  1
    stfld   Widget.Visible
    ldloc   4
    ldc.i4  1
    stfld   Widget.Enabled
    ldloc   4
    ldc.i4  40
    stfld   Widget.Width
    ldloc   4
    ldc.i4  10
    stfld   Widget.Height
    newobj  Button()
    stloc   5
    ldstr   "press"
    newobj  Button(string)
    stloc   6
    ldloc   6
    ldstr   "press"
    stfld   Button.Text
    ldloc   6
    dup
    ldfld   Button.OnClick
    ldnull
    ldfn    Corpus.UiCorpus.Clicked() -> void
    call    Delegate.Combine(Action, Action) -> Action
    stfld   Button.OnClick
    newobj  TextBox()
    stloc   7
    ldstr   "x"
    newobj  TextBox(string)
    stloc   8
    ldloc   8
    ldstr   "y"
    stfld   TextBox.Text
    ldloc   8
    dup
    ldfld   TextBox.OnChange
    ldnull
    ldfn    Corpus.UiCorpus.Clicked() -> void
    call    Delegate.Combine(Action, Action) -> Action
    stfld   TextBox.OnChange
    ldloc   8
    dup
    ldfld   TextBox.OnSubmit
    ldnull
    ldfn    Corpus.UiCorpus.Clicked() -> void
    call    Delegate.Combine(Action, Action) -> Action
    stfld   TextBox.OnSubmit
    newobj  CheckBox()
    stloc   9
    ldstr   "on"
    newobj  CheckBox(string)
    stloc   10
    ldstr   "on"
    ldc.i4  1
    newobj  CheckBox(string, bool)
    stloc   11
    ldloc   11
    ldstr   "on"
    stfld   CheckBox.Text
    ldloc   11
    ldc.i4  0
    stfld   CheckBox.Checked
    ldloc   11
    dup
    ldfld   CheckBox.OnToggle
    ldnull
    ldfn    Corpus.UiCorpus.Clicked() -> void
    call    Delegate.Combine(Action, Action) -> Action
    stfld   CheckBox.OnToggle
    newobj  ProgressBar()
    stloc   12
    ldc.i4  0
    ldc.i4  10
    newobj  ProgressBar(int, int)
    stloc   13
    ldloc   13
    ldc.i4  5
    stfld   ProgressBar.Value
    ldloc   13
    ldc.i4  0
    stfld   ProgressBar.Least
    ldloc   13
    ldc.i4  10
    stfld   ProgressBar.Most
    newobj  ListBox()
    stloc   14
    ldloc   14
    ldstr   "one"
    call    ListBox.Add(string) -> void
    ldloc   14
    ldstr   "two"
    ldstr   "second"
    call    ListBox.Add(string, string) -> void
    ldloc   14
    ldc.i4  0
    stfld   ListBox.Selected
    ldloc   14
    dup
    ldfld   ListBox.OnSelect
    ldnull
    ldfn    Corpus.UiCorpus.Clicked() -> void
    call    Delegate.Combine(Action, Action) -> Action
    stfld   ListBox.OnSelect
    newobj  Canvas()
    stloc   15
    ldc.i4  100
    ldc.i4  50
    newobj  Canvas(int, int)
    stloc   16
    ldloc   16
    ldc.i4  0
    call    Canvas.Clear(int) -> void
    ldloc   16
    ldc.i4  0
    ldc.i4  0
    ldc.i4  10
    ldc.i4  10
    ldc.i4  1
    call    Canvas.FillRect(int, int, int, int, int) -> void
    ldloc   16
    ldc.i4  0
    ldc.i4  0
    ldc.i4  10
    ldc.i4  10
    ldc.i4  2
    call    Canvas.DrawLine(int, int, int, int, int) -> void
    ldloc   16
    ldstr   "hi"
    ldc.i4  1
    ldc.i4  1
    ldc.i4  3
    call    Canvas.DrawText(string, int, int, int) -> void
    ldloc   16
    ldc.i4  2
    ldc.i4  2
    ldc.i4  4
    call    Canvas.SetPixel(int, int, int) -> void
    ldloc   16
    dup
    ldfld   Canvas.OnClick
    ldnull
    ldfn    Corpus.UiCorpus.Clicked() -> void
    call    Delegate.Combine(Action, Action) -> Action
    stfld   Canvas.OnClick
    ldloc   1
    ldloc   3
    call    Row.Add(Widget) -> void
    ldloc   1
    ldloc   4
    ldc.i4  1
    call    Row.Add(Widget, int) -> void
    ldloc   2
    ldloc   1
    call    Column.Add(Widget) -> void
    ldloc   2
    ldloc   16
    ldc.i4  1
    call    Column.Add(Widget, int) -> void
    ldloc   2
    ldloc   7
    call    Column.Add(Widget) -> void
    ldloc   2
    ldloc   9
    call    Column.Add(Widget) -> void
    ldloc   2
    ldloc   10
    call    Column.Add(Widget) -> void
    ldloc   2
    ldloc   12
    call    Column.Add(Widget) -> void
    ldloc   2
    ldloc   15
    call    Column.Add(Widget) -> void
    ldloc   0
    ldloc   2
    stfld   Window.Content
    ldloc   0
    ldloc   5
    ldc.i4  0
    ldc.i4  0
    ldc.i4  20
    ldc.i4  10
    call    Window.Add(Widget, int, int, int, int) -> void
    ldloc   0
    dup
    ldfld   Window.OnClose
    ldnull
    ldfn    Corpus.UiCorpus.Clicked() -> void
    call    Delegate.Combine(Action, Action) -> Action
    stfld   Window.OnClose
    ldloc   0
    call    Window.Show() -> void
    ldstr   ""
    ldloc   0
    ldfld   Window.Open
    call    string.Concat(string, bool) -> string
    ldloc   14
    ldfld   ListBox.Count
    call    string.Concat(string, int) -> string
    ldloc   16
    ldfld   Canvas.ClickX
    call    string.Concat(string, int) -> string
    ldloc   16
    ldfld   Canvas.ClickY
    call    string.Concat(string, int) -> string
    call    Console.PrintLine(string) -> void
    ldloc   1
    call    Row.Clear() -> void
    ldloc   2
    call    Column.Clear() -> void
    ldloc   14
    call    ListBox.Clear() -> void
    ldloc   0
    call    Window.Close() -> void
    ldstr   "title"
    ldstr   "text"
    call    MessageBox.Show(string, string) -> void
    ret

.method static void Clicked() slots 0
    ret
