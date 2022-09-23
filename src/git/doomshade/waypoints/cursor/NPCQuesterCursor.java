package git.doomshade.waypoints.cursor;

import git.doomshade.waypoints.Town;
import git.doomshade.waypoints.WaypointModule;
import io.github.bananapuncher714.cartographer.core.Cartographer;
import io.github.bananapuncher714.cartographer.core.api.WorldCursor;
import io.github.bananapuncher714.cartographer.core.api.map.WorldCursorProvider;
import io.github.bananapuncher714.cartographer.core.map.MapViewer;
import io.github.bananapuncher714.cartographer.core.map.Minimap;
import io.github.bananapuncher714.cartographer.core.renderer.PlayerSetting;
import me.ragan262.quester.Quester;
import me.ragan262.quester.exceptions.QuesterException;
import me.ragan262.quester.holder.QuestHolder;
import me.ragan262.quester.holder.QuestHolderManager;
import me.ragan262.quester.profiles.PlayerProfile;
import me.ragan262.quester.profiles.ProfileManager;
import me.ragan262.quester.quests.Quest;
import me.ragan262.quester.quests.QuestManager;
import me.ragan262.questernpcs.citizens.QuesterTrait;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.map.MapCursor;
import org.bukkit.util.NumberConversions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class NPCQuesterCursor implements WorldCursorProvider {
    // BLUE = side q
    // GREEN = q
    // WHITE = npc
    // CROSS = town
    private static final QuestHolderManager qhm = Quester.getInstance().getHolderManager();
    private static final QuestManager qMan = Quester.getInstance().getQuestManager();
    private static final ProfileManager profMan = Quester.getInstance().getProfileManager();

    @Override
    public Collection<WorldCursor> getCursors(Player p, Minimap minimap, PlayerSetting setting) {
        final Collection<WorldCursor> cursors = new ArrayList<>();

        registerNpcCursors(p, setting, cursors);
        registerTownCursors(p, cursors);
        return cursors;
    }

    private void registerTownCursors(Player p, Collection<WorldCursor> cursors) {
        final MapViewer viewer = Cartographer.getInstance().getPlayerManager().getViewerFor(p.getUniqueId());
        final boolean showName = viewer.getSetting(MapViewer.SHOWNAME).isTrue();
        for (Town town : WaypointModule.getTowns()) {
            WorldCursor wc = new WorldCursor(
                    showName ? town.name : null,
                    town.location,
                    Cursor.TOWN.cursorType,
                    true);
            cursors.add(wc);
        }
    }

    private void registerNpcCursors(Player p, PlayerSetting s, Collection<WorldCursor> cursors) {
        final PlayerProfile prof = profMan.getProfile(p);

        final MapViewer viewer = Cartographer.getInstance().getPlayerManager().getViewerFor(p.getUniqueId());
        final boolean showName = viewer.getSetting(MapViewer.SHOWNAME).isTrue();
        final Location playerLocation = s.getLocation();
        final int maxDistance = WaypointModule.getMaxRange();

        for (NPC npc : CitizensAPI.getNPCRegistry()) {
            final Entity en = npc.getEntity();
            if (en == null) {
                continue;
            }

            // check if the npc has valid location
            final Location location = getLocation(npc);
            if (location == null) {
                WaypointModule.getInstance()
                        .getLogger()
                        .info(String.format("NPC %s has no location!", npc.getName()));
                continue;
            }

            // check if the NPC is in range
            int distance = (int) Math.round(distance(playerLocation, location));
            if (distance > maxDistance) {
                continue;
            }

            // check if the npc is a quest giver
            final Quest quest = getQuest(npc);
            if (quest == null) {
                continue;
            }

            // check if the player meets conditions for the quest
            final boolean conditionsMet;
            try {
                conditionsMet =
                        qMan.areConditionsMet(p, quest,
                                Quester.getInstance().getLanguageManager().getDefaultLang());
            } catch (QuesterException e) {
                e.printStackTrace();
                continue;
            }

            // check if the player has not yet completed the quest and has met the conditions
            if (!prof.isCompleted(quest.getName()) && conditionsMet) {
                final WorldCursor wc = new WorldCursor(
                        showName ? npc.getFullName() : null,
                        location,
                        Cursor.NPC.cursorType,
                        false);
                final String lastColors = ChatColor.getLastColors(npc.getFullName());
                final ChatColor color = lastColors.isEmpty() ? ChatColor.BLACK :
                        ChatColor.getByChar(lastColors.charAt(1));

                // side quest if the name is chat color aqua
                if (color == ChatColor.AQUA) {
                    wc.setType(Cursor.SIDE_QUEST.cursorType);
                } else {
                    wc.setType(Cursor.ACTIVE_QUEST.cursorType);
                }
                cursors.add(wc);
            }

        }
    }

    /**
     * @param npc the npc
     *
     * @return the first quest from the npc or null if there's none
     */
    private Quest getQuest(NPC npc) {
        if (!npc.hasTrait(QuesterTrait.class)) {
            return null;
        }
        final QuesterTrait trait = npc.getTrait(QuesterTrait.class);

        // check if the quest giver has some holder
        final QuestHolder holder = qhm.getHolder(trait.getHolderID());
        if (holder == null) {
            return null;
        }

        // check if the holder has any quests
        final List<Integer> quests = holder.getQuests();
        if (quests.isEmpty()) {
            return null;
        }
        return qMan.getQuest(quests.get(0));
    }

    /**
     * Gets the location of an NPC
     *
     * @param npc the npc
     *
     * @return if entity does not exist, {@code null}, if the location of the entity is not null then entity's location,
     * otherwise NPC's stored location
     */
    public static Location getLocation(NPC npc) {
        Entity en = npc.getEntity();
        if (en == null) {
            return null;
        }
        Location location = en.getLocation();
        if (location == null) {
            location = npc.getStoredLocation();
        }
        return location;
    }

    /**
     * Calculates the distance on the X/Z axis between two locations
     *
     * @param a the first location
     * @param b the second location
     *
     * @return {@code Double.MAX_VALUE} if a or b is null OR the locations are not in the same world, the actual
     * distance otherwise
     */
    public static double distance(Location a, Location b) {
        if (a == null || b == null || a.getWorld() != b.getWorld() ||
                !a.getWorld().equals(b.getWorld())) {
            return Double.MAX_VALUE;
        }
        return Math.sqrt(NumberConversions.square(a.getX() - b.getX())
                + NumberConversions.square(a.getZ() - b.getZ()));
    }

    public enum Cursor {
        ACTIVE_QUEST("active-quest"),
        SIDE_QUEST("side-quest"),
        NPC("npc"),
        TOWN("town");

        public final String configName;
        public MapCursor.Type cursorType = MapCursor.Type.WHITE_CROSS;

        Cursor(String configName) {
            this.configName = configName;
        }
    }
}
