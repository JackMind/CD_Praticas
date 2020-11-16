import centralstubs.CentralServiceGrpc;
import centralstubs.Tariff;
import centralstubs.Track;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import rpcstubs.*;
import rpcstubs.Void;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Server extends ControlServiceGrpc.ControlServiceImplBase
{
    private static ManagedChannel channel;
    private static CentralServiceGrpc.CentralServiceStub noBlockStub;
    private static CentralServiceGrpc.CentralServiceBlockingStub blockingStub;
    private static CentralServiceGrpc.CentralServiceFutureStub futStub;

    private static Map<String, Initial> VeiculosEstrada;

    private static final int GROUP_ID = 12;
    private static final String svcIP = "35.230.146.225";
    private static final int svcPort = 7500;

    public static void main(String[] args)
    {
        try
        {
            channel = ManagedChannelBuilder.forAddress(svcIP, svcPort).usePlaintext().build();
            blockingStub = CentralServiceGrpc.newBlockingStub(channel);
            noBlockStub = CentralServiceGrpc.newStub(channel);
            futStub = CentralServiceGrpc.newFutureStub(channel);
            VeiculosEstrada = new HashMap<String, Initial>();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }



    @Override
    public void enter(Initial request, StreamObserver<Void> responseObserver)
    {
        System.out.println("Enter Called");

        //Adicionar Veiculo ao Mapa
        VeiculosEstrada.put(request.getId(), request);

        Void voidResponse = Void.newBuilder().build();
        responseObserver.onNext(voidResponse);
        responseObserver.onCompleted();
    }

    @Override
    public void leave(FinalPoint request, StreamObserver<Payment> responseObserver)
    {
        System.out.println("Leave Called");

        //Procurar Veiculo no Mapa
        Initial initialPoint = VeiculosEstrada.get(request.getId());
        if(initialPoint != null)
        {
            //Obter Valor da Tarifa
            Track track = Track.newBuilder().setGroup(GROUP_ID).setInPoint(initialPoint.getInPoint()).setOutPoint(request.getOutPoint()).build();
            Tariff tariff = blockingStub.payment(track);

            //Gerar Payment e Enviar ao cliente
            Payment pagamento = Payment.newBuilder().setValue(tariff.getValue()).build();
            responseObserver.onNext(pagamento);
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
        return responseObserver;
    }
}
