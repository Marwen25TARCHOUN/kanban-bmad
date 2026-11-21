# BMad script patches — re-apply after installer updates

The BMad installer regenerates `_bmad/scripts/*` on every install/update. If a
reinstall ever happens, re-apply the patches below.

## 2026-11-21 — `render_skill.py` short-config dedup on value

**Why.** `_resolve_short_config` strictly HALTs on `len(matches) > 1` for a short
config key (e.g. `{implementation_artifacts}`), even when the two matches carry
**identical values**. This project's install has both `modules.bmm.implementation_artifacts`
and `modules.gds.implementation_artifacts` set to the same path, so every skill
that relies on the short name (bmad-build was the first) HALTs with:

```
HALT: ambiguous config value `implementation_artifacts` found at:
  modules.bmm.implementation_artifacts, modules.gds.implementation_artifacts
```

The fix treats matches that all agree on the same value as non-ambiguous and
only HALTs when the values genuinely differ.

**Location.** `_bmad/scripts/render_skill.py`, inside `_resolve_short_config`
(around line 146).

**Diff (apply inside `if len(matches) > 1:`):**

Replace:

```python
if len(matches) > 1:
    paths = ", ".join(path for path, _ in matches)
    raise RenderError(f"ambiguous config value `{key}` found at: {paths}")
```

With:

```python
if len(matches) > 1:
    # LOCAL PATCH: treat matches that all agree on the same value as non-ambiguous.
    # BMad modules (bmm, gds, …) can legitimately register identical path values for
    # cross-module conveniences; the strict-first-match semantics would HALT those.
    # See _bmad/scripts/PATCH_NOTES.md for restoration if a BMad update overwrites this.
    distinct_values = {value for _, value in matches}
    if len(distinct_values) > 1:
        paths = ", ".join(path for path, _ in matches)
        raise RenderError(f"ambiguous config value `{key}` found at: {paths}")
```

The `matches[0]` pickup immediately after is unchanged — it now receives the
first of the agreed-upon matches instead of the sole match.

**Scope of the change.** Local to this project only. Does not affect BMad
upstream. Still HALTs correctly on genuine disagreements.
