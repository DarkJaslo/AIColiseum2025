package DarkJaslo;

import DarkJaslo.fast.FastLocSet;
import aic2025.user.*;

import java.util.Arrays;

public class UnitPlayer {

    public int[] RandomPermutation(UnitController uc)
    {
        int[] arr = {0, 1, 2, 3};

        // Fisher–Yates shuffle
        int j = (int)(uc.getRandomDouble() * 4);
        int t = arr[3]; arr[3] = arr[j]; arr[j] = t;

        j = (int)(uc.getRandomDouble() * 3);
        t = arr[2]; arr[2] = arr[j]; arr[j] = t;

        j = (int)(uc.getRandomDouble() * 2);
        t = arr[1]; arr[1] = arr[j]; arr[j] = t;

        return arr;
    }

    public void RandomMove(UnitController uc)
    {
        int randomDir = (int)(uc.getRandomDouble()*8);
        Direction dir = Direction.values()[randomDir];
        if (uc.canMove(dir)) uc.move(dir);
    }

    public void SafeRandomMove(UnitController uc)
    {
        if (uc.getUnitInfo().getCurrentMovementCooldown() > 1.0f)
            return;

        boolean[] tested = new boolean[8];
        int tested_num = 0;
        while (tested_num < 8)
        {
            int index = (int)(uc.getRandomDouble()*8);
            if (tested[index])
                continue;

            Direction dir = Direction.values()[index];
            Location next = uc.getLocation().add(dir);
            if (uc.isOutOfMap(next))
            {
                tested[index] = true;
                ++tested_num;
                continue;
            }

            if (OBSTACLES[next.x*MAP_SIZE_Y+next.y] != 3)
            {
                tested[index] = true;
                ++tested_num;
                continue;
            }

            if (uc.canMove(dir))
            {
                uc.move(dir);
                return;
            }
        }
    }

    public boolean SenseMaterials(UnitController uc)
    {
        if (HAS_VOIDS != null)
            return HAS_VOIDS;

        if (uc.hasCraftable(Craftable.BOAT))
        {
            if (sensed_materials == null)
                sensed_materials = uc.senseMaterials(GameConstants.UNIT_VISION_RANGE);
            MaterialInfo[] mat_locs = sensed_materials;
            for (MaterialInfo loc : mat_locs)
            {
                Location l = loc.getLocation();
                int index = l.x * MAP_SIZE_Y + l.y;
                if (OBSTACLES[index] > 2)
                    continue;
                else if (OBSTACLES[index] > 0)
                {
                    HAS_VOIDS = true;
                    continue;
                }

                if (loc.getMaterial() == Material.VOID)
                {
                    OBSTACLES[index] = 2;
                }
                else
                {
                    OBSTACLES[index] = 3;
                    WALLS[l.y+1] &= ~(2L<<l.x);
                }
            }
        }
        else
        {
            if (sensed_materials == null)
                sensed_materials = uc.senseMaterials(GameConstants.UNIT_VISION_RANGE);
            MaterialInfo[] mat_locs = sensed_materials;
            for (MaterialInfo loc : mat_locs)
            {
                Location l = loc.getLocation();
                int index = l.x * MAP_SIZE_Y + l.y;
                if (OBSTACLES[index] > 2)
                    continue;
                else if (OBSTACLES[index] > 0)
                {
                    HAS_VOIDS = true;
                    continue;
                }

                if (loc.getMaterial() == Material.VOID)
                {
                    OBSTACLES[index] = 2;
                }
                else if (loc.getMaterial() == Material.WATER)
                {
                    OBSTACLES[index] = 1;
                }
                else
                {
                    OBSTACLES[index] = 3;
                    WALLS[l.y+1] &= ~(2L<<l.x);
                }
            }
        }

        if (HAS_VOIDS == null)
            HAS_VOIDS = false;
        return HAS_VOIDS;
    }

    MaterialInfo[] SensedMaterials(UnitController uc)
    {
        if (sensed_useful != null)
            return sensed_useful;

        if (sensed_materials == null)
            sensed_materials = uc.senseMaterials(GameConstants.UNIT_VISION_RANGE);

        int count = 0;
        for (MaterialInfo info : sensed_materials){
            Material m = info.getMaterial();
            if (m == Material.GRASS || m == Material.DIRT || m == Material.WATER || m == Material.VOID)
                continue;
            ++count;
        }

        if (count == 0)
        {
            sensed_useful = new MaterialInfo[0];
            return sensed_useful;
        }

        MaterialInfo[] result = new MaterialInfo[count];
        int index = 0;
        for (MaterialInfo info : sensed_materials){
            Material m = info.getMaterial();
            if (m == Material.GRASS || m == Material.DIRT || m == Material.WATER || m == Material.VOID)
                continue;

            if (index >= count)
                break;

            result[index++] = info;
        }

        sensed_useful = result;
        return sensed_useful;
    }

    int[] TakeALook(UnitController uc)
    {
        if (VISIBLE_MATERIALS != null)
            return VISIBLE_MATERIALS;

        VISIBLE_MATERIALS = new int[Material.values().length];
        // TODO: consider spyglass
        SenseMaterials(uc);
        if (sensed_materials == null)
            sensed_materials = uc.senseMaterials(GameConstants.UNIT_VISION_RANGE);
        for (MaterialInfo info : sensed_materials)
        {
            ++VISIBLE_MATERIALS[info.getMaterial().ordinal()];
        }

        if (VISIBLE_MATERIALS[Material.WATER.ordinal()] > 0)
            HAS_SEEN_WATER = true;

        return VISIBLE_MATERIALS;
    }

    boolean CanIHaveABeacon(UnitController uc, int[] best_ally)
    {
        int iron_cost = 1 + uc.getCraftedBeacons();
        int gold_cost = 1 + uc.getCraftedBeacons();
        int diamond_cost = 1 + uc.getCraftedBeacons();

        int[] my_mats = uc.getUnitInfo().getCarriedMaterials();
        int ri = my_mats[Material.IRON.ordinal()];
        int rg = my_mats[Material.GOLD.ordinal()];
        int rd = my_mats[Material.DIAMOND.ordinal()];

        int mi = best_ally[Material.IRON.ordinal()];
        int mg = best_ally[Material.GOLD.ordinal()];
        int md = best_ally[Material.DIAMOND.ordinal()];

        // Can this replace the current result?
        if (md >= diamond_cost || md<rd)
            return true;

        // Needs diamonds and has either more or the same
        if (md > rd)
        {
            return false;
        }

        if (mg >= gold_cost || mg<rg)
            return true;

        if (mg > rg)
        {
            return false;
        }

        if (mi >= iron_cost || mi<ri)
            return true;

        return mi <= ri;
    }

    int[] VisibleFriendClosestToBeacon(UnitController uc)
    {
        int iron_cost = 1 + uc.getCraftedBeacons();
        int gold_cost = 1 + uc.getCraftedBeacons();
        int diamond_cost = 1 + uc.getCraftedBeacons();

        // Priority:
        // 1. Who needs less diamonds
        // 2. Who needs less gold
        // 3. Who needs less iron

        int[] result = new int[Material.values().length];

        for (UnitInfo info : uc.senseUnits(GameConstants.UNIT_VISION_RANGE, uc.getTeam()))
        {
            int ri = result[Material.IRON.ordinal()];
            int rg = result[Material.GOLD.ordinal()];
            int rd = result[Material.DIAMOND.ordinal()];

            int[] mats = info.getCarriedMaterials();
            int mi = mats[Material.IRON.ordinal()];
            int mg = mats[Material.GOLD.ordinal()];
            int md = mats[Material.DIAMOND.ordinal()];

            // Can this replace the current result?
            if (md >= diamond_cost || md<rd)
                continue;

            // Needs diamonds and has either more or the same
            if (md > rd)
            {
                result[Material.IRON.ordinal()] = mats[Material.IRON.ordinal()];
                result[Material.GOLD.ordinal()] = mats[Material.GOLD.ordinal()];
                result[Material.DIAMOND.ordinal()] = mats[Material.DIAMOND.ordinal()];
                continue;
            }

            if (mg >= gold_cost || mg<rg)
                continue;

            if (mg > rg)
            {
                result[Material.IRON.ordinal()] = mats[Material.IRON.ordinal()];
                result[Material.GOLD.ordinal()] = mats[Material.GOLD.ordinal()];
                continue;
            }

            if (mi >= iron_cost || mi<ri)
                continue;

            if (mi > ri)
            {
                result[Material.IRON.ordinal()] = mats[Material.IRON.ordinal()];
            }
        }

        return result;
    }

    public void UpdateClosestMine(UnitController uc, Location candidate)
    {
        if (CLOSEST_MINE == null)
            CLOSEST_MINE = candidate;
        else if (uc.getLocation().distanceSquared(candidate) < uc.getLocation().distanceSquared(CLOSEST_MINE))
            CLOSEST_MINE = candidate;
    }

    UnitInfo[] SensedUnits(UnitController uc)
    {
        if (sensed_units != null)
            return sensed_units;

        sensed_units = uc.senseUnits(GameConstants.UNIT_VISION_RANGE, enemy_team);
        return sensed_units;
    }

    public Craftable BestWeapon(UnitController uc, Location target, boolean combat)
    {
        if (combat && best_combat_weapon != null)
            return best_combat_weapon;
        else if (!combat && best_weapon != null)
            return best_weapon;

        int distance = target == null ? 2 : uc.getLocation().distanceSquared(target);
        int best = -1;
        int best_damage = 0;
        CraftableInfo[] craftables = uc.getUnitInfo().getCarriedCraftablesArray();

        for (int i = craftables.length; --i >= 0;)
        {
            Craftable c = craftables[i].getCraftable();
            if (c == Craftable.FIREWORK && !combat)
                continue;

            if (c == Craftable.TNT && !combat)
                return Craftable.TNT;

            if (distance > 2 && c == Craftable.BOW)
            {
                best = i;
                break;
            }
            else if (GameConstants.craftableDamage[c.ordinal()] > best_damage)
            {
                best_damage = GameConstants.craftableDamage[c.ordinal()];
                best = i;
            }
        }

        if (best >= 0)
        {
            if (combat)
                best_combat_weapon = craftables[best].getCraftable();
            else
                best_weapon = craftables[best].getCraftable();
        }

        return combat ? best_combat_weapon : best_weapon;
    }

    public void DefaultPeleriux(UnitController uc)
    {
        if (uc.getRound() < 5)
        {
            // First gen, reproduce :aga:
            VERSION = 100;
        }

        if (VERSION == 100)
        {
            // Has nothing, but is heavily encouraged to craft an axe
            Condition a = new DoubleCondition(new AliveForRounds(100, 1), new HasCraftable(Craftable.AXE, 1, true), 1);
            Condition b = new OrCondition(new HasCraftable(Craftable.SHOVEL, 4), new AliveForRounds(800, 6));
            condition = new OrCondition(a, b);

            objectives = new Objective[9];
            objectives[0] = new Heal();
            objectives[1] = new BreakPeople();
            objectives[2] = new BreakAgendas();
            objectives[3] = new CraftAlways(Craftable.AXE);
            objectives[4] = new Craft(Craftable.BED_BLUEPRINT);
            objectives[5] = new PlaceCraftable(Craftable.BED_BLUEPRINT);
            objectives[6] = new Gather(Material.STRING, 4, false, true);
            objectives[7] = new Craft(Craftable.BAKED_POTATO, 7);
            objectives[8] = new Explore();
        }
        if (VERSION == 1)
        {
            // Has nothing
            Condition a2 = new OrCondition(new HasExplored(2), new AliveForRounds(200, 2));
            Condition a = new OrCondition(new HasCraftable(Craftable.AXE, 3), a2);
            Condition b = new OrCondition(new HasCraftable(Craftable.SHOVEL, 4), new HasCraftable(Craftable.PICKAXE, 5));
            condition = new OrCondition(a, b);

            objectives = new Objective[8];
            objectives[0] = new Heal();
            objectives[1] = new BreakPeople();
            objectives[2] = new BreakAgendas();
            objectives[3] = new TycoonMentality();
            objectives[4] = new NewWorldPlayerMentality();
            objectives[5] = new DoubleGather(Material.WOOD, 2, Material.STONE, 2);
            objectives[6] = new Craft(Craftable.BOAT);
            objectives[7] = new Explore();
        }
        else if (VERSION == 2)
        {
            // TODO: make sure it eventually gets to business with at least a shovel. Maybe an alive rounds without a shovel limit?

            // Still has nothing but has seen all the map, implies a severe lack of resources
            Condition a = new HasCraftable(Craftable.AXE, 3);
            Condition b = new OrCondition(new HasCraftable(Craftable.SHOVEL, 4), new HasCraftable(Craftable.PICKAXE, 5));
            condition = new OrCondition(a, b);

            objectives = new Objective[8];
            objectives[0] = new Heal();
            objectives[1] = new BreakPeople();
            objectives[2] = new BreakAgendas();
            objectives[3] = new TycoonMentality();
            objectives[4] = new NewWorldPlayerMentality();
            objectives[5] = new DoubleGather(Material.WOOD, 2, Material.STONE, 2);
            objectives[6] = new Craft(Craftable.BOAT);
            objectives[7] = new Gather(Material.GRASS, 1, true, false);
        }
        else if (VERSION == 3)
        {
            condition = new HasExplored(6);

            // Has an axe, make good use of it
            objectives = new Objective[9];
            objectives[0] = new Heal();
            objectives[1] = new BreakPeople();
            objectives[2] = new BreakAgendas();
            objectives[3] = new Craft(Craftable.BED_BLUEPRINT);
            objectives[4] = new PlaceCraftable(Craftable.BED_BLUEPRINT);
            objectives[5] = new SingleGather(Material.WOOD, 4, false, false);
            objectives[6] = new SingleGather(Material.LEATHER, 2, false, true);
            objectives[7] = new Craft(Craftable.BAKED_POTATO, 7);
            objectives[8] = new Explore();
        }
        else if (VERSION == 4)
        {
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
        }
        else if (VERSION == 5)
        {
            Condition a = new HasCraftable(Craftable.AXE, 7);
            Condition b = new HasExplored(10);
            condition = new OrCondition(a, b);

            // Has a pickaxe, and only a pickaxe. Abuse it
            objectives = new Objective[19];
            objectives[0] = new Heal();
            objectives[1] = new BreakPeople();
            objectives[2] = new BreakAgendas();
            objectives[3] = new PlaceTurret();
            objectives[4] = new NewWorldPlayerMentality();
            objectives[5] = new Craft(Craftable.SWORD);
            objectives[6] = new Craft(Craftable.BEACON_BLUEPRINT);
            objectives[7] = new PlaceCraftable(Craftable.BEACON_BLUEPRINT);
            objectives[8] = new Craft(Craftable.TURRET_BLUEPRINT);
            objectives[9] = new UpgradeTools();
            objectives[10] = new GetRich();
            objectives[11] = new DoubleGather(Material.WOOD, 1, Material.STONE, 2);
            objectives[12] = new SingleGather(Material.STONE, 8, false, false);
            objectives[13] = new Craft(Craftable.BAKED_POTATO, 5);
            objectives[14] = new Craft(Craftable.TNT, 1);
            objectives[15] = new BreakMaterial(Material.COPPER, 5);
            objectives[16] = new BreakMaterial(Material.IRON, 5);
            objectives[17] = new BreakMaterial(Material.STONE, 5);
            objectives[18] = new Explore();
        }
        else if (VERSION == 6)
        {
            // Has an axe and has explored everything. Make a pickaxe if possible.
            condition = new HasCraftable(Craftable.PICKAXE, 7);

            // Has an axe, make good use of it
            objectives = new Objective[10];
            objectives[0] = new Heal();
            objectives[1] = new BreakPeople();
            objectives[2] = new BreakAgendas();
            objectives[3] = new SingleGather(Material.STONE, 1, false, false);
            objectives[4] = new Craft(Craftable.PICKAXE);
            objectives[5] = new Craft(Craftable.BED_BLUEPRINT);
            objectives[6] = new PlaceCraftable(Craftable.BED_BLUEPRINT);
            objectives[7] = new SingleGather(Material.LEATHER, 2, false, true);
            objectives[8] = new Craft(Craftable.BAKED_POTATO, 7);
            objectives[9] = new Explore();
        }
        else if (VERSION == 7)
        {
            // Has an axe and a pickaxe. Has explored the whole map.
            objectives = new Objective[22];
            objectives[0] = new Heal();
            objectives[1] = new BreakPeople();
            objectives[2] = new BreakAgendas();
            objectives[3] = new PlaceTurret();
            objectives[4] = new Craft(Craftable.SWORD);
            objectives[5] = new Craft(Craftable.ARMOR);
            objectives[6] = new Craft(Craftable.BEACON_BLUEPRINT);
            objectives[7] = new PlaceCraftable(Craftable.BEACON_BLUEPRINT);
            objectives[8] = new Craft(Craftable.BED_BLUEPRINT);
            objectives[9] = new PlaceCraftable(Craftable.BED_BLUEPRINT);
            objectives[10] = new Craft(Craftable.TURRET_BLUEPRINT);
            objectives[11] = new UpgradeTools();
            objectives[12] = new GetRich();
            objectives[13] = new SingleGather(Material.STONE, 8, false, false);
            objectives[14] = new Craft(Craftable.BAKED_POTATO, 5);
            objectives[15] = new Craft(Craftable.TNT, 3);
            objectives[16] = new Craft(Craftable.BOAT);
            objectives[17] = new Gather(Material.STRING, 4, false, true);
            objectives[18] = new BreakMaterial(Material.COPPER, 5);
            objectives[19] = new BreakMaterial(Material.IRON, 5);
            objectives[20] = new BreakMaterial(Material.STONE, 5);
            objectives[21] = new Explore();
        }
        else if (VERSION == 8)
        {
            // Has a shovel and has explored everything. Make a pickaxe if possible
            condition = new HasCraftable(Craftable.PICKAXE, 9);

            objectives = new Objective[9];
            objectives[0] = new Heal();
            objectives[1] = new BreakPeople();
            objectives[2] = new BreakAgendas();
            objectives[3] = new SingleGather(Material.STONE, 1, false, false);
            objectives[4] = new Craft(Craftable.PICKAXE);
            objectives[5] = new Craft(Craftable.BAKED_POTATO, 7);
            objectives[6] = new Craft(Craftable.COMPOSTER_BLUEPRINT);
            objectives[7] = new PlaceCraftable(Craftable.COMPOSTER_BLUEPRINT);
            objectives[8] = new Gather(Material.GRASS, 1, true, false);
        }
        else if (VERSION == 9)
        {
            // TODO: check if progressing objectives is needed here

            // Has a shovel and a pickaxe. Can't make beds, so compensate by revealing new materials
            objectives = new Objective[22];
            objectives[0] = new Heal();
            objectives[1] = new BreakPeople();
            objectives[2] = new BreakAgendas();
            objectives[3] = new PlaceTurret();
            objectives[4] = new Craft(Craftable.SWORD);
            objectives[5] = new Craft(Craftable.ARMOR);
            objectives[6] = new Craft(Craftable.BEACON_BLUEPRINT);
            objectives[7] = new PlaceCraftable(Craftable.BEACON_BLUEPRINT);
            objectives[8] = new Craft(Craftable.TURRET_BLUEPRINT);
            objectives[9] = new UpgradeTools();
            objectives[10] = new GetRich();
            objectives[11] = new SingleGather(Material.STONE, 8, false, false);
            objectives[12] = new Craft(Craftable.BAKED_POTATO, 5);
            objectives[13] = new Craft(Craftable.COMPOSTER_BLUEPRINT);
            objectives[14] = new PlaceCraftable(Craftable.COMPOSTER_BLUEPRINT);
            objectives[15] = new Craft(Craftable.TNT, 3);
            objectives[16] = new Craft(Craftable.BOAT);
            objectives[17] = new BreakMaterial(Material.COPPER, 5);
            objectives[18] = new BreakMaterial(Material.IRON, 5);
            objectives[19] = new BreakMaterial(Material.STONE, 5);
            objectives[20] = new GetThatGrass();
            objectives[21] = new Explore();
        }
        else if (VERSION == 10)
        {
            // Has a pickaxe and has explored everything
            objectives = new Objective[18];
            objectives[0] = new Heal();
            objectives[1] = new BreakPeople();
            objectives[2] = new BreakAgendas();
            objectives[3] = new PlaceTurret();
            objectives[4] = new Craft(Craftable.SWORD);
            objectives[5] = new Craft(Craftable.BEACON_BLUEPRINT);
            objectives[6] = new PlaceCraftable(Craftable.BEACON_BLUEPRINT);
            objectives[7] = new Craft(Craftable.TURRET_BLUEPRINT);
            objectives[8] = new UpgradeTools();
            objectives[9] = new GetRich();
            objectives[10] = new SingleGather(Material.STONE, 8, false, false);
            objectives[11] = new DoubleGather(Material.WOOD, 1, Material.STONE, 2);
            objectives[12] = new Craft(Craftable.BAKED_POTATO, 5);
            objectives[13] = new Craft(Craftable.TNT, 1);
            objectives[14] = new BreakMaterial(Material.COPPER, 5);
            objectives[15] = new BreakMaterial(Material.IRON, 5);
            objectives[16] = new BreakMaterial(Material.STONE, 5);
            objectives[17] = new Explore();
        }
    }

    public void Peleriux(UnitController uc)
    {
        DefaultPeleriux(uc);
    }

    public void Broadcast(UnitController uc, int code, int thing, Location l)
    {
        Broadcast(uc, code, thing, l.x, l.y);
    }

    public void Broadcast(UnitController uc, int code, int thing, int x, int y)
    {
        // Code: message code. 0->"StructureAtX,Y", 1->"StructureNOTAtX,Y", 2->"biomeAt"
        // What is it
        // Where is it
        int message = (code << 24) | (thing << 16) | (x << 8) | y;
        uc.broadcastMessage(message);
    }

    private void MarkTurretRange(int x, int y, int value)
    {
        for (int i = y-3; i <= y+3; ++i)
        {
            if (i < 0 || i >= MAP_SIZE_Y)
                continue;

            int index = i*MAP_SIZE_Y;

            for (int j = x-3; j <= x+3; ++j)
            {
                if (j < 0 || j >= MAP_SIZE_X)
                    continue;

                OBSTACLES[index+j] = value;
            }
        }
    }

    public void ProcessMessage(UnitController uc, int message)
    {
        int code = (message&0xFF000000) >>> 24;
        int thing = (message&0x00FF0000) >>> 16;
        int x = (message&0x0000FF00) >>> 8;
        int y = message&0x000000FF;

        if (code == 0 || code == 1)
        {
            StructureType structure = StructureType.values()[thing];
            if (code == 0)
            {
                if (structure == StructureType.TURRET)
                    MarkTurretRange(x, y, 2);
            }
            else
            {
                if (structure == StructureType.TURRET)
                    MarkTurretRange(x, y, 0);
            }
        }
    }

    public class BFSInfo
    {
        public Location[] queue;
        public int queue_front;
        public int queue_back;
        public int[] distances10;

        public Location source;
        public Location destination;

        public boolean IsFinished()
        {
            return queue_front > queue_back;
        }

        public BFSInfo()
        {

        }
    }

    public class Path{
        // Iterate backwards. 0 is the last element.
        public Direction[] path;
        public int index;
        public boolean reverse;
        public boolean empty = false;
        public BFSInfo bfs = null;

        public Path()
        {
            this(null, -1, true);
            empty = true;
        }

        public Path(Direction[] path, int index, boolean reverse)
        {
            this.path = path;
            this.index = index;
            this.reverse = reverse;
        }
    }

    public boolean IsAccessible(UnitController uc, Location source, Location destination, int max_steps)
    {
        // Uses a 3x3 boolean kernel to propagate what can be reached in n+1 steps from a grid of n-step reachability. This combined with storing the grid
        // in large ints, which allow an int per row, makes the process somewhat parallel. Uses some memoization that might be wrong depending on the order
        // of calls or source positions used when calling multiple times.

        if (ACCESSIBLE_MAP == null)
        {
            ACCESSIBLE_MAP = new long[WALLS.length];
            ACCESSIBLE_MAP[source.y+1] |= (2L<<source.x);
        }
        else
        {
            if (ACCESSIBLE_MAP_STEPS >= max_steps)
            {
                // Check it here
                if ((WALLS[destination.y+1]&(2L<<destination.x)) != 0L)
                {
                    // Kernel considers this a wall
                    long up = ACCESSIBLE_MAP[destination.y];
                    long cur = ACCESSIBLE_MAP[destination.y+1];
                    long down = ACCESSIBLE_MAP[destination.y+2];
                    long acc = (up | (up << 1) | (up >> 1)) | (cur | (cur << 1) | (cur >> 1)) | (down | (down << 1) | (down >> 1));
                    long value = (acc&(2L<<destination.x));
                    if (value != 0L)
                    {
                        return true;
                    }
                }
                else if ((ACCESSIBLE_MAP[destination.y+1]&(2L<<destination.x)) != 0L)
                {
                    return true;
                }
            }
        }

        int steps = Math.max(ACCESSIBLE_MAP_STEPS-1, 0);

        boolean destination_is_unknown = (WALLS[destination.y+1]&(2L<<destination.x)) != 0L;

        // We can define kernel limits depending on the number of steps considered
        int kernel_min = Math.max(1, source.y-max_steps-Math.abs(destination.x-source.x)/2);
        int kernel_max = Math.min(ACCESSIBLE_MAP.length-1, source.y+1+max_steps-Math.abs(destination.x-source.x)/2);

        while (++steps <= max_steps)
        {
            // Applies kernel
            for (int i = kernel_min; i < kernel_max; ++i)
            {
                long up = ACCESSIBLE_MAP[i-1];
                long cur = ACCESSIBLE_MAP[i];
                long down = ACCESSIBLE_MAP[i+1];

                ACCESSIBLE_MAP[i] = ~WALLS[i] & ((up | (up << 1) | (up >> 1)) | (cur | (cur << 1) | (cur >> 1)) | (down | (down << 1) | (down >> 1)));
            }

            if (!destination_is_unknown)
            {
                if ((ACCESSIBLE_MAP[destination.y+1]&(2L<<destination.x)) != 0L)
                {
                    return true;
                }
            }
            else
            {
                // The kernel considers this position a wall. Apply kernel without masking walls one time to check
                long up = ACCESSIBLE_MAP[destination.y];
                long cur = ACCESSIBLE_MAP[destination.y+1];
                long down = ACCESSIBLE_MAP[destination.y+2];
                long acc = (up | (up << 1) | (up >> 1)) | (cur | (cur << 1) | (cur >> 1)) | (down | (down << 1) | (down >> 1));
                long value = (acc&(2L<<destination.x));
                if (value != 0L)
                {
                    return true;
                }
            }

        }
        return false;
    }

    public boolean FindAccessible(UnitController uc)
    {
        Location source = uc.getLocation();
        SenseMaterials(uc);

        Location[] queue = new Location[3600];
        int queue_front = 0;
        int queue_back = -1;
        // Euclidean distance times 10
        int[] distances10 = new int[MAP_SIZE_X*MAP_SIZE_Y];

        queue[++queue_back] = uc.getLocation();
        // Trash value
        distances10[source.x*MAP_SIZE_Y+source.y] = 1<<16;

        while (queue_front <= queue_back)
        {
            // Pop
            Location current = queue[queue_front++];
            int current_index = current.x*MAP_SIZE_Y+current.y;
            int current_distance = distances10[current_index]&0x0000FFFF;

            if (OBSTACLES[current_index] == 0)
                return true;

            for (int i = 0; i < all_directions.length; ++i)
            {
                Location next = current.add(all_directions[i]);
                if (uc.isOutOfMap(next))
                    continue;

                int location_index = next.x*MAP_SIZE_Y + next.y;
                // Check if it's a void or water
                if (OBSTACLES[current_index] != 0 && OBSTACLES[location_index] != 3)
                    continue;

                int next_distance = current_distance + ALL_STEPS[all_directions[i].ordinal()];

                int d10 = distances10[location_index];
                int dist = d10 == 0 ? 30000 : d10&0x0000FFFF;
                if (next_distance < dist)
                {
                    // Write distance down
                    distances10[location_index] = next_distance;
                    distances10[location_index] |= (i+1)<<16;
                    // Push
                    queue[++queue_back] = next;
                }
            }
        }
        return false;
    }

    public Path Pathfind(UnitController uc, Location destination, BFSInfo bfs)
    {
        return Pathfind(uc, destination, bfs, false, 3000);
    }

    // Assumes destination is inside the view range of the unit.
    // May not find a path, in that case null will be returned
    public Path Pathfind(UnitController uc, Location destination, BFSInfo bfs, boolean never_pause, int max_distance)
    {
        // Inaccessible
        if (ACCESSIBILITY.contains(uc.getLocation().x*MAP_SIZE_Y+uc.getLocation().y, destination.x*MAP_SIZE_Y+destination.y))
            return null;

        Location source = uc.getLocation();
        if (source.equals(destination))
            return new Path();

        boolean found_voids = SenseMaterials(uc);
        if (!found_voids)
        {
            // Fast case, pathfind with .toDirection()
            Location current = source;
            Direction[] steps = new Direction[100];
            int added_steps = 0;

            while (!current.equals(destination))
            {
                Direction step = current.directionTo(destination);
                steps[added_steps++] = step;
                current = current.add(step);
            }

            Direction[] result = new Direction[added_steps];
            System.arraycopy(steps, 0, result, 0, added_steps);
            return new Path(result, 0, false);
        }
        else
        {
            // Take it into account, do the BFS
            Location[] queue;
            int queue_front;
            int queue_back;
            // Euclidean distance times 10
            int[] distances10;

            if (bfs != null && bfs.source.equals(uc.getLocation()) && bfs.destination.equals(destination))
            {
                // Just continue the search
                queue = bfs.queue;
                queue_front = bfs.queue_front;
                queue_back = bfs.queue_back;
                distances10 = bfs.distances10;
            }
            else
            {
                // Start the search
                SenseMaterials(uc);

                queue = new Location[3600];
                queue_front = 0;
                queue_back = -1;
                distances10 = new int[MAP_SIZE_X*MAP_SIZE_Y];

                source = uc.getLocation();
                queue[++queue_back] = source;
                // Trash value
                distances10[source.x*MAP_SIZE_Y+source.y] = 1<<16;
            }

            boolean pause = true;
            while ((never_pause || uc.getEnergyLeft() > 1000) && queue_front <= queue_back)
            {
                // Pop
                Location current = queue[queue_front++];
                if (current.equals(destination))
                {
                    pause = false;
                    break;
                }
                int current_distance = distances10[current.x*MAP_SIZE_Y+current.y]&0x0000FFFF;
                if (current_distance > max_distance)
                    return null;

                boolean can_be_outside = current.x <= 0 || current.x >= MAP_SIZE_X-1 || current.y <= 0 || current.y >= MAP_SIZE_Y-1;
                if (can_be_outside)
                {
                    for (int i = 0; i < all_directions.length; ++i)
                    {
                        Location next = current.add(all_directions[i]);
                        if (uc.isOutOfMap(next))
                            continue;

                        int location_index = next.x*MAP_SIZE_Y + next.y;
                        // Check if it's a void or water
                        if (OBSTACLES[location_index] != 3)
                            continue;

                        int next_distance = current_distance + ALL_STEPS[all_directions[i].ordinal()];
                        if (next_distance > max_distance)
                            continue;

                        int d10 = distances10[location_index];
                        int dist = d10 == 0 ? 30000 : d10&0x0000FFFF;
                        if (next_distance < dist)
                        {
                            // Write distance down
                            distances10[location_index] = next_distance;
                            distances10[location_index] |= (i+1)<<16;
                            // Push
                            queue[++queue_back] = next;
                        }
                    }
                }
                else
                {
                    for (int i = 0; i < all_directions.length; ++i)
                    {
                        Location next = current.add(all_directions[i]);
                        int location_index = next.x*MAP_SIZE_Y + next.y;
                        // Check if it's a void or water
                        if (OBSTACLES[location_index] != 3)
                            continue;

                        int next_distance = current_distance + ALL_STEPS[all_directions[i].ordinal()];

                        int d10 = distances10[location_index];
                        int dist = d10 == 0 ? 30000 : d10&0x0000FFFF;
                        if (next_distance < dist)
                        {
                            // Write distance down
                            distances10[location_index] = next_distance;
                            distances10[location_index] |= (i+1)<<16;
                            // Push
                            queue[++queue_back] = next;
                        }
                    }
                }
            }
            pause = pause && !never_pause;

            // Pause BFS
            if (pause && queue_front <= queue_back)
            {
                Path path = new Path();
                BFSInfo bfs_info = new BFSInfo();
                bfs_info.queue = queue;
                bfs_info.queue_front = queue_front;
                bfs_info.queue_back = queue_back;
                bfs_info.distances10 = distances10;
                bfs_info.source = source;
                bfs_info.destination = destination;

                path.bfs = bfs_info;

                //uc.println("Pausing bfs at " + uc.getEnergyUsed() + " energy.");
                return path;
            }

            //uc.println("Search. Energy: " + uc.getPercentageOfEnergyLeft());

            if (queue_front <= queue_back)
            {
                // Found it, now build steps
                Direction[] steps = new Direction[100];
                int steps_index = 0;

                Location path_current = destination;

                // No nulls should pop here
                while (!path_current.equals(source))
                {
                    //uc.drawPointDebug(path_current, 255, 255, 0);
                    int location_index = path_current.x*MAP_SIZE_Y + path_current.y;
                    int index = ((distances10[location_index] & 0xFFFF0000) >>> 16)-1;
                    Direction step = all_directions[index];
                    //uc.drawLineDebug(path_current, path_current.add(step), 255, 0, 0);
                    steps[steps_index++] = step;
                    path_current = path_current.add(DIRECTION_OPPOSITES[all_directions[index].ordinal()]);
                }

                return new Path(steps, steps_index-1, true);
            }

            if (!EXPLORING)
            {
                // We know the whole map, so if this destination can't be reached, it can't be reached from any attempted position
                int destination_index = destination.x*MAP_SIZE_Y+destination.y;
                for (int i = 0; i < queue_back; ++i)
                    ACCESSIBILITY.add(queue[i].x*MAP_SIZE_Y+queue[i].y, destination_index);
            }
        }

        // Can't reach from this position or any neighbour
        for (Direction dir : all_directions)
        {
            Location loc = source.add(dir);
            if (loc.x < 0 && loc.y < 0 && loc.x >= MAP_SIZE_X && loc.y >= MAP_SIZE_Y)
                ACCESSIBILITY.add(loc.x*MAP_SIZE_Y+loc.y, destination.x*MAP_SIZE_Y+destination.y);
        }
        ACCESSIBILITY.add(source.x*MAP_SIZE_Y+source.y, destination.x*MAP_SIZE_Y+destination.y);
        return null;
    }

    public Path PathfindUnexplored(UnitController uc, BFSInfo bfs, int max_distance, boolean return_path)
    {
        return PathfindUnexplored(uc, bfs, max_distance, return_path, false);
    }

    public Path PathfindUnexplored(UnitController uc, BFSInfo bfs, int max_distance, boolean return_path, boolean never_pause)
    {
        Location[] queue;
        Location source;
        Location destination;
        int queue_front;
        int queue_back;
        // Euclidean distance times 10
        int[] distances10;

        if (bfs != null && bfs.source.equals(uc.getLocation()))
        {
            //uc.println("Continue BFS.");

            // Just continue the search
            queue = bfs.queue;
            queue_front = bfs.queue_front;
            queue_back = bfs.queue_back;
            distances10 = bfs.distances10;

            source = bfs.source;
            destination = bfs.destination;
        }
        else
        {
            //uc.println("Start BFS.");

            // Start the search
            SenseMaterials(uc);

            queue = new Location[3600];
            queue_front = 0;
            queue_back = -1;
            distances10 = new int[MAP_SIZE_X*MAP_SIZE_Y];

            source = uc.getLocation();
            destination = null;
            queue[++queue_back] = source;
            // Trash value
            distances10[source.x*MAP_SIZE_Y+source.y] = 1<<16;
        }

        boolean pause = true;
        while ((never_pause || uc.getEnergyLeft() > 1000) && queue_front <= queue_back)
        {
            // Pop
            Location current = queue[queue_front++];
            int current_distance = distances10[current.x*MAP_SIZE_Y+current.y]&0x0000FFFF;

            int index = current.x*MAP_SIZE_Y+current.y;
            if (OBSTACLES[index] == 0)
            {
                destination = current;
                pause = false;
                break;
            }
            else if (current_distance >= max_distance)
            {
                pause = false;
                break;
            }

            if (current.x <= 0 || current.x >= MAP_SIZE_X-1 || current.y <= 0 || current.y >= MAP_SIZE_Y-1)
            {
                // Can be outside the map
                for (int i = 0; i < all_directions.length; ++i)
                {
                    Location next = current.add(all_directions[i]);
                    if (uc.isOutOfMap(next))
                        continue;

                    int location_index = next.x*MAP_SIZE_Y + next.y;
                    // Check if it's a void or water
                    if (OBSTACLES[location_index] != 0 && OBSTACLES[location_index] != 3)
                        continue;

                    int next_distance = current_distance + ALL_STEPS[all_directions[i].ordinal()];

                    int d10 = distances10[location_index];
                    int dist = d10 == 0 ? 30000 : d10&0x0000FFFF;
                    if (next_distance < dist)
                    {
                        // Write distance down
                        distances10[location_index] = next_distance;
                        distances10[location_index] |= (i+1)<<16;
                        // Push
                        queue[++queue_back] = next;
                    }
                }
            }
            else
            {
                // Can't be outside the map, omit the uc.isOutOfMap() check
                for (int i = 0; i < all_directions.length; ++i)
                {
                    Location next = current.add(all_directions[i]);
                    int location_index = next.x*MAP_SIZE_Y + next.y;
                    // Check if it's a void or water
                    if (OBSTACLES[location_index] != 0 && OBSTACLES[location_index] != 3)
                        continue;

                    int next_distance = current_distance + ALL_STEPS[all_directions[i].ordinal()];

                    int d10 = distances10[location_index];
                    int dist = d10 == 0 ? 30000 : d10&0x0000FFFF;
                    if (next_distance < dist)
                    {
                        // Write distance down
                        distances10[location_index] = next_distance;
                        distances10[location_index] |= (i+1)<<16;
                        // Push
                        queue[++queue_back] = next;
                    }
                }
            }
        }
        pause = pause && !never_pause;

        // Pause BFS
        if (pause && queue_front <= queue_back)
        {
            Path path = new Path();
            BFSInfo bfs_info = new BFSInfo();
            bfs_info.queue = queue;
            bfs_info.queue_front = queue_front;
            bfs_info.queue_back = queue_back;
            bfs_info.distances10 = distances10;
            bfs_info.source = source;
            bfs_info.destination = destination;

            path.bfs = bfs_info;

            //uc.println("Pausing bfs at " + uc.getEnergyUsed() + " energy.");
            return path;
        }

        //uc.println("Search ended: " + uc.getEnergyUsed() + " energy.");

        if (!return_path)
        {
            if (destination == null)
                return null;

            return new Path();
        }

        //uc.println("Search. Energy: " + uc.getPercentageOfEnergyLeft());

        if (queue_front <= queue_back && destination != null)
        {
            // Found it, now build steps
            Direction[] steps = new Direction[100];
            int steps_index = 0;

            Location path_current = destination;

            // No nulls should pop here
            while (!path_current.equals(source))
            {
                //uc.drawPointDebug(path_current, 255, 255, 0);
                int location_index = path_current.x*MAP_SIZE_Y + path_current.y;
                int index = ((distances10[location_index] & 0xFFFF0000) >>> 16)-1;
                Direction step = all_directions[index];
                //uc.drawLineDebug(path_current, path_current.add(step), 255, 0, 0);
                steps[steps_index++] = step;
                path_current = path_current.add(DIRECTION_OPPOSITES[all_directions[index].ordinal()]);
            }

            return new Path(steps, steps_index-1, true);
        }

        //if (destination != null)
            //OBSTACLES[destination.x*MAP_SIZE_Y+destination.y] = 4;
        return null;
    }

    public boolean TestPath(UnitController uc, Location source, Direction d1, Direction d2, Direction d3, Direction d4)
    {
        Location l1 = source.add(d1);
        Location l2 = l1.add(d2);
        Location l3 = l2.add(d3);
        Location l4 = l3.add(d4);

        if (uc.isOutOfMap(l1) || uc.isOutOfMap(l2) || uc.isOutOfMap(l3) || uc.isOutOfMap(l4))
            return false;

        return
                OBSTACLES[l1.x*MAP_SIZE_Y+l1.y] == 3 &&
                OBSTACLES[l2.x*MAP_SIZE_Y+l2.y] == 3 &&
                OBSTACLES[l3.x*MAP_SIZE_Y+l3.y] == 3 &&
                OBSTACLES[l4.x*MAP_SIZE_Y+l4.y] == 3;
    }

    public Direction PathfindWithDirections(UnitController uc, Location source, Location destination, int max_distance, Direction[] dirs, Location[] pre_add, Direction[] pre_add_dirs)
    {
        Location[] queue = new Location[3600];
        int queue_front = 0;
        int queue_back = -1;
        int[] steps = new int[MAP_SIZE_X*MAP_SIZE_Y];
        queue[++queue_back] = source;
        // Zero
        steps[source.x*MAP_SIZE_Y+source.y] = (Direction.ZERO.ordinal()+1)<<16;

        for (int i = pre_add.length; --i >= 0;)
        {
            queue[++queue_back] = pre_add[i];
            steps[pre_add[i].x*MAP_SIZE_Y+pre_add[i].y] = 1;
            steps[pre_add[i].x*MAP_SIZE_Y+pre_add[i].y] |= (pre_add_dirs[i].ordinal()+1)<<16;
        }

        while (queue_front <= queue_back)
        {
            // Pop
            Location current = queue[queue_front++];
            if (current.distanceSquared(destination) <= 2)
            {
                // Return direction from first step that gets here
                return Direction.values()[((steps[current.x*MAP_SIZE_Y+current.y]&0xFFFF0000)>>16)-1];
            }
            int stt = steps[current.x*MAP_SIZE_Y+current.y];
            int current_distance = stt&0x0000FFFF;
            Direction dir_to_current = Direction.ZERO;
            if (stt != 0)
                dir_to_current = Direction.values()[((stt&0xFFFF0000)>>16)-1];

            boolean can_be_outside = current.x <= 0 || current.x >= MAP_SIZE_X-1 || current.y <= 0 || current.y >= MAP_SIZE_Y-1;
            if (can_be_outside)
            {
                for (Direction dir : dirs)
                {
                    Location next = current.add(dir);
                    if (uc.isOutOfMap(next))
                        continue;

                    int location_index = next.x*MAP_SIZE_Y + next.y;
                    // Check if it's a void or water
                    if (OBSTACLES[location_index] != 3)
                        continue;

                    int next_distance = current_distance + 1;
                    if (next_distance > max_distance)
                        continue;

                    int st = steps[location_index];
                    int dist = st == 0 ? 30000 : st&0x0000FFFF;

                    if (next_distance < dist)
                    {
                        // Write distance down
                        steps[location_index] = next_distance;
                        if (dir_to_current != Direction.ZERO)
                            steps[location_index] |= (dir_to_current.ordinal()+1)<<16;
                        else
                            steps[location_index] |= (dir.ordinal()+1)<<16;
                        // Push
                        queue[++queue_back] = next;
                    }
                }
            }
            else
            {
                for (Direction dir : dirs)
                {
                    Location next = current.add(dir);
                    int location_index = next.x*MAP_SIZE_Y + next.y;
                    // Check if it's a void or water
                    if (OBSTACLES[location_index] != 3)
                        continue;

                    int next_distance = current_distance + 1;
                    if (next_distance > max_distance)
                        continue;

                    int st = steps[location_index];
                    int dist = st == 0 ? 30000 : st&0x0000FFFF;

                    if (next_distance < dist)
                    {
                        // Write distance down
                        steps[location_index] = next_distance;
                        if (dir_to_current != Direction.ZERO)
                            steps[location_index] |= (dir_to_current.ordinal()+1)<<16;
                        else
                            steps[location_index] |= (dir.ordinal()+1)<<16;
                        // Push
                        queue[++queue_back] = next;
                    }
                }
            }
        }
        return Direction.ZERO;
    }

    public Direction PathfindAccessible4Steps(UnitController uc, Location destination)
    {
        SenseMaterials(uc);

        Location source = uc.getLocation();
        int qdist = uc.getLocation().distanceSquared(destination);
        if (qdist == 20)
        {
            Direction side = destination.x > source.x ? Direction.EAST : Direction.WEST;
            Direction updown = destination.y > source.y ? Direction.NORTH : Direction.SOUTH;
            Direction diag = Direction.getDirection(side.dx, updown.dy);
            Direction special = diag.dx+diag.dy == 0 ? updown.rotateRight() : updown.rotateLeft();

            // Check paths that start with a diagonal move first because that usually allows you to stay in range
            if (
                    TestPath(uc, source, diag, updown, diag, updown) ||
                    TestPath(uc, source, diag, diag, updown, updown) ||
                    TestPath(uc, source, diag, updown, updown, diag) ||
                    TestPath(uc, source, diag, diag, diag, special)
            )
                return diag;

            return updown;

            /*
            if (
                    TestPath(uc, source, updown, updown, diag, diag) ||
                    TestPath(uc, source, updown, diag, updown, diag) ||
                    TestPath(uc, source, updown, diag, diag, updown)
            )
                return updown;
            */
        }
        else if (qdist == 18)
        {
            // Assuming -3,3 from you, BFS only allowing "up", "left" and their diagonal
            Direction diag = source.directionTo(destination);
            Direction up = diag.rotateLeft();
            Direction left = diag.rotateRight();
            return PathfindWithDirections(uc, source, destination, 4, new Direction[]{diag, up, left}, new Location[]{}, null);
        }
        else if (qdist == 17)
        {
            Direction updown;
            Direction diag;
            Direction special;

            // Far side and short side. Updown is the far side here
            int xdiff = Math.abs(destination.x-source.x);
            int ydiff = Math.abs(destination.y-source.y);
            if (ydiff > xdiff)
            {
                updown = destination.y > source.y ? Direction.NORTH : Direction.SOUTH;
                diag = uc.getLocation().directionTo(destination);
                special = diag.dx+diag.dy == 0 ? updown.rotateRight() : updown.rotateLeft();
            }
            else
            {
                updown = destination.x > source.x ? Direction.EAST : Direction.WEST;
                diag = uc.getLocation().directionTo(destination);
                special = diag.dx+diag.dy == 0 ? updown.rotateRight() : updown.rotateLeft();
            }

            if (
                    TestPath(uc, source, diag, updown, updown, updown) ||
                    TestPath(uc, source, diag, diag, updown, special) ||
                    TestPath(uc, source, diag, diag, special, updown) ||
                    TestPath(uc, source, diag, special, diag, updown) ||
                    TestPath(uc, source, diag, special, updown, diag)
            )
                return diag;
            else if (
                    TestPath(uc, source, updown, updown, updown, diag) ||
                    TestPath(uc, source, updown, updown, diag, updown) ||
                    TestPath(uc, source, updown, diag, updown, updown) ||
                    TestPath(uc, source, updown, diag, diag, special) ||
                    TestPath(uc, source, updown, special, updown, diag) ||
                    TestPath(uc, source, updown, special, diag, updown)
            )
                return updown;
            return special;
        }
        else if (qdist == 16)
        {
            Direction updown = source.directionTo(destination);
            Direction diag1 = updown.rotateLeft();
            Direction diag2 = updown.rotateRight();

            if (
                    TestPath(uc, source, updown, updown, updown, updown) ||
                    TestPath(uc, source, updown, diag1, updown, diag2) ||
                    TestPath(uc, source, updown, diag1, diag2, updown) ||
                    TestPath(uc, source, updown, diag2, updown, diag1) ||
                    TestPath(uc, source, updown, diag2, diag1, updown) ||
                    TestPath(uc, source, updown, updown, diag1, diag2) ||
                    TestPath(uc, source, updown, updown, diag2, diag1)
            )
                return updown;
            else if (
                    TestPath(uc, source, diag1, diag1, diag2, diag2) ||
                    TestPath(uc, source, diag1, diag2, diag1, diag2) ||
                    TestPath(uc, source, diag1, updown, updown, diag2) ||
                    TestPath(uc, source, diag1, diag2, updown, updown) ||
                    TestPath(uc, source, diag1, updown, diag2, updown)
            )
                return diag1;
            return diag2;
        }
        else if (qdist == 13 || qdist == 10)
        {
            // Assuming it's -3,2 from you, preadd northeast and bfs with west, north, northwest. Only works if bfs stops at <=2 qdistance
            // qdist=10: same conditions meet

            Direction diag = source.directionTo(destination);
            Direction left = diag.rotateLeft();
            Direction right = diag.rotateRight();
            Direction special = right.rotateRight();

            Location[] pre_add = new Location[]{};
            Direction[] pre_add_dir = null;
            if (!uc.isOutOfMap(source.add(special)) && OBSTACLES[source.add(special).x*MAP_SIZE_Y+source.add(special).y] == 3)
            {
                pre_add = new Location[]{ source.add(special) };
                pre_add_dir = new Direction[]{special};
            }
            return PathfindWithDirections(uc, source, destination, 4, new Direction[]{diag, left, right}, pre_add, pre_add_dir);
        }
        else if (qdist == 9)
        {
            // Assuming it's 0,3 from you, preadd west, east and bfs with only north, northeast and northwest
            Direction direct = source.directionTo(destination);
            Direction diag1 = direct.rotateLeft();
            Direction diag2 = direct.rotateRight();
            Direction sp1 = diag1.rotateLeft();
            Direction sp2 = diag2.rotateRight();

            Location[] pre_add = new Location[]{};
            Direction[] pre_add_dir = null;
            if (!uc.isOutOfMap(source.add(sp1)) && OBSTACLES[source.add(sp1).x*MAP_SIZE_Y+source.add(sp1).y] == 3 && !uc.isOutOfMap(source.add(sp2)) && OBSTACLES[source.add(sp2).x*MAP_SIZE_Y+source.add(sp2).y] == 3)
            {
                pre_add = new Location[]{ source.add(sp1), source.add(sp2) };
                pre_add_dir = new Direction[]{sp1, sp2};
            }
            else if (!uc.isOutOfMap(source.add(sp1)) && OBSTACLES[source.add(sp1).x*MAP_SIZE_Y+source.add(sp1).y] == 3)
            {
                pre_add = new Location[]{ source.add(sp1) };
                pre_add_dir = new Direction[]{sp1};
            }
            else if (!uc.isOutOfMap(source.add(sp2)) && OBSTACLES[source.add(sp2).x*MAP_SIZE_Y+source.add(sp2).y] == 3)
            {
                pre_add = new Location[]{ source.add(sp2) };
                pre_add_dir = new Direction[]{sp2};
            }
            return PathfindWithDirections(uc, source, destination, 4, new Direction[]{direct, diag1, diag2}, pre_add, pre_add_dir);
        }
        else if (qdist == 8 || qdist == 5)
        {
            // Assuming it's 2,2 from you, add northwest and southeast to the queue and bfs with only north, east & northeast
            // Same for qdist=5 (assuming 2,1 from you this time)

            Direction diag = source.directionTo(destination);
            Direction left = diag.rotateLeft();
            Direction right = diag.rotateRight();
            Direction sp1 = left.rotateLeft();
            Direction sp2 = right.rotateRight();

            Location[] pre_add = new Location[]{};
            Direction[] pre_add_dir = null;
            if (!uc.isOutOfMap(source.add(sp1)) && OBSTACLES[source.add(sp1).x*MAP_SIZE_Y+source.add(sp1).y] == 3 && !uc.isOutOfMap(source.add(sp2)) && OBSTACLES[source.add(sp2).x*MAP_SIZE_Y+source.add(sp2).y] == 3)
            {
                pre_add = new Location[]{ source.add(sp1), source.add(sp2) };
                pre_add_dir = new Direction[]{sp1, sp2};
            }
            else if (!uc.isOutOfMap(source.add(sp1)) && OBSTACLES[source.add(sp1).x*MAP_SIZE_Y+source.add(sp1).y] == 3)
            {
                pre_add = new Location[]{ source.add(sp1) };
                pre_add_dir = new Direction[]{sp1};
            }
            else if (!uc.isOutOfMap(source.add(sp2)) && OBSTACLES[source.add(sp2).x*MAP_SIZE_Y+source.add(sp2).y] == 3)
            {
                pre_add = new Location[]{ source.add(sp2) };
                pre_add_dir = new Direction[]{sp2};
            }
            return PathfindWithDirections(uc, source, destination, 4, new Direction[]{diag, left, right}, pre_add, pre_add_dir);
        }
        else if (qdist == 4)
        {
            Direction right = source.directionTo(destination);
            Direction diag1 = right.rotateLeft();
            Direction up = right.rotateLeft();
            Direction diag3 = up.rotateLeft();
            Direction diag2 = right.rotateRight();
            Direction down = diag2.rotateRight();
            Direction diag4 = down.rotateRight();

            if (TestPath(uc, source, right, right, Direction.ZERO, Direction.ZERO))
                return right;
            else if (TestPath(uc, source, diag1, diag2, Direction.ZERO, Direction.ZERO))
                return diag1;
            else if (TestPath(uc, source, diag2, diag1, Direction.ZERO, Direction.ZERO))
                return diag2;
            else if (TestPath(uc, source, up, diag1, diag2, down))
                return up;
            else if (TestPath(uc, source, down, diag2, diag1, up))
                return down;
            else if (TestPath(uc, source, diag3, diag1, diag2, diag2))
                return diag3;

            return diag4;
        }
        else if (qdist <= 2)
        {
            // No need to do stuff
            return Direction.ZERO;
        }
        return Direction.ZERO;
    }

    // Defines an objective to be followed
    public interface Objective{
        boolean IsCompleted(UnitController uc);
        // Returns true if it could follow, false if objective is deemed impossible
        boolean Follow(UnitController uc);

        boolean IsActive();
        void SetActive(boolean active);
    }

    public class Craft implements Objective
    {
        private boolean active;
        private final Craftable craftable;
        private final int[] used_materials; // Materials used for the crafting recipe
        private final int[] required_tools; // For each material on used_materials
        // index i refers to the element i of used_materials, which is an index inside the Material enum
        private final int[] missing;
        Objective subobjective = null;
        Location subobjective_target = null;
        private final int amount;
        private final boolean once;
        private boolean completed = false;

        public Craft(Craftable craftable)
        {
            this(craftable, 1, false);
        }

        public Craft(Craftable craftable, boolean once)
        {
            this(craftable, 1, once);
        }

        public Craft(Craftable craftable, int amount)
        {
            this(craftable, amount, false);
        }

        public Craft(Craftable craftable, int amount, boolean once){
            active = true;
            this.craftable = craftable;
            this.amount = amount;
            this.once = once;

            int used_amount = 0;
            for (int x : GameConstants.craftingRecipe[craftable.ordinal()]){
                if (x > 0)
                    used_amount++;
            }

            used_materials = new int[used_amount];
            required_tools = new int[used_amount];

            missing = new int[used_amount];

            int index = 0;
            int used_index = 0;
            for (int x : GameConstants.craftingRecipe[craftable.ordinal()]){
                if (x > 0)
                    used_materials[index++] = used_index;

                ++used_index;
            }

            for (int i = 0; i < used_amount; ++i)
            {
                Material mat = Material.values()[used_materials[i]];
                if (mat.canBeGathered())
                    required_tools[i] = 4242;
                else if (mat != Material.VOID && mat != Material.DIRT && mat != Material.WATER)
                    required_tools[i] = mat.gatheringTool().ordinal();
            }
        }

        @Override
        public boolean IsCompleted(UnitController uc){
            return (once && completed) || (uc.hasCraftable(craftable) && (amount == 1 || uc.getUnitInfo().getCarriedCraftables()[craftable.ordinal()][0] == amount));
        }

        private boolean OnSubobjectiveComplete(UnitController uc)
        {
            UnitInfo unit_info = uc.getUnitInfo();
            if (unit_info.getCurrentActionCooldown() > 0.0f)
                return true;

            Material material = uc.senseMaterialAtLocation(subobjective_target);
            if (material == null)
            {
                subobjective = null;
                return false;
            }
            // Gather thing
            Craftable gathering_tool = material.gatheringTool();
            if (gathering_tool != null && uc.hasCraftable(gathering_tool))
            {
                uc.useCraftable(material.gatheringTool(), subobjective_target);
            }
            else if (gathering_tool != null)
            {
                uc.gather();
            }

            subobjective = null;
            return true;
        }

        @Override
        public boolean Follow(UnitController uc){
            if (craftable == Craftable.BOAT && !HAS_SEEN_WATER)
                return false;

            // Craft if possible
            if (uc.canCraft(craftable)){
                uc.craft(craftable);

                if (craftable == Craftable.BOAT)
                {
                    ACCESSIBILITY.clear();

                    // TODO: this is far from perfect, but it'll do
                    for (int index = OBSTACLES.length; --index >= 0;)
                        if (OBSTACLES[index] == 1)
                        {
                            OBSTACLES[index] = 3;
                            int x = index/MAP_SIZE_Y;
                            int y = index%MAP_SIZE_Y;
                            WALLS[y+1] &= ~(2L<<x);
                        }
                }

                completed = true;
                return true;
            }

            // If a subobjective is active, we are already going for some material. Don't do the code below, just continue the subobjective
            if (subobjective != null && subobjective.IsActive())
            {
                if (subobjective.IsCompleted(uc))
                {
                    return OnSubobjectiveComplete(uc);
                }
                else
                {
                    boolean result = subobjective.Follow(uc);
                    if (!result)
                        subobjective = null;
                    else if (subobjective.IsCompleted(uc))
                        return OnSubobjectiveComplete(uc);
                    return result;
                }
            }

            // Find materials
            //if (craftable == Craftable.BEACON_BLUEPRINT && !CanIHaveABeacon(uc, VisibleFriendClosestToBeacon(uc)))
            //    return false;

            // Compute how many of each is missing
            int[] materials = uc.getUnitInfo().getCarriedMaterials();
            int[][] craftables = uc.getUnitInfo().getCarriedCraftables();
            for (int i = 0; i < missing.length; ++i)
            {
                int required = GameConstants.craftingRecipe[craftable.ordinal()][used_materials[i]];
                missing[i] = Math.max(0, required*amount - materials[used_materials[i]] - required*craftables[craftable.ordinal()][0]);
            }

            // Look for missing materials
            MaterialInfo[] mat_info = SensedMaterials(uc);

            // Go for the nearest useful one
            Location mat_loc = null;

            boolean has_tool = false;
            outer:
            for (MaterialInfo info : mat_info)
            {
                for (int i = 0; i < used_materials.length; ++i)
                {
                    // Check if it's relevant, missing and minable
                    if (info.getMaterial().ordinal() == used_materials[i] && missing[i] > 0 && (required_tools[i] == 4242 || uc.hasCraftable(Craftable.values()[required_tools[i]])))
                    {
                        mat_loc = info.getLocation();
                        has_tool = uc.hasCraftable(info.getMaterial().gatheringTool());
                        break outer;
                    }
                }
            }

            if (mat_loc != null)
            {
                //uc.drawLineDebug(uc.getLocation(), mat_loc, 255,0,0);
                subobjective = new GoToLocation(mat_loc, false, has_tool ? 2 : 0);
                subobjective_target = mat_loc;
                subobjective.SetActive(true);
                boolean result = subobjective.Follow(uc);
                if (!result)
                    subobjective = null;
                else if (subobjective.IsCompleted(uc))
                    return OnSubobjectiveComplete(uc);
                return result;
            }

            // No material has been found, tell so and signal that following this objective might be impossible.
            subobjective = null;
            return false;
        }

        @Override
        public boolean IsActive() {
            return active;
        }

        @Override
        public void SetActive(boolean active){
            this.active = active;
        }
    }

    public class GoToLocation implements Objective
    {
        private boolean active;
        private final Location destination;
        private final boolean no_pathfinding;
        private final int stop_distance;
        private boolean completed = false;

        private Path path = null;

        public GoToLocation(Location destination, boolean no_pathfinding, int stop_distance) {
            active = true;
            this.destination = destination;
            this.no_pathfinding = no_pathfinding;
            this.stop_distance = stop_distance;
        }

        @Override
        public boolean IsCompleted(UnitController uc){
            return completed;
        }

        @Override
        public boolean Follow(UnitController uc){
            if (uc.getLocation().distanceSquared(destination) <= stop_distance)
                completed = true;

            if (completed)
                return true;

            if (no_pathfinding)
            {
                Direction dir = uc.getLocation().directionTo(destination);
                if (uc.canMove(dir))
                    uc.move(dir);

                return true;
            }

            if (path == null || (path.empty && path.bfs != null && !path.bfs.IsFinished())){
                BFSInfo bfs = path != null ? path.bfs : null;
                path = Pathfind(uc, destination, bfs);
                if (path == null)
                    return false;
                else if (path.empty && path.bfs != null && !path.bfs.IsFinished())
                {
                    // BFS was paused
                    //uc.println("Paused BFS. return true");
                    return true;
                }
            }

            // Make sure we can move first
            if (uc.canMove())
            {
                if (path.index < 0 || path.index >= path.path.length)
                    return false;

                uc.move(path.path[path.index]);
                if (path.reverse)
                    --path.index;
                else
                    ++path.index;

                if (uc.getLocation().distanceSquared(destination) <= stop_distance)
                {
                    path = null;
                    completed = true;
                }

            }
            return true;
        }

        @Override
        public boolean IsActive() {
            return active;
        }

        @Override
        public void SetActive(boolean active){
            this.active = active;
        }
    }

    public class Explore implements Objective{
        private boolean active;

        private Direction current_direction = null;
        private int current_steps = 0;
        private boolean pathfind = true;
        private Path path = null;
        Location last_location = null;

        Objective mine_checker = new CheckMine();

        public Explore(){
            this.active = true;
        }

        @Override
        public boolean IsCompleted(UnitController uc){
            return false;
        }

        @Override
        public boolean Follow(UnitController uc)
        {
            if (!uc.getLocation().equals(last_location))
            {
                path = null;
            }

            if (pathfind && (path == null || (path.empty && path.bfs != null && !path.bfs.IsFinished())))
            {
                BFSInfo bfs = path != null ? path.bfs : null;
                path = PathfindUnexplored(uc, bfs,1000, true);
                if (path == null)
                {
                    // Don't do that again
                    pathfind = false;
                    EXPLORING = false;
                }
            }

            if (path != null)
            {
                // BFS was paused
                if (path.empty && path.bfs != null && !path.bfs.IsFinished())
                {
                    last_location = uc.getLocation();
                    return true;
                }

                if (path.index < 0 || path.index >= path.path.length)
                {
                    path = null;
                }
                else
                {
                    if (!uc.canMove())
                        return true;

                    uc.move(path.path[path.index]);
                    last_location = uc.getLocation();
                    if (path.reverse)
                        --path.index;
                    else
                        ++path.index;

                    return true;
                }
            }

            if (current_steps > MAX_EXPLORE_STEPS)
                current_direction = null;

            if (!uc.canMove())
                return true;

            boolean moved = false;

            Direction last = Direction.ZERO;
            int iterations = 0;
            while (!moved && ++iterations < 100)
            {
                if (current_direction == null)
                {
                    do current_direction = all_directions[(int) (uc.getRandomDouble() * all_directions.length)];
                    while (current_direction == last);
                    current_steps = 0;
                }

                Location next = uc.getLocation().add(current_direction);
                if (uc.canMove(current_direction) && OBSTACLES[next.x*MAP_SIZE_Y+next.y] != 2)
                {
                    uc.move(current_direction);
                    ++current_steps;
                    moved = true;
                }
                else
                {
                    last = current_direction;
                    current_direction = null;
                }
            }
            return true;
        }

        @Override
        public boolean IsActive() {
            return active;
        }

        @Override
        public void SetActive(boolean active){
            this.active = active;
        }
    }

    public class CraftAlways implements Objective{
        private boolean active;
        private boolean completed = false;
        private final boolean do_grass;
        private final boolean alternate;
        private final Craftable craftable;

        Objective craft;
        Objective explore = new Explore();
        Objective grass = new Gather(Material.GRASS, true, false);

        public CraftAlways(Craftable craftable) {
            this(craftable, false);
        }

        public CraftAlways(Craftable craftable, boolean do_grass) {
            this(craftable, do_grass, false);
        }

        public CraftAlways(Craftable craftable, boolean do_grass, boolean alternate) {
            this.active = true;
            this.do_grass = do_grass;
            this.alternate = alternate;
            this.craftable = craftable;

            craft = new Craft(craftable);
            craft.SetActive(true);
            explore.SetActive(true);
        }

        @Override
        public boolean IsCompleted(UnitController uc){
            return completed || uc.hasCraftable(craftable);
        }

        @Override
        public boolean Follow(UnitController uc)
        {
            if (craft.IsCompleted(uc))
            {
                completed = true;
                return true;
            }
            else if (!craft.Follow(uc))
            {
                if (alternate)
                {
                    if (uc.getRandomDouble() < 0.5)
                        grass.Follow(uc);
                    else
                        explore.Follow(uc);
                }
                else
                {
                    if (do_grass)
                        grass.Follow(uc);
                    else
                        explore.Follow(uc);
                }
            }
            else if (craft.IsCompleted(uc))
            {
                completed = true;
            }

            return true;
        }

        @Override
        public boolean IsActive() {
            return active;
        }

        @Override
        public void SetActive(boolean active){
            this.active = active;
        }
    }

    public class BreakAgendas implements Objective{
        private boolean active;

        GoToLocation subobjective = null;
        private Location subobjective_target = null;

        public BreakAgendas(){
            this.active = true;
        }

        @Override
        public boolean IsCompleted(UnitController uc){
            return false;
        }

        Craftable HighestDamageWeapon(UnitInfo unit)
        {
            CraftableInfo[] craftables = unit.getCarriedCraftablesArray();
            Craftable best = null;
            int best_damage = 0;
            for (CraftableInfo craftable : craftables)
            {
                Craftable c = craftable.getCraftable();
                if (GameConstants.craftableDamage[c.ordinal()] > best_damage)
                {
                    best_damage = GameConstants.craftableDamage[c.ordinal()];
                    best = c;
                }
            }
            return best;
        }

        private boolean OnSubobjectiveCompleted(UnitController uc)
        {
            // Check if there's still a bed and destroy it
            StructureInfo info = uc.senseStructureAtLocation(subobjective_target);
            if (info != null && (info.getTeam() == null || info.getTeam() == enemy_team))
            {
                // Get on top
                if (uc.canMove() && uc.getLocation().distanceSquared(subobjective_target) > 0)
                    uc.move(uc.getLocation().directionTo(subobjective_target));

                // Break it
                Craftable weapon = HighestDamageWeapon(uc.getUnitInfo());
                if (uc.canUseCraftable(weapon, subobjective_target))
                {
                    uc.useCraftable(weapon, subobjective_target);

                    // Effectively reset the objective
                    subobjective = null;
                }
                else if (uc.canAct())
                {
                    subobjective = new GoToLocation(subobjective_target, false, 2);
                }
                return true;
            }
            else
                return false;
        }

        @Override
        public boolean Follow(UnitController uc)
        {
            Craftable weapon = HighestDamageWeapon(uc.getUnitInfo());
            boolean has_to_craft_shovel = weapon == null && uc.canCraft(Craftable.SHOVEL);

            if (subobjective != null)
            {
                if (subobjective.IsCompleted(uc))
                {
                    return OnSubobjectiveCompleted(uc);
                }
                else
                {
                    boolean result = subobjective.Follow(uc);
                    if (!result)
                    {
                        subobjective = null;
                        return false;
                    }
                    else if (subobjective.IsCompleted(uc))
                        return OnSubobjectiveCompleted(uc);

                    return true;
                }
            }

            StructureInfo[] structures = uc.senseStructures(GameConstants.UNIT_VISION_RANGE, enemy_team, null);
            subobjective_target = null;

            Location structure_loc = null;
            for (StructureInfo info : structures)
            {
                Location loc = info.getLocation();
                if (info.getType() == StructureType.BED || info.getType() == StructureType.BEACON)
                {
                    if (structure_loc != null || (!has_to_craft_shovel && weapon == null))
                        continue;

                    structure_loc = loc;
                }
                else if (info.getType() == StructureType.TURRET)
                {
                    if (OBSTACLES[loc.x*MAP_SIZE_Y+loc.y] != 2)
                    {
                        MarkTurretRange(loc.x, loc.y, 2);
                        //Broadcast(uc, 0, StructureType.TURRET.ordinal(), loc.x, loc.y);
                    }

                    if (!uc.hasCraftable(Craftable.TNT))
                        continue;

                    if (structure_loc != null)
                        continue;

                    structure_loc = loc;
                }
            }

            // Try to get chests
            if (structure_loc == null)
            {
                structures = uc.senseStructures(GameConstants.UNIT_VISION_RANGE, null, StructureType.CHEST);
                if (structures.length > 0)
                    structure_loc = structures[0].getLocation();
            }

            // Go for a defenseless enemy (stored before)
            if (structure_loc == null && DEFENSELESS_ENEMY != null)
            {
                if (uc.canMove())
                    uc.move(PathfindAccessible4Steps(uc, DEFENSELESS_ENEMY));

                if (weapon == Craftable.TNT)
                    weapon = BestWeapon(uc, DEFENSELESS_ENEMY, true);

                if (weapon != null && uc.canUseCraftable(weapon, DEFENSELESS_ENEMY))
                    uc.useCraftable(weapon, DEFENSELESS_ENEMY);

                return true;
            }

            if (structure_loc != null)
            {
                if (weapon == null)
                {
                    if (uc.canCraft(Craftable.SHOVEL))
                    {
                        uc.craft(Craftable.SHOVEL);
                        weapon = Craftable.SHOVEL;
                    }
                    else
                        return false;
                }

                if (IsAccessible(uc, uc.getLocation(), structure_loc, 4))
                {
                    if (uc.canMove())
                    {
                        Direction dir = PathfindAccessible4Steps(uc, structure_loc);
                        uc.move(dir);
                    }

                    if (has_to_craft_shovel)
                        uc.craft(Craftable.SHOVEL);

                    if (uc.canUseCraftable(weapon, structure_loc))
                        uc.useCraftable(weapon, structure_loc);

                    return true;
                }
                else if (!IsAccessible(uc, uc.getLocation(), structure_loc, 8))
                    return false;
                else {
                    subobjective = new GoToLocation(structure_loc, false, weapon == Craftable.TNT ? GameConstants.craftableRange[Craftable.TNT.ordinal()] : 2);
                    subobjective_target = structure_loc;
                    return true;
                }
            }

            return false;
        }

        @Override
        public boolean IsActive() {
            return active;
        }

        @Override
        public void SetActive(boolean active){
            this.active = active;
        }
    }

    public class BreakPeople implements Objective{
        private boolean active;

        public BreakPeople(){
            this.active = true;
        }

        @Override
        public boolean IsCompleted(UnitController uc){
            return false;
        }

        Craftable HighestDamageWeapon(UnitInfo unit)
        {
            CraftableInfo[] craftables = unit.getCarriedCraftablesArray();
            Craftable best = null;
            int best_damage = 0;
            for (CraftableInfo craftable : craftables)
            {
                Craftable c = craftable.getCraftable();
                if (c == Craftable.FIREWORK || c == Craftable.TNT)
                    continue;

                if (GameConstants.craftableDamage[c.ordinal()] > best_damage)
                {
                    best_damage = GameConstants.craftableDamage[c.ordinal()];
                    best = c;
                }
            }
            return best;
        }

        private int CanWin(UnitController uc, UnitInfo me, UnitInfo enemy)
        {
            // 1. Weapon vs no weapon
            Craftable weapon_me = HighestDamageWeapon(me);
            Craftable weapon_other = HighestDamageWeapon(enemy);
            boolean has_to_craft_shovel = false;

            if (weapon_me == null && weapon_other == null)
            {
                // Ignore
                if (!uc.canCraft(Craftable.SHOVEL))
                    return 0;

                weapon_me = Craftable.SHOVEL;
                has_to_craft_shovel = true;
            }
            else if (weapon_me != null && weapon_other == null)
            {
                // Enemy can't fight back
                best_combat_weapon = weapon_me;
                return 3;
            }
            else if (weapon_me == null)
            {
                // If we can't craft a shovel, run
                if (!uc.canCraft(Craftable.SHOVEL))
                    return 1;

                weapon_me = Craftable.SHOVEL;
                has_to_craft_shovel = true;
            }

            int dmg_me = GameConstants.craftableDamage[weapon_me.ordinal()];
            int dmg_enemy = weapon_other != null ? GameConstants.craftableDamage[weapon_other.ordinal()] : 0;

            // 2. Check armor and exit early if my dmg = 0
            int[][] craftables_me = me.getCarriedCraftables();
            if (me.hasCraftable(Craftable.ARMOR))
            {
                dmg_enemy -= 1;
            }
            int[][] craftables_enemy = enemy.getCarriedCraftables();
            if (enemy.hasCraftable(Craftable.ARMOR))
            {
                dmg_me -= 1;
            }

            if (dmg_me <= 0 && dmg_enemy <= 0)
                return 0; // Ignore
            else if (dmg_me <= 0)
                return 1; // Get out
            else if (dmg_enemy <= 0)
            {
                best_combat_weapon = weapon_me;
                if (has_to_craft_shovel)
                    uc.craft(Craftable.SHOVEL);
                return 3; // Enemy can't fight back
            }

            // 3. Everyone does damage:
            //      1. Calc cooldowns
            //      2. Calc "life"
            //      3. Calc dmg/turn
            //      4. Calc turns
            //      5. Ret true if my turns>=their turns
            double cd_me = me.getWeight() / 100.0;
            double cd_enemy = enemy.getWeight() / 100.0;

            int hp_me = me.getHealth() + craftables_me[Craftable.BAKED_POTATO.ordinal()][0]*2;
            int hp_enemy = enemy.getHealth() + craftables_enemy[Craftable.BAKED_POTATO.ordinal()][0]*2;

            double dmg_turn_me = dmg_me/cd_me;
            double dmg_turn_enemy = dmg_enemy/cd_enemy;

            double turns_me = hp_me/dmg_turn_enemy + (int)enemy.getCurrentActionCooldown();
            double turns_enemy = hp_enemy/dmg_turn_me + (int)me.getCurrentActionCooldown();

            // Fight
            //uc.println("Turns I last: " + turns_me + ", he lasts: " + turns_enemy);

            if (turns_me+0.5 >= turns_enemy)
            {
                if (has_to_craft_shovel)
                    uc.craft(Craftable.SHOVEL);

                best_combat_weapon = weapon_me;
                return 2;
            }

            // Check if there's an ally that can see, pathfind to and hit this enemy
            UnitInfo[] allies = uc.senseUnits(GameConstants.UNIT_VISION_RANGE, uc.getTeam());
            UnitInfo selected_ally = null;
            for (UnitInfo ally : allies)
            {
                if (ally.getLocation().distanceSquared(enemy.getLocation()) <= GameConstants.UNIT_VISION_RANGE)
                {
                    if (IsAccessible(uc, ally.getLocation(), enemy.getLocation(), 5))
                    {
                        selected_ally = ally;
                        break;
                    }
                }
            }

            if (selected_ally != null)
            {
                Craftable weapon_ally = HighestDamageWeapon(selected_ally);
                if (weapon_ally != null)
                {
                    int dmg_ally = GameConstants.craftableDamage[weapon_ally.ordinal()];
                    if (enemy.hasCraftable(Craftable.ARMOR))
                        dmg_ally -= 1;

                    if (dmg_ally > 0)
                    {
                        if (has_to_craft_shovel)
                            uc.craft(Craftable.SHOVEL);

                        best_combat_weapon = weapon_me;
                        return 2;
                    }
                }
            }

            // Get out, but try to hit them first
            if (!has_to_craft_shovel)
            {
                best_combat_weapon = weapon_me;
                if (uc.canUseCraftable(weapon_me, enemy.getLocation()))
                    uc.useCraftable(weapon_me, enemy.getLocation());
            }
            return 1;
        }

        @Override
        public boolean Follow(UnitController uc)
        {
            UnitInfo[] units = SensedUnits(uc);
            if (units.length == 0)
                return false;

            if (!uc.canMove() && !uc.canAct())
                return true;

            Location target = null;
            for (UnitInfo unit : units)
            {
                int fight_result = CanWin(uc, uc.getUnitInfo(), unit);
                if (fight_result == 1)
                {
                    // GTFO
                    Direction dir = DIRECTION_OPPOSITES[uc.getLocation().directionTo(unit.getLocation()).ordinal()];
                    if (uc.canMove(dir))
                        uc.move(dir);
                    else if (uc.canMove())
                    {
                        if (uc.canMove(dir.rotateLeft()))
                            uc.move(dir.rotateLeft());
                        else if (uc.canMove(dir.rotateRight()))
                            uc.move(dir.rotateRight());
                        else if (uc.canMove(dir.rotateLeft().rotateLeft()))
                            uc.move(dir.rotateLeft().rotateLeft());
                        else if (uc.canMove(dir.rotateRight().rotateRight()))
                            uc.move(dir.rotateRight().rotateRight());

                        if (best_combat_weapon != null && uc.canUseCraftable(best_combat_weapon, unit.getLocation()))
                            uc.useCraftable(best_combat_weapon, unit.getLocation());
                    }
                    return true;
                }
                else if (fight_result == 2)
                {
                    target = unit.getLocation();
                    break;
                }
                else if (fight_result == 3)
                {
                    // Enemy that can't fight back. Store for later, go for them if no buildings have been found
                    int qdist = uc.getLocation().distanceSquared(unit.getLocation());
                    if (DEFENSELESS_ENEMY == null)
                        DEFENSELESS_ENEMY = unit.getLocation();
                    else if (qdist < uc.getLocation().distanceSquared(DEFENSELESS_ENEMY))
                        DEFENSELESS_ENEMY = unit.getLocation();
                }
            }

            // Empirically better to just go for them always
            if (target == null && DEFENSELESS_ENEMY != null)
            {
                target = DEFENSELESS_ENEMY;
            }

            if (target == null)
                return false;

            if (uc.hasCraftable(Craftable.TURRET_BLUEPRINT) && uc.canUseCraftable(Craftable.TURRET_BLUEPRINT, uc.getLocation()))
            {
                uc.useCraftable(Craftable.TURRET_BLUEPRINT, uc.getLocation());
                return true;
            }

            if (uc.canUseCraftable(best_combat_weapon, target))
            {
                uc.useCraftable(best_combat_weapon, target);
                return true;
            }
            else
            {
                int distance = uc.getLocation().distanceSquared(target);
                if (distance == 2)
                    return true;
            }

            if (!IsAccessible(uc, uc.getLocation(), target, 4))
                return false;

            // Pathfind and move
            Direction dir = PathfindAccessible4Steps(uc, target);
            if (dir != Direction.ZERO)
            {
                if (uc.canMove(dir))
                    uc.move(dir);
                else if (uc.canMove(dir.rotateRight()))
                    uc.move(dir.rotateRight());
                else if (uc.canMove(dir.rotateLeft()))
                    uc.move(dir.rotateLeft());
            }

            if (uc.canUseCraftable(best_combat_weapon, target))
                uc.useCraftable(best_combat_weapon, target);
            return true;
        }

        @Override
        public boolean IsActive() {
            return active;
        }

        @Override
        public void SetActive(boolean active){
            this.active = active;
        }
    }

    public class PlaceCraftable implements Objective{
        private boolean active;
        private final Craftable craftable;
        private final boolean once;
        private boolean completed = false;

        public PlaceCraftable(Craftable craftable){
            this(craftable, false);
        }

        public PlaceCraftable(Craftable craftable, boolean once){
            this.active = true;
            this.craftable = craftable;
            this.once = once;
        }

        @Override
        public boolean IsCompleted(UnitController uc){
            return once && completed;
        }

        @Override
        public boolean Follow(UnitController uc)
        {
            if (!uc.hasCraftable(craftable))
                return false;

            if (uc.canUseCraftable(craftable, uc.getLocation()))
            {
                uc.useCraftable(craftable, uc.getLocation());
                completed = true;
                return true;
            }
            else if (!uc.canAct())
                return true;
            else
                // Bad positioning
                return false;
        }

        @Override
        public boolean IsActive() {
            return active;
        }

        @Override
        public void SetActive(boolean active){
            this.active = active;
        }
    }

    public class PlaceBlueprint implements Objective
    {
        private boolean active;
        private final Craftable blueprint;
        private final boolean always;
        private final boolean anywhere;
        private final Location where;
        private final Location from;
        private boolean completed = false;
        private Objective subobjective = null;

        public PlaceBlueprint(Craftable blueprint, Location location, Location from, boolean always){
            this.blueprint = blueprint;
            this.active = true;
            this.always = always;
            this.anywhere = location == null;
            this.where = location;
            this.from = from;
        }

        @Override
        public boolean IsCompleted(UnitController uc){
            return completed;
        }

        @Override
        public boolean Follow(UnitController uc)
        {
            if (!uc.hasCraftable(blueprint))
                return false;

            if (!anywhere)
            {
                int distance = uc.getLocation().distanceSquared(where);
                if (uc.canUseCraftable(blueprint, where))
                {
                    uc.useCraftable(blueprint, where);
                    if (blueprint == Craftable.BRIDGE_BLUEPRINT)
                    {
                        OBSTACLES[where.x*MAP_SIZE_Y+where.y] = 3;
                        for (int index = 0; index < OBSTACLES.length; ++index)
                            if (OBSTACLES[index] == 4)
                                OBSTACLES[index] = 0;
                    }
                    completed = true;
                }
                else if (distance <= 2 && !uc.canAct())
                {
                    return true;
                }
                else
                {
                    if (subobjective == null)
                        subobjective = new GoToLocation(from, true, 0);

                    subobjective.Follow(uc);
                }

                return true;
            }
            else
            {
                // Try all neighbour squares
                for (Direction dir : all_directions)
                {
                    Location option = uc.getLocation().add(dir);
                    if (uc.canUseCraftable(blueprint, option))
                    {
                        uc.useCraftable(blueprint, option);
                        completed = true;
                        return true;
                    }
                    else if (!uc.canAct())
                        return true;
                }

                return false;
            }
        }

        @Override
        public boolean IsActive() {
            return active;
        }

        @Override
        public void SetActive(boolean active){
            this.active = active;
        }
    }

    public class Defend implements Objective{
        private boolean active;

        public Defend(){
            this.active = true;
        }

        @Override
        public boolean IsCompleted(UnitController uc){
            return false;
        }

        @Override
        public boolean Follow(UnitController uc)
        {
            UnitInfo me = uc.getUnitInfo();
            float movement = me.getCurrentMovementCooldown();
            float action = me.getCurrentActionCooldown();

            // Eat potato
            if (me.getHealth() <= GameConstants.UNIT_MAX_HEALTH-3)
            {
                if (uc.canUseCraftable(Craftable.BAKED_POTATO, uc.getLocation()))
                {
                    uc.useCraftable(Craftable.BAKED_POTATO, uc.getLocation());
                    return true;
                }
            }

            UnitInfo[] units = SensedUnits(uc);
            if (units.length == 0)
                return false;

            if (action > 0.0f)
                return false;

            Location target = units[0].getLocation();

            // Pick the best weapon and use it
            Craftable weapon = BestWeapon(uc, target, true);
            if (weapon != null)
            {
                if (uc.canUseCraftable(weapon,target))
                {
                    uc.useCraftable(weapon, target);
                    return true;
                }
            }

            return false;
        }

        @Override
        public boolean IsActive() {
            return active;
        }

        @Override
        public void SetActive(boolean active){
            this.active = active;
        }
    }

    public class UpgradeTools implements Objective
    {
        private boolean active;

        public UpgradeTools(){
            this.active = true;
        }

        @Override
        public boolean IsCompleted(UnitController uc){
            return false;
        }

        @Override
        public boolean Follow(UnitController uc)
        {
            if (!uc.canAct())
                return false;

            UnitInfo me = uc.getUnitInfo();
            int[] materials = me.getCarriedMaterials();
            int iron_beacon = 1 + uc.getCraftedBeacons();
            int gold_beacon = 1 + uc.getCraftedBeacons();
            int diamond_beacon = 1 + uc.getCraftedBeacons();
            int iron = materials[Material.IRON.ordinal()];
            int gold = materials[Material.GOLD.ordinal()];
            int diamond = materials[Material.DIAMOND.ordinal()];
            if (iron <= iron_beacon && gold <= gold_beacon && diamond <= diamond_beacon)
                return false;

            CraftableInfo[] craftables = me.getCarriedCraftablesArray();
            for (CraftableInfo info : craftables)
            {
                Craftable craftable = info.getCraftable();
                if (iron > iron_beacon && uc.canUpgrade(craftable, Upgrade.IRON))
                {
                    uc.upgrade(craftable, Upgrade.IRON);
                    return true;
                }
                else if (gold > gold_beacon && uc.canUpgrade(craftable, Upgrade.GOLD))
                {
                    uc.upgrade(craftable, Upgrade.GOLD);
                    return true;
                }
                else if (diamond > diamond_beacon && uc.canUpgrade(craftable, Upgrade.DIAMOND))
                {
                    uc.upgrade(craftable, Upgrade.DIAMOND);
                    return true;
                }
            }

            return false;
        }

        @Override
        public boolean IsActive() {
            return active;
        }

        @Override
        public void SetActive(boolean active){
            this.active = active;
        }
    }

    public class GetRich implements Objective
    {
        private boolean active;
        Objective subobjective = null;
        Location subobjective_location = null;

        public GetRich(){
            active = true;
        }

        @Override
        public boolean IsCompleted(UnitController uc){
            return false;
        }

        @Override
        public boolean Follow(UnitController uc) {
            if (!uc.hasCraftable(Craftable.PICKAXE))
                return false;

            // If a subobjective is active, we are already going for some material. Don't do the code below, just continue the subobjective
            if (subobjective != null && subobjective.IsActive())
            {
                if (subobjective.IsCompleted(uc))
                {
                    if (!uc.canAct())
                        return true;

                    Material material = uc.senseMaterialAtLocation(subobjective_location);
                    if (material == null)
                    {
                        subobjective = null;
                        return false;
                    }
                    // Use tool if available
                    // Gather thing
                    Craftable gathering_tool = material.gatheringTool();

                    if (gathering_tool != null && uc.hasCraftable(gathering_tool))
                        uc.useCraftable(material.gatheringTool(), subobjective_location);
                    else if (gathering_tool != null)
                        uc.gather();

                    subobjective = null;
                    return true;
                }
                else
                {
                    boolean result = subobjective.Follow(uc);
                    if (!result)
                        subobjective = null;
                    return result;
                }
            }
            else
            {
                //if (!CanIHaveABeacon(uc, VisibleFriendClosestToBeacon(uc)))
                //    return false;

                // Sense for valuable materials
                MaterialInfo[] mat_info = SensedMaterials(uc);

                // Go for the nearest
                Location mat_loc = null;
                for (MaterialInfo info : mat_info)
                {
                    Material material = info.getMaterial();
                    if (material == Material.COPPER || material == Material.IRON || material == Material.GOLD || material == Material.DIAMOND)
                    {
                        mat_loc = info.getLocation();
                        break;
                    }
                }

                if (mat_loc != null)
                {
                    //uc.drawLineDebug(uc.getLocation(), mat_loc, 255,0,0);
                    subobjective = new GoToLocation(mat_loc, false, 2);
                    subobjective_location = mat_loc;
                    subobjective.SetActive(true);
                    boolean result = subobjective.Follow(uc);
                    if (!result)
                        subobjective = null;
                    return result;
                }

                // No material has been found, tell so and signal that following this objective might be impossible.
                subobjective = null;
                return false;
            }
        }

        @Override
        public boolean IsActive() {
            return active;
        }

        @Override
        public void SetActive(boolean active){
            this.active = active;
        }
    }

    public class Heal implements Objective{
        private boolean active;

        public Heal(){
            this.active = true;
        }

        @Override
        public boolean IsCompleted(UnitController uc){
            return false;
        }

        @Override
        public boolean Follow(UnitController uc)
        {
            if (!uc.hasCraftable(Craftable.BAKED_POTATO) || uc.getUnitInfo().getHealth() == GameConstants.UNIT_MAX_HEALTH)
                return false;

            UnitInfo[] units = SensedUnits(uc);
            if (units.length == 0 || uc.getUnitInfo().getHealth() <= GameConstants.UNIT_MAX_HEALTH-3)
            {
                if (uc.canUseCraftable(Craftable.BAKED_POTATO, uc.getLocation()))
                {
                    uc.useCraftable(Craftable.BAKED_POTATO, uc.getLocation());
                    return false;
                }
            }
            return false;
        }

        @Override
        public boolean IsActive() {
            return active;
        }

        @Override
        public void SetActive(boolean active){
            this.active = active;
        }
    }

    public class Gather implements Objective
    {
        private boolean active;
        private final Material material;
        Objective subobjective = null;
        Location subobjective_target = null;
        private final int amount;
        private final boolean always;
        private final boolean only_with_tool;
        private boolean completed = false;
        Craftable tool = null;
        boolean tool_required = false;
        Objective gather_grass = null;
        Location grass_location = null;

        public Gather(Material material)
        {
            this(material, 1, false, false);
        }

        public Gather(Material material, boolean always, boolean only_with_tool)
        {
            this(material, 1, always, only_with_tool);
        }

        public Gather(Material material, int amount, boolean always, boolean only_with_tool){
            active = true;
            this.material = material;
            this.amount = amount;
            this.always = always;
            this.only_with_tool = only_with_tool;

            tool = material.gatheringTool();
            if (material.canBeGathered())
                tool_required = false;
        }

        @Override
        public boolean IsCompleted(UnitController uc){
            if (completed)
                return true;

            completed = uc.getUnitInfo().getCarriedMaterials()[material.ordinal()] >= amount;
            return completed;
        }

        private boolean OnSubobjectiveComplete(UnitController uc)
        {
            if (!uc.canAct())
                return true;

            Material material = uc.senseMaterialAtLocation(subobjective_target);

            // Gather thing
            if (material == null)
            {
                subobjective = null;
                return false;
            }
            Craftable gathering_tool = material.gatheringTool();
            if (gathering_tool != null && uc.hasCraftable(gathering_tool))
            {
                uc.useCraftable(material.gatheringTool(), subobjective_target);
            }
            else if (gathering_tool != null)
            {
                uc.gather();
            }

            subobjective = null;
            return true;
        }

        @Override
        public boolean Follow(UnitController uc){
            if (only_with_tool && !uc.hasCraftable(tool))
                return false;

            // If a subobjective is active, we are already going for some material. Don't do the code below, just continue the subobjective
            if (subobjective != null && subobjective.IsActive())
            {
                if (subobjective.IsCompleted(uc))
                {
                    return OnSubobjectiveComplete(uc);
                }
                else
                {
                    boolean result = subobjective.Follow(uc);
                    if (subobjective.IsCompleted(uc))
                        return OnSubobjectiveComplete(uc);

                    if (!result)
                        subobjective = null;
                    return result;
                }
            }
            else if (gather_grass != null && gather_grass.IsActive())
            {
                if (gather_grass.IsCompleted(uc))
                {
                    UnitInfo unit_info = uc.getUnitInfo();
                    if (unit_info.getCurrentActionCooldown() > 0.0f)
                        return true;

                    if (uc.hasCraftable(Craftable.SHOVEL))
                        uc.useCraftable(Craftable.SHOVEL, grass_location);
                    else
                        uc.gather();

                    gather_grass = null;
                }
                else
                {
                    if (!gather_grass.Follow(uc))
                        gather_grass = null;
                }
                return true;
            }

            // Find materials
            // Look for missing materials
            MaterialInfo[] mat_info = SensedMaterials(uc);

            // Go for the nearest useful one
            Location mat_loc = null;
            boolean has_tool = false;
            for (MaterialInfo info : mat_info)
            {
                // Check if it's relevant, missing and minable
                if (info.getMaterial() == material && (!tool_required || uc.hasCraftable(tool)))
                {
                    mat_loc = info.getLocation();
                    has_tool = tool != null && uc.hasCraftable(tool);
                    break;
                }
            }

            if (mat_loc != null)
            {
                // Check accessibility first to avoid doing an expensive BFS
                int source_index = uc.getLocation().x*MAP_SIZE_Y+uc.getLocation().y;
                int mat_loc_index = mat_loc.x*MAP_SIZE_Y+mat_loc.y;
                if (ACCESSIBILITY.contains(source_index, mat_loc_index))
                    return false;
                else if (!IsAccessible(uc, uc.getLocation(), mat_loc, 9))
                {
                    // Add to accessible set
                    ACCESSIBILITY.add(source_index, mat_loc_index);
                    return false;
                }

                subobjective = new GoToLocation(mat_loc, false, has_tool ? 2 : 0);
                subobjective_target = mat_loc;
                subobjective.SetActive(true);
                boolean result = subobjective.Follow(uc);
                if (!result)
                    subobjective = null;
                return result;
            }

            // No material has been found, tell so and signal that following this objective might be impossible.
            subobjective = null;
            gather_grass = null;

            if (!always)
                return false;

            // If always is set to true, break nearest grass instead. If that is not an option, either explore or wait. Just return true
            Location[] grass_locations = uc.senseMaterials(Material.GRASS, GameConstants.UNIT_VISION_RANGE);
            for (Location grass_loc : grass_locations)
            {
                if (OBSTACLES[grass_loc.x*MAP_SIZE_Y+grass_loc.y] == 4)
                    continue;
                gather_grass = new GoToLocation(grass_loc, false, uc.hasCraftable(Craftable.SHOVEL) ? 2 : 0);
                grass_location = grass_loc;
                gather_grass.Follow(uc);
                return true;
            }

            RandomMove(uc);
            return true;
        }

        @Override
        public boolean IsActive() {
            return active;
        }

        @Override
        public void SetActive(boolean active){
            this.active = active;
        }
    }

    public class FollowPath implements Objective
    {
        private boolean active;

        private boolean completed = false;
        private final Objective[] steps;
        private int current_step = 0;
        private final Location[] checkpoints;
        private final boolean[] pathfind;

        // checkpoints and pathfind MUST have the same size, there pathfind[0] says if going to checkpoint[0] will require any pathfinding
        public FollowPath(Location[] checkpoints, boolean[] pathfind) {
            active = true;
            this.checkpoints = checkpoints;
            this.pathfind = pathfind;

            steps = new Objective[checkpoints.length];
            steps[0] = new GoToLocation(checkpoints[0], !pathfind[0], 0);
        }

        @Override
        public boolean IsCompleted(UnitController uc){
            return completed;
        }

        @Override
        public boolean Follow(UnitController uc)
        {
            // Just in case I don't want to think
            if (current_step >= steps.length){
                completed = true;
                return true;
            }

            steps[current_step].Follow(uc);
            if (steps[current_step].IsCompleted(uc))
            {
                ++current_step;
                if (current_step >= steps.length)
                    completed = true;
                else
                    steps[current_step] = new GoToLocation(checkpoints[current_step], !pathfind[current_step], 0);
            }
            return true;
        }

        @Override
        public boolean IsActive() {
            return active;
        }

        @Override
        public void SetActive(boolean active){
            this.active = active;
        }
    }

    public class IncreaseVersion implements Objective
    {
        private boolean active;
        private boolean completed = false;

        // checkpoints and pathfind MUST have the same size, there pathfind[0] says if going to checkpoint[0] will require any pathfinding
        public IncreaseVersion() {
            active = true;
        }

        @Override
        public boolean IsCompleted(UnitController uc){
            return completed;
        }

        @Override
        public boolean Follow(UnitController uc)
        {
            ++VERSION;
            completed = true;
            return true;
        }

        @Override
        public boolean IsActive() {
            return active;
        }

        @Override
        public void SetActive(boolean active){
            this.active = active;
        }
    }

    public class BreakCraftable implements Objective
    {
        private boolean active;
        private final Craftable craftable;
        private boolean completed = false;

        public BreakCraftable(Craftable craftable) {
            active = true;
            this.craftable = craftable;
        }

        @Override
        public boolean IsCompleted(UnitController uc){
            return completed;
        }

        @Override
        public boolean Follow(UnitController uc)
        {
            if (completed)
                return true;

            if (uc.hasCraftable(craftable))
            {
                uc.destroyCraftable(craftable);
                completed = true;
            }
            else
            {
                completed = true;
            }

            return true;
        }

        @Override
        public boolean IsActive() {
            return active;
        }

        @Override
        public void SetActive(boolean active){
            this.active = active;
        }
    }

    public class BreakMaterial implements Objective
    {
        private boolean active;
        private final Material material;
        private final int min_amount;

        public BreakMaterial(Material material) {
            this(material, 1);
        }

        public BreakMaterial(Material material, int min_amount) {
            active = true;
            this.material = material;
            this.min_amount = min_amount;
        }

        @Override
        public boolean IsCompleted(UnitController uc){
            return false;
        }

        @Override
        public boolean Follow(UnitController uc)
        {
            if (uc.getUnitInfo().getCarriedMaterials()[material.ordinal()] >= min_amount)
            {
                uc.destroyMaterial(material);
            }
            return false;
        }

        @Override
        public boolean IsActive() {
            return active;
        }

        @Override
        public void SetActive(boolean active){
            this.active = active;
        }
    }

    public class YardMaintenance implements Objective
    {
        private boolean active;

        public YardMaintenance() {
            active = true;
        }

        @Override
        public boolean IsCompleted(UnitController uc){
            return false;
        }

        @Override
        public boolean Follow(UnitController uc)
        {
            if (uc.senseMaterialAtLocation(uc.getLocation()) == Material.GRASS)
            {
                if (!uc.canAct())
                    return true;

                if (uc.hasCraftable(Craftable.SHOVEL) && uc.canUseCraftable(Craftable.SHOVEL, uc.getLocation()))
                    uc.useCraftable(Craftable.SHOVEL, uc.getLocation());
                else if (uc.canGather())
                    uc.gather();

                return true;
            }

            Location[] grass_locs = uc.senseMaterials(Material.GRASS, GameConstants.UNIT_VISION_RANGE);
            for (Location grass : grass_locs)
            {
                if (uc.hasCraftable(Craftable.SHOVEL) && uc.canUseCraftable(Craftable.SHOVEL, grass))
                    uc.useCraftable(Craftable.SHOVEL, grass);

                Direction dir = uc.getLocation().directionTo(grass);
                Location next = uc.getLocation().add(dir);
                if (OBSTACLES[next.x*MAP_SIZE_Y+next.y] == 2)
                    continue;

                if (uc.canMove(dir))
                    uc.move(dir);
                else if (uc.getUnitInfo().getCurrentMovementCooldown() < 1.0f)
                    SafeRandomMove(uc);

                break;
            }

            return true;
        }

        @Override
        public boolean IsActive() {
            return active;
        }

        @Override
        public void SetActive(boolean active){
            this.active = active;
        }
    }

    public class MoveFromChest implements Objective
    {
        private boolean active;
        private Craftable craftable = null;
        private int upgrade_index;
        private Material material = null;
        private final boolean is_craftable;
        private final boolean once;
        private boolean completed = false;
        private final boolean deposit;

        public MoveFromChest(boolean deposit, Material material, boolean once)
        {
            this(deposit, false, once);
            this.material = material;
        }

        public MoveFromChest(boolean deposit, Craftable craftable, int upgrade_index, boolean once)
        {
            this(deposit, true, once);
            this.craftable = craftable;
            this.upgrade_index = upgrade_index;
        }

        private MoveFromChest(boolean deposit, boolean is_craftable, boolean once) {
            active = true;
            this.once = once;
            this.is_craftable = is_craftable;
            this.deposit = deposit;
        }

        @Override
        public boolean IsCompleted(UnitController uc){
            return once && completed;
        }

        @Override
        public boolean Follow(UnitController uc)
        {
            if (once && completed)
                return true;

            if (uc.canMoveItemFromChest(deposit, !is_craftable, is_craftable ? craftable.ordinal() : material.ordinal(), upgrade_index))
            {
                uc.moveItemFromChest(deposit, !is_craftable, is_craftable ? craftable.ordinal() : material.ordinal(), upgrade_index);
                if (once)
                    completed = true;
                return true;
            }

            // TODO: this does not work and is not being used
            StructureInfo[] chests = uc.senseStructures(GameConstants.UNIT_VISION_RANGE, uc.getTeam(), StructureType.CHEST);
            if (chests.length == 0)
            {
                // No chest, wtf?
                return false;
            }

            Location chest_loc = chests[0].getLocation();
            if (uc.getLocation().distanceSquared(chest_loc) > 0)
            {
                uc.move(uc.getLocation().directionTo(chest_loc));
                return true;
            }

            return true;
        }

        @Override
        public boolean IsActive() {
            return active;
        }

        @Override
        public void SetActive(boolean active){
            this.active = active;
        }
    }

    public class BlockLocation implements Objective
    {
        private boolean active;
        private final Location location;
        private final boolean block;

        public BlockLocation(Location location) {
            this(location, false);
        }

        public BlockLocation(Location location, boolean unblock) {
            active = true;
            this.location = location;
            this.block = !unblock;
        }

        @Override
        public boolean IsCompleted(UnitController uc){
            if (block)
                return OBSTACLES[location.x * MAP_SIZE_Y + location.y] == 2;
            return OBSTACLES[location.x * MAP_SIZE_Y + location.y] == 3;
        }

        @Override
        public boolean Follow(UnitController uc)
        {
            if (block)
                OBSTACLES[location.x * MAP_SIZE_Y + location.y] = 2;
            else
                OBSTACLES[location.x * MAP_SIZE_Y + location.y] = 3;
            return true;
        }

        @Override
        public boolean IsActive() {
            return active;
        }

        @Override
        public void SetActive(boolean active){
            this.active = active;
        }
    }

    public class TycoonMentality implements Objective
    {
        private boolean active;
        Path path = null;
        Location destination = null;

        public TycoonMentality() {
            active = true;
        }

        @Override
        public boolean IsCompleted(UnitController uc){
            return uc.hasCraftable(Craftable.PICKAXE);
        }

        @Override
        public boolean Follow(UnitController uc)
        {
            if (!uc.canCraft(Craftable.PICKAXE))
                return false;

            int[] mats = TakeALook(uc);
            if (mats[Material.GOLD.ordinal()] > 0 || mats[Material.DIAMOND.ordinal()] > 0)
            {
                MaterialInfo[] mat_infos = SensedMaterials(uc);
                for (MaterialInfo info : mat_infos)
                {
                    if (info.getMaterial() == Material.GOLD || info.getMaterial() == Material.DIAMOND)
                    {
                        destination = info.getLocation();
                        break;
                    }
                }

                if (IsAccessible(uc, uc.getLocation(), destination, 6))
                {
                    uc.craft(Craftable.PICKAXE);
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean IsActive() {
            return active;
        }

        @Override
        public void SetActive(boolean active){
            this.active = active;
        }
    }

    public class DoubleGather implements Objective
    {
        private boolean active;
        private final Material mat1;
        private final int amount1;
        private final Material mat2;
        private final int amount2;

        private Material gathering = null;

        private Objective subobjective = null;

        public DoubleGather(Material mat1, int amount1, Material mat2, int amount2) {
            active = true;
            this.mat1 = mat1;
            this.amount1 = amount1;
            this.mat2 = mat2;
            this.amount2 = amount2;
        }

        @Override
        public boolean IsCompleted(UnitController uc)
        {
            return uc.getUnitInfo().getCarriedMaterials()[mat1.ordinal()] >= amount1 && uc.getUnitInfo().getCarriedMaterials()[mat2.ordinal()] >= amount2;
        }

        @Override
        public boolean Follow(UnitController uc)
        {
            int[] carried = uc.getUnitInfo().getCarriedMaterials();
            boolean need1 = carried[mat1.ordinal()] < amount1;
            boolean need2 = carried[mat2.ordinal()] < amount2;
            if (!need1 && !need2)
            {
                return false;
            }

            if (!need1 && gathering == mat1)
            {
                gathering = null;
                subobjective = null;
            }
            if (!need2 && gathering == mat2)
            {
                gathering = null;
                subobjective = null;
            }

            int[] mats = TakeALook(uc);
            if (mats[mat1.ordinal()] > 0 && mats[mat2.ordinal()] > 0)
            {
                // Prioritize
                int missing1 = amount1-carried[mat1.ordinal()];
                int missing2 = amount2-carried[mat2.ordinal()];

                if (missing1 > missing2)
                {
                    if (gathering == null || !gathering.equals(mat1))
                    {
                        subobjective = new Gather(mat1, 100, false, false);
                        gathering = mat1;
                    }
                }
                else
                {
                    if (gathering == null || !gathering.equals(mat2))
                    {
                        subobjective = new Gather(mat2, 100, false, false);
                        gathering = mat2;
                    }
                }
            }
            else if (need1 && mats[mat1.ordinal()] > 0)
            {
                if (gathering == null || !gathering.equals(mat1))
                {
                    subobjective = new Gather(mat1, 100, false, false);
                    gathering = mat1;
                }
            }
            else if (need2 && mats[mat2.ordinal()] > 0)
            {
                if (gathering == null || !gathering.equals(mat2))
                {
                    subobjective = new Gather(mat2, 100, false, false);
                    gathering = mat2;
                }
            }

            if (subobjective != null)
                return subobjective.Follow(uc);

            return false;
        }

        @Override
        public boolean IsActive() {
            return active;
        }

        @Override
        public void SetActive(boolean active){
            this.active = active;
        }
    }

    public class NewWorldPlayerMentality implements Objective
    {
        private boolean active;
        Path path = null;
        Location destination = null;

        public NewWorldPlayerMentality() {
            active = true;
        }

        @Override
        public boolean IsCompleted(UnitController uc){
            return uc.hasCraftable(Craftable.AXE);
        }

        @Override
        public boolean Follow(UnitController uc)
        {
            if (!uc.canCraft(Craftable.AXE))
                return false;

            int[] mats = TakeALook(uc);
            if (mats[Material.STRING.ordinal()] > 0)
            {
                if (!uc.hasCraftable(Craftable.AXE))
                {
                    // Where is that material?
                    MaterialInfo[] mat_infos = SensedMaterials(uc);
                    for (MaterialInfo info : mat_infos)
                    {
                        if (info.getMaterial() == Material.STRING)
                        {
                            destination = info.getLocation();
                            break;
                        }
                    }

                    if (IsAccessible(uc, uc.getLocation(), destination, 7))
                    {
                        uc.craft(Craftable.AXE);
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public boolean IsActive() {
            return active;
        }

        @Override
        public void SetActive(boolean active){
            this.active = active;
        }
    }

    public class SingleGather implements Objective
    {
        private boolean active;
        private final Material mat;
        private final int amount;

        private Objective subobjective = null;

        public SingleGather(Material mat, int amount, boolean always, boolean only_with_tool) {
            active = true;
            this.mat = mat;
            this.amount = amount;
            subobjective = new Gather(mat, 100, always, only_with_tool);
        }

        @Override
        public boolean IsCompleted(UnitController uc)
        {
            return uc.getUnitInfo().getCarriedMaterials()[mat.ordinal()] >= amount;
        }

        @Override
        public boolean Follow(UnitController uc)
        {
            int[] carried = uc.getUnitInfo().getCarriedMaterials();
            boolean need = carried[mat.ordinal()] < amount;
            if (!need)
                return false;

            return subobjective.Follow(uc);
        }

        @Override
        public boolean IsActive() {
            return active;
        }

        @Override
        public void SetActive(boolean active){
            this.active = active;
        }
    }

    public class GetThatGrass implements Objective
    {
        private boolean active;

        private Objective subobjective = null;

        public GetThatGrass() {
            active = true;
        }

        @Override
        public boolean IsCompleted(UnitController uc)
        {
            return false;
        }

        @Override
        public boolean Follow(UnitController uc)
        {
            if (uc.canAct())
            {
                Material mat = uc.senseMaterialAtLocation(uc.getLocation());
                if (mat == Material.GRASS)
                {
                    if (uc.hasCraftable(Craftable.SHOVEL))
                        uc.useCraftable(Craftable.SHOVEL, uc.getLocation());
                    else
                        uc.gather();
                }
            }
            return false;
        }

        @Override
        public boolean IsActive() {
            return active;
        }

        @Override
        public void SetActive(boolean active){
            this.active = active;
        }
    }

    public class AlternateObjectives implements Objective
    {
        private boolean active;
        private boolean current_a = true;
        private final int maintain_a;
        private final int maintain_b;
        private int counter_a = 0;
        private int counter_b = 0;

        private final Objective a;
        private final Objective b;

        public AlternateObjectives(Objective a, Objective b, int maintain_a, int maintain_b) {
            active = true;
            this.maintain_a = maintain_a;
            this.maintain_b = maintain_b;
            this.a = a;
            this.b = b;
        }

        @Override
        public boolean IsCompleted(UnitController uc)
        {
            return false;
        }

        @Override
        public boolean Follow(UnitController uc)
        {
            boolean can_act = uc.canAct();
            boolean can_move = uc.canMove();

            if (current_a)
                a.Follow(uc);
            else
                b.Follow(uc);

            if ((can_act && !uc.canAct()) || (can_move && !uc.canMove()))
            {
                if (current_a)
                {
                    if (++counter_a >= maintain_a)
                    {
                        counter_a = 0;
                        current_a = false;
                    }
                }
                else
                {
                    if (++counter_b >= maintain_b)
                    {
                        counter_b = 0;
                        current_a = true;
                    }
                }
            }
            return true;
        }

        @Override
        public boolean IsActive() {
            return active;
        }

        @Override
        public void SetActive(boolean active){
            this.active = active;
        }
    }

    public class CheckMine implements Objective
    {
        private boolean active;
        private Location last_check = null;

        public CheckMine() {
            active = true;
        }

        @Override
        public boolean IsCompleted(UnitController uc)
        {
            return false;
        }

        @Override
        public boolean Follow(UnitController uc)
        {
            if (last_check != null && last_check.distanceSquared(uc.getLocation()) < 16)
                return false;

            last_check = uc.getLocation();

            int[] mats = TakeALook(uc);
            int copper = mats[Material.COPPER.ordinal()];
            int iron = mats[Material.IRON.ordinal()];
            int gold = mats[Material.GOLD.ordinal()];
            int diamond = mats[Material.DIAMOND.ordinal()];
            int dg = diamond+gold;
            int dgi = dg+iron;
            int dgic = dgi+copper;

            if (diamond > 1 ||
                dg > 2 ||
                dgi > 3 ||
                dgic > 5)
            {
                UpdateClosestMine(uc, uc.getLocation());
            }

            // TODO: broadcast this information (or add to a broadcast queue)
            return false;
        }

        @Override
        public boolean IsActive() {
            return active;
        }

        @Override
        public void SetActive(boolean active){
            this.active = active;
        }
    }

    public class TycoonBehaviour implements Objective
    {
        private boolean active;
        private final Objective obj_if_shovel;
        private final Objective obj_if_no_shovel;
        private Objective go_to_mine;
        private final Objective obj_mine_if_shovel;
        private final Objective obj_mine_if_no_shovel;
        private Location current_mine;
        
        

        public TycoonBehaviour() {
            active = true;
            obj_if_shovel = new AlternateObjectives(new Gather(Material.GRASS, 1, true, false), new Explore(), 18, 2);
            obj_if_no_shovel = new Explore();
            obj_mine_if_shovel = new Gather(Material.GRASS, 1, true, false);
            obj_mine_if_no_shovel = new SingleGather(Material.STONE, 100, true, false);
        }

        @Override
        public boolean IsCompleted(UnitController uc)
        {
            return false;
        }

        private boolean IfNoMine(UnitController uc)
        {
            if (uc.hasCraftable(Craftable.SHOVEL))
                obj_if_shovel.Follow(uc);
            else
                obj_if_no_shovel.Follow(uc);
            
            return true;
        }

        @Override
        public boolean Follow(UnitController uc)
        {
            if (CLOSEST_MINE == null)
            {
                return IfNoMine(uc);
            }
            else
            {
                if (!CLOSEST_MINE.equals(current_mine))
                {
                    current_mine = CLOSEST_MINE;
                    go_to_mine = null;
                }
                
                if (uc.getLocation().distanceSquared(current_mine) > 10)
                {
                    if (go_to_mine == null)
                    {
                        go_to_mine = new GoToLocation(current_mine, false, 4);
                        go_to_mine.Follow(uc);
                    }
                    else
                    {
                        if (!go_to_mine.Follow(uc))
                            return IfNoMine(uc);
                    }
                    return true;
                }
            }

            if (uc.hasCraftable(Craftable.SHOVEL))
                return obj_mine_if_shovel.Follow(uc);

            return obj_mine_if_no_shovel.Follow(uc);
        }

        @Override
        public boolean IsActive() {
            return active;
        }

        @Override
        public void SetActive(boolean active){
            this.active = active;
        }
    }

    public class PlaceTurret implements Objective
    {
        private boolean active;
        private Objective subobjective = null;

        public PlaceTurret() {
            active = true;
        }

        @Override
        public boolean IsCompleted(UnitController uc)
        {
            return false;
        }

        private boolean OnSubobjectiveCompleted(UnitController uc)
        {
            // Place turret wherever possible
            if (!uc.canAct())
                return true;

            subobjective = null;

            Location option = uc.getLocation();
            if (uc.canUseCraftable(Craftable.TURRET_BLUEPRINT, option))
            {
                uc.useCraftable(Craftable.TURRET_BLUEPRINT, option);
                return true;
            }
            option = uc.getLocation().add(Direction.NORTH);
            if (uc.canUseCraftable(Craftable.TURRET_BLUEPRINT, option))
            {
                uc.useCraftable(Craftable.TURRET_BLUEPRINT, option);
                return true;
            }
            option = uc.getLocation().add(Direction.SOUTH);
            if (uc.canUseCraftable(Craftable.TURRET_BLUEPRINT, option))
            {
                uc.useCraftable(Craftable.TURRET_BLUEPRINT, option);
                return true;
            }
            option = uc.getLocation().add(Direction.WEST);
            if (uc.canUseCraftable(Craftable.TURRET_BLUEPRINT, option))
            {
                uc.useCraftable(Craftable.TURRET_BLUEPRINT, option);
                return true;
            }
            option = uc.getLocation().add(Direction.EAST);
            if (uc.canUseCraftable(Craftable.TURRET_BLUEPRINT, option))
            {
                uc.useCraftable(Craftable.TURRET_BLUEPRINT, option);
                return true;
            }
            option = uc.getLocation().add(Direction.NORTHEAST);
            if (uc.canUseCraftable(Craftable.TURRET_BLUEPRINT, option))
            {
                uc.useCraftable(Craftable.TURRET_BLUEPRINT, option);
                return true;
            }
            option = uc.getLocation().add(Direction.NORTHWEST);
            if (uc.canUseCraftable(Craftable.TURRET_BLUEPRINT, option))
            {
                uc.useCraftable(Craftable.TURRET_BLUEPRINT, option);
                return true;
            }
            option = uc.getLocation().add(Direction.SOUTHEAST);
            if (uc.canUseCraftable(Craftable.TURRET_BLUEPRINT, option))
            {
                uc.useCraftable(Craftable.TURRET_BLUEPRINT, option);
                return true;
            }
            option = uc.getLocation().add(Direction.SOUTHWEST);
            if (uc.canUseCraftable(Craftable.TURRET_BLUEPRINT, option))
            {
                uc.useCraftable(Craftable.TURRET_BLUEPRINT, option);
                return true;
            }

            return false;
        }

        @Override
        public boolean Follow(UnitController uc)
        {
            if (!uc.hasCraftable(Craftable.TURRET_BLUEPRINT))
                return false;

            // Override objective here if anything that's closer is seen
            StructureInfo[] ally_stuff = uc.senseStructures(GameConstants.UNIT_VISION_RANGE, uc.getTeam(), null);
            if (ally_stuff.length > 0)
            {
                if (ally_stuff[0].getLocation().distanceSquared(uc.getLocation()) <= 2)
                    return OnSubobjectiveCompleted(uc);
                else if (uc.canMove() && IsAccessible(uc, uc.getLocation(), ally_stuff[0].getLocation(), 4))
                {
                    uc.move(PathfindAccessible4Steps(uc, ally_stuff[0].getLocation()));
                    if (ally_stuff[0].getLocation().distanceSquared(uc.getLocation()) <= 2)
                        return OnSubobjectiveCompleted(uc);
                }
            }

            if (subobjective != null)
            {
                // Follow
                if (!subobjective.Follow(uc))
                    subobjective = null;
                else if (subobjective.IsCompleted(uc))
                    return OnSubobjectiveCompleted(uc);
                else
                    return true;
            }

            // Set objective
            // Get closest accessible bed/beacon location
            BedInfo[] beds = uc.getAllBedsInfo();
            Location closest = null;
            int closest_dist = 30000;
            for (BedInfo bed : beds)
            {
                int distance = bed.getLocation().distanceSquared(uc.getLocation());
                if (distance < closest_dist && IsAccessible(uc, uc.getLocation(), bed.getLocation(), 20))
                {
                    closest = bed.getLocation();
                    closest_dist = distance;
                }
            }
            if (closest != null)
            {
                subobjective = new GoToLocation(closest, false, 1);
                if (!subobjective.Follow(uc))
                    subobjective = null;
                else if (subobjective.IsCompleted(uc))
                     return OnSubobjectiveCompleted(uc);
                else
                    return true;
            }

            return false;
        }

        @Override
        public boolean IsActive() {
            return active;
        }

        @Override
        public void SetActive(boolean active){
            this.active = active;
        }
    }

    // Version upgrade condition
    public interface Condition{
        public boolean Check(UnitController uc);
        public int NewVersion();
    }

    public class HasCraftable implements Condition
    {
        private final Craftable craftable;
        private final int new_version;
        private final boolean not_have;

        public HasCraftable(Craftable craftable, int new_version)
        {
            this(craftable, new_version, false);
        }

        public HasCraftable(Craftable craftable, int new_version, boolean not_have)
        {
            this.craftable = craftable;
            this.new_version = new_version;
            this.not_have = not_have;
        }

        @Override
        public boolean Check(UnitController uc)
        {
            if (!not_have)
                return uc.hasCraftable(craftable);
            else
                return !uc.hasCraftable(craftable);
        }

        @Override
        public int NewVersion() { return new_version; }
    }

    public class HasMaterial implements Condition
    {
        private final Material material;
        private final int amount;
        private final int new_version;

        public HasMaterial(Material material, int amount, int new_version)
        {
            this.material = material;
            this.amount = amount;
            this.new_version = new_version;
        }

        @Override
        public boolean Check(UnitController uc)
        {
            return uc.getUnitInfo().getCarriedMaterials()[material.ordinal()] >= amount;
        }

        @Override
        public int NewVersion() { return new_version; }
    }

    public class DoubleCondition implements Condition
    {
        private final Condition a;
        private final Condition b;
        private final int new_version;

        DoubleCondition(Condition a, Condition b, int new_version)
        {
            this.a = a;
            this.b = b;
            this.new_version = new_version;
        }

        @Override
        public boolean Check(UnitController uc)
        {
            return a.Check(uc) && b.Check(uc);
        }

        @Override
        public int NewVersion() { return new_version; }
    }

    public class OrCondition implements Condition
    {
        private final Condition a;
        private final Condition b;
        private int new_version;

        OrCondition(Condition a, Condition b)
        {
            this.a = a;
            this.b = b;
        }

        @Override
        public boolean Check(UnitController uc)
        {
            boolean ac = a.Check(uc);
            boolean bc = b.Check(uc);

            if (ac)
                new_version = a.NewVersion();
            else if (bc)
                new_version = b.NewVersion();

            return ac || bc;
        }

        @Override
        public int NewVersion() { return new_version; }
    }

    public class AliveForRounds implements Condition
    {
        private final int new_version;
        private final int rounds;

        AliveForRounds(int rounds, int new_version)
        {
            this.rounds = rounds;
            this.new_version = new_version;
        }

        @Override
        public boolean Check(UnitController uc)
        {
            return alive_rounds >= rounds;
        }

        @Override
        public int NewVersion() { return new_version; }
    }

    public class HasPlaced implements Condition
    {
        private final Craftable craftable;
        private boolean has = false;
        private final int new_version;

        public HasPlaced(Craftable craftable, int new_version)
        {
            this.craftable = craftable;
            this.new_version = new_version;
        }

        @Override
        public boolean Check(UnitController uc)
        {
            if (uc.hasCraftable(craftable))
                has = true;

            return has && !uc.hasCraftable(craftable);
        }

        @Override
        public int NewVersion() { return new_version; }
    }

    public class HasExplored implements Condition
    {
        private final int new_version;

        public HasExplored(int new_version)
        {
            this.new_version = new_version;
        }

        @Override
        public boolean Check(UnitController uc)
        {
            return !EXPLORING;
        }

        @Override
        public int NewVersion() { return new_version; }
    }

    // Members
    int MAP_SIZE_X;
    int MAP_SIZE_Y;

    // 3 means known material, 2 means void, 1 means water, 0 means unknown
    int[] OBSTACLES;
    Direction[] all_directions; // = {Direction.NORTHEAST, Direction.NORTHWEST, Direction.SOUTHEAST, Direction.SOUTHWEST, Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH};
    int[] ALL_STEPS = { 10, 14, 10, 14, 10, 14, 10, 14};
    Direction[] DIRECTION_OPPOSITES = {Direction.SOUTH, Direction.SOUTHEAST, Direction.EAST, Direction.NORTHEAST, Direction.NORTH, Direction.NORTHWEST, Direction.WEST, Direction.SOUTHWEST};
    Objective[] objectives;
    Condition condition = null;
    Team enemy_team;
    int MAX_EXPLORE_STEPS = 5;
    MaterialInfo[] sensed_materials = null;
    MaterialInfo[] sensed_useful = null;
    UnitInfo[] sensed_units = null;
    Craftable best_weapon = null;
    Craftable best_combat_weapon = null;
    Boolean HAS_VOIDS = null;
    int[] VISIBLE_MATERIALS = null;
    int alive_rounds = 0;
    boolean[] MARKED_BIOME;
    FastLocSet ACCESSIBILITY = new FastLocSet();
    boolean EXPLORING = true;
    boolean HAS_SEEN_WATER = false;
    Location CLOSEST_MINE = null;
    Location DEFENSELESS_ENEMY = null;

    // (boolean)WALLS[y+1]&(2<<x) means "WALL at x,y?"
    long[] WALLS;
    long[] ACCESSIBLE_MAP;
    int ACCESSIBLE_MAP_STEPS;

    int VERSION = 1; // Only change if you know what you're doing
    boolean PRINTS = false;

    public void run(UnitController uc) {
        try {
            // Code to be executed only at the beginning of the unit's lifespan
            all_directions = new Direction[8];
            int[] p1 = RandomPermutation(uc);
            for (int i = 0; i < p1.length; ++i)
                all_directions[i] = Direction.values()[p1[i]*2+1];

            int[] p2 = RandomPermutation(uc);
            for (int i = 0; i < p2.length; ++i)
                all_directions[i+4] = Direction.values()[p2[i]*2];

            enemy_team = uc.getTeam() == Team.A ? Team.B : Team.A;

            MAP_SIZE_Y = uc.getMapHeight();
            MAP_SIZE_X = uc.getMapWidth();

            WALLS = new long[MAP_SIZE_Y+2];
            // Everything is a wall until proven otherwise
            Arrays.fill(WALLS, 0xFFFFFFFFFFFFFFFFL);
            OBSTACLES = new int[MAP_SIZE_X*MAP_SIZE_Y];
            MARKED_BIOME = new boolean[MAP_SIZE_X*MAP_SIZE_Y];

            while (true) {
                // Code to be executed every round
                sensed_materials = null;
                sensed_useful = null;
                sensed_units = null;
                best_weapon = null;
                best_combat_weapon = null;
                HAS_VOIDS = null;
                VISIBLE_MATERIALS = null;
                ACCESSIBLE_MAP = null;
                ACCESSIBLE_MAP_STEPS = 0;
                DEFENSELESS_ENEMY = null;

                // If not on the map, find an empty bed and spawn there
                while (!uc.isOnMap()) {
                    boolean spawned = false;
                    BedInfo[] beds = uc.getAllBedsInfo();
                    for (BedInfo bed : beds) {
                        if (uc.canSpawn(bed.getLocation()))
                        {
                            uc.spawn(bed.getLocation());
                            alive_rounds = 0;
                            spawned = true;
                            VERSION = 1;

                            Peleriux(uc);
                        }
                    }

                    while (uc.getPercentageOfEnergyLeft() > 0.1)
                    {
                        BroadcastInfo bc_info = uc.pollBroadcast();
                        if (bc_info == null)
                            break;

                        ProcessMessage(uc, bc_info.getMessage());
                    }

                    if (!spawned)
                        uc.yield();
                }

                ++alive_rounds;

                boolean followed_one = false;

                if (condition != null && condition.Check(uc))
                {
                    VERSION = condition.NewVersion();
                    condition = null;
                    Peleriux(uc);
                }

                for (Objective objective : objectives) {
                    //uc.println("Current objective: " + objective + " energy: " + uc.getPercentageOfEnergyLeft());
                    if (!objective.IsActive() || objective.IsCompleted(uc))
                        continue;

                    if (objective.Follow(uc))
                    {
                        if (PRINTS)
                            uc.println("Followed " + objective + " (version " + VERSION + ")");

                        followed_one = true;
                        break;
                    }
                }

                if (!followed_one)
                    Peleriux(uc);

                while (uc.getPercentageOfEnergyLeft() > 0.1)
                {
                    BroadcastInfo bc_info = uc.pollBroadcast();
                    if (bc_info == null)
                        break;

                    ProcessMessage(uc, bc_info.getMessage());
                }

                // Craft heaviest possible object
                if (uc.getRound() == GameConstants.MAX_ROUNDS)
                {
                    if (uc.canAct())
                    {
                        if (uc.canCraft(Craftable.BOW))
                            uc.craft(Craftable.BOW);
                        else if (uc.canCraft(Craftable.SHOVEL))
                            uc.craft(Craftable.SHOVEL);
                        else if (uc.canCraft(Craftable.SWORD))
                            uc.craft(Craftable.SWORD);
                        else if (uc.canCraft(Craftable.ARMOR))
                            uc.craft(Craftable.ARMOR);
                    }
                }
                uc.yield(); // End of turn
            }

        } catch (Exception e) {
            uc.println("Exception: " + e.toString());
        }
    }
}