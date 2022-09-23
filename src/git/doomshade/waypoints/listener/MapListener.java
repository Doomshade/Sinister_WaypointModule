package git.doomshade.waypoints.listener;

import git.doomshade.waypoints.cursor.NPCQuesterCursor;
import io.github.bananapuncher714.cartographer.core.api.events.minimap.MinimapLoadEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class MapListener implements Listener {

    @EventHandler
    public void onLoad(MinimapLoadEvent e) {
        NPCQuesterCursor provider = new NPCQuesterCursor();
        e.getMinimap().registerProvider(provider);
    }
}
