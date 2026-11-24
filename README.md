# Plug & Plant Farming
<img src="icon.png" alt="Plug & Plant Farming icon: a terracotta plug whose cable is a sprout" align="right" width="96">

A farm run helper that works with what you have and the plugins you already use. Bring whatever seeds and saplings
you've got: any herb seed goes in any herb patch, and allotments and flowers are tended for as long as your seeds
last. It plans herb runs and tree runs on top of RuneLite's own Time Tracking and Bank Tags plugins and the
Shortest Path plugin, so there's nothing to set up beyond picking what to bring.

## Works with
- **Time Tracking** (core, required): where patch states come from. Runs can't start without it.
- **Bank Tags** (core): filters the bank down to the run's items. Works alongside Bank Tag Layouts.
- **[Shortest Path](https://github.com/Skretzo/shortest-path)** (Plugin Hub, optional): routes between stops. Runs
  work without it, just without a drawn path.

The side panel says which of these are missing.

## Features
- **Herb runs and tree runs.** Switch between them at the top of the side panel. A herb run stops at each location
  whose herb patch needs attention when the run starts; a tree run at each location whose tree or fruit tree does.
  Each stop is visited once, so a patch that turns ready later in the run doesn't send you back. Arrive somewhere
  whose patch needs work but wasn't predicted ready, and it joins the run; arrive where nothing needs doing, and the
  in-game box says how long the patch has left.
- **Any seed.** A patch needs a seed of its kind and the plugin doesn't care which. If you don't carry one for the
  next stop, Shortest Path routes you through a bank first. Seeds in a seed box count once the game has shown its
  contents.
- **Run options.** Before a run, pick in the side panel what it includes: compost, allotments, flowers, gardener
  payments, graceful and volcanic ash. An allotment takes three seeds of one kind.
- **Tree runs.** Trees at Lumbridge, Varrock, Falador, Taverley, the Gnome Stronghold, Auburnvale and the Farming
  Guild, and fruit trees at the Gnome Stronghold, Tree Gnome Village, Catherby, Brimhaven, Lletya, Kastori and the
  Farming Guild. A watered seedling counts as a sapling, as it will have grown into one by the time you plant it.
  Each tree goes through checking its health, taking it down (chop it, or pay the gardener 200
  coins, or carry an axe), digging up the stump, planting any sapling of the right kind, and paying for protection.
  The checklist counts the payment for every sapling you carry, the bank filter shows the payments, and the
  gardener is highlighted from the moment the tree is in. Without the payment on you, the step is skipped and the
  tree goes unprotected.
- **Step-by-step highlights.** During a run each patch that needs something gets a box in the colour of its next
  step: Harvest, Check health, Remove, Dig up, Clear, Cure or Prune, Rake, Compost, Plant or Pay. The inventory items
  the patches you are at need next are highlighted in the same colour.
- **Checklist.** What to bring for the rest of the run, checked off against what you carry: seeds or saplings,
  payments, compost, tools and teleports. What is left to fetch comes first and what you have is tucked below.
  Tools and compost you keep with the tool leprechaun show as "At leprechaun", so you can pick them up there or take
  them from the bank. Spell runes count those in a rune pouch, combination runes, and an equipped staff or tome. The
  checklist is in the side panel, and in a box in the game while you are at a bank; away from one the box keeps to
  the steps at the stop you are at.
- **Wear to make room.** Run items in your inventory that you could wear in a free slot, such as a teleport cloak,
  graceful or an axe, are outlined in white while you pack for a run and during one.
- **Bank filter.** Opening the bank during a run shows only the run's items, in sections: everything that can be
  worn first, so you can take it and put it on before the rest, then equipment, seeds or saplings, gardener
  payments and teleports. A tree run adds the seeds still on their way to becoming saplings, with plant pots, a
  trowel and watering cans. A button with the Farming icon at the top of the bank turns it on and off at any time.
- **Preferred teleports.** Pick what to bring for each location: any teleport item that goes there, one in
  particular, the runes for a teleport spell, a way into your house, or nothing. During a run Shortest Path is
  limited to the teleport items you carry, so the route follows what you brought.
- **Stop order.** A setting picks how the next stop is chosen: whichever is quickest to reach from where you are
  (the default), or the order of the OSRS Wiki's farming run guides.
- **Farming contracts.** If your Farming Guild contract is for a crop a run grows and the guild is on that run, the
  guild's patch is planned with the contract seed or sapling.
- **Farming Guild tiers.** Guild patches above your Farming level (boosts count) are left out.

During a run the panel follows it: finished stops shrink to a line each, the stop you are at or headed to shows each
patch's next step with the very next one marked, and the stops to come show how much is left to do there. If a stop
can't be finished, its "Skip stop" button leaves it out of the rest of the run.

## Limitations
- Patches you haven't visited since they last changed are as Time Tracking predicts them.
- Tree Gnome Village has no teleport item to pick; Shortest Path still routes there by spirit tree.
- Calquat, celastrus, hardwood, spirit tree, hops and bush patches aren't part of a run.

## Development
Run a development client with the plugin loaded:

```bash
./gradlew run
```

Log in using the [Using Jagex Accounts](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts) instructions.
