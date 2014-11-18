package consolechat.server;

import java.net.*;
import java.io.*;
import java.util.*;

/**
 * @author Nicolás A. Ortega
 * @copyright Nicolás A. Ortega
 * @license MIT
 * @year 2014
 * 
 */
public class Server implements Runnable {
	private String version = "v1.1";
	private ArrayList<ServerThread> clients = new ArrayList<ServerThread>();
	private ServerSocket sSocket = null;
	private Thread thread = null;
	private int clientCount = 0;
	private String passwd = "admin";
	private ArrayList<String> banned = new ArrayList<String>();

	public static void main(String[] args) {
		if(args.length != 1) {
			System.out.println("Usage: consolechat-server [port]");
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

		// Read admin password
		try {
			BufferedReader passwdbuffer = new BufferedReader(new FileReader("adminpasswd.txt"));
			try {
				passwd = passwdbuffer.readLine();
			} catch(IOException ioe) {
				System.out.println("Error reading from adminpassword.txt.");
			}
		} catch(FileNotFoundException fnfe) { System.out.println("No adminpasswd.txt file."); }

		// Read banned list
		try {
			String bannedIP;
			BufferedReader banbuffer = new BufferedReader(new FileReader("banned.txt"));
			try {
				while((bannedIP = banbuffer.readLine()) != null) {
					banned.add(bannedIP);
				}
			} catch(IOException ioe) { System.out.println("Error while reading from banned.txt"); }
		} catch(FileNotFoundException fnfe) { System.out.println("No banned.txt file."); }
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
			} else if(input.length() > 11 && input.substring(0, 4).equals("/ban")) {
				if(clients.get(findClient(id)).isAdmin()) {
					int bID = Integer.parseInt(input.substring(5, 10));
					String reason = input.substring(11);
					ban(bID, reason);
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

	// Ban a client
	public void ban(int id, String reason) {
		int pos = findClient(id);
		if(pos < 0) return;
		String clientAddress = clients.get(pos).getSocket().getRemoteSocketAddress().toString();
		String clientIP = clientAddress.substring(1, clientAddress.indexOf(':'));
		try {
			PrintWriter banwriter = new PrintWriter(new BufferedWriter(new FileWriter("banned.txt", true)));
			banwriter.println(clientIP);
			banwriter.close();
		} catch(IOException ioe) { System.out.println("Error printing client IP '" + clientIP + "' to banned.txt ."); }
		clients.get(pos).send("You have been banned from this server.\nReason: " + reason);
		remove(id);
	}

	// Remove a client
	public synchronized void remove(int id) {
		int pos = findClient(id);
		if(pos < 0) return;
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

	// Add a new client
	public void addThread(Socket socket) {
		// If IP is banned to not admit!
		String clientAddress = socket.getRemoteSocketAddress().toString();
		String clientIP = clientAddress.substring(1, clientAddress.indexOf(':'));
		for(int i = 0; i < banned.size(); i++) {
			if(clientIP.equals(banned.get(i))) return;
		}
		// Otherwise add them
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
	public void setPasswd(String npasswd) {
		this.passwd = npasswd;
		try {
			PrintWriter writer = new PrintWriter("adminpasswd.txt", "UTF-8");
			writer.println(npasswd);
			writer.close();
		} catch(UnsupportedEncodingException uee) {
			System.out.println("Error writing new password to adminpasswd.txt");
		} catch(FileNotFoundException fnfe) {
			System.out.println("Error writing new password to adminpasswd.txt");
		}
	}
}
