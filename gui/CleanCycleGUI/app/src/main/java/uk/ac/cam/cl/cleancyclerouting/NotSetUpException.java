package uk.ac.cam.cl.cleancyclerouting;

public class NotSetUpException extends Exception {
    public int type;
    NotSetUpException(int _type){
        type = _type;
    }
}