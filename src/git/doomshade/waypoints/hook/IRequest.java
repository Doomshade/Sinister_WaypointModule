package git.doomshade.waypoints.hook;

import net.citizensnpcs.api.npc.NPC;
import org.bukkit.entity.Player;

import java.util.function.BiFunction;

/**
 * A request function
 *
 * @author Doomshade
 * @version 1.0
 */
@FunctionalInterface
public interface IRequest {

    /**
     * @return A request function which takes NPC and a Player as a parameter and returns something
     */
    BiFunction<NPC, Player, ?> function();
}
