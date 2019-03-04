package CleanCycle.Analytics;

public class MapInfoContainer {
    private double pollutionPercentile5;
    private double pollutionPercentile10;
    private double pollutionPercentile90;
    private double pollutionPercentile95;
    private double minLogPollution;
    private double maxLogPollution;

    public MapInfoContainer() {

    }

    public double getPollutionPercentile10() {
        return pollutionPercentile10;
    }

    public void setPollutionPercentile10(double pollutionPercentile10) {
        this.pollutionPercentile10 = pollutionPercentile10;
    }

    public double getPollutionPercentile90() {
        return pollutionPercentile90;
    }

    public void setPollutionPercentile90(double pollutionPercentile90) {
        this.pollutionPercentile90 = pollutionPercentile90;
    }

    public double getMinLogPollution() {
        return minLogPollution;
    }

    public void setMinLogPollution(double minLogPollution) {
        this.minLogPollution = minLogPollution;
    }

    public double getMaxLogPollution() {
        return maxLogPollution;
    }

    public void setMaxLogPollution(double maxLogPollution) {
        this.maxLogPollution = maxLogPollution;
    }

    public double getPollutionPercentile5() {
        return pollutionPercentile5;
    }

    public void setPollutionPercentile5(double pollutionPercentile5) {
        this.pollutionPercentile5 = pollutionPercentile5;
    }

    public double getPollutionPercentile95() {
        return pollutionPercentile95;
    }

    public void setPollutionPercentile95(double pollutionPercentile95) {
        this.pollutionPercentile95 = pollutionPercentile95;
    }
}
