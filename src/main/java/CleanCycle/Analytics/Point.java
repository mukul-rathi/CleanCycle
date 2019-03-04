package CleanCycle.Analytics;

import java.io.Serializable;

/**
 * Class for a Clean Cycle data point.
 */
public class Point implements Serializable {
    /* Latitude and Longitude are the location where the point was measured.
    Pollution10 and Pollution2_5 are PM10 and PM2.5 particulate measurements respectively.
     */
    public final double Latitude;
    public final double Longitude;
    public final double Pollution10;
    public final double Pollution2_5;

    public Point(double lat_, double long_, double poll10_, double poll2_5_) {
        Latitude = lat_;
        Longitude = long_;
        Pollution10 = poll10_;
        Pollution2_5 = poll2_5_;
    }
}