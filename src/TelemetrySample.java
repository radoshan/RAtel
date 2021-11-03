import java.util.Map;

// klasa za prihvatanje podataka - vrv radi
public class TelemetrySample {
    private String vehicleId;
    private long recordedAt;
    private Map<String, Double> signalValues;

    public TelemetrySample(String vehicleId, long recordedAt, Map<String, Double> signalValues) {
        this.vehicleId = vehicleId;
        this.recordedAt = recordedAt;
        this.signalValues = signalValues;
    }
    public TelemetrySample(String vehicleId, long recordedAt) {
        this.vehicleId = vehicleId;
        this.recordedAt = recordedAt;
    }
    public TelemetrySample(String vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getVehicleId() {
//        System.out.println(signalValues.get("currentSpeed"));
        return vehicleId;
    }

    public void setVehicleId(String vehicleId) {
        this.vehicleId = vehicleId;
    }

    public long getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(long recordedAt) {
        this.recordedAt = recordedAt;
    }
    public String toString() {
        StringBuilder res = new StringBuilder("");
        res.append("vehicleId: ").append(vehicleId).append("\n");
        res.append("recordedAt: ").append(recordedAt).append("\n");
        res.append("signalValues:").append("\n");
        res.append("  currentSpeed: ").append(getSignalValues().get("currentSpeed")).append("\n");
        res.append("  odometer: ").append(getSignalValues().get("odometer")).append("\n");
        res.append("  drivingTime: ").append(getSignalValues().get("drivingTime")).append("\n");
        res.append("  isCharging: ").append(getSignalValues().get("isCharging")).append("\n");
        return res.toString();
    }
    public Map<String, Double> getSignalValues() {
        return signalValues;
    }

    public void setSignalValues(Map<String, Double> signalValues) {
        this.signalValues = signalValues;
    }
}
