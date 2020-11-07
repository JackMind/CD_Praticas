import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

public class RMIServer implements ILeiloes{

    public static String serverIP = "localhost";
    public static final int registerPort = 7000;
    public static final int svcPort = 7001;

    //  Id    , Info (iNotification, value)
    public static Map<String, Info > repository;

    public static void main(String[] args) {
        try {
            if(args.length > 0){
                serverIP = args[0];
                System.out.println(serverIP);
            }
            repository = new HashMap<>();

            Properties props = System.getProperties();
            props.put("java.rmi.server.hostname", serverIP);

            RMIServer svc = new RMIServer();
            ILeiloes stubSvc = (ILeiloes) UnicastRemoteObject.exportObject(svc, svcPort);
            Registry registry = LocateRegistry.createRegistry(registerPort);

            registry.rebind("Batatas", stubSvc);  //regista skeleton com nome lógico

            System.out.println("Server ready: Press any key to finish server");
            java.util.Scanner scanner = new java.util.Scanner(System.in);
            String line = scanner.nextLine(); System.exit(0);
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (Exception ex) {
            System.err.println("Server unhandled exception: " + ex.toString());
            ex.printStackTrace();
        }
    }

    class Info{
        private float value;
        private INotification iNotification;

        public Info(int value, INotification iNotification) {
            this.value = value;
            this.iNotification = iNotification;
        }

        public void setValue(float value) {
            this.value = value;
        }

        public float getValue() {
            return value;
        }

        public INotification getiNotification() {
            return iNotification;
        }
    }



    @Override                                       // Notification to notify later
    public void initLeilao(SomeObject someObject, INotification iNotification) throws RemoteException {
        if(iNotification == null){
            return;
        }
        if(someObject == null){
            iNotification.sendNotification("Please provide a not null SomeObject");
        }
        if(someObject.getId().isEmpty()){
            iNotification.sendNotification( "Please provide a valid Id");
        }
        if(repository.containsKey(someObject.getId())){
            iNotification.sendNotification( "Leilao already started for id: " + someObject.getId());
        }
        repository.put(someObject.getId(), new Info(0, iNotification));
        iNotification.sendNotification( "Leilao inited for: " + someObject.getId());
    }

    @Override
    public SomeObject[] getAllLeiloes() throws RemoteException {
        List<SomeObject> list = new LinkedList<>();
        repository.forEach((key, value) -> {
            list.add(new SomeObject(key));
        });
        return list.toArray(SomeObject[]::new);
    }

    @Override           // Id   , notify now
    public void licitar(String s, float v, INotification iNotification) throws RemoteException {
        if(iNotification == null){
            System.out.println("iNotification null");
            return;
        }
        if(!repository.containsKey(s)){
            iNotification.sendNotification("There is no leilão started for id: " + s);
        }

        repository.get(s).setValue(v);

        iNotification.sendNotification("Curren tbididng for object " + s + " is now " + v);

        for (Map.Entry<String, Info> set: repository.entrySet()){

            if(!set.getKey().equals(s)){

                if( v > set.getValue().getValue()) {
                    set.getValue().getiNotification().sendNotification(
                            "Your " + set.getValue().getValue() + " bidding is lower than current top "  + v);
                }
            }

        }

    }
}