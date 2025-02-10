package com.example.chatapp;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class ChatAppServer {
    // Use a thread-safe set for client handlers.
    private static Set<ClientHandler> clientHandlers = ConcurrentHashMap.newKeySet();

    public static void main(String[] args) {
        int port = 1234;  // Server port

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Chat server started on port " + port + "...");
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New client connected: " + socket);
                ClientHandler clientHandler = new ClientHandler(socket);
                clientHandlers.add(clientHandler);
                new Thread(clientHandler).start();  // Handle client in a new thread
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Broadcast a message to clients.
     * 
     * If the sender is null, the message is sent to all clients.
     * Otherwise, the sender is skipped (useful for chat messages where the sender already displays it locally).
     */
    static void broadcastMessage(String message, ClientHandler sender) {
        for (ClientHandler client : clientHandlers) {
            if (sender == null || client != sender) {
                client.sendMessage(message);
            }
        }
    }

    static class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String username;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // Receive the first message as the username
                username = in.readLine();
                System.out.println(username + " has joined the chat.");

                // Broadcast join message to all clients (including the new client)
                ChatAppServer.broadcastMessage("Server: " + username + " has joined the chat.", null);

                String clientMessage;
                while ((clientMessage = in.readLine()) != null) {
                    String formattedMessage = username + ": " + clientMessage;
                    System.out.println(formattedMessage);
                    // Broadcast chat messages only to others (sender will display their own message)
                    ChatAppServer.broadcastMessage(formattedMessage, this);
                }
            } catch (IOException e) {
                System.out.println("Client disconnected: " + socket);
            } finally {
                cleanup();
            }
        }

        void sendMessage(String message) {
            out.println(message);
        }

        private void cleanup() {
            // Remove this client handler from the set first so that the leaving client won't receive the broadcast.
            clientHandlers.remove(this);
            
            // If username is known, broadcast that the client has left.
            if (username != null) {
                ChatAppServer.broadcastMessage("Server: " + username + " has left the chat.", null);
                System.out.println(username + " has left the chat.");
            }
            
            try {
                if (socket != null && !socket.isClosed()) socket.close();
                if (in != null) in.close();
                if (out != null) out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}