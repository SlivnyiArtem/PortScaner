public class ObserveResult {
    public final int port;
    public final boolean isOpen;
    public final String protocol;

    public ObserveResult(int port, boolean isOpen, String protocol) {
        this.port = port;
        this.isOpen = isOpen;
        this.protocol = protocol;
    }
}
