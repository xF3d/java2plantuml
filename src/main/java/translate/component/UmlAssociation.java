package translate.component;

public record UmlAssociation(String from, String type, String to) {
    @Override
    public String toString() {
        return from + " " + type + " " + to;
    }
}
