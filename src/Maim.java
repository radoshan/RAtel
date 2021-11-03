import com.google.gson.Gson;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;


public class Maim {
    static CarServiceMock carService = new CarServiceMock();
    static String filePath = new File("").getAbsolutePath();
//    static File dataPath = new File(filePath + "\\src\\resources");
//    static String[] filesList = null;
    static long currentTimestamp = 0;
    static long lastTimestampUpdated = 0;
    static Gson gson = new Gson();
    static ArrayList<TelemetrySample> arayListTS = null;

    public static void main(String[] args) {
        prepareData();
        readTelemetryData();
        javax.swing.SwingUtilities.invokeLater(() -> createAndShowGUI());
        javax.swing.SwingUtilities.invokeLater(() -> new SwingWorker<Boolean, Void>() {
            @Override
            public Boolean doInBackground() {
                while (true) {
                    updateCars();
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        System.out.println("puko sam:");
                        e.printStackTrace();
                    }
                }
            }

            @Override
            protected void done() {

            }
        }.execute());
    }

    private static void prepareData() {
        Gson gson = new Gson();
        TelemetrySample[] tsList = new TelemetrySample[0];

        String filePath = new File("").getAbsolutePath();
        try (Reader reader = new FileReader(filePath + "\\src\\resources\\finalData123.json")) {
            tsList = gson.fromJson(reader, TelemetrySample[].class);

        } catch (IOException e) {
            e.printStackTrace();
        }
        // adds 2s to the difference between now and the first data entry, the ammount for which we will shift all timestamps in the data JSON
        long dt = System.currentTimeMillis() - tsList[0].getRecordedAt() + 2000;

        for (TelemetrySample ts: tsList) {
            ts.setRecordedAt(ts.getRecordedAt() + dt);
        }
        try (Writer writer = new FileWriter(filePath + "\\src\\resources\\finalData123.json")) {
            gson.toJson(tsList, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private static void updateCars() {
        currentTimestamp = System.currentTimeMillis();
        ArrayList<TelemetrySample> tempList = new ArrayList<TelemetrySample>();
        for (TelemetrySample ts: arayListTS) {
            if (ts.getRecordedAt() < currentTimestamp
                    && ts.getRecordedAt() > lastTimestampUpdated
            ) {
                tempList.add(ts);
            }
        }
        for (TelemetrySample ts: tempList) {
            arayListTS.remove(ts);
            updateAppropriateCar(ts);
        }
    }

    private static void updateAppropriateCar(TelemetrySample ts) {
        for (Car car: carService.getCars()) {
            if (ts.getVehicleId().equals(car.getId())) {
                car.setAverageSpeed(ts.getSignalValues().get("drivingTime")>0?ts.getSignalValues().get("odometer")/ts.getSignalValues().get("drivingTime")*1000*60*60:0); // update average speed

                double currentSpeed = ts.getSignalValues().get("currentSpeed");
                boolean isCharging = ts.getSignalValues().get("isCharging") > 0.5;

                if (currentSpeed > car.getMaximumSpeed()) { // update maximum speed
                    car.setMaximumSpeed(currentSpeed);
                }

                car.setLastMessageTimestamp(ts.getRecordedAt());  // update last message timestamp

                if (isCharging && !car.wasCharging()) {// start charging
                    car.startCharging();
                }
                if (!isCharging && car.wasCharging()) {// stop charging and increment number of charges
                    car.stopCharging();
                    car.incrementNumberOfCharges();
                }

                int newVehicleState = 0; // unknown vehicle state by default
                if (currentSpeed > 0 && !isCharging) newVehicleState = 1; // driving
                if (currentSpeed < 0.001 && isCharging) newVehicleState = 2; // charging
                if (currentSpeed < 0.001 && !isCharging) newVehicleState = 3; // parked
                car.setCurrentVehicleState(newVehicleState);  // update current vehicle state

                break;
            }
        }
    }

    private static void printTelemetryData() {
        for (TelemetrySample ts: arayListTS) {
            System.out.println("v");
            System.out.println(ts.toString());
            System.out.println("^");
        }
    }

    private static void readTelemetryData() {
        String filePath = new File("").getAbsolutePath();
        try (Reader reader = new FileReader(filePath + "\\src\\resources\\finalData123.json")) {
            arayListTS = new ArrayList<TelemetrySample>(Arrays.asList(gson.fromJson(reader, TelemetrySample[].class)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void printTime() {
        long currentTimestamp = System.currentTimeMillis();
        System.out.println("Current epoch timestamp in millis: " + currentTimestamp);
    }

    private static void createAndShowGUI() {
        JFrame mainFrame = new JFrame("RA Telemetry");
        mainFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        mainFrame.setSize(300, 500);
        mainFrame.setLocationRelativeTo(null);


        DefaultListModel searchResultListModel = new DefaultListModel();
        DefaultListSelectionModel searchResultSelectionModel = new DefaultListSelectionModel();
        searchResultSelectionModel
                .setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        Document searchInput = new PlainDocument();

        CarDetailsAction carDetailsAction = new CarDetailsAction(
                searchResultSelectionModel, searchResultListModel);
        carDetailsAction.putValue(Action.NAME, "Car Details");

        Action searchCarAction = new SearchCarAction(searchInput,
                searchResultListModel, carService);
        searchCarAction.putValue(Action.NAME, "Search");

        Container contentPane = mainFrame.getContentPane();

        JPanel searchInputPanel = new JPanel();
        searchInputPanel.setLayout(new BorderLayout());

        JTextField searchField = new JTextField(searchInput, null, 0);
        searchInputPanel.add(searchField, BorderLayout.CENTER);
        searchField.addActionListener(searchCarAction);

        JButton searchButton = new JButton(searchCarAction);
        searchInputPanel.add(searchButton, BorderLayout.EAST);

        JList searchResultList = new JList();
        searchResultList.setModel(searchResultListModel);
        searchResultList.setSelectionModel(searchResultSelectionModel);

        JPanel searchResultPanel = new JPanel();
        searchResultPanel.setLayout(new BorderLayout());
        JScrollPane scrollableSearchResult = new JScrollPane(searchResultList);
        searchResultPanel.add(scrollableSearchResult, BorderLayout.CENTER);

        JPanel selectionOptionsPanel = new JPanel();

        JButton showCarDetailsButton = new JButton(carDetailsAction);
        selectionOptionsPanel.add(showCarDetailsButton);

        contentPane.add(searchInputPanel, BorderLayout.NORTH);
        contentPane.add(searchResultPanel, BorderLayout.CENTER);
        contentPane.add(selectionOptionsPanel, BorderLayout.SOUTH);

        mainFrame.setVisible(true);
    }

}

class CarDetailsAction extends AbstractAction {

    private static final long serialVersionUID = -8816163868526676625L;

    private ListSelectionModel carSelectionModel;
    private DefaultListModel carListModel;

    public CarDetailsAction(ListSelectionModel carSelectionModel,
                            DefaultListModel carListModel) {
        boolean unsupportedSelectionMode = carSelectionModel
                .getSelectionMode() != ListSelectionModel.SINGLE_SELECTION;
        if (unsupportedSelectionMode) {
            throw new IllegalArgumentException(
                    "CarDetailAction can only handle single list selections. "
                            + "Please set the list selection mode to ListSelectionModel.SINGLE_SELECTION");
        }
        this.carSelectionModel = carSelectionModel;
        this.carListModel = carListModel;
        carSelectionModel
                .addListSelectionListener(new ListSelectionListener() {

                    public void valueChanged(ListSelectionEvent e) {
                        ListSelectionModel listSelectionModel = (ListSelectionModel) e
                                .getSource();
                        updateEnablement(listSelectionModel);
                    }
                });
        updateEnablement(carSelectionModel);
    }

    public void actionPerformed(ActionEvent e) {
        Maim.printTime();
        int selectionIndex = carSelectionModel.getMinSelectionIndex();
        CarElementModel carElementModel = (CarElementModel) carListModel
                .get(selectionIndex);

        Car car = carElementModel.getCar();
        String carDetials = createCarDetailsHTML(car);

        JLabel messageLabel = new JLabel(carDetials);
        messageLabel.setFont(new Font("monospaced", Font.BOLD, 12));
        JOptionPane.showMessageDialog(null, messageLabel);
    }

    private String createCarDetailsHTML(Car car) {
        StringBuilder sb = new StringBuilder("<html>");
        sb.append(formatLabel(Car.labelID)).append(formatId(car.getId())).append(formatUnit(Car.jedinicaID)).append("<br>");
        sb.append(formatLabel(Car.labelAverageSpeed)).append(formatField((int)car.getAverageSpeed())).append(formatUnit(Car.jedinicaAverageSpeed)).append("<br>");
        sb.append(formatLabel(Car.labelMaximumSpeed)).append(formatField((int)car.getMaximumSpeed())).append(formatUnit(Car.jedinicaMaximumSpeed)).append("<br>");
        sb.append(formatLabel(Car.labelLastMessageTimestamp)).append(formatFieldL(car.getLastMessageTimestamp())).append(formatUnit(Car.jedinicaLastMessageTimestamp)).append("<br>");
        sb.append(formatLabel(Car.labelNumberOfCharges)).append(formatField(car.getNumberOfCharges())).append(formatUnit(Car.jedinicaNumberOfCharges)).append("<br>");
        sb.append(formatLabel(Car.labelCurrentVehicleState)).append(formatField(car.getCurrentVehicleState())).append(formatUnit(Car.jedinicaCurrentVehicleState)).append("</html>");
        return sb.toString();
    }

    private String formatId(String id) {
        int valueLen = 20;
        return (" ".repeat(valueLen - id.length()) + id).replace(" ", "&nbsp;");
    }

    private String formatLabel(String label) {
        int labelLen = 25;
        return String.format("%1$-" + labelLen + "s", label + ":").replace(" ", "&nbsp;");
    }

    private String formatField(int value) {
        int valueLen = 20;
        return String.format("%1$" + valueLen + "s", value).replace(" ", "&nbsp;");
    }

    private String formatFieldL(long value) {
        int valueLen = 20;
        Date date = new Date(value);
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss dd.MM.yyyy");
        String strDate = sdf.format(date);
        return (" ".repeat(valueLen - strDate.length()) + strDate).replace(" ", "&nbsp;");
    }

    private String formatUnit(String unit) {
        int unitLen = 4;
        return String.format(" %1$" + unitLen + "s", unit).replace(" ", "&nbsp;");
    }


    private void updateEnablement(ListSelectionModel listSelectionModel) {
        boolean emptySelection = listSelectionModel.isSelectionEmpty();
        setEnabled(!emptySelection);
    }

}

class SearchCarAction extends AbstractAction {

    private static final long serialVersionUID = 4083406832930707444L;

    private Document searchInput;
    private DefaultListModel searchResult;
    private CarService carService;

    public SearchCarAction(Document searchInput,
                           DefaultListModel searchResult, CarService carService) {
        this.searchInput = searchInput;
        this.searchResult = searchResult;
        this.carService = carService;
    }

    public void actionPerformed(ActionEvent e) {
        String searchString = getSearchString();

        List<Car> matchedCars = carService.searchCars(searchString);

        searchResult.clear();
        for (Car car : matchedCars) {
            Object elementModel = new CarElementModel(car);
            searchResult.addElement(elementModel);
        }
    }

    private String getSearchString() {
        try {
            return searchInput.getText(0, searchInput.getLength());
        } catch (BadLocationException e) {
            return null;
        }
    }

}

class CarElementModel {

    private Car car;

    public CarElementModel(Car car) {
        this.car = car;
    }

    public Car getCar() {
        return car;
    }

    @Override
    public String toString() {
        return car.getId();
    }
}

interface CarService {

    List<Car> searchCars(String searchString);
}

class Car {
    static final String labelID = "vehicle ID";
    static final String labelAverageSpeed = "average speed";
    static final String labelMaximumSpeed = "maximum speed";
    static final String labelLastMessageTimestamp = "last message timestamp";
    static final String labelNumberOfCharges = "number of charges";
    static final String labelCurrentVehicleState = "current vehicle state";

    static final String jedinicaID = "";
    static final String jedinicaAverageSpeed = "km/h";
    static final String jedinicaMaximumSpeed = "km/h";
    static final String jedinicaLastMessageTimestamp = "";
    static final String jedinicaNumberOfCharges = "";
    static final String jedinicaCurrentVehicleState = "";

    private String id;
    private double averageSpeed = 0;
    private double maximumSpeed = 0;
    private long lastMessageTimestamp = 1635791262851L;
    private int numberOfCharges = 0;
    private int currentVehicleState = 0;// Vehicle can be in one of driving - 3/charging - 2/parked - 1/unknown state - 0
    private boolean charging = false;

    public boolean wasCharging() {
        return charging;
    }
    public void startCharging() {
        this.charging = true;
    }

    public void stopCharging() {
        this.charging = false;
    }

    public void setAverageSpeed(double averageSpeed) {
        this.averageSpeed = averageSpeed;
    }

    public void setMaximumSpeed(double maximumSpeed) {
        this.maximumSpeed = maximumSpeed;
    }

    public void setLastMessageTimestamp(long lastMessageTimestamp) {
        this.lastMessageTimestamp = lastMessageTimestamp;
    }

    public void incrementNumberOfCharges() {
        this.numberOfCharges += 1;
    }

    public void setCurrentVehicleState(int currentVehicleState) {
        this.currentVehicleState = currentVehicleState;
    }

    public Car(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public double getAverageSpeed() {
        return averageSpeed;
    }

    public double getMaximumSpeed() {
        return maximumSpeed;
    }

    public long getLastMessageTimestamp() {
        return lastMessageTimestamp;
    }

    public int getNumberOfCharges() {
        return numberOfCharges;
    }

    public int getCurrentVehicleState() {
        return currentVehicleState;
    }
}

class CarServiceMock implements CarService {

    private List<Car> carDB;

    public List<Car> getCars() {
        return carDB;
    }

    public CarServiceMock() {
        carDB = new ArrayList<Car>();
        carDB.add(new Car("1"));
        carDB.add(new Car("2"));
        carDB.add(new Car("3"));
    }

    public List<Car> searchCars(String srchID) {
        List<Car> matches = new ArrayList<Car>();

        if (srchID.equals("")) {
            return carDB;
        }

        for (Car car : carDB) {
            if(car.getId().contains(srchID)) {
//            if(srchID.equals(car.getId())) {
                matches.add(car);
            }

        }
        return matches;
    }
}