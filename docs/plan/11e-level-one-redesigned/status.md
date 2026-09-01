# Phase 11e — Level 1 redesigned, balance and the boss · status

**State:** done on the phase branch — six issues closed through six pull requests, the candidate played by the project owner on 01/09/2026 and tuned from that session. **Pull request open against `dev` and unmerged**, waiting on the project owner's direct approval.
**Updated:** 01/09/2026

This file holds the phase's `State:` line and its narrative, and the coordinator writes it — at the phase's opening and at its close.

**Per-task progress does not live here.** It lives in `status/`, one file per task, written by whoever did that task on its own branch. Read those for what each one did; this is what the phase amounts to.

## Done

Six issues, six pull requests, all merged into `phase/11e-level-one-redesigned`.

| Issue | What | PR |
|---|---|---|
| [#198](https://github.com/LuchoC-Dev/little-spaceship/issues/198) | Level 1 rebuilt on the beats, and the shapes 11c built finally used | [#207](https://github.com/LuchoC-Dev/little-spaceship/pull/207) |
| [#199](https://github.com/LuchoC-Dev/little-spaceship/issues/199) | Real `Health` on the enemy archetypes | [#204](https://github.com/LuchoC-Dev/little-spaceship/pull/204) |
| [#200](https://github.com/LuchoC-Dev/little-spaceship/issues/200) | The boss aims at the player instead of at fixed angles | [#203](https://github.com/LuchoC-Dev/little-spaceship/pull/203) |
| [#201](https://github.com/LuchoC-Dev/little-spaceship/issues/201) | The play session, and the six questions answered | [#211](https://github.com/LuchoC-Dev/little-spaceship/pull/211) |
| [#202](https://github.com/LuchoC-Dev/little-spaceship/issues/202) | The document read back against the level built | [#209](https://github.com/LuchoC-Dev/little-spaceship/pull/209) |
| [#210](https://github.com/LuchoC-Dev/little-spaceship/issues/210) | Tuned from the session | [#212](https://github.com/LuchoC-Dev/little-spaceship/pull/212) |

**Task 7 needed no branch.** [#23](https://github.com/LuchoC-Dev/little-spaceship/issues/23) was already closed and tested when the phase opened: `core/domain/system/SpawnSystem.java` attaches the `Drop` only when `i == event.dropSlot()`, and `SpawnSystemTest.designedDropAttachesToExactlyOneSlot` asserts it.

**What level 1 is now.** Twelve waves over fourteen beats, the wave chain ending at 134.5 s with the boss entering at 134.5 s, against 302 s when the phase opened. `grep -c trajectory assets/data/waves.json` went from 0 to 13, which closes the gap 11c left behind — no level had used a movement shape until this phase. The boss's spread and sweep no longer fire at fixed outward and inward angles: `core/domain/system/BossSystem.java` locks the player's position at the instant a tell begins and fans five rays per part at that frozen point, built from vector arithmetic and `Math.sqrt` rather than `sin`/`cos` so that determinism survives TeaVM.

## The phase's own result

**The candidate was wrong in ways only playing could find, and that is the finding.**

The phase was structured around a rule this project decided twice, on 22/08 and 25/08: balance is tuned by playing, not by arithmetic. The running order made it unavoidable — agents build a candidate, the project owner plays it, the phase tunes from what they report, and **the candidate is not the deliverable**. Three things came out of that structure that no other arrangement would have produced.

**It reversed a change made the day before, on the same evidence.** [#199](https://github.com/LuchoC-Dev/little-spaceship/issues/199) raised enemy health because the repository's own arithmetic said a heavy carrier died in 1.2 s against the 32 s its stretch reserves. The session said the first ninety seconds were too hard, and the health came down again the next day: `enemy-basic` 30 to 20, `enemy-light` to no component at all, `enemy-shooter` 40 to 30, `enemy-tank` 300 to 200, `enemy-carrier` 1000 to 700. Both passes were right about their own question, and only one of them was about the game.

**It found two defects that every check passed.** Beat 1 put five `enemy-light` at second zero of what the campaign document calls an audiovisual introduction. Beat 14 was a 7 s escort starting exactly at `boss.entersAt`, built so that the acceptance criterion "fourteen waves, one per beat" would come out even; the owner read it as a bug — *"creo que este no es la última ola"*. `tools/build-level-docs.js` reported the Checks section clean for both. `tools/pre-pr-check` was green. `reviewer` audited beat 14 specifically, on the coordinator's explicit request, and called it *"a sound design call, correctly argued"*; the coordinator agreed with that reading and merged it. **One run of `./gradlew :desktop:run` found it.**

**The criterion those two waves existed to satisfy was rewritten rather than worked around.** Level 1 is now fourteen beats, twelve of which carry a wave — which is what `docs/plan/11c-movement-shapes/shape-catalogue.md`'s original beat map already said for beats 1 and 14 before the phase began. `assets/data/` cannot express the alternative: `JsonContentSource.loadWaves` rejects a wave with no spawns, so a beat that occupies time and spawns nothing has no representation. The criterion is struck through and dated in [`plan.md`](plan.md), and the reopening is recorded in `docs/planning/08-decisions-and-open-items.md` under "Level 1 played, 01/09/2026", following the form the 27/08 reopening of the level's length used. **A beat is a unit of design; a wave is a unit of content, and the two do not have to be one-to-one.**

**And the boss was confirmed rather than tuned.** The owner called the fight's difficulty *ideal* — one session after the same boss was diagnosed as *"a positioning problem solved once, not a dodge"*, beatable by parking at screen centre. The only change asked for was framed as minimal and applied as such: `spreadProjectileSpeed` 95 to 85 and `sweepProjectileSpeed` 140 to 125, with the aim, the tell, `patternCooldown 0.7` and the part health all untouched.

## Decisions taken while implementing

- **Level 1 is fourteen beats, twelve with a wave.** Above, and in `docs/planning/08-decisions-and-open-items.md`.
- **`enemy-light` has no `health` component rather than a low value.** `core/domain/system/DamageSystem.java` makes any value at or below `weaponProjectileDamage` behave exactly like no component, so a `health` of 10 would have read in the JSON as a decision while being a no-op. The JSON says what is true.
- **The carrier's `Spawner` interval came from 4.0 s to 3.0 s**, decided by the project owner when the coordinator put three options to them. At 700 hp the carrier dies before its first child under ideal fire, which is [#199](https://github.com/LuchoC-Dev/little-spaceship/issues/199)'s defect reappearing from the other side. **The fix went to the mechanism rather than to the health**, so the difficulty the owner had just approved by playing stayed where they put it.
- **`boss.entersAt` was moved by task 1**, which [#198](https://github.com/LuchoC-Dev/little-spaceship/issues/198) had placed out of scope. `reviewer` judged it defensible rather than an overstep — a candidate cannot be played without a number there — and the session then confirmed the length. Recorded because the next phase should not read it as precedent for a task deciding a neighbouring task's number.

## What is open

**Four things this phase opened and did not close**, none of them blocking:

- [#205](https://github.com/LuchoC-Dev/little-spaceship/issues/205) — `LevelScoreReplayTest`'s javadoc cites enemy health values that no longer exist. The test passes; the sentence is false. `core-domain`'s.
- [#206](https://github.com/LuchoC-Dev/little-spaceship/issues/206) — the generated document's `shots to kill` counts projectiles, while `core/domain/system/WeaponSystem.java` fires up to five per trigger pull. Found by the first reader of 11d's document who had not helped build it, which is the test [`plan.md`](plan.md) predicted this phase would be.
- [#208](https://github.com/LuchoC-Dev/little-spaceship/issues/208) — nothing fails when `shape-catalogue.md` names a wave id that no longer exists. See below.
- The carrier's mechanism. **The 3.0 s interval closes beat 8 by 0.5 s and does not close beat 11**, where an ideally firing player at shot level 4 kills a carrier in 2.1 s. Beat 11 is the difficult encounter that hands over the attachment, so it is the beat where the mechanism matters most. Going below 3.0 s was refused: a carrier producing a child every 2 s is a different encounter rather than the same one repaired.

**Two of the session's six questions are not settled, and are recorded that way** — the acceptance criteria say an unclear answer is an acceptable one:

1. Whether `enemy-basic` reads as firing less often than `enemy-shooter`: **"se nota poco"**, hinted at and not clear.
2. Whether the boss's spread and sweep still read as **two** patterns, now that both fan around an aimed direction and differ only in which parts fire and at what speed: **not answered**. This one touches a decided rule from 21/08 and is the first item for the next session.

## Notes for whoever comes next

**The one place in the chain where a level document can still be wrong is the one it delegates to.** `docs/levels/level-01.md` is generated and `.github/workflows/ci.yml` fails if it drifts, so the generated half cannot rot. Its beat map deliberately cannot be generated — `assets/data/` has no field for design intent — and sends the reader to `shape-catalogue.md`'s hand-written table. [#198](https://github.com/LuchoC-Dev/little-spaceship/issues/198) replaced all fourteen level 1 wave ids and **every row of that table named a wave that no longer existed**, one day after C7 of `docs/plan/11d-per-level-document/document-contract.md` blessed that pointer with the word "stays", with CI green throughout. Corrected by hand and dated, recorded as **C8**, and filed as [#208](https://github.com/LuchoC-Dev/little-spaceship/issues/208).

**Two pull requests were merged without a `reviewer` pass** — [#211](https://github.com/LuchoC-Dev/little-spaceship/pull/211) and [#212](https://github.com/LuchoC-Dev/little-spaceship/pull/212) — on the project owner's explicit instruction. The coordinator verified the five health values, the twelve wave definitions and twelve placements, `boss.entersAt`, both projectile speeds and the Checks section directly against `assets/data/`. **That is a coordinator's check and not an independent audit**, and it is written here rather than left to be inferred from the absence of a review. `reviewer` did audit the other four.

**The next session's first item is now two items**, because the 3.0 s interval split them: does the carrier spawn children in beat 8, and does it in beat 11?

**Phase 12 is the honest measurement of whether this group worked**, and the roadmap says to treat that as an acceptance criterion rather than a hope. Level 1 was built by the phase that also built the mechanisms; levels 2 and 3 will be built by someone using them cold.
