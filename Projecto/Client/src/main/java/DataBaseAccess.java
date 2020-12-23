import io.grpc.ManagedChannel;
import rpcsclientstubs.ClientServiceGrpc;
import rpcsclientstubs.Data;
import rpcsclientstubs.Key;

public class DataBaseAccess
{
    private final ManagedChannel channelServer;

    public DataBaseAccess(ManagedChannel channelServer)
    {
        this.channelServer = channelServer;
    }

    public Data read(String key)
    {
        return ClientServiceGrpc.newBlockingStub(channelServer).read(Key.newBuilder().setKey(key).build());
    }

    public void write(String key, String value)
    {
        ClientServiceGrpc.newBlockingStub(channelServer).write(Data.newBuilder().setData(value).setKey(key).build());
    }
}
