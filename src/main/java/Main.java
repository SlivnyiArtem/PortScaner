import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public class Main {
    public static void main(String[] args) {
        final ThreadPoolExecutor threadExecutor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        int notFinalStartIndex;
        int notFinalFinishIndex;
        boolean notFinalIsUdpSearch;


        try {
            BufferedReader reader;
            String stingJson;
            reader = new BufferedReader(new FileReader(
                    ".\\src\\main\\java\\inputJSON"));
            stingJson = reader.readLine();
            if (stingJson != null && stingJson.length()>0){
                    var jsonObject = new JSONObject(stingJson);
                notFinalStartIndex = jsonObject.getInt("StartIndex");
                notFinalFinishIndex = jsonObject.getInt("FinishIndex");
                notFinalIsUdpSearch = jsonObject.getBoolean("isUdpSearch");
            }
            else
            {
                notFinalStartIndex = 0;
                notFinalFinishIndex = 65535;
                notFinalIsUdpSearch = false;
            }
        } catch (IOException exc){
            notFinalStartIndex = 0;
            notFinalFinishIndex = 65535;
            notFinalIsUdpSearch = false;
        }
        final int startIndex = notFinalStartIndex;
        final int finishIndex = notFinalFinishIndex;
        final boolean isUdpSearch = notFinalIsUdpSearch;


        try {
            var resultList = searchForTcpPort(threadExecutor, startIndex, finishIndex, isUdpSearch);
            for (String result:resultList) {
                System.out.println(result);
            }
        }
        catch (ExecutionException|InterruptedException exception) {
            System.out.println("PROBLEM");
        }
    }

    public static Callable<ObserveResult> ObserveUdpPortFuture(int port) {
        return () -> {
            try(var udpSocket = new DatagramSocket()) {
                var address = InetAddress.getByName("127.0.0.1");
                udpSocket.connect(address, port);
                var message = new byte[]{80, 73, 78, 71};;
                var udpPacket = new DatagramPacket(message, message.length);
                udpSocket.send(udpPacket);
                udpSocket.receive(new DatagramPacket(new byte[100], 100));
                return new ObserveResult(port, true, "UDP");
            }
            catch (SocketTimeoutException timeoutExc){
                return new ObserveResult(port, true, "UDP");
            }
        };
    }

    public static Callable<ObserveResult> ObserveTcpPortFuture(int port) {
        return () -> {
            try (var tcpSocket = new Socket()) {
                var endPoint = new InetSocketAddress("127.0.0.1", port);
                tcpSocket.connect(endPoint);
                return new ObserveResult(port, true, "TCP");
            } catch (Exception ex) {
                System.out.println(ex.getMessage());
                return new ObserveResult(port, false, "TCP");
            }
        };
    }

    public static ArrayList<String> searchForTcpPort(ThreadPoolExecutor threadExecutor, int startPort,
                                                     int lastPort, boolean isUDPSearch)
            throws ExecutionException, InterruptedException {
        final List<Future<ObserveResult>> observeResults = new ArrayList<>();
        for (var i=startPort; i<=lastPort; i++)
        {
            if (isUDPSearch)
                observeResults.add(threadExecutor.submit(ObserveUdpPortFuture(i)));
            else
                observeResults.add(threadExecutor.submit(ObserveTcpPortFuture(i)));
        }
        threadExecutor.shutdown();
        var resultList = new ArrayList<String>();
        for (Future<ObserveResult> result:observeResults) {
            resultList.add(makeReportMessage(result.get()));
        }
        return resultList;
    }

    private static String makeReportMessage(ObserveResult observeResult) {
        return ("Protocol type: " + observeResult.protocol + " " +
                "port: " + observeResult.port + " " + "is open: " + observeResult.isOpen);
    }
}
