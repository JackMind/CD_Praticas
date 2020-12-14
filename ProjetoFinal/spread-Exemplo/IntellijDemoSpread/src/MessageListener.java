import spread.BasicMessageListener;
import spread.SpreadConnection;
import spread.SpreadMessage;

public class MessageListener implements BasicMessageListener {
    private SpreadConnection connection;
    public MessageListener(SpreadConnection connection) {
        this.connection=connection;
    }
    @Override
    public void messageReceived(SpreadMessage spreadMessage) {
        try {
            PrintMessages.MessageDetails(spreadMessage);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
