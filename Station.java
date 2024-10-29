import java.util.*;

class Station {
    String name;
    Map<Station, Integer> connections;

    public Station(String name) {
        this.name = name;
        this.connections = new HashMap<>();
    }

    @Override
    public String toString() {
        return name;
    }
}
