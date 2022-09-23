package git.doomshade.waypoints.hook;

import me.ragan262.questernpcs.citizens.QuesterTrait;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;

import java.util.Optional;
import java.util.function.BiFunction;

/**
 * Manager for external plugin hooks
 *
 * @author Doomshade
 * @version 1.0
 * @implNote NOT YET IMPLEMENTED
 */
public class HookManager {
    private static final HookManager instance = new HookManager();

    private HookManager() {
    }

    /**
     * @return the singleton
     */
    public static HookManager getInstance() {
        return instance;
    }

    /**
     * Re/loads the manager
     */
    public void load() {
        PluginManager pm = Bukkit.getPluginManager();

        for (Hook hook : Hook.values()) {
            hook.available = pm.isPluginEnabled(hook.plugin);
        }
    }

    public enum Hook {
        QUESTER("quester");

        private final String plugin;
        private boolean available = false;

        Hook(String plugin) {
            this.plugin = plugin;
        }

        public final <T, E extends IRequest> Optional<T> request(NPC npc, Player p, E request) {
            return available ? Optional.ofNullable((T) request.function().apply(npc, p)) : Optional.empty();
        }
    }

    /**
     * Class for external plugin requests
     *
     * @author Doomshade
     * @version 1.0
     */
    public static class Requests {

        /**
         * Quester plugin requests
         *
         * @author Doomshade
         * @version 1.0
         */
        public enum QuesterRequest implements IRequest {
            /**
             * Checks whether the NPC has a trait<br> Returns boolean
             *
             * @see Boolean
             */
            NPC_HAS_TRAIT((x, y) -> x.hasTrait(QuesterTrait.class));

            /**
             * Gets the quester trait<br> Returns optional of QuesterTrait
             *
             * @see Optional
             * @see QuesterTrait
             */
            /*NPC_GET_TRAIT((x, y) -> {
                boolean hasTrait = (boolean) NPC_HAS_TRAIT.function().apply(x, y);
                if (!hasTrait) {
                    return Optional.empty();
                }
                return Optional.of(x.getTrait(QuesterTrait.class));
            });*/

            private final BiFunction<NPC, Player, ?> function;

            QuesterRequest(BiFunction<NPC, Player, ?> function) {
                this.function = function;
            }

            @Override
            public BiFunction<NPC, Player, ?> function() {
                return function;
            }
        }
    }
}
