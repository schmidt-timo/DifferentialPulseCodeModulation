public enum PredictorType {

    A("A (horizontal)"),
    B("B (vertical)"),
    C("C (diagonal)"),
    ABC("A + B – C"),
    ADAPTIVE("adaptive");

    private final String name;

    PredictorType(String s) {
        name = s;
    }

    public String toString() {
        return this.name;
    }

};