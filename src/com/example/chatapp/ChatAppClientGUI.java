package com.example.chatapp;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import javax.sound.sampled.*;

public class ChatAppClientGUI {
    private String serverAddress = "localhost";  // Server address
    private int port = 1234;  // Server port
    private String username;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    // GUI components
    private JFrame frame;
    private JTextArea messageArea;
    private JTextField inputField;
    private JButton sendButton;

    public ChatAppClientGUI() {
        // Get the system username
        username = System.getProperty("user.name");

        setupGUI();      // Build the GUI
        setupNetworking(); // Establish network connection
    }

    private void setupGUI() {
        // Create the frame
        frame = new JFrame("ChatApp - Client (" + username + ")");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 400);
        frame.setLayout(new BorderLayout());

        // Message display area
        messageArea = new JTextArea();
        messageArea.setEditable(false);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(messageArea);
        frame.add(scrollPane, BorderLayout.CENTER);

        // Input panel with input field and send button
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputField = new JTextField();
        sendButton = new JButton("Send");

        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        frame.add(inputPanel, BorderLayout.SOUTH);

        // Add action listeners for sending messages
        ActionListener sendListener = e -> sendMessage();
        sendButton.addActionListener(sendListener);
        inputField.addActionListener(sendListener);

        // Ensure connections are closed when window is closed
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closeConnections();
                super.windowClosing(e);
            }
        });

        frame.setVisible(true);
    }

    private void setupNetworking() {
        try {
            socket = new Socket(serverAddress, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Send the username to the server as the first message
            out.println(username);

            // Thread to listen for messages from the server
            new Thread(this::listenForMessages).start();
        } catch (IOException e) {
            showError("Unable to connect to server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void sendMessage() {
        String message = inputField.getText().trim();
        if (!message.isEmpty()) {
            // Display the user's own message on the EDT
            SwingUtilities.invokeLater(() -> {
                messageArea.append("You: " + message + "\n");
                messageArea.setCaretPosition(messageArea.getDocument().getLength());
            });
            // Send the message to the server
            out.println(message);
            inputField.setText("");  // Clear the input field
        }
    }

    private void listenForMessages() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                final String received = message;
                // Update the GUI on the EDT
                SwingUtilities.invokeLater(() -> {
                    messageArea.append(received + "\n");
                    messageArea.setCaretPosition(messageArea.getDocument().getLength());
                });
                // Play the notification sound for incoming messages
                playBeep();
            }
        } catch (IOException e) {
            SwingUtilities.invokeLater(() -> messageArea.append("Connection closed.\n"));
        } finally {
            closeConnections();
        }
    }

    private void playBeep() {
        try {
            // Load the sound file (beep.wav should be in the project directory)
            File soundFile = new File("beep.wav");
            if (!soundFile.exists()) {
                System.err.println("Sound file not found: beep.wav");
                return;
            }
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(soundFile);
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            clip.start();  // Play the sound
        } catch (Exception e) {
            System.err.println("Error playing sound: " + e.getMessage());
        }
    }

    private void closeConnections() {
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.err.println("Error closing connections: " + e.getMessage());
        }
    }

    private void showError(String errorMessage) {
        JOptionPane.showMessageDialog(frame, errorMessage, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ChatAppClientGUI::new);  // Launch GUI in the Swing thread
    }
}