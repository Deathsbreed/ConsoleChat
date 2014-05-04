import java.net.*;
import java.io.*;
import java.lang.*;

/**
 * @author Nicolás A. Ortega
 * @license GNU GPLv3
 * @year 2014
 * 
 * For details on the copyright, look at the COPYRIGHT file that came with
 * this program.
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
			Client client = new Client(args[0], Integer.parseInt(args[1]));
		}
	}

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

	public void run() {
		while(thread != null) {
			try {
				streamOut.writeUTF(console.readLine());
				streamOut.flush();
			} catch(IOException e) {
				System.out.println("Sending error: " + e.getMessage());
				stop();
			}
		}
	}

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

	private void start() throws IOException {
		console = new BufferedReader(new InputStreamReader(System.in));
		streamOut = new DataOutputStream(socket.getOutputStream());
		
		if(thread == null) {
			cThread = new ClientThread(this, socket);
			thread = new Thread(this);
			thread.start();
		}
	}

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