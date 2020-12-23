public class Server
{
    private final String server_name;
    private final String server_address;
    private final int server_port;

    public Server(String server_name, String server_address, int server_port) {
        this.server_name = server_name;
        this.server_address = server_address;
        this.server_port = server_port;
    }

    public String getServer_name() {
        return server_name;
    }

    public String getServer_address() {
        return server_address;
    }

    public int getServer_port() {
        return server_port;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "Server{" +
                "server_name=" + server_name +
                ", server_address=" + server_address +
                ", server_port=" + server_port +
                '}';
    }
}
