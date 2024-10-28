// Main class to launch the application
import java.awt.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.*;

// Main GUI Class
class MetroBookingGUI extends JFrame {
    private Map<String, Station> stations;
    private JComboBox<String> fromStation;
    private JComboBox<String> toStation;
    private JSpinner timeSpinner;
    private JTextArea resultArea;
    private List<TrainSchedule> schedules;
    private JPanel routesPanel;
    private ButtonGroup routeButtonGroup;
    private JButton confirmButton;
    private List<List<TrainSchedule>> currentRoutes;

    public MetroBookingGUI() {
        initializeStations();
        initializeGUI();
        generateSchedules();
    }

    private void initializeStations() {
        stations = new HashMap<>();
        
        // Create stations
        String[] stationNames = {"A", "B", "C", "D", "E", "F"};
        for (String name : stationNames) {
            stations.put(name, new Station(name));
        }

        // Add connections with distances
        addConnection("A", "B", 10);
        addConnection("A", "C", 22);
        addConnection("A", "E", 8);
        addConnection("B", "C", 15);
        addConnection("B", "D", 9);
        addConnection("B", "F", 7);
        addConnection("C", "D", 9);
        addConnection("D", "E", 5);
        addConnection("D", "F", 12);
        addConnection("E", "F", 16);
    }

    private void addConnection(String from, String to, int distance) {
        stations.get(from).connections.put(stations.get(to), distance);
        stations.get(to).connections.put(stations.get(from), distance);
    }

    private void initializeGUI() {
        setTitle("Metro Booking System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
    
        // Create input panel
        JPanel inputPanel = new JPanel(new GridLayout(4, 2, 5, 5));
        
        fromStation = new JComboBox<>(stations.keySet().toArray(new String[0]));
        toStation = new JComboBox<>(stations.keySet().toArray(new String[0]));
    
        SpinnerDateModel timeModel = new SpinnerDateModel();
        timeSpinner = new JSpinner(timeModel);
        JSpinner.DateEditor timeEditor = new JSpinner.DateEditor(timeSpinner, "HH:mm");
        timeSpinner.setEditor(timeEditor);
    
        inputPanel.add(new JLabel("From Station:"));
        inputPanel.add(fromStation);
        inputPanel.add(new JLabel("To Station:"));
        inputPanel.add(toStation);
        inputPanel.add(new JLabel("Departure Time:"));
        inputPanel.add(timeSpinner);
    
        JButton searchButton = new JButton("Search Trains");
        searchButton.addActionListener(e -> searchTrains());
        inputPanel.add(new JPanel()); // Empty panel for grid alignment
        inputPanel.add(searchButton);
    
        // Create routes panel for radio buttons and route details
        routesPanel = new JPanel();
        routesPanel.setBorder(BorderFactory.createTitledBorder("Available Routes"));
    
        // Create confirm button
        confirmButton = new JButton("Confirm Booking");
        confirmButton.addActionListener(e -> confirmBooking());
        confirmButton.setEnabled(false);
    
        // Create result area
        resultArea = new JTextArea(10, 40);
        resultArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(resultArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Journey Information"));
    
        // Layout all components
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        mainPanel.add(inputPanel, BorderLayout.NORTH);
        
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(new JScrollPane(routesPanel), BorderLayout.CENTER);
        centerPanel.add(scrollPane, BorderLayout.SOUTH);
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        
        mainPanel.add(confirmButton, BorderLayout.SOUTH);
    
        add(mainPanel);
    
        // Set initial time to current time
        timeSpinner.setValue(new Date());
        
        // Set frame properties
        setSize(600, 800);
        setLocationRelativeTo(null);
        setVisible(true);
    }
    
    private void generateSchedules() {
        schedules = new ArrayList<>();
        LocalTime startTime = LocalTime.of(6, 0);
        LocalTime endTime = LocalTime.of(20, 0);

        // Generate schedules for each connection
        for (Station from : stations.values()) {
            for (Map.Entry<Station, Integer> connection : from.connections.entrySet()) {
                Station to = connection.getKey();
                int distance = connection.getValue();
                
                LocalTime currentTime = startTime;
                while (currentTime.isBefore(endTime)) {
                    // Calculate travel time based on 30 km/h speed
                    int travelMinutes = (distance * 60) / 30;
                    LocalTime arrivalTime = currentTime.plusMinutes(travelMinutes);
                    
                    schedules.add(new TrainSchedule(currentTime, arrivalTime, from, to));
                    
                    // Next train after 10 minutes
                    currentTime = currentTime.plusMinutes(10);
                }
            }
        }
    }

    private void searchTrains() {
        String from = (String) fromStation.getSelectedItem();
        String to = (String) toStation.getSelectedItem();
        Date time = (Date) timeSpinner.getValue();
        LocalDateTime searchDateTime = LocalDateTime.ofInstant(time.toInstant(), ZoneId.systemDefault());
        LocalTime searchTime = searchDateTime.toLocalTime();

        // Find shortest path using Dijkstra's algorithm
        Map<Station, Integer> shortestDistances = findShortestPath(stations.get(from), stations.get(to));
        
        if (shortestDistances == null) {
            resultArea.setText("No route available between selected stations.");
            return;
        }

        // Find available trains
        List<List<TrainSchedule>> possibleRoutes = findTrainRoutes(
            stations.get(from), 
            stations.get(to), 
            searchTime, 
            shortestDistances
        );

        displayRoutes(possibleRoutes);
    }

    private Map<Station, Integer> findShortestPath(Station start, Station end) {
        Map<Station, Integer> distances = new HashMap<>();
        PriorityQueue<Station> queue = new PriorityQueue<>(
            Comparator.comparingInt(s -> distances.getOrDefault(s, Integer.MAX_VALUE))
        );
        
        distances.put(start, 0);
        queue.offer(start);

        while (!queue.isEmpty()) {
            Station current = queue.poll();
            
            if (current == end) {
                return distances;
            }

            for (Map.Entry<Station, Integer> neighbor : current.connections.entrySet()) {
                Station next = neighbor.getKey();
                int newDist = distances.get(current) + neighbor.getValue();
                
                if (newDist < distances.getOrDefault(next, Integer.MAX_VALUE)) {
                    distances.put(next, newDist);
                    queue.offer(next);
                }
            }
        }

        return null;
    }

    private List<List<TrainSchedule>> findTrainRoutes(
    Station start, 
    Station end, 
    LocalTime searchTime,
    Map<Station, Integer> shortestDistances
) {
    List<List<TrainSchedule>> routes = new ArrayList<>();
    
    // Find trains 10 minutes before and after the intended time
    LocalTime searchStartTime = searchTime.minusMinutes(10);
    LocalTime searchEndTime = searchTime.plusMinutes(10);
    
    // Find direct trains first
    List<TrainSchedule> directTrains = schedules.stream()
        .filter(s -> s.from == start && s.to == end)
        .filter(s -> !s.departureTime.isBefore(searchStartTime))
        .filter(s -> s.departureTime.isBefore(searchEndTime.plusHours(1)))
        .sorted(Comparator.comparing(s -> s.departureTime))
        .collect(Collectors.toList());

    if (!directTrains.isEmpty()) {
        for (TrainSchedule train : directTrains) {
            routes.add(Arrays.asList(train));
        }
    }

    // Find connecting trains with minimum 5-minute buffer
    for (Station intermediate : stations.values()) {
        if (intermediate != start && intermediate != end) {
            List<TrainSchedule> firstLeg = schedules.stream()
                .filter(s -> s.from == start && s.to == intermediate)
                .filter(s -> !s.departureTime.isBefore(searchStartTime))
                .filter(s -> s.departureTime.isBefore(searchEndTime.plusHours(1)))
                .sorted(Comparator.comparing(s -> s.departureTime))
                .collect(Collectors.toList());

            for (TrainSchedule first : firstLeg) {
                List<TrainSchedule> secondLeg = schedules.stream()
                    .filter(s -> s.from == intermediate && s.to == end)
                    .filter(s -> {
                        // Ensure minimum 5-minute buffer between trains
                        LocalTime earliestDeparture = first.arrivalTime.plusMinutes(5);
                        return s.departureTime.isAfter(earliestDeparture) &&
                               s.departureTime.isBefore(searchTime.plusHours(1));
                    })
                    .sorted(Comparator.comparing(s -> s.departureTime))
                    .collect(Collectors.toList());

                if (!secondLeg.isEmpty()) {
                    routes.add(Arrays.asList(first, secondLeg.get(0)));
                }
            }
        }
    }

    return routes;
}

private void displayRoutes(List<List<TrainSchedule>> routes) {
    currentRoutes = routes;
    if (routes.isEmpty()) {
        resultArea.setText("No trains available for the selected route and time.");
        routesPanel.removeAll();
        routesPanel.revalidate();
        routesPanel.repaint();
        confirmButton.setEnabled(false);
        return;
    }

    routesPanel.removeAll();
    routeButtonGroup = new ButtonGroup();
    routesPanel.setLayout(new BoxLayout(routesPanel, BoxLayout.Y_AXIS));

    StringBuilder result = new StringBuilder();
    result.append("Available Routes:\n");
    result.append("----------------\n\n");

    for (int i = 0; i < routes.size(); i++) {
        List<TrainSchedule> route = routes.get(i);
        
        // Create radio button for route selection
        JRadioButton routeButton = new JRadioButton("Select Route " + (i + 1));
        routeButton.setActionCommand(String.valueOf(i));
        routeButtonGroup.add(routeButton);
        routesPanel.add(routeButton);
        
        StringBuilder routeDetails = new StringBuilder();
        routeDetails.append("Route ").append(i + 1).append(":\n");
        
        for (int j = 0; j < route.size(); j++) {
            TrainSchedule schedule = route.get(j);
            routeDetails.append("Train ").append(j + 1).append(": ")
                      .append(schedule.from.name)
                      .append(" to ")
                      .append(schedule.to.name)
                      .append("\n  Departure: ")
                      .append(schedule.departureTime.format(DateTimeFormatter.ofPattern("HH:mm")))
                      .append("hrs\n  Arrival: ")
                      .append(schedule.arrivalTime.format(DateTimeFormatter.ofPattern("HH:mm")))
                      .append("hrs\n");
            
            if (j < route.size() - 1) {
                TrainSchedule nextSchedule = route.get(j + 1);
                long waitingTime = Duration.between(schedule.arrivalTime, nextSchedule.departureTime).toMinutes();
                routeDetails.append("  Connection wait time: ").append(waitingTime).append(" minutes\n");
            }
        }

        LocalTime startTime = route.get(0).departureTime;
        LocalTime endTime = route.get(route.size() - 1).arrivalTime;
        long totalMinutes = Duration.between(startTime, endTime).toMinutes();
        routeDetails.append("Total journey time = ").append(totalMinutes).append(" minutes\n\n");

        // Add route details in a bordered panel
        JTextArea routeArea = new JTextArea(routeDetails.toString());
        routeArea.setEditable(false);
        routeArea.setBackground(null);
        routeArea.setBorder(BorderFactory.createEmptyBorder(0, 20, 10, 0));
        routesPanel.add(routeArea);
    }

    routesPanel.add(Box.createVerticalStrut(10));
    
    result.append("Please note:\n");
    result.append("- All trains shown depart within 10 minutes before/after your requested time\n");
    result.append("- Each connection has a minimum 5-minute buffer time\n");
    result.append("- Only trains departing within the next hour are shown\n");
    result.append("\nPlease select a route and click 'Confirm Booking' to proceed.");

    resultArea.setText(result.toString());
    confirmButton.setEnabled(true);
    
    routesPanel.revalidate();
    routesPanel.repaint();
}

private void confirmBooking() {
    if (routeButtonGroup.getSelection() == null) {
        JOptionPane.showMessageDialog(this,
            "Please select a route first",
            "No Route Selected",
            JOptionPane.WARNING_MESSAGE);
        return;
    }

    int selectedRoute = Integer.parseInt(routeButtonGroup.getSelection().getActionCommand());
    List<TrainSchedule> selected = currentRoutes.get(selectedRoute);

    StringBuilder confirmation = new StringBuilder();
    confirmation.append("Booking Confirmed!\n\n");
    confirmation.append("Your Itinerary:\n");
    confirmation.append("---------------\n");

    for (int i = 0; i < selected.size(); i++) {
        TrainSchedule schedule = selected.get(i);
        confirmation.append("Train ").append(i + 1).append(":\n");
        confirmation.append("From: ").append(schedule.from.name)
                   .append(" at ").append(schedule.departureTime.format(DateTimeFormatter.ofPattern("HH:mm")))
                   .append("hrs\n");
        confirmation.append("To: ").append(schedule.to.name)
                   .append(" at ").append(schedule.arrivalTime.format(DateTimeFormatter.ofPattern("HH:mm")))
                   .append("hrs\n\n");
    }

    confirmation.append("Total journey time: ")
                .append(Duration.between(
                    selected.get(0).departureTime,
                    selected.get(selected.size() - 1).arrivalTime).toMinutes())
                .append(" minutes\n\n");
    confirmation.append("Please arrive at least 10 minutes before departure time.\n");
    confirmation.append("Have a pleasant journey!");

    // Show confirmation dialog
    JOptionPane.showMessageDialog(this,
        confirmation.toString(),
        "Booking Confirmation",
        JOptionPane.INFORMATION_MESSAGE);

    // Reset the form for new booking
    fromStation.setSelectedIndex(0);
    toStation.setSelectedIndex(0);
    timeSpinner.setValue(new Date());
    routeButtonGroup.clearSelection();
    resultArea.setText("");
    routesPanel.removeAll();
    routesPanel.revalidate();
    routesPanel.repaint();
    confirmButton.setEnabled(false);
}

}

