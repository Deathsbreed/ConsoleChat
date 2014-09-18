import java.net.*;
import java.io.*;
import java.lang.*;

/**
 * @author Nicolás A. Ortega
 * @copyright Nicolás A. Ortega
 * @license MIT
 * @year 2014
 * 
 */
public class Client implements Runnable {
	private String version = "v1.0";
	private Socket socket = null;
	private ClientThread cThread = null;
	private DataOutputStream streamOut = null;
	private BufferedReader console = null;
	private Thread thread = null;

	public static void main(String[] args) {
		if(args.length != 2) {
			System.out.println("Usage: java Client [server] [port]");
		} else {
			new Client(args[0], Integer.parseInt(args[1]));
		}
	}

	// Constructor method
	public Client(String server, int port) {
		System.out.println("ConsoleChat client " + version + " Copyright (C) 2014 Nicolás A. Ortega\n" +
			"This program comes with ABSOLUTELY NO WARRANTY; details in WARRANTY file.\n" +
			"This is free software, and you are welcome to redistribute it\n" +
			"under certain conditions; details in LICENSE file.\n");

		try {
			// Create a new socket connection
			System.out.println("Connecting to server...");
			socket = new Socket(server, port);
			System.out.println("Connected!");
			start();
		} catch(UnknownHostException uhe) {
			System.out.println("Host unknown: " + uhe.getMessage());
		} catch(IOException e) {
			System.out.println("Unknown exception: " + e.getMessage());
		}
	}

	// The run method containing the main loop
	public void run() {
		String uinput;
		while(thread != null) {
			try {
				uinput = console.readLine();
				if(uinput.equals("/clientVersion")) {
					System.out.println(version);
				} else {
					streamOut.writeUTF(uinput);
					streamOut.flush();
				}
			} catch(IOException e) {
				System.out.println("Sending error: " + e.getMessage());
				stop();
			}
		}
	}

	// Handle messages
	public void handle(String msg) {
		if(msg.equals("/quit")) {
			System.out.println("Goodbye bye. Press RETURN to exit...");
			stop();
		} else {
			System.out.println(msg);
			if(msg.length() > 6 && msg.substring(0, 5).equals("Kick:")) {
				stop();
			}
		}
	}

	// Open and start all necessary threads
	private void start() throws IOException {
		console = new BufferedReader(new InputStreamReader(System.in));
		streamOut = new DataOutputStream(socket.getOutputStream());
		
		if(thread == null) {
			cThread = new ClientThread(this, socket);
			thread = new Thread(this);
			thread.start();
		}
	}

	// Stop and close all necessary threads
	public void stop() {
		if(thread != null) {
			thread.interrupt();
			thread = null;
		}

		try {
			if(console != null) { console.close(); }
			if(streamOut != null) { streamOut.close(); }
			if(socket != null) { socket.close(); }
		} catch(IOException e) {
			System.out.println("Error closing...");
		}

		try {
			cThread.close();
		} catch(IOException e) {
			System.out.println("Error closing the thread: " + e);
		}
		cThread.interrupt();
	}
}
