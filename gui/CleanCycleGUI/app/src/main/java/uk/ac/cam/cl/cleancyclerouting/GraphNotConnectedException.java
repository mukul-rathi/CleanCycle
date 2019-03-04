package uk.ac.cam.cl.cleancyclerouting;

public class GraphNotConnectedException extends Exception {
    public GraphNotConnectedException () {}
    public GraphNotConnectedException (String message) { super(message); }
}
