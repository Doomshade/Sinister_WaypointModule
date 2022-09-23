package git.doomshade.waypoints;

import git.doomshade.waypoints.cursor.NPCQuesterCursor;
import git.doomshade.waypoints.listener.MapListener;
import io.github.bananapuncher714.cartographer.core.Cartographer;
import io.github.bananapuncher714.cartographer.core.api.command.CommandBase;
import io.github.bananapuncher714.cartographer.core.api.command.CommandParameters;
import io.github.bananapuncher714.cartographer.core.api.command.SubCommand;
import io.github.bananapuncher714.cartographer.core.map.Minimap;
import io.github.bananapuncher714.cartographer.core.module.Module;
import io.github.bananapuncher714.cartographer.core.util.FileUtil;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.map.MapCursor;

import java.io.File;
import java.util.*;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The main module class
 *
 * @author Doomshade
 * @version 1.0
 */
public class WaypointModule extends Module {
    /** Pattern: 'world-name;waypoint-name;x z' */
    private static final Pattern TOWN_PATTERN = Pattern.compile("(\\w+);(\\w+);(-?\\d+) (-?\\d+)");
    private static final List<Town> TOWNS = new ArrayList<>();
    private static WaypointModule instance;
    private static int maxRange = 100;
    private File configFile;

    /**
     * @return the singleton
     */
    public static WaypointModule getInstance() {
        return instance;
    }

    /**
     * @return all registered towns
     */
    public static List<Town> getTowns() {
        return Collections.unmodifiableList(TOWNS);
    }

    /**
     * @return max range at which the NPC cursor should render
     */
    public static int getMaxRange() {
        return maxRange;
    }

    @Override
    public void onEnable() {
        instance = this;
        configFile = new File(getDataFolder(), "config.yml");
        FileUtil.saveToFile(getResource("config.yml"), configFile, false);
        loadConfig();
        registerCommands();
        registerListener(new MapListener());
        runTask(this::checkNpcLocations, 600L);
    }

    private void registerCommands() {
        CommandBase base = new CommandBase("siniwp")
                //.setPermission("waypoint.*")
                .setDescription("Main waypoint cmd");
        SubCommand cmd = new SubCommand("siniwp")
                .add(new SubCommand("reload").defaultTo(this::reloadCommand))
                .add(new SubCommand("set").defaultTo(this::setCommand))
                .defaultTo(this::showInfo);
        base.setSubCommand(cmd);

        registerCommand(base.build());
    }

    private void showInfo(CommandSender sender, String[] args, CommandParameters parameters) {
        sender.sendMessage(getCommand("reload", "", "Reloads the module"));
        sender.sendMessage(getCommand("set", "<quest_id>", "Sets the waypoint of a quest"));
    }

    private String getCommand(String cmd, String args, String desc) {
        return String.format("%s/waypoint %s %s - %s%s", ChatColor.GOLD, cmd, args, ChatColor.DARK_AQUA, desc);
    }

    private void reloadCommand(CommandSender sender, String[] args, CommandParameters parameters) {
        loadConfig();
    }

    private void setCommand(CommandSender sender, String[] args, CommandParameters parameters) {
        sender.sendMessage("Test");
    }

    public void loadConfig() {
        getLogger().log(Level.INFO, "Loading config...");
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        loadTowns(config);
        loadCursors(config);
        loadSettings(config);
    }

    private void loadSettings(FileConfiguration config) {
        maxRange = config.getInt("max-check-range", maxRange);
    }

    private void loadCursors(FileConfiguration config) {
        for (NPCQuesterCursor.Cursor cursor : NPCQuesterCursor.Cursor.values()) {
            final String s = config.getString(cursor.configName, "WHITE_POINTER");
            try {
                cursor.cursorType = MapCursor.Type.valueOf(s);
            } catch (IllegalArgumentException e) {
                getLogger().log(Level.WARNING, "Invalid cursor type " + s);
            }
        }
    }

    private void loadTowns(FileConfiguration config) {
        TOWNS.clear();
        List<String> strs = new ArrayList<>(config.getStringList("towns"));

        for (String s : strs) {
            Matcher m = TOWN_PATTERN.matcher(s);
            if (!m.find()) {
                getLogger().log(Level.WARNING, String.format("Invalid town format! (%s)", s));
                continue;
            }
            final String world = m.group(1);
            final String name = m.group(2);
            final int x, z;
            try {
                x = Integer.parseInt(m.group(3));
                z = Integer.parseInt(m.group(4));
            } catch (NumberFormatException e) {
                getLogger().log(Level.WARNING, String.format("Invalid town format! (%s)", s));
                continue;
            }
            final World w = Bukkit.getWorld(world);
            if (w == null) {
                getLogger().log(Level.WARNING, String.format("World %s does not exist!", world));
            }
            Location location = new Location(w, x, 0, z);
            TOWNS.add(new Town(location, name));
        }
    }

    private void checkNpcLocations() {
        World w = Bukkit.getWorld("Dragoncraft");
        World bossland = Bukkit.getWorld("Bossland");
        World thodos = Bukkit.getWorld("Sinister");
        checkLocationsForWorld(w);
        checkLocationsForWorld(bossland);
        checkLocationsForWorld(thodos);
    }

    private void checkLocationsForWorld(World w) {
        List<Location> approxLocations = Arrays.asList(
                new Location(w, -40, 60, 530),
                new Location(w, -480, 60, 400),
                new Location(w, 20, 60, 510),
                new Location(w, 15, 60, 430),
                new Location(w, -45, 60, 365)
        );

        final PriorityQueue<NPCLocCache> npcLocs = new PriorityQueue<>();

        for (NPC npc : CitizensAPI.getNPCRegistry()) {
            final NPCLocCache nlc = new NPCLocCache(npc);
            for (Location loc : approxLocations) {
                nlc.updateDistance(loc);
            }
            //System.out.println("Adding NPC Loc cache " + nlc);
            npcLocs.offer(nlc);
        }

        System.out.println(w.getName());
        for (int i = 0; i < 20; i++) {
            System.out.println(npcLocs.poll());
        }
        System.out.println();
    }
}
