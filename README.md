# AI Coliseum 2025

My submission for [AI Coliseum 2025](https://coliseum.ai/ranking?lang=en&tournament=aic2025). First time participating but wow was it fun! I even got to win the Sprint Tournament and finish in **2nd** place in the Final Tournament, which was very close too! Yes, it was decided by a literal coinflip, but the whole tournament was still very enjoyable.

(Sorry for not having Git history, the original was really messed up)

## My approach
I like having fun:

- As much own code as possible
- As many own ideas as possible

I only needed to copy this [fast set](https://github.com/chenyx512/battlecode25-gonewhalin/blob/main/src/bot1/fast/FastLocSet.java) that uses string operations, which are unfairly broken in terms of bytecode. I also got the idea to use a binary kernel for fast accessibility checks from [here](https://youtu.be/ORcI2Q6cDg4?t=311) as I was looking for ways to hack the bytecode part. They really should  give some pre-made custom instrumented structures.

Everything else here is mine.

## About the code
Some considerations:
- Units have 15000 energy (bytecode) to spend each round which is too little to actually compute stuff. The idea is good and it has many advantages but it also punishes many common programming practices and encourages bad things like code repetition.

- Anything that's not an array or a string operation exploit is just not viable. If arrays are used only the default initialization is free. Arrays.fill() is expensive.

- Good pathfinding is _extremely_ expensive. Depending on your implementation, a BFS of just visible tiles takes longer than a turn. Now imagine BFSing a location that is 20 tiles away even with the fastest BFS implementation.

## Structure

The structure is really simple:
Each crafter has a current __list of objectives__ and a __strategy switch condition__. The main loop runs objectives from the list in order until one has been followed (returns `true`) and yields. 

Here's an example:
```.java
Condition a = new HasExplored(8);
Condition b = new HasCraftable(Craftable.PICKAXE, 9);
condition = new OrCondition(b, a);

// Has a shovel (had to fight)
objectives = new Objective[8];
objectives[0] = new Heal();
objectives[1] = new BreakPeople();
objectives[2] = new BreakAgendas();
objectives[3] = new TycoonMentality();
objectives[4] = new DoubleGather(Material.STONE, 1, Material.WOOD , 2);
objectives[5] = new Craft(Craftable.BAKED_POTATO, 10);
objectives[6] = new GetThatGrass();
objectives[7] = new Explore();
```
This is the objective list and switch condition for when crafters have crafted just a shovel soon after spawning. When running the loop, it will:

1. Try to heal. Done optimally and never yields
2. Look for enemies and run micro if needed (if it does then it yields. Applies to all following objectives too)
3. If no enemies, look for enemy structures and destroy any visible one.
4. If no structures, take a quick look to see if a) a pickaxe can be crafted **and** b) there are a few mine resources in sight. If both conditions are true then it crafts a pickaxe.
5. Get stone or wood if visible, but at most 1 stone and 2 wood.
6. Get potatoes and/or craft baked potatoes.
7. Explore the map. Never fails to run so if no other objectives can be accomplished this will always be run.

~~Yes, the objective names are extremely descriptive.~~

Now take a look at the condition:
- HasExplored() is true when the crafter "has seen all the map"
- HasCraftable(Craftable.PICKAXE) is kind of obvious

Each condition also specifies a number that points to the next list of objectives+condition to switch to if true.

The condition here is the OR operation of those two, meaning that if HasExplored() is true then we go to whatever **8** means and if it gets a pickaxe instead we go to **9** (basically, in strategy 8 we explore but also get grass, and in strategy 9 we use the pickaxe).

The crafter works towards both these objectives either by getting materials for a potential pickaxe and looking at mine locations, or by just exploring. In some cases, the switch condition is just "Has this crafter been alive for more than 200 rounds?", for example.


This idea allows:
1. Priorities
2. Limiting allowed actions
3. Easily rewiring the whole strategy depending on specific conditions while reusing a lot of code

### Downsides?
If I had to say, in some cases it's better to run a few different checks first and decide the best option later. This model is designed for specific checks and actions in the same objective. However, it's actually compatible with other approaches if you separate certain objectives in two or three phases (it's just less comfortable).

It's also a bit redundant in some cases, but copying ~5 lines of objectives that are always there is OK to me.

## Strategy

In the end, no communications were used. They definitely had potential but I didn't want to consider the cooldown issues.

There is a big reason why strategies for this game won't be very detailed. 

Maps can be too different. For a given set of maps one knows what to expect, but the tournaments are always run on new maps. In this game specifically, the spawn points of units are random in practice and can be whatever. 

There can be new maps where there's no contact ever between teams, and maps where you spawn next to an enemy and the game is over in 30 rounds. And preparing for them is not worth it because the other ~60 maps never meet these conditions. This is common here, and that's why a generalist adaptive strategy works better.

Now, a summary list of important stuff to go for to win. 

Stuff you can do to improve your chances:
- Beds/increasing number of crafters. A no-brainer, helps prevent loss by destruction and expands quicker.
- Beacons/resources. For late game or somewhat isolated maps. Total destruction is difficult against good players.
- Improve battle gear. Usually also for mid/late, greatly shifts any fight in your favor.

Stuff you can do to make the opponent's life miserable, thus improving your chances:
- Kill crafters, especially those with at least a tool.
- Destroy beds/beacons/structures. Beacons hurt and spawn prevention is also really good.
- Expand quickly and make sure you win fights early. They lose all their tempo in most games and you win. This is completely broken when opponents don't properly check their chances at winning a fight.

To have a chance at winning first I prioritize crafting beds, which means you need an axe early. All "Geneneration 1" crafters will be wired to do this. Important because if you don't you're not only playing with less crafters, you're also giving the opponent your side of the map's worth in axe materials.

Any crafter that spawns later than the first rounds gets a more adaptable strategy that collects basic materials for any of the three tools and only crafts one of them depending on what they find first. Then they are rewired to specialize in that tool. This ensures variety. 

Usually, units that have crafted an axe or a shovel also try to make a pickaxe later for late game, but units with only a pickaxe don't craft any other tools.

- They craft a shovel if they don't have other tools but see something you can hit (and actually damage). This helps defense micro too
- They craft an axe if they are Generation 1 or if they see accessible wool
- They craft a pickaxe if they see some minerals (more than one, they check a few hardcoded combinations which I associate with only mine-level spawn rates).

It looks like there are some cases when crafting a shovel is suboptimal if you can craft something else. I've seen it's usually better because units with a shovel also refresh materials on the ground and I need some of them too. Of course I could test more specific conditions for optimal results (crafting a more expensive tool can remove a weight tie for instance).

### A few specific things it does other than that, in no particular order
- New crafters find materials at the beginning, but if they fail to for some rounds (100-200) they start gathering grass as well.
- Never gather unnecessary materials unless explicitly told to by an objective. In some niche cases, destroy extra materials.
- Crafters with at least a pickaxe craft turrets if they can, and they try to place them next to beds or beacons. They will also place them when they run into an enemy crafter.
- Not their main priority, but stack potatoes too (max 7).
- Upgrades to gear are only done if the crafter has at least 1 more of that material than what would be required to craft the next beacon.
- Battle calcs are done when crafters see an enemy to decide if they can win the fight or not. Simplifying a bit (but it works well) every baked potato is +2 health for the crafter that has it, both crafters will use their highest damage weapon and armor is considered. After computing how many turns each one will last (counting the current cooldown) it decides to run or engage. We engage in ties too.
- I don't do full group calcs, but the most common bad case is many of our crafters running from a single enemy crafter that is not that much stronger than us (a 2v1 would kill them). On some losing matchups we can check if there's an ally in sight that can also see, access and damage the enemy, and in that case we engage. Comms should be used for this, but them having 1 action cooldown makes it terrible. They should either spend energy or have a separate cooldown.
- The micro is done correctly in that movement and hits are well timed and units that run from enemies still try to hit if in range.
- Exploration is actually done using a BFS to find the closest unseen location. This is very expensive, but has proven great against the exploration tech used by almost everyone on maps with a lot of separation/obstacles, where others usually orbit around them. To minimize the impact, the BFS is paused when close to the energy limit, allowing the crafter to check urgent updates like enemies or new materials on the next round without losing already computed values. Once the path is computed, each step is very cheap.
- Some operations like pathfinding take the longest when there's not an answer so we pay the most when we get the least. There are many small optimizations like remembering already deemed unaccessible source-location pairs or fast accessibility checks in N steps using boolean kernels before actually trying to compute the path.
- Pathfinding at vision range has to be fast so I did a specific version for each different case (there are 12 with 20 quadratic distance vision). In some cases it involves just checking hardcoded paths as I could name all of them; in others it's a cheap BFS where only two or three directions are allowed.
- Craft boats at some point if water has been seen. This is terrible but with the variance in maps it's the only reliable and feasible way to take advantage of oceans. The devs had also teased a bit that it'd be necessary (it wasn't).

In this kind of competition, and I think that in this game in particular it applies even more, you are at the mercy of the maps. The devs did a pretty good job with this overall. 

## Testing
I originally had [this tester](tester.py) but it ran too slowly as it only ran one game at a time, so I ~~made~~ generated [tester2.py](tester2.py) that does the same but in parallel with the subprocess module thanks to a custom ant target in [build.xml](build.xml).

The idea is to run as many games as possible in all maps matching an older version and the newer one to see if the new version is better. Two limitations:

1. Games take some time to run.
2. They also write to disk no matter what (easily 10MB per game...). Terrible for SSD lifespan, I've already lost one not to this, but to something similar in the past.

So I used to run about 10 games per map for all maps (running some more when we only had like 19 maps) and check winrates. Overall it's difficult to make changes that improve winrate in almost all maps, usually it goes down in a few and goes up in like half of them.