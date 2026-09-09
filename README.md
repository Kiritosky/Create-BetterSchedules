# Create: BetterSchedules

An add-on for **Create** that teaches train schedules how to handle trains with more than one wagon,
and how to make decisions instead of just repeating a fixed loop.

> **Early version.** Everything here loads and runs, but the shunting has not been through a long
> playtest yet. Try it on a copy of your world first.

**Needs:** Minecraft 1.21.1 · NeoForge · Create 6.0.10 or newer

---

## The problem it solves

A Create station serves exactly one spot on the track. If your train has four fluid tanks, the pump
fills whichever tank happens to be parked next to it and the other three go home empty. The usual
workarounds are building four pumps, or splitting the train up.

This mod adds a schedule condition that just moves the train along by one wagon and waits again,
until every tank or vault has had its turn. Same for unloading.

While we were in there, we added the other things schedules could not express: routing based on what
the train is carrying, repeating a section of the schedule, and holding a train back until the
platform it's heading for is actually free.

---

## Where to find it

Everything lives in the normal **Schedule** item. Open it, add an entry, and the new options appear
in the same dropdowns as Create's own — orange lines for **destinations**, blue lines for **wait
conditions**. There are no new blocks or items to craft.

One thing worth knowing about Create's editor, because these features lean on it:

- Conditions **stacked in one column** happen one after another, in order. Top to bottom.
- **Separate columns** are alternatives. The train leaves as soon as any one column finishes.

So a column reading *"Serve every wagon" → "Next platform is free"* means "load everything, then
hold until the exit is clear". A second column holding just a 5 minute timer would mean "…or give up
after five minutes, whichever comes first".

---

## Loading and unloading every wagon

### Serve every wagon

Add this as a wait condition on any stop and the train will work through its whole length:

1. Waits until nothing has been loaded or unloaded anywhere on the train for a few seconds.
2. Rolls forward by exactly one wagon.
3. Repeats until every wagon has been parked at your loading spot.
4. Rolls back to where it originally stopped.

It doesn't need to be told whether you're filling or draining, and it doesn't care whether the cargo
is items or fluid. It only watches whether anything moved recently. One condition handles a five-tank
train on a single pump, or a five-vault train on a single Portable Item Interface.

**Settings**

| Setting | What it does |
| --- | --- |
| Idle time before advancing | How long nothing may move before the train shuffles on. Default 3s. Raise it if your pump is slow or stutters. |
| When every wagon is done | *Roll back to the start* (default) puts the train back where it docked. *Stay where it is* leaves it at the last wagon. |

The train stays docked at the station the whole time — display links, the station UI and the rest of
the schedule keep working normally.

### Shunt the train

The manual version: roll a set distance, then carry on with the schedule. Use it when your wagons
aren't evenly spaced, or when you want to interleave shunting with your own conditions.

Distance can be in **meters** or **wagon lengths**, and a negative number reverses.

```
Wait until:   Fluid  >= 8b
and then:     Shunt the train   6 meters forward
and then:     Fluid  >= 16b
and then:     Shunt the train   6 meters forward
```

---

## Measuring one wagon at a time

Create's own cargo conditions add up the entire train, so "wagon 1 is full" is impossible to say.
These four are scoped to a single wagon — or to "any wagon", which behaves like Create's originals.

- **Wagon holds fluid** / **Wagon holds items** — an exact amount, like Create's, plus a wagon picker.
- **Wagon fill level (fluid)** / **Wagon fill level (items)** — a **percentage** of the capacity that
  is actually installed.

The percentage ones are the better default. "Wait for 16 buckets" quietly becomes wrong the moment
you add a wagon; "wait until 80% full" keeps working on a two-tank train and an eight-tank train
alike. They're just as useful inverted for unloading — *leave when under 10% full*.

Put an item in the slot to filter by cargo type, or leave it empty to mean "anything".

---

## Routing by cargo

### Destination if carrying

A destination the train only travels to when its cargo matches. If it doesn't match, the entry is
skipped and the schedule moves straight on to the next one.

That turns a cyclic schedule into a list of rules, read top to bottom. **Leave the filter slot empty**
and it means "any cargo at all", which is how you express loaded vs. empty:

```
1. Destination if carrying   [empty slot]  carrying      ->  "Refinery"
     Wait until:  Serve every wagon
2. Destination if carrying   [empty slot]  not carrying  ->  "Oil Field"
     Wait until:  Serve every wagon
```

An empty train skips rule 1 and heads to the Oil Field. Once loaded it matches rule 1 and goes to the
Refinery. No redstone, no second schedule.

Sorting by cargo type works the same way — put the item or a bucket of the fluid in the slot:

```
1. Destination if carrying   [iron ore]    carrying      ->  "Smelter"
2. Destination if carrying   [coal]        carrying      ->  "Power Plant"
3. Destination if carrying   [empty slot]  not carrying  ->  "Mine"
```

### Jump to entry

Continues the schedule from a different entry number, so a train can run the same few stops
repeatedly without you pasting them in over and over.

```
1. Travel to "Farm"        wait until  fill level >= 90%
2. Travel to "Silo"        wait until  fill level <= 10%
3. Jump to entry 1, max 4 times
4. Travel to "Depot"       wait 30s
```

Entries 1 and 2 run four times, then the jump falls through to entry 4 and the counter resets for
next time. Set **Max jumps** to 0 to jump every time instead.

---

## Keeping the network moving

### Next platform is free

Create's pathfinding *prefers* an empty station, but it will still send a train to an occupied one,
where it stops on the approach and blocks the signal behind it. On a busy network that's how a
five-train pile-up starts.

Add this condition to a stop and the train waits **where it is** until the station it's about to
travel to is genuinely clear, so the queue forms harmlessly at a platform instead of across your main
line.

**Setting:** count a platform as busy when a train is *docked or on approach* (default, more
cautious) or only when a train is *docked*.

It reads ahead to the next destination in the schedule. If that destination is a "Destination if
carrying" that ends up being skipped, it will have checked the wrong station — in that case put the
condition on a stop with a plain destination after it.

---

## Good to know

- **Shunting needs track ahead of the train.** If the way is blocked by a signal or the track simply
  ends, the shunt gives up after about 5 seconds and the schedule carries on rather than freezing.
- **Keep shunting in one column.** Two shunting conditions in two different columns of the same stop
  will fight over the train and neither will land where you want.
- **"Serve every wagon" assumes evenly spaced wagons.** It moves by one wagon pitch each time. If
  your train is a mix of long and short wagons, use "Shunt the train" with explicit distances.
- **A wagon number higher than your train has wagons** counts as satisfied rather than stranding the
  train forever. If a condition seems to pass instantly, check the wagon picker.
- **Percentages are measured against installed capacity.** A train with no tanks at all reads as
  satisfied, for the same reason.

---

## Building from source

```
./gradlew build        # jar lands in build/libs/
./gradlew runClient    # dev client with Create already installed
```

Needs JDK 21. Dependency versions live in `gradle.properties` and follow the
[Create wiki's add-on guide](https://wiki.createmod.net/developers/depend-on-create/neoforge-1.21.1).

---
