"""Run from the repo root (the folder containing app/ and gradlew):  python apply_patches.py
Patches the two large files that only needed one-block edits."""
import pathlib, sys

base = pathlib.Path("app/src/main/java/com/cascade/attentiontwin")

# 1) Policy.kt: demo mode skips the cold-start pass-through
p = base / "engine/Policy.kt"
t = p.read_text(encoding="utf-8")
old = "if (st.nSeen < 3) {"
new = "if (st.nSeen < 3 && !s.demoMode) {"
if new in t: print("Policy.kt already patched")
elif old in t: p.write_text(t.replace(old, new, 1), encoding="utf-8"); print("Policy.kt patched")
else: sys.exit("Policy.kt: cold-start line not found")

# 2) Components.kt: flat, minimal GlassCard
c = base / "ui/components/Components.kt"
t = c.read_text(encoding="utf-8")
old = """        modifier = modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(GlassBgTop, GlassBgBottom)
                )
            )
            .border(1.dp, CardBorder, shape)
            .padding(16.dp),"""
new = """        modifier = modifier
            .clip(shape)
            .background(Surface1)
            .padding(16.dp),"""
imp = "import com.cascade.attentiontwin.ui.theme.Surface2\n"
if new in t: print("Components.kt already patched")
elif old in t:
    t = t.replace(old, new, 1)
    if "theme.Surface1" not in t:
        t = t.replace(imp, "import com.cascade.attentiontwin.ui.theme.Surface1\n" + imp, 1)
    c.write_text(t, encoding="utf-8"); print("Components.kt patched")
else: sys.exit("Components.kt: GlassCard block not found")
