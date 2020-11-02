package com.company;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

public class RMIServer implements ILeiloes{

    public static final String serverIP = "localhost";
    public static final int registerPort = 7000;
    public static final int svcPort = 7001;

    //  Id    , Info (iNotification, value)
    public static Map<String, Info > repository;

    public static void main(String[] args) {
        try {
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
        private int value;
        private INotification iNotification;

        public Info(int value, INotification iNotification) {
            this.value = value;
            this.iNotification = iNotification;
        }

        public void setValue(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public INotification getiNotification() {
            return iNotification;
        }
    }



    @Override                                       // Notification to notify later
    public String initLeilao(SomeObject someObject, INotification iNotification) throws RemoteException {
        if(iNotification != null){
            return "Please provide a not null iNotification";
        }
        if(someObject != null){
            return "Please provide a not null SomeObject";
        }
        if(someObject.Id.isEmpty()){
            return "Please provide a valid Id";
        }
        if(repository.containsKey(someObject.Id)){
            return "Leilao already started for id: " + someObject.Id;
        }
        repository.put(someObject.Id, new Info(0, iNotification));
        return someObject.Id;
    }

    @Override
    public SomeObject[] getAllLeiloes() throws RemoteException {
        List<SomeObject> list = new LinkedList<>();
        repository.forEach((key, value) -> {
            SomeObject a = new SomeObject();
            a.Id = key;
            list.add(a);
        });
        return list.toArray(SomeObject[]::new);
    }

    @Override           // Id   , notify now
    public void licitar(String s, INotification iNotification) throws RemoteException {
        if(iNotification != null){
            System.out.println("iNotification null");
            return;
        }
        if(!repository.containsKey(s)){
            iNotification.sendNotification("There is no leilão started for id: " + s);
        }

        int bidding = repository.get(s).getValue();
        bidding++;
        repository.get(s).setValue(bidding);

        iNotification.sendNotification("Your current bidding is now " + bidding);

        for (Map.Entry<String, Info> set: repository.entrySet()){

            if(!set.getKey().equals(s)){

                if( bidding > set.getValue().getValue()) {
                    set.getValue().getiNotification().sendNotification(
                            "Your " + set.getValue().getValue() + "bidding is lower than current top "  + bidding);
                }
            }

        }

    }
}
