import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.Socket;

public class RunNameServer {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java RunNameServer <file>");
            return;
        }
        String fileName = args[0];
        int id = 0;
        int port = 0;
        String ip = "";
        try {
            ip = java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (java.net.UnknownHostException e) {
            System.out.println("Error retrieving local IP address: " + e.getMessage());
            return;
        }
        String bnsIp = "";
        int bnsPort = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            id = Integer.parseInt(br.readLine().trim());
            port = Integer.parseInt(br.readLine().trim());
            String[] bnsDetails = br.readLine().trim().split(" ");
            bnsIp = bnsDetails[0];
            bnsPort = Integer.parseInt(bnsDetails[1]);
        } catch (IOException | NumberFormatException e) {
            System.out.println("Error reading or parsing the file: " + e.getMessage());
            return;
        }

        System.out.println("ID: " + id + ", Port: " + port);
        System.out.println("BNS IP: " + bnsIp + ", BNS Port: " + bnsPort);

        NameServer ns = new NameServer(id, ip, port);
        ns.setBN(bnsIp, bnsPort);
        ns.start();
        try (BufferedReader userInput = new BufferedReader(new java.io.InputStreamReader(System.in))) {
            String input;
            while (true) {
                input = userInput.readLine().trim();
                if ("exit".equalsIgnoreCase(input)) {
                    ns.remove();
                    System.out.println("Handed off range: (" + ns.predecessorId + ", " + ns.id + "]");
                    System.out.println("Successful exit");
                } else if (input.startsWith("enter")) {
                    ns.sendMessage("placeNode " + id + " " + ip + " " + port, bnsIp, bnsPort);
                    System.out.println("Node range: (" + ns.predecessorId + ", " + ns.id + "]");
                } else if (input.startsWith("quit")) {
                    return;
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading user input: " + e.getMessage());
        }
    }
}