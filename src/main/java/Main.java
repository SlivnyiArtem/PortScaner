import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

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
                notFinalStartIndex = jsonObject.getInt("FirstPort");
                notFinalFinishIndex = jsonObject.getInt("LastPort");
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
            var resultList = ObservePorts(threadExecutor, startIndex, finishIndex, isUdpSearch);
            for (String result:resultList) {
                System.out.println(result);
            }
        } catch (ExecutionException e) {
            System.out.println("An error occurred while accessing the port");
        } catch (InterruptedException exception) {
            System.out.println("A program was interrupted");
        } finally {
            threadExecutor.shutdownNow();
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
                udpSocket.receive(new DatagramPacket(new byte[50], 50));
                return new ObserveResult(port, false, "UDP");
            } catch (SocketTimeoutException timeoutExc) {
                return new ObserveResult(port, true, "UDP");
            } catch (PortUnreachableException exception){
                return new ObserveResult(port, false, "UDP");
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
                return new ObserveResult(port, false, "TCP");
            }
        };
    }

    public static ArrayList<String> ObservePorts(ThreadPoolExecutor threadExecutor, int startPort,
                                                 int lastPort, boolean isUDPSearch)
            throws ExecutionException, InterruptedException {
        final Map<Integer, Future<ObserveResult>> observeResults = new HashMap<>();
        for (var i=startPort; i<=lastPort; i++)
        {
            if (isUDPSearch)
                observeResults.put(i, threadExecutor.submit(ObserveUdpPortFuture(i)));
            else
                observeResults.put(i, threadExecutor.submit(ObserveTcpPortFuture(i)));
        }
        threadExecutor.shutdown();
        var resultList = new ArrayList<String>();
        for (Map.Entry<Integer, Future<ObserveResult>> observeResult:observeResults.entrySet()) {
            try {
                var result = observeResult.getValue().get(500, TimeUnit.MILLISECONDS);
                resultList.add(makeReportMessage(result));
            }
            catch (TimeoutException timeException){
                resultList.add(makeReportMessage(new ObserveResult(observeResult.getKey(), false, "UDP")));
            }
        }
        return resultList;
    }

    private static String makeReportMessage(ObserveResult observeResult) {
        return ("Protocol type: " + observeResult.protocol + " " +
                "port: " + observeResult.port + " " + "is open: " + observeResult.isOpen);
    }
}
