# Working with agents

Defined on 19/08/2026, before writing the first line of code of the real project.

## The model

A **boss** who does not carry out the fine-grained work, but decides, plans and distributes it. Below, specialised agents doing the work within a clear boundary.

The boss is the main Claude session, not a subagent. It holds the context of the whole planning stage —the measurements, the traps found, why each alternative was discarded— and that context would be lost by delegating it to an agent that starts cold.

## Memory: how context is preserved

We distinguish two things:

**Live memory.** A launched agent keeps its context while the session stays open, and work can keep being sent to it without repeating anything.

**Persistent memory.** Each agent has its own directory in `.claude/agent-memory/<agent>/`, declared with `memory: project` in its definition. When any new instance starts, the harness automatically loads its `MEMORY.md`. That way an instance launched tomorrow knows what today's learned.

That memory is **not loaded into the main conversation**: it is exclusive to the agent, and that is why it does not inflate the boss's context.

Since it lives under `.claude/agent-memory/` and not in `agent-memory-local/`, it goes into git: the accumulated knowledge is part of the repository.

### What is saved and what is not

A rule that avoids the most common problem: **an agent's memory does not repeat what is already in `docs/`**.

It saves only what that agent discovered and is not written anywhere else: where certain code lives, which trap cost it an hour, what decision of its own it made and why. If we duplicate the specification across six memories, in two weeks there are six sources of truth contradicting each other.

It is written **when a task finishes**, not continuously.

## Roster

| Agent | Role | Writes in |
|---|---|---|
| boss (main session) | decides, plans, delegates, reviews | everything |
| `core-domain` | rules, ECS, systems | `core/` |
| `game-presentation` | rendering, HUD, screens, audio, input | `game/`, `desktop/`, `web/` |
| `visual-designer` | visual direction and specs | documents |
| `test-engineer` | unit tests and replays | tests |
| `reviewer` | auditing | nothing |

### Why these boundaries

They are not arbitrary: **they come from the architecture**. Since the modules already have dependencies in a single direction, file ownership splits itself and two agents cannot step on each other.

`core-domain` and `game-presentation` are the back/front separation the hexagonal architecture already imposes, applied to who works on each side.

The boundaries are enforced by instruction, not by tooling, and it is worth being exact about that. `reviewer` is told to read and report, but its definition grants `Bash`, which can write, delete and run Git like any other shell. An earlier version of this document claimed it had no write tools and therefore could not modify anything; that was never true. What actually keeps a reviewer honest is the prompt and the fact that its work is reviewed in turn.

### Why there is no content agent

It was considered and discarded. Content —balance JSON, the odd sprite— is touched rarely and with small changes. An agent dedicated to that would be bureaucracy: it is done by whoever is working at the time.

## What a session costs, and when to stop

Added on 22/08/2026 after an audit of five days of agent use. The architecture held; the way it was
run did not. Roughly 3,300 model calls processed 665 million cached input tokens, and the run hit a
spend limit **fourteen times** across nine contexts. Two thirds of the equivalent cost was re-reading
conversation history, not new work.

The lesson is not "use fewer agents". It is that a long-lived coordinator is the most expensive thing
in the system, because every turn re-reads a prefix that only grows. A late call in a days-old session
cost four times what an identical early one did.

**One coordinator per phase, or per a few hours of work.** Close it with the state in Git and start a
new one. `docs/STATUS.md` plus each phase's `status.md` is what a fresh session reads to catch up —
that is what they are for, and they work.

**Sonnet coordinates; Opus decides.** Git, launching agents, checking builds and relaying reports do
not need the heavier model. Bring Opus in for architecture, real ambiguity, and audits that turn on
judgement — then leave.

**`reviewer` defaults to Sonnet**, as its definition already says. Escalating to Opus needs a reason
written into the prompt. Nine reviews ran on Opus here on the strength of one approval given for one
specific audit; that is not what was authorised.

**A worker is one issue.** Past roughly 60–80 model calls, split it. One phase-05 agent reached 312
calls and 104 million processed tokens in a single run — most of it because correction rounds were
sent back to the same agent instead of closing it and reopening against the state already in Git.

**Any spend-limit message stops the flow.** Report it and ask before relaunching. Treating a limit as
"nothing was lost, continue" is how this project hit fourteen of them.

**Keep image inspection short and separate.** A screenshot's tokens stay in context for every turn
that follows. Look, write the conclusion down, and close the context.

## How a task is distributed

1. The boss decides what has to be done and against which documents it is validated.
2. It writes a concrete plan for the corresponding agent, with the objective, the boundary and the invariants at play.
3. The agent consults its memory, executes and saves what it learned.
4. The boss reviews the result and, if the work justifies it, passes it through `reviewer`.

The boss **does not launch agents by default**. Delegating costs: each agent starts cold and re-reads context the boss already has. Delegation happens when the task is large and isolated, or when it is convenient for its output not to occupy the main context.

## Branches

Work happens on branches, never directly on `main`. Names follow `type/description`, and the branch merges back into `main` once the change is complete.

## Parallel work

Two different situations, two mechanisms.

**Several Claude sessions at once.** Each session gets its own **git worktree**. They share the repository and its history but not the working tree, so two sessions cannot overwrite each other's files. Each works on its own branch.

**Subagents launched from one session.** No agent definition carries an isolation field, and the one attempt to use the harness's `isolation: worktree` option failed here: this repository resolves the created worktree outside the intended directory, so the harness refused it. What works is the plain thing — create the worktree by hand with `git worktree add ../little-spaceship-<task> -b <type>/<description>` and give the agent that absolute path as its working directory. Return the main worktree to `main` afterwards: an agent branches from wherever it finds HEAD, and leaving a feature branch checked out there is what once made a phase branch off the art lane by mistake.

Either way, the first line of defence remains the module boundary: two agents that own different modules rarely collide even sharing a working tree.

## Language

The agent definitions are **in English**, like everything that lives in the repository, and the same applies to what they produce: code, comments and logs.

The planning documentation in `docs/planning/` has now been translated to English as well, so agents read and write in English throughout. The only Spanish that remains is the conversation with the user, plus the verbatim transcript in `docs/sources/`, which is kept untouched because it is the record of a real conversation.
