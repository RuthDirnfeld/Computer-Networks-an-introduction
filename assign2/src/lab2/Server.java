package lab2;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
	public static String contentPath = "src//lab2";

	private static final int MYPORT = 4950;
	
	public static void main(String[] args) throws IOException {
		/* Create socket */
		ServerSocket server = null;
		if (args.length != 0) {
			contentPath += "/" + args[0];
		} else {
			contentPath += "/inner"; // inner directory
		}

		try {
			server = new ServerSocket(MYPORT);
		} catch (IOException e) {
			System.out.println("Port is taken");
			System.exit(1);
		}
		System.out.println("***running***");
		/* When connected, execute thread */
		try {

			while (true) {
				/* Accept connection */
				Socket socket = server.accept();
				/* Creates the Client and executes the run() */
				 new Thread(new Client(socket)).start();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("Server is terminated.");
	}
}
