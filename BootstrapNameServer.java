import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

public class BootstrapNameServer extends NameServer {
    public BootstrapNameServer(int id, String ip, int port) {
        super(id, ip, port);
    }


    public void acceptCommands() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("> ");
            String command = scanner.nextLine();
            if (command.equals("exit")) {
                break;
            }
            String[] parts = command.split(" ");
            if (parts[0].equals("lookup")) {
                System.out.println("Contacting #" + this.id + " to lookup key " + parts[1]);
                int key = Integer.parseInt(parts[1]);
                String value = map.get(key);
                if (value != null) {
                    System.out.println("found " + value);
                } else if (successorPort != -1) {
                    this.sendMessage(command, this.successorIp, this.successorPort);
                } else {
                    System.out.println("Key not found");
                }
            } else if (parts[0].equals("insert")) {
                int key = Integer.parseInt(parts[1]);
                String value = parts[2];
                insert(key, value);
            } else if (parts[0].equals("delete")) {
                int key = Integer.parseInt(parts[1]);
                delete(key);
            } else if (parts[0].equals("setSuccessor")) {
                int id = Integer.parseInt(parts[1]);
                String ip = parts[2];
                int port = Integer.parseInt(parts[3]);
                setSuccessor(id, ip, port);
            } else if (parts[0].equals("setPredecessor")) {
                int id = Integer.parseInt(parts[1]);
                String ip = parts[2];
                int port = Integer.parseInt(parts[3]);
                setPredecessor(id, ip, port);
            } else {
                System.out.println("Unknown command: " + command);
            }
        }
        scanner.close();
    }

    @Override
    public void insert(int key, String value) {
        System.out.println("Contacting #" + this.id + " to insert key " + key);
        if (key <= this.id || successorIp.equals("") || successorPort == -1) {
            map.put(key, value);
            System.out.println("Key inserted " + key + " " + value);
        } else {
            this.sendMessage("insert " + key + " " + value, this.successorIp, this.successorPort);
        }
    }
    


    @Override
    public void listen(Socket clientSocket) {
        try {
            java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(clientSocket.getInputStream()));
            String message = in.readLine();
            System.out.println("Received message: " + message);
            if (message != null) {
                String[] parts = message.split(" ");
                if (parts[0].equals("lookup")) {
                    System.out.println("Key not found");
                } else if (parts[0].equals("insert")) {
                    int key = Integer.parseInt(parts[1]);
                    String value = parts[2];
                    System.out.println("Key inserted " + key + " " + value);
                    map.put(key, value);
                } else if (parts[0].equals("!insert")) {
                    int id = Integer.parseInt(parts[1]);
                    System.out.println("Key inserted at " + id);
                } else if (parts[0].equals("#insert")) {
                    int key = Integer.parseInt(parts[1]);
                    String value = parts[2];
                    System.out.println("Key migrated to this node: " + key + " " + value);
                    map.put(key, value);
                } else if (parts[0].equals("delete")) {
                    System.out.println("Key not found");
                } else if (parts[0].equals("setSuccessor")) {
                    int id = Integer.parseInt(parts[1]);
                    String ip = parts[2];
                    int port = Integer.parseInt(parts[3]);
                    setSuccessor(id, ip, port);
                } else if (parts[0].equals("setPredecessor")) {
                    int id = Integer.parseInt(parts[1]);
                    String ip = parts[2];
                    int port = Integer.parseInt(parts[3]);
                    setPredecessor(id, ip, port);
                } else if (parts[0].equals("placeNode")) {
                    int id = Integer.parseInt(parts[1]);
                    String ip = parts[2];
                    int port = Integer.parseInt(parts[3]);
                    placeNode(id, ip, port);
                } else if (parts[0].equals("migrate")) {
                    ArrayList<Integer> keysToMigrate = new ArrayList<>();
                    for (int key : map.keySet()) {
                        String value = map.get(key);
                        if (value != null && key <= this.predecessorId) {
                            System.out.println("Migrating key value: " + key + " " + value + " to predecessor");
                            this.sendMessage("#insert " + key + " " + value, this.predecessorIp, this.predecessorPort);
                            keysToMigrate.add(key);
                        }
                    }
                    for (int key : keysToMigrate) {
                        map.remove(key);
                    }
                }
            }
        } catch (java.io.IOException e) {
            System.err.println("Error handling client: " + e.getMessage());
        }
    }

    @Override
    public void placeNode(int id, String ip, int port) {
        if (successorPort == -1) {
            System.out.println("Contacting #" + this.id + " to place node " + id);
            this.sendMessage("setSuccessor " + this.id + " " + this.ip + " " + this.port, ip, port);
            this.sendMessage("setPredecessor " + this.id + " " + this.ip + " " + this.port, ip, port);
            this.setSuccessor(id, ip, port);
            this.setPredecessor(id, ip, port);
          
            this.sendMessage("migrate", this.ip, this.port);
        } else {
            super.placeNode(id, ip, port);
        }
        System.out.println("Node " + id + " has been placed");
    }

    
}