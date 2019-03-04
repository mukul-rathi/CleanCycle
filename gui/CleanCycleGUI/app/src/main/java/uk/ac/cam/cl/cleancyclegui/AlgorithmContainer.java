package uk.ac.cam.cl.cleancyclegui;

import uk.ac.cam.cl.cleancyclerouting.Algorithm;

/**
 * A container used to hold the value of the current algorithm to be used by the routing system.
 * This is used so that multiple objects with the same algorithm container can have the algorithm
 * which they are using updated simultaneously without the need to recreate said objects.
 */
class AlgorithmContainer {
    private Algorithm algorithm;

    public AlgorithmContainer(Algorithm algorithm) {
        this.algorithm = algorithm;
    }

    public synchronized Algorithm getAlgorithm() {
        return algorithm;
    }

    public synchronized void setAlgorithm(Algorithm algorithm) {
        this.algorithm = algorithm;
    }
}
