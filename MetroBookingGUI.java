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
    private JPanel selectionPanel;
    private JButton nextButton;
    private JButton startOverButton;

    private List<Station> shortestPath;
    private int currentSegmentIndex = 0;
    private List<TrainSchedule> selectedTrains = new ArrayList<>();
    private LocalTime currentTime;

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
    
        JButton searchButton = new JButton("Start Journey Planning");
        searchButton.addActionListener(e -> startJourneyPlanning());
        inputPanel.add(new JPanel()); // Empty panel for grid alignment
        inputPanel.add(searchButton);
    
        // Create selection panel for train options
        selectionPanel = new JPanel();
        selectionPanel.setLayout(new BoxLayout(selectionPanel, BoxLayout.Y_AXIS));
        selectionPanel.setBorder(BorderFactory.createTitledBorder("Available Trains"));
    
        // Create buttons panel
        JPanel buttonsPanel = new JPanel(new FlowLayout());
        nextButton = new JButton("Select Train");
        nextButton.addActionListener(e -> selectTrainAndContinue());
        nextButton.setEnabled(false);
        
        startOverButton = new JButton("Start Over");
        startOverButton.addActionListener(e -> resetBooking());
        startOverButton.setEnabled(false);
        
        buttonsPanel.add(nextButton);
        buttonsPanel.add(startOverButton);
    
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
        centerPanel.add(new JScrollPane(selectionPanel), BorderLayout.CENTER);
        centerPanel.add(scrollPane, BorderLayout.SOUTH);
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        
        mainPanel.add(buttonsPanel, BorderLayout.SOUTH);
    
        add(mainPanel);
    
        // Set initial time to current time
        timeSpinner.setValue(new Date());
        
        // Set frame properties
        setSize(800, 600);
        setLocationRelativeTo(null);
    }

    private void generateSchedules() {
        schedules = new ArrayList<>();
        LocalTime startTime = LocalTime.of(6, 0);
        LocalTime endTime = LocalTime.of(23, 0);

        for (Station from : stations.values()) {
            for (Map.Entry<Station, Integer> connection : from.connections.entrySet()) {
                Station to = connection.getKey();
                int distance = connection.getValue();
                
                LocalTime currentTime = startTime;
                while (currentTime.isBefore(endTime)) {
                    int travelMinutes = (distance * 60) / 30; // 30 km/h speed
                    LocalTime arrivalTime = currentTime.plusMinutes(travelMinutes);
                    
                    schedules.add(new TrainSchedule(currentTime, arrivalTime, from, to));
                    currentTime = currentTime.plusMinutes(30); // Train every 30 minutes
                }
            }
        }
    }

    private void startJourneyPlanning() {
        String from = (String) fromStation.getSelectedItem();
        String to = (String) toStation.getSelectedItem();
        Date time = (Date) timeSpinner.getValue();
        currentTime = LocalDateTime.ofInstant(time.toInstant(), ZoneId.systemDefault()).toLocalTime();

        // Find shortest path
        shortestPath = findShortestPath(stations.get(from), stations.get(to));
        
        if (shortestPath == null || shortestPath.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No route available between selected stations.");
            return;
        }

        // Reset for new journey
        currentSegmentIndex = 0;
        selectedTrains.clear();
        startOverButton.setEnabled(true);
        
        // Show first segment options
        showTrainOptions();
    }

    private List<Station> findShortestPath(Station start, Station end) {
        Map<Station, Station> previousStation = new HashMap<>();
        Map<Station, Integer> distances = new HashMap<>();
        PriorityQueue<Station> queue = new PriorityQueue<>(
            Comparator.comparingInt(s -> distances.getOrDefault(s, Integer.MAX_VALUE))
        );
        
        distances.put(start, 0);
        queue.offer(start);

        while (!queue.isEmpty()) {
            Station current = queue.poll();
            
            if (current == end) {
                // Reconstruct path
                List<Station> path = new ArrayList<>();
                Station station = end;
                while (station != null) {
                    path.add(0, station);
                    station = previousStation.get(station);
                }
                return path;
            }

            for (Map.Entry<Station, Integer> neighbor : current.connections.entrySet()) {
                Station next = neighbor.getKey();
                int newDist = distances.get(current) + neighbor.getValue();
                
                if (newDist < distances.getOrDefault(next, Integer.MAX_VALUE)) {
                    distances.put(next, newDist);
                    previousStation.put(next, current);
                    queue.offer(next);
                }
            }
        }

        return null;
    }

    private void showTrainOptions() {
        if (currentSegmentIndex >= shortestPath.size() - 1) {
            showFinalItinerary();
            return;
        }

        Station from = shortestPath.get(currentSegmentIndex);
        Station to = shortestPath.get(currentSegmentIndex + 1);
        
        LocalTime searchTime = currentSegmentIndex == 0 ? currentTime :
            selectedTrains.get(selectedTrains.size() - 1).arrivalTime;

        List<TrainSchedule> availableTrains = findAvailableTrains(from, to, searchTime);
        displayTrainOptions(availableTrains);
    }

    private List<TrainSchedule> findAvailableTrains(Station from, Station to, LocalTime searchTime) {
        LocalTime searchStartTime = searchTime.minusMinutes(10);
        LocalTime searchEndTime = searchTime.plusHours(1);

        return schedules.stream()
            .filter(s -> s.from == from && s.to == to)
            .filter(s -> !s.departureTime.isBefore(searchStartTime))
            .filter(s -> s.departureTime.isBefore(searchEndTime))
            .filter(s -> currentSegmentIndex == 0 || 
                Duration.between(selectedTrains.get(selectedTrains.size() - 1).arrivalTime, 
                    s.departureTime).toMinutes() >= 5)
            .sorted(Comparator.comparing(s -> s.departureTime))
            .collect(Collectors.toList());
    }

    private void displayTrainOptions(List<TrainSchedule> trains) {
        selectionPanel.removeAll();
        
        if (trains.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "No available trains found for this segment. Please try a different time.");
            resetBooking();
            return;
        }

        ButtonGroup buttonGroup = new ButtonGroup();
        
        JLabel headerLabel = new JLabel(String.format("Select train from %s to %s:",
            shortestPath.get(currentSegmentIndex),
            shortestPath.get(currentSegmentIndex + 1)));
        headerLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        selectionPanel.add(headerLabel);
        selectionPanel.add(Box.createVerticalStrut(10));

        for (TrainSchedule train : trains) {
            JRadioButton option = new JRadioButton(String.format(
                "Departure: %s - Arrival: %s (Journey time: %d minutes)",
                train.departureTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                train.arrivalTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                Duration.between(train.departureTime, train.arrivalTime).toMinutes()
            ));
            option.setActionCommand(trains.indexOf(train) + "");
            buttonGroup.add(option);
            option.setAlignmentX(Component.LEFT_ALIGNMENT);
            selectionPanel.add(option);
        }

        nextButton.setEnabled(true);
        selectionPanel.revalidate();
        selectionPanel.repaint();
        
        // Store trains for selection
        selectionPanel.putClientProperty("availableTrains", trains);
        selectionPanel.putClientProperty("buttonGroup", buttonGroup);
    }

    private void selectTrainAndContinue() {
        ButtonGroup buttonGroup = (ButtonGroup) selectionPanel.getClientProperty("buttonGroup");
        if (buttonGroup.getSelection() == null) {
            JOptionPane.showMessageDialog(this, "Please select a train to continue.");
            return;
        }

        List<TrainSchedule> trains = (List<TrainSchedule>) selectionPanel.getClientProperty("availableTrains");
        int selectedIndex = Integer.parseInt(buttonGroup.getSelection().getActionCommand());
        TrainSchedule selectedTrain = trains.get(selectedIndex);
        
        selectedTrains.add(selectedTrain);
        currentSegmentIndex++;
        
        updateJourneyInfo();
        showTrainOptions();
    }

    private void updateJourneyInfo() {
        StringBuilder info = new StringBuilder();
        info.append(String.format("Trip from %s to %s\n", 
            shortestPath.get(0), 
            shortestPath.get(shortestPath.size() - 1)));
        for (int i = 0; i < 20; i++) {
            info.append("-");
        }
        info.append("\n");

        for (TrainSchedule train : selectedTrains) {
            info.append(String.format("%s to %s : Start at %s - Stops at %s\n",
                train.from.name,
                train.to.name,
                train.departureTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                train.arrivalTime.format(DateTimeFormatter.ofPattern("HH:mm"))));
        }

        if (!selectedTrains.isEmpty()) {
            LocalTime startTime = selectedTrains.get(0).departureTime;
            LocalTime endTime = selectedTrains.get(selectedTrains.size() - 1).arrivalTime;
            long totalMinutes = Duration.between(startTime, endTime).toMinutes();
            info.append(String.format("\nTotal time = %d minutes", totalMinutes));
        }

        resultArea.setText(info.toString());
    }

    private void showFinalItinerary() {
        nextButton.setEnabled(false);
        selectionPanel.removeAll();
        
        JLabel finalLabel = new JLabel("Journey Planning Complete!");
        finalLabel.setFont(new Font(finalLabel.getFont().getName(), Font.BOLD, 14));
        finalLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        selectionPanel.add(finalLabel);
        
        JButton newBookingButton = new JButton("Make New Booking");
        newBookingButton.addActionListener(e -> resetBooking());
        newBookingButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        selectionPanel.add(Box.createVerticalStrut(10));
        selectionPanel.add(newBookingButton);
        
        selectionPanel.revalidate();
        selectionPanel.repaint();
    }

    private void resetBooking() {
        currentSegmentIndex = 0;
        selectedTrains.clear();
        nextButton.setEnabled(false);
        startOverButton.setEnabled(false);
        selectionPanel.removeAll();
        resultArea.setText("");
        fromStation.setSelectedIndex(0);
        toStation.setSelectedIndex(0);
        timeSpinner.setValue(new Date());
        
        selectionPanel.revalidate();
        selectionPanel.repaint();
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new MetroBookingGUI().setVisible(true);
        });
    }
}
