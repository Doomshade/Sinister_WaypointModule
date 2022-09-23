package git.doomshade.waypoints;

import org.bukkit.Location;

import java.util.Objects;

public class Town {
    public final Location location;
    public final String name;

    /**
     * @param location the town location
     * @param name     the town name
     */
    public Town(Location location, String name) {
        this.location = location;
        this.name = name;
    }

    @Override
    public int hashCode() {
        return Objects.hash(location, name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Town town = (Town) o;
        return location.equals(town.location) &&
                name.equals(town.name);
    }
}
