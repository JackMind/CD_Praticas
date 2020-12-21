import spread.BasicMessageListener;
import spread.SpreadConnection;
import spread.SpreadMessage;

public class MessageListener implements BasicMessageListener {
    public MessageListener(SpreadConnection connection) {
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
