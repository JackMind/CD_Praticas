import centralstubs.CentralServiceGrpc;
import centralstubs.Tariff;
import centralstubs.Track;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import rpcstubs.*;
import rpcstubs.Void;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class ControlService extends ControlServiceGrpc.ControlServiceImplBase{

    private final Map<String, Initial> VeiculosEstrada;
    private final ManagedChannel channel;
    private final int groupId;

    public ControlService(Map<String, Initial> veiculosEstrada, ManagedChannel channel, int groupId) {
        VeiculosEstrada = veiculosEstrada;
        this.channel = channel;
        this.groupId = groupId;
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
                    .setGroup(groupId)
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
        ServerObserver observer = new ServerObserver(responseObserver);
        Server.messageBroadCast.clientsObservers.put(observer.observerId, responseObserver);
        return observer;
    }
}
