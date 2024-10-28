import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.time.*;
import java.time.format.*;
import java.util.*;

// Class to represent a Train Schedule
class TrainSchedule {
    LocalTime departureTime;
    LocalTime arrivalTime;
    Station from;
    Station to;

    public TrainSchedule(LocalTime departureTime, LocalTime arrivalTime, Station from, Station to) {
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.from = from;
        this.to = to;
    }
}