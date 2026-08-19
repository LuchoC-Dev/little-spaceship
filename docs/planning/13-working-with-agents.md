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

It saves only what that agent discovered and is not written anywhere else: where certain code lives, which trap cost it an hour, what decision of its own it made and why. If we duplicate the specification across five memories, in two weeks there are six sources of truth contradicting each other.

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

The boundaries do not rely on good will alone: `reviewer` has no write tools, so it cannot modify anything even if it wanted to.

### Why there is no content agent

It was considered and discarded. Content —balance JSON, the odd sprite— is touched rarely and with small changes. An agent dedicated to that would be bureaucracy: it is done by whoever is working at the time.

## How a task is distributed

1. The boss decides what has to be done and against which documents it is validated.
2. It writes a concrete plan for the corresponding agent, with the objective, the boundary and the invariants at play.
3. The agent consults its memory, executes and saves what it learned.
4. The boss reviews the result and, if the work justifies it, passes it through `reviewer`.

The boss **does not launch agents by default**. Delegating costs: each agent starts cold and re-reads context the boss already has. Delegation happens when the task is large and isolated, or when it is convenient for its output not to occupy the main context.

## Parallel work

When two agents have to work at the same time, `isolation: worktree` is used: each one gets its own git worktree and they do not step on each other's files.

Even so, the first line of defence remains the module boundary.

## Language

The agent definitions are **in English**, like everything that lives in the repository, and the same applies to what they produce: code, comments and logs.

The planning documentation in `docs/planning/` has now been translated to English as well, so agents read and write in English throughout. The only Spanish that remains is the conversation with the user, plus the verbatim transcript in `docs/sources/`, which is kept untouched because it is the record of a real conversation.
