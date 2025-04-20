import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

public class NameServer {
    public volatile int id;
    public volatile String ip;
    public volatile int port;
    public volatile HashMap<Integer, String> map = new HashMap<>();
    public volatile int successorId;
    public volatile String successorIp;
    public volatile int successorPort;
    public volatile int predecessorId;
    public volatile String predecessorIp;
    public volatile int predecessorPort;
    public volatile int bnPort;
    public volatile String bnIp;

    public NameServer(int id , String ip, int port) {
        this.id = id;
        this.ip = ip;
        this.port = port;
        this.successorIp = "";
        this.successorPort = -1; 
        this.predecessorIp = "";
        this.predecessorPort = -1;
        this.bnIp = "";
        this.bnPort = -1;      
    }
    public void setSuccessor(int id, String ip, int port) {
        this.successorId = id;
        this.successorIp = ip;
        this.successorPort = port;
        System.out.println("Successor set to #" + id + " at " + ip + ":" + port);
    }
    public void setPredecessor(int id, String ip, int port) {
        this.predecessorId = id;
        this.predecessorIp = ip;
        this.predecessorPort = port;
        System.out.println("Predecessor set to #" + id + " at " + ip + ":" + port);
    }
    public void setBN(String ip, int port) {
        this.bnIp = ip;
        this.bnPort = port;
    }

    public void start() {
        new Thread(() -> {
            try {
                // Start the server socket and listen for incoming connections
                java.net.ServerSocket serverSocket = new java.net.ServerSocket(port);
                System.out.println("Name Server started on port " + port);
                while (true) {
                    java.net.Socket clientSocket = serverSocket.accept();
                    new Thread(() -> listen(clientSocket)).start();
                }
            } catch (java.io.IOException e) {
                System.err.println("Error starting server: " + e.getMessage());
            }
        }).start();
    }

    public void listen(Socket clientSocket) {
        try {
            java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(clientSocket.getInputStream()));
            String message = in.readLine();
            if (message != null) {
                String[] parts = message.split(" ");
                if (parts[0].equals("lookup")) {
                    int key = Integer.parseInt(parts[1]);
                    lookup(key);
                } else if (parts[0].equals("insert")) {
                    int key = Integer.parseInt(parts[1]);
                    String value = parts[2];
                    insert(key, value);
                } else if (parts[0].equals("#insert")) {
                    System.out.println("Key migrated to this node: " + parts[1] + " " + parts[2]);
                    int key = Integer.parseInt(parts[1]);
                    String value = parts[2];
                    map.put(key, value);
                } else if (parts[0].equals("delete")) {
                    int key = Integer.parseInt(parts[1]);
                    this.delete(key);
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
                } else if (parts[0].equals("setBN")) {
                    String ip = parts[1];
                    int port = Integer.parseInt(parts[2]);
                    setBN(ip, port);
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
                } else {
                    System.out.println(message);
                }
            }
        } catch (java.io.IOException e) {
            System.err.println("Error handling client: " + e.getMessage());
        }
    }


    public void delete(int key) {
        String message = "Contacting #" + this.id + " to delete key " + key;
        System.out.println("Contacting #" + this.id + " to delete key " + key);
        if (bnPort != -1) this.sendMessage(message, this.bnIp, this.bnPort);
        else System.out.println(message);
        String value = map.remove(key);
        if (value == null) {
            this.sendMessage("delete " + key, this.successorIp, this.successorPort);
        } else {
            if (bnPort != -1) this.sendMessage("Successful deletion", this.bnIp, this.bnPort);
            else System.out.println("Successful deletion");
        }
    }

    public void lookup(int key) {
        String value = map.get(key);
        System.out.println("Contacting #" + this.id + " for key " + key);
        if (bnPort != -1) this.sendMessage("Contacting #" + this.id + " for key " + key, this.bnIp, this.bnPort);
        if (value != null) {
            Socket socket = null;
            try {
                socket = new Socket(bnIp, bnPort);
                String message = "found " + value;
                socket.getOutputStream().write(message.getBytes());
                socket.close();
            } catch (java.io.IOException e) {
                System.err.println("Error connecting to successor: " + e.getMessage());
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (java.io.IOException e) {
                        System.err.println("Error closing socket: " + e.getMessage());
                    }
                }
            }
            return;
        } else {
            // If the key is not found, check the successor
            try (java.net.Socket socket = new java.net.Socket(successorIp, successorPort)) {
                String message = "lookup " + key;
                socket.getOutputStream().write(message.getBytes());
                return;
            } catch (java.io.IOException e) {
                System.err.println("Error connecting to successor: " + e.getMessage());
            }
            }
    }

    public void insert(int key, String value) {
        String message = "Contacting #" + this.id + " to place " + key + " " + value;
        if (bnPort != - 1) this.sendMessage(message, this.bnIp, this.bnPort);
        else System.out.println(message);
        if (key <= this.id) {
            try (Socket socket = new Socket(bnIp, bnPort)) {
                message = "!insert " + this.id;
                socket.getOutputStream().write(message.getBytes());
            } catch (java.io.IOException e) {
                System.err.println("Error connecting to BNS: " + e.getMessage());
            }
            map.put(key, value);
            System.out.println("Key inserted " + key + " " + value);
        } else {
            if (!successorIp.isEmpty() && successorPort != -1) {
                try (Socket socket = new Socket(successorIp, successorPort)) {
                    message = "insert " + key + " " + value;
                    socket.getOutputStream().write(message.getBytes());
                } catch (java.io.IOException e) {
                    System.err.println("Error connecting to successor: " + e.getMessage());
                }
            }
        }
    }

    public void sendMessage(String message, String ip, int port) {
        try (Socket socket = new Socket(ip, port)) {
            socket.getOutputStream().write(message.getBytes());
        } catch (java.io.IOException e) {
            System.err.println("Error sending message: " + e.getMessage());
        }
    }

    public void remove() {
        if (successorIp == predecessorIp && successorPort == predecessorPort) {
            this.sendMessage("setPredecessor -1 0 -1", this.bnIp, this.bnPort);
            this.sendMessage("setSuccessor -1 0 -1", this.bnIp, this.bnPort);
        } else {
            this.sendMessage("setPredecessor " + this.predecessorId + " " + this.predecessorIp + " " + this.predecessorPort, this.successorIp, this.successorPort);
            this.sendMessage("setSuccessor " + this.successorId + " " + this.successorIp + " " + this.successorPort, this.predecessorIp, this.predecessorPort);
        }
        for (int key : map.keySet()) {
            String value = map.get(key);
            if (value != null) {
                System.out.println("Migrating key value: " + key + " " + value + " to successor");
                this.sendMessage("#insert " + key + " " + value, this.successorIp, this.successorPort);
            }
        }
    }

    public void placeNode(int id, String ip, int port) {
        String message = "Contacting #" + this.id + " to place node #" + id;
        this.sendMessage(message, ip, port);
        System.out.println(message);
        if (id <= this.id) {
            this.sendMessage("setSuccessor " + this.id + " " + this.ip + " " + this.port, ip, port);
            this.sendMessage("setPredecessor " + this.predecessorId + " " + this.predecessorIp + " " + this.predecessorPort, ip, port);
            try (Socket socket = new Socket(predecessorIp, predecessorPort)) {
                message = "setSuccessor " + id + " " + ip + " " + port;
                socket.getOutputStream().write(message.getBytes());
                setPredecessor(id, ip, port);
                this.sendMessage("migrate", this.ip, this.port);
            } catch (java.io.IOException e) {
                System.err.println("Error connecting to BNS: " + e.getMessage());
            }
        } else if (successorIp.equals(bnIp) && successorPort == bnPort) {
            this.sendMessage("setSuccessor " + 0 + " " + this.bnIp + " " + this.bnPort, ip, port);
            this.sendMessage("setPredecessor " + this.id + " " + this.ip + " " + this.port + "", ip, port);
          
            try (Socket socket = new Socket(bnIp, bnPort)) {
                message = "setPredecessor " + id + " " + ip + " " + port;
                socket.getOutputStream().write(message.getBytes());
            } catch (java.io.IOException e) {
                System.err.println("Error connecting to BNS: " + e.getMessage());
            }
            this.setSuccessor(id, ip, port);
            this.sendMessage("migrate", this.bnIp, this.bnPort);
        } else {
            if (!successorIp.isEmpty() && successorPort != -1) {
                try (Socket socket = new Socket(successorIp, successorPort)) {
                    message = "placeNode " + id + " " + ip + " " + port;
                    socket.getOutputStream().write(message.getBytes());
                } catch (java.io.IOException e) {
                    System.err.println("Error connecting to successor: " + e.getMessage());
                }
            }
        }
    }





}