import spread.*;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Scanner;
public class MemberApp { //implements BasicMessageListener {

      private SpreadConnection connection; // The Spread Connection
    public MemberApp(String user, String address, int port) {
        // Establish the spread connection.
        ///////////////////////////////////
        try  {
            connection = new SpreadConnection();
            connection.connect(InetAddress.getByName(address), port, user, false, true);
        }
        catch(SpreadException e)  {
            System.err.println("There was an error connecting to the daemon.");
            e.printStackTrace();
            System.exit(1);
        }
        catch(UnknownHostException e) {
            System.err.println("Can't find the daemon " + address);
            System.exit(1);
        }
    }






    private static String read(String msg, Scanner input) {
        System.out.println(msg);
        return input.nextLine();
    }
    private  static int Menu() {
        int op;
        Scanner scan = new Scanner(System.in);
        do {
            System.out.println();
            System.out.println("    MENU");
            System.out.println(" 1 - Join to a Group");
            System.out.println(" 2 - Send Message to Group");
            System.out.println(" 3 - Leave from a Group");
            System.out.println("99 - Exit");
            System.out.println();
            System.out.println("Choose an Option?");
            op = scan.nextInt();
        } while (!((op >= 1 && op <= 3) || op == 99));
        return op;
    }


    private static String daemonIP="localhost";
    //private static String daemonIP="35.230.146.225";
    //private static String daemonIP="34.105.137.146";
    private static int daemonPort=4803;
    //private static int daemonPort=3333;

    private  Map<String, SpreadGroup> myGroups=new HashMap<String,SpreadGroup>();
    public static void main(String[] args) {
        try {
            if (args.length > 0 ) {
                daemonIP=args[0];
            }
            Scanner scaninput = new Scanner(System.in);
            String myName = read("MemberApp name? ",scaninput);
            MemberApp app = new MemberApp(myName, daemonIP, daemonPort);

//            ExecutorService executor = Executors.newFixedThreadPool(3);
//            Runnable handler=new MessageHandling(app.connection);
//            executor.execute(handler);

            MessageListener msgHandling =new MessageListener(app.connection);
            app.connection.add(msgHandling);

            boolean end=false;
            while (!end) {
                int option = Menu();
                switch (option) {
                    case 1:
                        String groupName = read("Join to group named? ", scaninput);
                        SpreadGroup newGroup=new SpreadGroup();
                        newGroup.join(app.connection, groupName);
                        app.myGroups.put(groupName,newGroup);
                        //System.out.println("Joined " + app.group + ".");
                        break;
                    case 2:
                        SpreadMessage msg = new SpreadMessage();
                        msg.setSafe();
                        msg.addGroup(read("Group Name to send Message? ", scaninput));
                        msg.setData(read("Message Data? ", scaninput).getBytes());
                        app.connection.multicast(msg);
                        break;
                    case 3:
                        // Leave a group.
                          if (app.myGroups.size() == 0) {
                              System.out.println("No group to leave.");
                              break;
                          }
                          for (String gname : app.myGroups.keySet())
                              System.out.println("Joined to: " + gname + ".");
                          String nameToLeave=read("Group name to leave", scaninput);
                          SpreadGroup sp=app.myGroups.get(nameToLeave);
                          if(sp != null) {
                            sp.leave();
                            app.myGroups.remove(nameToLeave);
                            System.out.println("Left " + sp.toString() + ".");
                          } else  { System.out.println("No group to leave."); }
                          break;
                    case 99:
                        end = true;
                        break;
                }
            }
            app.connection.remove(msgHandling);
            // Disconnect.
            app.connection.disconnect();
            // Quit.
            System.exit(0);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }
}
