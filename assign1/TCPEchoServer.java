package lab1;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TCPEchoServer {
	public static final int MYPORT = 4950;

	public static void main(String[] args) throws IOException {
		/* Create socket */
		ServerSocket server = new ServerSocket(MYPORT);
		ExecutorService executor = Executors.newCachedThreadPool();
		System.out.println("***running***");
		int clientID = 0;

		while (true) {
			/* Accept connection */
			Socket socket = server.accept();

			/* When connected, execute thread */
			try {
				/* Creates the Client and executes the run() */
				executor.execute(new Client(socket, clientID)); 
				clientID++;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}

class Client implements Runnable {

	private final int BUFSIZE = 1024;
	private final int clientID;
	private Socket socket;
	private InputStream input;
	private OutputStream output;

	public Client(Socket socket, int clientID) {
		this.socket = socket;
		this.clientID = clientID;

		try {
			/* Create input stream for receiving message */
			input = new DataInputStream(this.socket.getInputStream());

			/* Create output stream for sending message */
			output = new DataOutputStream(this.socket.getOutputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void run() {
		try {
			String receivedStr = "";

			/*
			 * do until received message is empty
			 */
			do {
				byte[] buffer = new byte[BUFSIZE];
				/* Read */
				String receivedMsg;
				StringBuilder temp = new StringBuilder();
				do {
					/* Number of read bytes assigned to inBytes */
					int inBytes = input.read(buffer, 0, BUFSIZE);
					if (inBytes < 1)
						break;
					receivedMsg = new String(buffer, 0, inBytes); 
					/* Add received Part to StringBuilder */
					temp.append(receivedMsg);

				} while (input.available() > 0);
				/* End of read */

				/*
				 * Assemble into String, if message is empty the socket has
				 * disconnected
				 */
				receivedStr = temp.toString();
				if (receivedStr.length() < 1)
					break;
				/* Sending message */
				output.write(receivedStr.getBytes());

				System.out.println("TCP echo request from Client " + clientID + " || IP: " + socket.getInetAddress()
						+ " || Port: " + socket.getPort() + " || Received " + receivedStr.length()
						+ " bytes || Sent " + receivedStr.length() + " bytes || Buffer size = " + BUFSIZE + "\n");

			} while (!receivedStr.isEmpty());

			try {
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			/* catch exception, when time is out and client socket is closed */
		} catch (Exception e) {
		}
		System.out.println("--Connection for Client " + clientID + " was closed--\n");
	}
}