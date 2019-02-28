package uk.ac.cam.cl.cleancyclegui;

import uk.ac.cam.cl.cleancyclerouting.Algorithm;

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
