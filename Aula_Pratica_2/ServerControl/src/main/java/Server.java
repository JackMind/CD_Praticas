import centralstubs.CentralServiceGrpc;
import centralstubs.Tariff;
import centralstubs.Track;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.ServerBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import rpcstubs.*;
import rpcstubs.Void;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class Server extends ControlServiceGrpc.ControlServiceImplBase
{
    private static ManagedChannel channel;
    private static Map<String, Initial> VeiculosEstrada;

    private static final int GROUP_ID = 12;
    private static String centralIp = "35.230.146.225";
    private static int centralPort = 7500;
    private static int svcPort = 6000;

    public static void main(String[] args)
    {
        if(args.length > 0){
            centralIp = args[0].isEmpty() ? centralIp : args[0];
            //centralPort = args[1].isEmpty() ? centralPort : Integer.parseInt( args[1] );
        }
        System.out.println("Central Server ip: " + centralIp + " ,Central Server port:  " + centralPort);


        channel = ManagedChannelBuilder
                .forAddress(centralIp, centralPort)
                .usePlaintext()
                .build();

        Track track = Track.newBuilder()
                .setGroup(GROUP_ID)
                .setInPoint(1)
                .setOutPoint(2).build();

        try{
            Tariff tariff = CentralServiceGrpc
                    .newBlockingStub(channel)
                    //Set timeout
                    .withDeadlineAfter(5, TimeUnit.SECONDS)
                    .payment(track);

            System.out.println("Central service is OK! " + tariff);
        }catch (StatusRuntimeException ex){
            return;
        }

        VeiculosEstrada = new HashMap<>();

        try{
             io.grpc.Server svc = ServerBuilder
                    .forPort(svcPort)
                    .addService(new Server())
                    .build();
            svc.start();
            
            System.out.println("Grpc Server started, listening on " + svcPort);
            Scanner scan= new Scanner(System.in); scan.nextLine();
            svc.shutdown();
        } catch(Exception ex) { ex.printStackTrace(); }
    }



    @Override
    public void enter(Initial request, StreamObserver<Void> responseObserver)
    {
        System.out.println("Enter Called by " + request.getId() + " entered at " + request.getInPoint());

        //Adicionar Veiculo ao Mapa
        VeiculosEstrada.put(request.getId(), request);

        Void voidResponse = Void.newBuilder().build();
        responseObserver.onNext(voidResponse);
        responseObserver.onCompleted();
    }

    @Override
    public void leave(FinalPoint request, StreamObserver<Payment> responseObserver)
    {
        if(request.getId() == null || request.getId().isEmpty()){
            responseObserver.onError(new Exception("Provide an id"));
        }

        System.out.println("Leave Called by " + request.getId() + " exited at " + request.getOutPoint());

        //Procurar Veiculo no Mapa
        Initial initialPoint = VeiculosEstrada.get(request.getId());
        if(initialPoint != null)
        {
            //Obter Valor da Tarifa
            Track track = Track.newBuilder()
                    .setGroup(GROUP_ID)
                    .setInPoint(initialPoint.getInPoint())
                    .setOutPoint(request.getOutPoint()).build();

            Tariff tariff;
            try{
                tariff = CentralServiceGrpc
                        .newBlockingStub(channel)
                        //Set timeout
                        .withDeadlineAfter(5, TimeUnit.SECONDS)
                        .payment(track);

            }catch (StatusRuntimeException ex){
                responseObserver.onError(ex);
                //TODO: handle behaviour in case the payment is not received.
                responseObserver.onCompleted();
                return;
            }

            System.out.println("Received payment for " + request.getId() + " -> " + tariff);
            //Gerar Payment e Enviar ao cliente
            Payment pagamento = Payment.newBuilder().setValue(tariff.getValue()).build();
            responseObserver.onNext(pagamento);

            VeiculosEstrada.remove(request.getId());

            responseObserver.onCompleted();
        }else{
            System.out.println("Car " + request.getId() + " did not exist on db, maybe it is a ghost rider!" +
                    " It exited on " + request.getOutPoint());
            responseObserver.onError(new RuntimeException("No car"));
            responseObserver.onCompleted();
        }
    }


    @Override
    public StreamObserver<WarnMsg> warning(StreamObserver<WarnMsg> responseObserver)
    {


        //RECEBER A MENSAGEM DE 1
        //FAZER BROADCAST PARA TODOS

        //WarnMsg warnMsg = WarnMsg.newBuilder().setId().setWarning().build();


        for (Map.Entry<String, Initial> entry : VeiculosEstrada.entrySet())
        {
            String matricula = entry.getKey();
            //ENVIAR MENSAGEM COM BASE NO ID
        }

        //responseObserver.onNext(warnMsg);
        responseObserver.onCompleted();

        return new ServerObserver(responseObserver, VeiculosEstrada);
    }
}
