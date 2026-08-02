package translate.dummy;

public interface ReversibleAction {
    void apply();
    void revert();
}
