import java.net.*;
import java.io.*;
import java.util.*;

/**
 * @author Nicolás A. Ortega
 * @copyright Nicolás A. Ortega
 * @license MIT
 * @year 2014
 * 
 * For details on the copyright, look at the COPYRIGHT file that came with
 * this program.
 * 
 */
public class Server implements Runnable {
	private String version = "v1.0";
	private ArrayList<ServerThread> clients = new ArrayList<ServerThread>();
	private ServerSocket sSocket = null;
	private Thread thread = null;
	private int clientCount = 0;
	private String passwd = "admin";

	public static void main(String[] args) {
		if(args.length != 1) {
			System.out.println("You are using the program incorrectly.");
		} else {
			new Server(Integer.parseInt(args[0]));
		}
	}

	// The constructor method
	public Server(int port) {
		System.out.println("ConsoleChat server " + version + " Copyright (C) 2014 Nicolás A. Ortega\n" +
			"This program comes with ABSOLUTELY NO WARRANTY; details in WARRANTY file.\n" +
			"This is free software, and you are welcome to redistribute it\n" +
			"under certain conditions; details in LICENSE file.\n");
		try {
			System.out.println("Binding to port " + port + "...");
			// Try to open a port on the specified port number
			sSocket = new ServerSocket(port);
			System.out.println("Server started: " + sSocket);
			start();
		} catch(IOException e) {
			System.out.println(e);
		}
	}

	// The run method that will be called every frame
	public void run() {
		while(thread != null) {
			try {
				System.out.println("Waiting for clients...");
				addThread(sSocket.accept());
			} catch(IOException e) {
				System.out.println("Acceptance error: " + e);
				stop();
			}
		}
	}

	// Start the server thread
	public void start() {
		if(thread == null) {
			thread = new Thread(this);
			thread.start();
		}
	}

	// Stop the server thread and all other threads
	public void stop() {
		if(thread != null) {
			thread.interrupt();
			thread = null;
		}

		for(int i = 0; i < clients.size(); i++) {
			try {
				clients.get(i).close();
			} catch(IOException e) {
				System.out.println("Error closing thread: " + e);
			}
		}
	}

	// This function loops through all the clients and returns the one with the ID entered
	public int findClient(int id) {
		for(int i = 0; i < clientCount; i++) {
			if(clients.get(i).getID() == id) {
				return i;
			}
		}

		return -1;
	}

	// Handle any messages the server recieves
	public synchronized void handle(int id, String username, String input) {
		if(input.startsWith("/")) {
			if(input.equals("/quit")) {
				clients.get(findClient(id)).send("/quit");
				remove(id);
			} else if(input.equals("/serverVersion")) {
				clients.get(findClient(id)).send(version);
			} else if(input.equals("/list")) {
				int pos = findClient(id);
				for(int i = 0; i < clients.size(); i++) {
					String list = Integer.toString(clients.get(i).getID()) + " - " + clients.get(i).getUsername() + " |";
					if(pos == i) { list += " (you)"; }
					if(clients.get(i).isAdmin()) { list += " (admin)"; }
					clients.get(pos).send(list);
				}
				clients.get(pos).send("\n");
			} else if(input.substring(0, 3).equals("/pm")) {
				int pos = findClient(Integer.parseInt(input.substring(4, 9)));
				clients.get(pos).send("<" + username + ">: " + input.substring(10));
			} else if(input.length() > 12 && input.substring(0, 5).equals("/kick")) {
				if(clients.get(findClient(id)).isAdmin()) {
					int kID = Integer.parseInt(input.substring(6, 11));
					String reason = input.substring(12);
					clients.get(findClient(kID)).send("Kick: " + reason);
					remove(kID);
				} else {
					clients.get(findClient(id)).send("You are not admin.");
				}
			} else if(input.length() > 10 && input.substring(0, 10).equals("/giveadmin")) {
				if(clients.get(findClient(id)).isAdmin()) {
					int aID = Integer.parseInt(input.substring(11));
					clients.get(findClient(aID)).setAdmin(true);
					clients.get(findClient(aID)).send("You have been made admin.\n");
				} else {
					clients.get(findClient(id)).send("You are not admin.");
				}
			} else {
				clients.get(findClient(id)).runCommand(input);
			}
		} else {
			for(int i = 0; i < clientCount; i++) {
				if(clients.get(i).getID() != id) {
					clients.get(i).send(username + ": " + input);
				}
			}
		}
	}

	// Remove a client
	public synchronized void remove(int id) {
		int pos = findClient(id);
		if(pos >= 0) {
			ServerThread toTerminate = clients.get(pos);
			System.out.println("Remove client thread: " + id + " at " + pos);
			try {
				clients.get(pos).close();
			} catch(IOException e) {
				System.out.println("Error closing thread: " + e);
			}
			clients.remove(pos);
			clientCount--;
			try {
				toTerminate.close();
			} catch(IOException e) {
				System.out.println("Error closing thread: " + e);
			}
			toTerminate.interrupt();
		}
	}

	// Add a new client
	public void addThread(Socket socket) {
		clients.add(new ServerThread(this, socket));
		try {
			clients.get(clientCount).open();
			clients.get(clientCount).start();
			clientCount++;
		} catch(IOException e) {
			System.out.println("Error opening thread: " + e);
		}
	}

	// Getter methods
	public String getPasswd() { return passwd; }

	// Setter methods
	public void setPasswd(String npasswd) { this.passwd = npasswd; }
}
