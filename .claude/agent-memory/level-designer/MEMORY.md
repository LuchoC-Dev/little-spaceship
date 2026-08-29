# level-designer memory

- [Level 1 content mechanics](project_level-one-content-mechanics.md) — where a formation actually
  lands, `offsetY` as a head start in pixels rather than seconds, motion being archetype data with no
  per-event override, nothing despawning except projectiles, the carrier's lockstep spawners, and why
  `combatY` decides whether the boss can hit anything.
- [Writing content through Bash](feedback_bash-heredoc-for-content.md) — heredocs carrying long JSON
  or Markdown abort before running; write to the scratchpad and `cat` it in.
- [Verifying content against the loader](project_verifying-content-against-the-loader.md) — load the
  whole `assets/data` set through a real `JsonContentSource`, and the Windows classpath details that
  make the throwaway program run.
- [Wave migration mechanics](project_wave-migration-mechanics.md) — a wave's `FixedDuration` is shared
  across every placement that reuses it, negative offsets are a no-op in `SpawnSystem` as written, no
  test touches the real `assets/data/level-01.json`, and how to live-run it without a full app context.

This file indexes what this agent learns that `docs/` has no reason to hold:
pacing that did not survive contact with the build, a formation that reads differently than it
looked on paper, a limit of the content format found while using it.

Phase progress does not belong here — that is `status.md`'s job, and two copies of it rot.
