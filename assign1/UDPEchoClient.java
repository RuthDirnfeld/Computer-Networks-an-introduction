package lab1;

/*
UDPEchoClient.java
A simple echo client with no error handling
*/
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UDPEchoClient implements Runnable {
	public static int BUFSIZE = 1024;
	public static final int MYPORT = 0;
	public static final String MSG = "An Echo Message!";
	public static int transferRate;
	public static long runTime = 0;
	public static int sent = 0;
	public static DatagramSocket socket;
	public static DatagramPacket sendPacket;
	public static DatagramPacket receivePacket;
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
		byte[] buffer = new byte[BUFSIZE];
		BUFSIZE = Integer.valueOf(args[2]);

		if (args.length != 4) {
			System.err.printf("Wrong number of arguments. Enter: IP, PORT, TRANSFERRATE, BUFSIZE");
			System.exit(1);
		}
		
		isValidType(args); // Error check
		
		try {
		/* Create socket */
		socket = new DatagramSocket(null);

		/* Create local endpoint using bind() */
		SocketAddress localBindPoint = new InetSocketAddress(MYPORT);
		socket.bind(localBindPoint);

		/* Create remote endpoint */
		remoteBindPoint = new InetSocketAddress(args[0], Integer.valueOf(args[1]));
		} catch (SocketException e) {
			e.printStackTrace();
		}
		
		/* Create datagram packet for sending message */
		sendPacket = new DatagramPacket(MSG.getBytes(), MSG.length(), remoteBindPoint);

		/* Create datagram packet for receiving echoed message */
		receivePacket = new DatagramPacket(buffer, buffer.length);
				
		oneSecond(); //VG - limit to 1 second
		
		socket.close();
	}
	
	private static void start() {
		try {
			/* Send packet */
			socket.send(sendPacket);

			/* Receive packet */
			socket.receive(receivePacket);

			/* Compare sent and received message */
			String receivedStr = new String(receivePacket.getData(), receivePacket.getOffset(),
					receivePacket.getLength());
			if (receivedStr.compareTo(MSG) == 0)
				System.out.printf("%d bytes sent and received || Buffer Size: " + BUFSIZE + " ||\n", receivePacket.getLength());
			else
				System.out.printf("Sent bytes: " + MSG.length() + " Received bytes: " + receivedStr.length() 
				+ " msg not equal! || Buffer Size: " + BUFSIZE + " ||\n");
		} catch (Exception e) {
			return; //catch if time is already out but socket is still sending or receiving
		}
	}
	
	private static void oneSecond() { //taken from the thread examples from OS 1DV512
		/* New thread pool */
		ExecutorService executor = Executors.newFixedThreadPool(1);

		/* Submit thread */ 
		executor.submit(new UDPEchoClient());

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
		} catch (Exception e) {
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