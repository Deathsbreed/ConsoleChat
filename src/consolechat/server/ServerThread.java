package consolechat.server;

import java.net.*;
import java.io.*;

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
public class ServerThread extends Thread {
	private Server server = null;
	private Socket socket = null;
	private int id = -1;
	private String username = "User";
	private DataInputStream streamIn = null;
	private DataOutputStream streamOut = null;
	private boolean run = false;
	private boolean admin = false;

	// Constructor method
	public ServerThread(Server _server, Socket _socket) {
		super();
		this.socket = _socket;
		this.server = _server;
		id = socket.getPort();
	}

	// Method used to send a message to this client
	public void send(String msg) {
		try {
			streamOut.writeUTF(msg);
			streamOut.flush();
		} catch(IOException e) {
			System.out.println(id + " Error sending: " + e.getMessage());
			server.remove(id);
			interrupt();
		}
	}

	// The run method which will run in a loop
	public void run() {
		System.out.println("Server thread " + id + " running.");
		while(run) {
			try {
				server.handle(id, username, streamIn.readUTF());
			} catch(IOException e) {
				System.out.println(id + " error reading: " + e.getMessage());
				server.remove(id);
				interrupt();
			}
		}
	}

	// Run a command
	public void runCommand(String command) {
		if(command.equals("/help")) {
			send(" - /admin [passwd] -- Gain admin privileges.\n" +
				" - /list -- Returns all the user's ID's and usernames.\n" +
				" - /myID -- Returns your ID.\n" +
				" - /myUserName -- Returns your username.\n" +
				" - /pm [id] [msg] -- Sends a message to only one of the clients.\n" +
				" - /setUserName [newusername] -- Change your username.\n" +
				" - /clientVersion -- Returns the version of the client.\n" +
				" - /serverVersion -- Returns the version of the server.\n" +
				" - /help -- Show this information.\n" +
				" - /quit -- Quit.\n");
			if(admin) {
				send("Admin commands:\n" +
					" - /chgpasswd [newpasswd] -- Change the server admin password.\n" +
					" - /giveadmin [id] -- Give another client admin as well.\n" +
					" - /kick [id] [reason] -- Kick out a user and state a reason for the kick.\n");
			}
		} else if(command.length() > 6 && command.substring(0, 6).equals("/admin")) {
			if(command.substring(7).equals(server.getPasswd())) {
				admin = true;
				send("You are now admin.\n");
			} else {
				send("Wrong password.\n");
			}
		} else if(command.equals("/myID")) {
			send(Integer.toString(id) + "\n");
		} else if(command.equals("/myUserName")) {
			send(username + "\n");
		} else if(command.length() > 12 && command.substring(0, 12).equals("/setUserName")) {
			if(command.length() <= 13) {
				send("You did not enter a new username.");
			} else {
				username = command.substring(13);
				send("Your username is now: " + username + "\n");
			}
		} else if(admin && command.length() > 10 && command.substring(0, 10).equals("/chgpasswd")) {
			if(command.length() > 11) {
				server.setPasswd(command.substring(11));
			} else {
				send("You did not enter a new password.\n");
			}
		} else {
			send("You have either entered an invalid command, or used an improper form.\n");
		}
	}

	// Open the streams
	public void open() throws IOException {
		streamIn = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
		streamOut = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));

		run = true;
	}

	// Close the streams
	public void close() throws IOException {
		if(socket != null) { socket.close(); }
		if(streamIn != null) { streamIn.close(); }
		if(streamOut != null) { streamOut.close(); }
		run = false;
	}

	// Getter methods
	public int getID() { return id; }
	public String getUsername() { return username; }
	public boolean isAdmin() { return admin; }

	// Setter methods
	public void setAdmin(boolean a) { this.admin = a; }
}
