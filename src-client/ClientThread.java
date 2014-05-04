import java.io.*;
import java.net.*;

/**
 * @author Nicolás A. Ortega
 * @license GNU GPLv3
 * @year 2014
 * 
 * For details on the copyright, look at the COPYRIGHT file that came with
 * this program.
 * 
 */
public class ClientThread extends Thread {
	private Socket socket = null;
	private Client client = null;
	private DataInputStream streamIn = null;
	private boolean run = false;

	public ClientThread(Client _client, Socket _socket) {
		client = _client;
		socket = _socket;
		open();
		start();
	}

	public void open() {
		try {
			streamIn = new DataInputStream(socket.getInputStream());
		} catch(IOException e) {
			System.out.println("Error getting input stream: " + e);
			client.stop();
		}
		run = true;
	}

	public void close() throws IOException {
		if(streamIn != null) { streamIn.close(); }
		run = false;
	}

	public void run() {
		while(run) {
			try {
				client.handle(streamIn.readUTF());
			} catch(IOException e) {
				System.out.println("Listening error: " + e.getMessage());
				client.stop();
			}
		}
	}
}