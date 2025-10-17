package DarkJaslo.fast;

import aic2025.user.*;

// Got this from https://github.com/chenyx512/battlecode25-gonewhalin/blob/main/src/bot1/fast/FastLocSet.java
// I didn't write this!

public class FastLocSet {
    public StringBuilder keys;
    public int size;

    public FastLocSet() {
        keys = new StringBuilder();
        size = 0;
    }

    public int size() {
        return size;
    }

    private String locToStr(Location loc) {
        return "^" + (char) (loc.x) + (char) (loc.y);
    }

    public void add(Location loc) {
        String key = "^" + (char) (loc.x) + (char) (loc.y);
        if (keys.indexOf(key) == -1) {
            keys.append(key);
            size++;
        }
    }

    public void add(int x, int y) {
        String key = "^" + (char) x + (char) y;
        if (keys.indexOf(key) == -1) {
            keys.append(key);
            size++;
        }
    }

    public void remove(Location loc) {
        String key = "^" + (char) (loc.x) + (char) (loc.y);
        // String key = locToStr(loc);
        int index;
        if ((index = keys.indexOf(key)) >= 0) {
            keys.delete(index, index + 3);
            size--;
        }
    }

    public void remove(int x, int y) {
        String key = "^" + (char) x + (char) y;
        int index;
        if ((index = keys.indexOf(key)) >= 0) {
            keys.delete(index, index + 3);
            size--;
        }
    }

    public boolean contains(Location loc) {
        // return keys.indexOf(locToStr(loc)) >= 0;
        return keys.indexOf("^" + (char) (loc.x) + (char) (loc.y)) >= 0;
    }

    public boolean contains(int x, int y) {
        return keys.indexOf("^" + (char) x + (char) y) >= 0;
    }

    public void clear() {
        size = 0;
        keys = new StringBuilder();
    }

    public void replace(String newSet) {
        keys.replace(0, keys.length(), newSet);
        size = newSet.length() / 3;
    }

    public void union(FastLocSet s) {
        for (int i = 1; i < s.keys.length(); i += 3) {
            add((int) s.keys.charAt(i), (int) s.keys.charAt(i + 1));
        }
    }

    public Location[] getKeys() {
        // note that this op is expensive
        Location[] locs = new Location[size];
        for (int i = 1; i < keys.length(); i += 3) {
            locs[i / 3] = new Location((int) keys.charAt(i), (int) keys.charAt(i + 1));
        }
        return locs;
    }

    public Location pop() {
        if (size == 0)
            return null;
        Location loc = new Location((int) keys.charAt(1), (int) keys.charAt(2));
        remove(loc);
        return loc;
    }
}