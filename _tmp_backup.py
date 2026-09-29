import io
p = r"D:\Agent\APP-烧\app\AiApiAssistant\app\src\main\java\com\aiassistant\ui\screens\settings\SettingsSecurityAndBackupTab.kt"
lines = io.open(p, encoding="utf-8").read().split("\n")

# locate the showMessage Card item block (around 692-708)
c1 = None
for i, l in enumerate(lines):
    if l.strip().startswith("showMessage?.let { message ->"):
        c1 = i
        break
assert c1 is not None
assert "Card(" in lines[c1+2], lines[c1+2]
# block: showMessage?.let { message -> / item { / Card( ... } / } / }
depth = 0
end = None
for j in range(c1, len(lines)):
    depth += lines[j].count("{") - lines[j].count("}")
    if j > c1 and depth <= 0:
        end = j
        break
assert end is not None
del lines[c1:end+1]
# also remove preceding blank line if doubled
if lines[c1-1].strip() == "" and lines[c1].strip() == "":
    del lines[c1]

src = "\n".join(lines)

# add snackbar state + LaunchedEffect after showMessage declaration
old_state = "    var showMessage by remember { mutableStateOf<String?>(null) }"
assert src.count(old_state) == 1
src = src.replace(old_state, """    var showMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(showMessage) {
        showMessage?.let {
            snackbarHostState.showSnackbar(it)
            showMessage = null
        }
    }""")

# find the LazyColumn at old line 612 (now shifted): locate the one belonging to BackupTab — it's the LazyColumn after showMessage? showMessage at ~584 was BEFORE LazyColumn at 612? grep said 584 showMessage and 612 LazyColumn, but 894 is another LazyColumn. The BackupTab LazyColumn is the one containing the removed block. Find LazyColumn start before c1 region: search for "LazyColumn(" occurrence nearest above the deletion point.
# After deletion, re-locate: find the LazyColumn that contains "备份列表" item (the backup list). We need to wrap THAT composable in a Box. Let me find the BackupTab function's LazyColumn: search backwards from where block was for "    LazyColumn(" with modifier fillMaxSize and contentPadding param.
idx = src.find("    LazyColumn(")
# ensure it's the BackupTab one: it should appear after "fun BackupTab"
assert idx > src.find("fun BackupTab")
old_lazy = """    LazyColumn(
        modifier = modifier.fillMaxSize(),"""
assert src.count(old_lazy) >= 1, src.count(old_lazy)
# replace only the first occurrence after BackupTab
pre = src[:idx]
seg = src[idx:]
seg = seg.replace(old_lazy, """    Box(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),""", 1)
src = pre + seg

# close the Box after the LazyColumn's matching end: find the closing "    }" that ends LazyColumn — locate by scanning from idx with brace counting on the modified text
start = src.find("    Box(modifier = modifier) {", src.find("fun BackupTab"))
assert start != -1
i = src.find("LazyColumn(", start)
# count braces from the LazyColumn line to find its end
depth = 0
j = src.find("{", i)
k = j
while k < len(src):
    if src[k] == "{":
        depth += 1
    elif src[k] == "}":
        depth -= 1
        if depth == 0:
            break
    k += 1
lazy_end = k  # index of closing brace of LazyColumn
insert_at = lazy_end + 1
snack = """
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }"""
src = src[:insert_at] + snack + src[insert_at:]

io.open(p, "w", encoding="utf-8", newline="\n").write(src)
print("OK")
