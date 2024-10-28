import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.time.*;
import java.time.format.*;
import java.util.*;

// Class to represent a Metro Station
class Station {
    String name;
    Map<Station, Integer> connections;

    public Station(String name) {
        this.name = name;
        this.connections = new HashMap<>();
    }
}