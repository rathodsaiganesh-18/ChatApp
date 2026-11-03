import java.io.*;
import java.net.*;
//import java.util.*;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class ChatServer {
    
    private static final int PORT = 8080;
    private static final String USERS_FILE = "users.txt";

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("[SERVER] Server started on port " + PORT);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("[SERVER] New client connected: " + clientSocket);
                new ClientHandler(clientSocket).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static boolean isValidUser(String username, String password) {
        try (BufferedReader reader = new BufferedReader(new FileReader(USERS_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 2 && parts[0].equals(username) && parts[1].equals(password)) {
                    return true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static boolean registerUser(String username, String password) {
        try (BufferedReader reader = new BufferedReader(new FileReader(USERS_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length > 0 && parts[0].equals(username)) {
                    return false;
                }
            }
        } catch (IOException e) {
            // Ignore if file doesn't exist
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(USERS_FILE, true))) {
            writer.write(username + "," + password);
            writer.newLine();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    static class ClientHandler extends Thread {
        private Socket socket;
        private String username;
        private PrintWriter out;
        private BufferedReader in;

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                String command = in.readLine();
                if ("REGISTER".equals(command)) {
                    String user = in.readLine();
                    String pass = in.readLine();
                    if (registerUser(user, pass)) {
                        out.println("REGISTERED");
                    } else {
                        out.println("EXISTS");
                    }
                } else if ("LOGIN".equals(command)) {
                    String user = in.readLine();
                    String pass = in.readLine();
                    if (isValidUser(user, pass)) {
                        out.println("SUCCESS");
                        username = user;
                        handleClient();
                    } else {
                        out.println("FAIL");
                    }
                }
            } catch (IOException e) {
                System.out.println("[SERVER] Client disconnected or error occurred.");
            }
        }

        private void handleClient() throws IOException {
            String line;
            while ((line = in.readLine()) != null) {
                if ("GET_HISTORY_AND_USERS".equals(line)) {
                    sendUsersAndChatHistory();
                } else {
                    String recipient = line;
                    String message = in.readLine();
                    saveMessage(username, recipient, message);
                }
            }
        }

        private void saveMessage(String from, String to, String message) {
            String formatted = from + ": " + message;
            try {
                String encrypted = encrypt(message);
                System.out.println("[SERVER] Encrypted message from " + from + " to " + to + ": " + encrypted);
                File f1 = new File("chats/" + from);
                f1.mkdirs();
                File toFile = new File(f1, to + ".txt");
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(toFile, true))) {
                    writer.write(formatted);
                    writer.newLine();
                }

                File f2 = new File("chats/" + to);
                f2.mkdirs();
                File fromFile = new File(f2, from + ".txt");
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(fromFile, true))) {
                    writer.write(formatted);
                    writer.newLine();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        private String decrypt(String encryptedMsg) throws Exception {
            Cipher cipher = Cipher.getInstance("AES");
            SecretKeySpec key = new SecretKeySpec(SECRET_KEY.getBytes(), "AES");
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decoded = Base64.getDecoder().decode(encryptedMsg);
            byte[] decrypted = cipher.doFinal(decoded);
            return new String(decrypted);
        }
       
        private void sendUsersAndChatHistory() throws IOException {
            out.println("USERS");
            try (BufferedReader reader = new BufferedReader(new FileReader(USERS_FILE))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(",");
                    if (!parts[0].equals(username)) {
                        out.println(parts[0]);
                    }
                }
            }
            out.println("END_USERS");

            File userFolder = new File("chats/" + username);
            if (!userFolder.exists()) {
                out.println("END_ALL");
                return;
            }

            File[] files = userFolder.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isFile()) {
                        String contact = f.getName().replace(".txt", "");
                        out.println("CHAT_WITH:" + contact);
                        try (BufferedReader chatReader = new BufferedReader(new FileReader(f))) {
                            String chatLine;
                            while ((chatLine = chatReader.readLine()) != null) {
                                out.println(chatLine);
                            }
                        }
                        out.println("END_CHAT");
                    }
                }
            }
            out.println("END_ALL");
        }

        private static final String SECRET_KEY = "1234567890123456";

        private String encrypt(String msg) throws Exception {
            Cipher cipher = Cipher.getInstance("AES");
            SecretKeySpec key = new SecretKeySpec(SECRET_KEY.getBytes(), "AES");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encrypted = cipher.doFinal(msg.getBytes());
            return Base64.getEncoder().encodeToString(encrypted);
        }
    }
}
