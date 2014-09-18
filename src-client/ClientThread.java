import java.io.*;
import java.net.*;

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
public class ClientThread extends Thread {
	private Socket socket = null;
	private Client client = null;
	private DataInputStream streamIn = null;
	private boolean run = false;

	// Constructor
	public ClientThread(Client _client, Socket _socket) {
		client = _client;
		socket = _socket;
		open();
		start();
	}

	// Open all necessary streams/threads
	public void open() {
		try {
			streamIn = new DataInputStream(socket.getInputStream());
		} catch(IOException e) {
			System.out.println("Error getting input stream: " + e);
			client.stop();
		}
		run = true;
	}

	// Close the streams
	public void close() throws IOException {
		if(streamIn != null) { streamIn.close(); }
		run = false;
	}

	// The run method which will be called every frame
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
