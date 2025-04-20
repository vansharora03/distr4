import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class RunBootstrapNameServer {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java RunBootstrapNameServer <file>");
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
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(fileName));
            id = Integer.parseInt(br.readLine().trim());
            port = Integer.parseInt(br.readLine().trim());
        } catch (IOException | NumberFormatException e) {
            System.out.println("Error reading or parsing the file: " + e.getMessage());
            return;
        }
        


        BootstrapNameServer bns = new BootstrapNameServer(id, ip, port);
        String line;
        try {
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(" ", 2);
                if (parts.length == 2) {
                    int key = Integer.parseInt(parts[0].trim());
                    String value = parts[1].trim();
                    bns.insert(key, value);
                } else {
                    System.out.println("Invalid line format: " + line);
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading the file: " + e.getMessage());
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException e) {
                System.out.println("Error closing the file: " + e.getMessage());
            }
        }
        bns.start();
        bns.acceptCommands();
    }
}