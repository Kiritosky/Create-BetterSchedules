<p align="center">
  <img src="PASTE-LOGO-URL-HERE" width="180" alt="Create: BetterSchedules">
</p>

<h1 align="center">Create: BetterSchedules</h1>

<p align="center"><b>Your train has four tanks. Your station fills one of them.<br>Let's fix that.</b></p>

<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.21.1-62B47A?style=for-the-badge" alt="Minecraft 1.21.1">
  <img src="https://img.shields.io/badge/NeoForge-21.1-F16436?style=for-the-badge" alt="NeoForge">
  <img src="https://img.shields.io/badge/Requires-Create%206.0.10+-C9A44C?style=for-the-badge" alt="Requires Create 6.0.10+">
</p>

---

## 🚋 The one-wagon problem

A Create station serves exactly **one spot on the track**. Build a train with four fluid tanks and
the pump fills whichever tank happens to stop next to it — the other three go home empty.

So you build four pumps. Or four stations. Or you give up and run four short trains.

**BetterSchedules adds one condition that fixes it:** the train loads a wagon, rolls forward by
exactly one wagon, and loads the next. Until the whole train is full. Then it rolls back and carries
on with its schedule.

<!-- SCREENSHOT 1 — the hero shot. A 4-tank train at a single pump, mid-shunt. Paste image here. -->

It works exactly the same for **unloading**, and exactly the same for **item vaults**. It never needed
to be told which — it just watches whether any cargo moved recently.

---

## ✨ What's in the box

Nine new options inside the schedule you already use. **No new blocks, no new items, nothing to
craft.** They show up right next to Create's own entries.

<!-- SCREENSHOT 2 — the schedule editor dropdown open, showing the new entries mixed in with Create's. -->

### 🛢️ Serve every wagon
The headline. Works through the entire train at one loading spot, then returns to where it docked.
Set how long to wait before shuffling on, and whether to roll back at the end.

### 🔧 Shunt the train
The manual version — roll a set distance in meters or wagon lengths, then continue. For trains where
the wagons aren't evenly spaced.

### 📊 Wagon fill level (fluid / items)
Wait until a wagon is **80% full** instead of "16 buckets". Add a wagon later and the schedule still
works. Just as good inverted: *leave when under 10% full*.

### 📦 Wagon holds fluid / items
Create's cargo conditions, but scoped to **one specific wagon** instead of adding up the whole train.

### 🔀 Destination if carrying
A destination the train only takes **if its cargo matches** — otherwise it's skipped. Chain a few and
your schedule becomes a list of routing rules. Leave the filter empty and it means "any cargo at
all", which is how you finally express *loaded vs. empty* without a single redstone link.

### 🔁 Jump to entry
Continue from an earlier entry, optionally only a set number of times. Run the farm-to-silo shuttle
four times, *then* go refuel — without pasting the same two stops in eight times.

### 🚦 Next platform is free
Create will happily send a train to an occupied station, where it parks on the approach and blocks
the signal behind it. This holds the train **where it is** until the destination is genuinely clear,
so queues form at a platform instead of across your main line.

<!-- SCREENSHOT 3 — two trains at a junction, one held at a platform while the other clears. -->

---

## 📋 Copy these

**Fill a four-tank train from a single pump — the entire schedule:**

```
Travel to Station    "Oil Field"
  Wait until:        Serve every wagon      3s, roll back to the start

Travel to Station    "Refinery"
  Wait until:        Serve every wagon      3s, roll back to the start
```

**Loaded goes one way, empty goes the other — no redstone:**

```
1. Destination if carrying  [empty slot]  carrying      ->  "Refinery"
     Wait until:  Serve every wagon
2. Destination if carrying  [empty slot]  not carrying  ->  "Oil Field"
     Wait until:  Serve every wagon
```

**Sort cargo across three destinations:**

```
1. Destination if carrying  [iron ore]    carrying      ->  "Smelter"
2. Destination if carrying  [coal]        carrying      ->  "Power Plant"
3. Destination if carrying  [empty slot]  not carrying  ->  "Mine"
```

**Four laps of a shuttle, then a service stop:**

```
1. Travel to "Farm"     wait until  fill level >= 90%
2. Travel to "Silo"     wait until  fill level <= 10%
3. Jump to entry 1, max 4 times
4. Travel to "Depot"    wait 30s
```

<!-- SCREENSHOT 4 — a finished schedule item open, showing one of the examples above. -->

---

## 📦 Requirements

| | |
| --- | --- |
| Minecraft | 1.21.1 |
| Loader | NeoForge |
| Create | 6.0.10 or newer |
| Client & server | Needed on both |

Existing schedules and existing worlds are untouched — the new entries only appear if you add them.

---

## ❓ FAQ

**Does it need to go on the server?**
Yes, on both sides. It's not a client-side mod.

**Will it break my existing trains?**
No. It only adds options to the schedule editor. Schedules you've already built keep working exactly
as they do now.

**Does the train leave the station while shunting?**
No. It stays docked the whole time, so display links, the station screen and the rest of your
schedule keep working.

**What if the track ahead is blocked?**
The shunt gives up after about 5 seconds and the schedule carries on, rather than freezing the train.

**Fabric?**
Not planned right now.

---

## ⚠️ Early version

Everything here loads and runs, but the shunting hasn't been through a long playtest yet. Try it on a
copy of your world first, and please report anything odd — bug reports with a screenshot of the
schedule are enormously helpful.

---

<!--
======================================================================
BEFORE YOU PUBLISH — delete this whole block afterwards
======================================================================

Screenshots to take (there are 4 slots marked SCREENSHOT 1-4 above):

  1. HERO — a 4-tank train at one pump, caught mid-shunt. Shoot it from the
     side, slightly elevated, so all four tanks and the pump are in frame.
     This is the image that sells the mod. A short GIF works even better.

  2. EDITOR — the schedule item open with the entry-type dropdown expanded,
     so people can see the new options sitting next to Create's.

  3. JUNCTION — two trains near a shared platform, one visibly held back.
     Optional; skip if it's hard to stage.

  4. SCHEDULE — a finished schedule from the "Copy these" section, so people
     can see what they're aiming for.

Also:
  - Replace PASTE-LOGO-URL-HERE at the top with your logo (upload it to the
    Modrinth gallery first, then copy the image URL).
  - Once the project is live you can swap the static badges for real ones:
    https://img.shields.io/modrinth/dt/YOUR-PROJECT-SLUG?style=for-the-badge
  - Set the Modrinth "Environment" fields to: Client REQUIRED, Server REQUIRED.
  - Add Create as a REQUIRED dependency on the project's Dependencies tab.
-->
