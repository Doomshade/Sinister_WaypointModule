package git.doomshade.waypoints;

import git.doomshade.waypoints.cursor.NPCQuesterCursor;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Location;

public class NPCLocCache implements Comparable<NPCLocCache> {

    private final NPC npc;
    private final Location loc;
    private double distance = Double.MAX_VALUE;

    public NPCLocCache(NPC npc) {
        this.npc = npc;
        this.loc = NPCQuesterCursor.getLocation(npc);
    }

    /**
     * Attempts to update the min distance based on the other location
     *
     * @param other the other location
     */
    public void updateDistance(Location other) {
        this.distance = Math.min(distance(other), this.distance);
    }

    /**
     * @param other the other location
     *
     * @return distance from the other location
     */
    private double distance(Location other) {
        return NPCQuesterCursor.distance(this.loc, other);
    }

    @Override
    public String toString() {
        return String.format("(%s|%s|%s)", npc.getName(), loc.getBlock().getLocation().toVector(), distance);
    }

    @Override
    public int compareTo(NPCLocCache o) {
        return Double.compare(distance, o.distance);
    }
}
