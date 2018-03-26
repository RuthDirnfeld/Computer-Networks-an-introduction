package lab1;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TCPEchoClient implements Runnable {
	
	public static int BUFSIZE = 1024;
	public static final int MYPORT = 0;
	public static byte[] buffer = new byte[BUFSIZE];
	public static final String MSG = "An Echo Message!";
	public static int transferRate;
	public static long runTime = 0;
	public static int sent = 0;
	public static Socket socket;
	public static DataInputStream input;
	public static DataOutputStream output;
	public static SocketAddress localBindPoint;
	public static SocketAddress remoteBindPoint;
	
	@Override
	public void run() {
		while (sent < transferRate) {
			start();
			try {
				Thread.sleep(1000 / transferRate);
			} catch (InterruptedException e) {
				return; 
			}
			sent++;
		}	
	}
	
	public static void main(String[] args) throws IOException {
		BUFSIZE = Integer.valueOf(args[2]);

		if (args.length != 4) {
			System.err.printf("Wrong number of arguments. Enter: IP, PORT, TRANSFERRATE, BUFSIZE");
			System.exit(1);
		}

		isValidType(args); // Error check
		
		try {
			/* Create socket */
			socket = new Socket();

			/* Create local endpoint using bind() */
			localBindPoint = new InetSocketAddress(MYPORT);
			socket.bind(localBindPoint);

			/* Create remote endpoint */
			remoteBindPoint = new InetSocketAddress(args[0], Integer.valueOf(args[1]));
			socket.connect(remoteBindPoint);
			
			/* create input stream for reading message */
			input = new DataInputStream(socket.getInputStream());

			/* create output stream for sending message */
			output = new DataOutputStream(socket.getOutputStream());
		} catch (Exception e) {
			System.err.println("***Server not running***");
			System.exit(1);
		}
		
		oneSecond(); //VG - limit to 1 second

		try {
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void start() {
		try {
			/* Send message */
			output.write(MSG.getBytes());

			String receivedStr = "";
			StringBuilder temp = new StringBuilder();
			/*
			 * do until input stream gets byte
			 */
			while (receivedStr.length() < MSG.length()) {
				buffer = new byte[BUFSIZE];

				/* Read message + save in array + get number of received bytes */
				int inBytes = input.read(buffer);

				/* Copy from buffer */
				receivedStr = new String(buffer, 0, inBytes); 
				/* Add the message into the StringBuilder */
				temp.append(receivedStr); 
			}
			/* Assemble into String */
			String receivedMsg = temp.toString();
			
			/* Compare sent and received message */
			if (receivedMsg.compareTo(MSG) == 0)
				System.out.printf("%d bytes sent and received || Buffer Size: " + BUFSIZE + " ||\n", receivedMsg.length());
			else
				System.out.printf("Sent bytes: " + MSG.length() + " Received bytes: " + receivedMsg.length() 
				+ " msg not equal! || Buffer Size: " + BUFSIZE + " ||\n");
			
		} catch (Exception e) {
			return; //catch if time is already out but socket is still sending or receiving
		}
	}
	
	private static void oneSecond() { //taken from the thread examples from OS 1DV512
		/* New thread pool */
		ExecutorService executor = Executors.newFixedThreadPool(1);

		/* Submit thread */ 
		executor.submit(new TCPEchoClient());

		/* Stop accepting new tasks */ 
		executor.shutdown();

		/* run process */
		try {
			runTime = System.currentTimeMillis();
			
			/* waits for 1000milliseconds/1second to finish tasks */
			executor.awaitTermination(1000, TimeUnit.MILLISECONDS);
			
			runTime = System.currentTimeMillis() - runTime;
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		System.out.println("Time: " + runTime + " ms. Not sent: " + ((transferRate - sent)-1));
	}
	
	/*
	 * Validation for IP, Port, BUFSIZE, TransferRate
	 * Limitations like max number or min number are mostly taken
	 * from the Software Testing 2DV610
	 */
	public static void isValidType(String[] args) { 
		String IP = args[0];
		int port = Integer.valueOf(args[1]);
		BUFSIZE = Integer.valueOf(args[2]);
		transferRate = Integer.valueOf(args[3]);

		String IP_PATTERN =
				"^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
				"([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
				"([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
				"([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";
		
		Pattern pattern = Pattern.compile(IP_PATTERN);
		Matcher matcher = pattern.matcher(IP);
		
		try {
		if (!matcher.matches())
			System.err.println("**********Invalid IP address**********");
		//	System.exit(1);
		} catch (IllegalArgumentException e) {
			System.err.println("Invalid IP address format");
			System.exit(1);
		}
		if (port <= 0 || port > 65535) {
			System.err.println("Invalid port number please choose a port between 1 and 65535");
			System.exit(1);
		}
		if (BUFSIZE < 1) {
			System.err.println("Invalid buffer size");
			System.exit(1);
		}
		if (transferRate < 0) {
			System.err.println("Invalid transfer rate / Rate must be bigger than 0");
			System.exit(1);
		}
		/* send at least 1 time */
		if (transferRate == 0) {
			transferRate = 1;
		}
	}
}