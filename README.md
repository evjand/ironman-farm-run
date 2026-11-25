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
- **Runs of your own.** A herb run and a tree run are ready to go; pick one at the top of the side panel, or make
  your own with New. A run sets each kind of patch (herbs, flowers, allotments, trees, fruit trees and the special
  trees) to "Visit for",
  "Tend while there" or off. A location becomes a stop when a patch it visits for needs attention when the run
  starts, and patches it tends get done at the stops it makes anyway, while your seeds last. Each location can be
  left out, or have a kind of patch left out, such as the fruit tree at the Farming Guild, right from its card in
  the side panel; the run remembers it. Each stop is visited once, so a patch that turns ready later in the run
  doesn't send you back. Arrive somewhere whose patch needs work but wasn't predicted ready, and it joins the run;
  arrive where nothing needs doing, and the in-game box says how long the patch has left.
- **Any seed.** A patch needs a seed of its kind and the plugin doesn't care which. If you don't carry one for the
  next stop, Shortest Path routes you through a bank first. Seeds in a seed box count once the game has shown its
  contents.
- **Run options.** Each run also remembers whether to compost, pay gardeners, note produce, and bring graceful
  and volcanic ash. An allotment takes three seeds of one kind.
- **Note herbs.** With this option on, once the herb patch is replanted its last step is to use your herbs, grimy
  or clean, on the tool leprechaun, before going on to the next patch with room to spare. The leprechaun is
  highlighted along with the herbs in your inventory, and if a full inventory stops a harvest halfway, the
  leprechaun is highlighted then too. A separate option, "Note allotments & flowers", does the same for their
  produce; leave it off if it goes to the compost bin instead.
- **Tree runs.** Trees at Lumbridge, Varrock, Falador, Taverley, the Gnome Stronghold, Nemus Retreat and the
  Farming Guild, and fruit trees at the Gnome Stronghold, Tree Gnome Village, east of Catherby, Brimhaven, Lletya,
  Kastori and the Farming Guild. A watered seedling counts as a sapling, as it will have grown into one by the time you plant it.
  Each tree goes through checking its health, taking it down (chop it, or pay the gardener 200
  coins, or carry an axe), digging up the stump, planting any sapling of the right kind, and paying for protection.
  The checklist counts the payment for every sapling you carry, the bank filter shows the payments, and the
  gardener is highlighted from the moment the tree is in. Without the payment on you, the step is skipped and the
  tree goes unprotected.
- **Special trees.** Make a run that visits for any of these, alone or alongside other patches:
  - Hardwood trees on Fossil Island (three patches) and at Locus Oasis.
  - Calquats at Tai Bwo Wannai, Kastori and the Summer Shore.
  - Spirit trees at Port Sarim, Etceteria, Brimhaven, Hosidius and the Farming Guild. A run only goes to an empty
    spirit tree patch while your Farming level lets you plant another: one tree at 83, two at 88, three at 93,
    four at 96, and any number at 99.
  - Celastrus and redwood at the Farming Guild, and the crystal tree in Prifddinas.

  Patches you don't have the Farming level to plant in are left out. Calquats and spirit trees are cleared with a
  spade rather than taken down by the gardener. Celastrus bark and crystal shards are harvested with an axe. A
  crystal tree can't get diseased, so there is no payment for it. The spirit tree gardener wants 5 monkey nuts, a
  monkey bar and a ground tooth, and the checklist counts each.
- **Step-by-step highlights.** During a run each patch that needs something gets a box in the colour of its next
  step: Harvest, Check health, Remove, Dig up, Clear, Cure or Prune, Rake, Compost, Plant, Pay or Note produce. The inventory items
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
  payments and teleports. A run with trees adds the seeds of its trees still on their way to becoming saplings,
  with plant pots, a trowel and watering cans. A button with the Farming icon at the top of the bank turns it on and off at any time.
  Under "Bank filter items" at the bottom of the side panel you can list items to always show, in a section of
  their own, and items to never show, by name or item ID; * in a name matches any text, so "Ring of dueling*"
  covers every charge. This also covers new items the plugin doesn't know about yet.
- **Preferred teleports.** Pick what to bring for each location, shared by every run: any teleport item that goes
  there, one in particular, your house, a fairy ring, or nothing. Teleports are named after their item or spell; a
  spell with a tablet, such as Falador Teleport, is one choice that the tablet or the runes satisfy, and the bank
  filter shows both. House takes a house tablet, the Teleport to House runes or a Construction cape. During a run
  Shortest Path is limited to the teleport items you carry, so the route follows what you brought.
- **Stop order.** A setting picks how the next stop is chosen: whichever is quickest to reach from where you are
  (the default), or the order of the OSRS Wiki's farming run guides.
- **Farming contracts.** If your Farming Guild contract is for a crop a run grows (a seed crop on a run with seed
  patches, a tree on one with trees) and the guild is on that run, the guild's patch is planned with the contract
  seed or sapling.
- **Farming Guild tiers.** Guild patches above your Farming level (boosts count) are left out.

During a run the panel follows it: finished stops shrink to a line each, the stop you are at or headed to shows each
patch's next step with the very next one marked, and the stops to come show how much is left to do there. If a stop
can't be finished, its "Skip stop" button leaves it out of the rest of the run.

## Limitations
- Patches you haven't visited since they last changed are as Time Tracking predicts them.
- Tree Gnome Village has no teleport item; pick the fairy ring, or let Shortest Path route there by spirit tree.
- The hardwood patch at Anglers' Retreat isn't included, as it is only reached by boat.
- Hops, bush, cactus, mushroom, belladonna, seaweed, anima and hespori patches aren't part of a run.

## Development
Run a development client with the plugin loaded:

```bash
./gradlew run
```

Log in using the [Using Jagex Accounts](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts) instructions.
