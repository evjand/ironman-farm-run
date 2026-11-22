# Ironman Farm Run
A farm run helper that works with what you have. It shows which herb patches need attention, routes you between
them, filters your bank to the items for the run, and walks you through each patch step by step. Allotments and
flower patches can be tended along the way.

## Features
- **Patch states from Time Tracking.** The plugin reads the core Time Tracking plugin's data, so it knows which
  patches are ready, empty, weedy, diseased or dead, how long growing ones have left, and which have been
  composted. A patch shows "No data" until you have visited it once with Time Tracking on.
- **Stops decided by the herb patch.** A run stops at each location whose herb patch needs attention when the run
  starts, and visits each one once. A patch that turns ready later in the run doesn't send you back.
- **Run options.** Before a run, pick in the side panel what it includes: compost, allotments, flowers, graceful and
  volcanic ash. Allotments and flowers are tended at each stop for as long as you carry the seeds to replant them,
  so bring as many as you like. An allotment takes three seeds of one kind.
- **Routes from Shortest Path.** Starting a run asks the [Shortest Path](https://github.com/Skretzo/shortest-path)
  plugin for the route to whichever remaining stop is cheapest to reach. When a stop is finished, or you leave it
  once its herb patch is done, the route moves on to the next one.
- **Any seed.** A patch needs a seed of its kind and the plugin doesn't care which, so you plant what you like.
  If you don't carry a herb seed for a stop, Shortest Path routes you through a bank first.
- **Farming contracts.** If your Farming Guild contract is for a herb, allotment or flower crop and the guild is on
  the run, the guild's patch is planned with the contract seed: it is on the checklist, highlighted in your
  inventory, and the patch is labelled with the crop to plant.
- **Preferred teleports.** Pick what to bring for each location: any teleport item that goes there, one in
  particular, the runes for a teleport spell, a way into your house (tab, runes or Construction cape), or
  nothing. During a run Shortest Path is limited to the teleport items you carry, so the route follows what you
  brought; a faster teleport you happen to carry will still be used. House routes use the house facilities set in
  your Shortest Path settings.
- **Step-by-step highlights.** During a run each patch that needs something gets a box in the colour of its next
  step, labelled Harvest, Clear, Cure, Rake, Compost or Plant. Compost can go on before or after planting. The
  inventory items the patches you are at need next are highlighted in the same colour: seeds when a patch needs
  planting, compost when it needs compost, and so on.
- **Checklist.** What to bring for the rest of the run, checked off against what you carry: seeds, compost, tools
  and teleports, plus graceful and volcanic ash if you pick them. It is in the side panel and in a box in the
  game, which also lists the steps at the location you are standing at.
- **Bank filter.** Opening the bank during a run shows only farm run items, in sections: herb seeds, allotment
  seeds, flower seeds, equipment and teleports. A button with the Farming icon at the top of the bank turns it on
  and off at any time. Needs the core Bank Tags plugin, and works alongside Bank Tag Layouts.

Untick a location in the panel to leave it out of your runs, for example one you haven't unlocked.

## Things it can't see
- Tools and compost stored with the tool leprechaun, and the contents of a seed box. These show as grey rather than
  missing on the checklist.
- Runes supplied by a staff or by combination runes. A spell's runes are looked for in the inventory and rune pouch.

## Development
Run a development client with the plugin loaded:

```bash
./gradlew run
```

Log in using the [Using Jagex Accounts](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts) instructions.
