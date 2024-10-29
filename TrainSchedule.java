import java.time.*;

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
