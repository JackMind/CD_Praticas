import spread.SpreadConnection;

public class MessageHandling implements Runnable {
    boolean end=false;
    private SpreadConnection connection;
    public MessageHandling(SpreadConnection connection) {
        this.connection=connection;
    }
    @Override
    public void run() {

      while (!end) {
          try {
              PrintMessages.MessageDetails(connection.receive());
          } catch(Exception e) {
             e.printStackTrace();
          }
        }
    }
}
