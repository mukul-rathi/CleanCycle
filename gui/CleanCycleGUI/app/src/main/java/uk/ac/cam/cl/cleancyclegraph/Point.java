package uk.ac.cam.cl.cleancyclegraph;

public class Point {
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