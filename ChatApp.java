import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;

public class ChatApp {
    private static BufferedReader in;
    private static PrintWriter out;
    private static JFrame frame;
    private static JTextArea chatArea;
    private static JTextField inputField;
    private static JComboBox<String> userList;
    private static Map<String, List<String>> chatHistory = new HashMap<>();
    private static List<String> allUsers = new ArrayList<>();
    private static JPanel leftPanel;
    private static String currentUser = "";

    public static void main(String[] args) {
        System.out.println("[CLIENT] Starting ChatApp...");
        try {
            Socket socket = new Socket("localhost", 8080);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            String[] options = {"Login", "Register"};
            int choice = JOptionPane.showOptionDialog(null, "Choose an option", "Chat Login",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);

            String username = JOptionPane.showInputDialog("Enter username:");
            currentUser = username;
            String password = JOptionPane.showInputDialog("Enter password:");

            if (choice == 1) {
                out.println("REGISTER");
                out.println(username);
                out.println(password);
                String response = in.readLine();
                if (!"REGISTERED".equals(response)) {
                    JOptionPane.showMessageDialog(null, "User already exists.");
                    return;
                } else {
                    JOptionPane.showMessageDialog(null, "Registered successfully. Please login.");
                    return;
                }
            } else {
                out.println("LOGIN");
                out.println(username);
                out.println(password);
                String response = in.readLine();
                if (!"SUCCESS".equals(response)) {
                    JOptionPane.showMessageDialog(null, "Login failed.");
                    return;
                }
                out.println("GET_HISTORY_AND_USERS");
                loadUsersAndChats();
                createChatWindow(username);
                new Thread(ChatApp::pollForNewMessages).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void loadUsersAndChats() throws IOException {
        String line;
        while (!(line = in.readLine()).equals("END_ALL")) {
            if ("USERS".equals(line)) {
                while (!(line = in.readLine()).equals("END_USERS")) {
                    allUsers.add(line);
                }
            } else if (line.startsWith("CHAT_WITH:")) {
                String contact = line.substring(10);
                List<String> messages = new ArrayList<>();
                while (!(line = in.readLine()).equals("END_CHAT")) {
                    messages.add(line);
                }
                chatHistory.put(contact, messages);
            }
        }
    }

    private static void createChatWindow(String username) {
        frame = new JFrame("Chat - " + username);
        frame.setLayout(new BorderLayout());

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setFont(new Font("Monospaced", Font.PLAIN, 14));

        inputField = new JTextField();
        JButton sendButton = new JButton("Send");
        sendButton.addActionListener(e -> sendMessage(username));
        inputField.addActionListener(e -> sendMessage(username));

        userList = new JComboBox<>(allUsers.toArray(new String[0]));
        userList.addActionListener(e -> updateChatArea());

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(userList, BorderLayout.WEST);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(inputField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);

        leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        updateLeftPanel();

        frame.add(topPanel, BorderLayout.NORTH);
        frame.add(bottomPanel, BorderLayout.SOUTH);
        frame.add(new JScrollPane(chatArea), BorderLayout.CENTER);
        frame.add(new JScrollPane(leftPanel), BorderLayout.WEST);

        frame.setSize(700, 500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        updateChatArea();
    }

    private static void updateChatArea() {
        String selectedUser = (String) userList.getSelectedItem();
        chatArea.setText("");
        if (selectedUser != null && chatHistory.containsKey(selectedUser)) {
            for (String msg : chatHistory.get(selectedUser)) {
                chatArea.append(msg + "\n");
            }
        }
    }

    private static void sendMessage(String fromUser) {
        String toUser = (String) userList.getSelectedItem();
        String message = inputField.getText().trim();

        System.out.println("Decrypted message from " + fromUser + " to " + toUser + ": " +message);
        if (toUser != null && !message.isEmpty()) {
            out.println(toUser);
            out.println(message);
            String formatted = fromUser + ": " + message;
            chatArea.append(formatted + "\n");
            chatHistory.computeIfAbsent(toUser, k -> new ArrayList<>()).add(formatted);
            updateLeftPanel();
            inputField.setText("");
        }
    }

    private static void updateLeftPanel() {
        leftPanel.removeAll();
        for (String user : chatHistory.keySet()) {
            JButton btn = new JButton(user);
            btn.addActionListener(e -> {
                userList.setSelectedItem(user);
                updateChatArea();
            });
            leftPanel.add(btn);
        }
        leftPanel.revalidate();
        leftPanel.repaint();
    }

    private static void pollForNewMessages() {
        try {
            while (true) {
                Thread.sleep(2000); // Poll every 2 seconds
                out.println("GET_HISTORY_AND_USERS");
                loadUsersAndChats();
                SwingUtilities.invokeLater(ChatApp::updateChatArea);
                SwingUtilities.invokeLater(ChatApp::updateLeftPanel);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static final String SECRET_KEY = "1234567890123456";

    private String decrypt(String encryptedMsg) throws Exception {
    Cipher cipher = Cipher.getInstance("AES");
    SecretKeySpec key = new SecretKeySpec(SECRET_KEY.getBytes(), "AES");
    cipher.init(Cipher.DECRYPT_MODE, key);
    byte[] decoded = Base64.getDecoder().decode(encryptedMsg);
    byte[] decrypted = cipher.doFinal(decoded);
    return new String(decrypted);
}
}