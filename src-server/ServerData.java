import java.io.*;

/**
 * @author Nicolás A. Ortega
 * @license GNU GPLv3
 * @year 2014
 * 
 * For details on the copyright, look at the COPYRIGHT file that came with
 * this program.
 * 
 */
public class ServerData {
	private File admin = null;
	private FileWriter adminOut = null;
	private BufferedReader adminIn = null;

	public ServerData(Server server) {
		admin = new File("adminpasswd.data");
		if(!admin.exists()) {
			try {
				admin.createNewFile();
				adminOut = new FileWriter(admin, true);
				adminOut.append(server.getPasswd());
			} catch(IOException e) {
				System.out.println("Error writing to admin file: " + e.getMessage());
			}
		} else {
			try {
				adminIn = new BufferedReader(new FileReader(admin));
				server.setPasswd(adminIn.readLine());
				System.out.println(server.getPasswd());
			} catch(IOException e) {
				System.out.println("Error reading admin file: " + e.getMessage());
			}
		}
	}
}