package lab3;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.file.FileAlreadyExistsException;
import java.util.Arrays;

public class TFTPServer {
	public static final int TFTPPORT = 4970;
	public static final int BUFSIZE = 516;
	public static final String READDIR = "src/lab3/inner/read/"; // custom address at your PC
	public static final String WRITEDIR = "src/lab3/inner/write/"; // custom address at your PC

	public static InetSocketAddress socketAddress;
	public static String transferMode;

	// OP codes
	public static final int OP_RRQ = 1;
	public static final int OP_WRQ = 2;
	public static final int OP_DAT = 3;
	public static final int OP_ACK = 4;
	public static final int OP_ERR = 5;

	public static void main(String[] args) throws IOException {
		if (args.length > 0) {
			System.err.printf("usage: java %s\n", TFTPServer.class.getCanonicalName());
			System.exit(1);
		}
		// Starting the server
		try {
			TFTPServer server = new TFTPServer();
			server.start();
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}

	private void start() throws IOException, SocketException {
		byte[] buf = new byte[BUFSIZE];

		// Create socket
		DatagramSocket socket = new DatagramSocket(null);

		// Create local bind point
		SocketAddress localBindPoint = new InetSocketAddress(TFTPPORT);
		socket.bind(localBindPoint);

		System.out.printf("Listening at port %d for new requests\n", TFTPPORT);

		// Loop to handle client requests
		while (true) {

			final InetSocketAddress clientAddress = receiveFrom(socket, buf);

			// If clientAddress is null, an error occurred in receiveFrom()
			if (clientAddress == null)
				continue;

			final StringBuffer requestedFile = new StringBuffer();
			final int reqtype = ParseRQ(buf, requestedFile);

			new Thread() {
				public void run() {
					try {
						DatagramSocket sendSocket = new DatagramSocket(0);

						// Connect to client
						sendSocket.connect(clientAddress);

						System.out.printf("%s request for %s from %s using port %d\n",
								(reqtype == OP_RRQ) ? "Read" : "Write", clientAddress.getHostName(),
								clientAddress.getPort(), TFTPPORT);

						// Read request
						if (reqtype == OP_RRQ) {
							requestedFile.insert(0, READDIR);
							HandleRQ(sendSocket, requestedFile.toString(), OP_RRQ);
							// System.out.println("RRQ works");
						}
						// Write request
						else {
							requestedFile.insert(0, WRITEDIR);
							HandleRQ(sendSocket, requestedFile.toString(), OP_WRQ);
							// System.out.println("WRQ works");
						}
						sendSocket.close();
					} catch (SocketException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}.start();
		}
	}

	/**
	 * Reads the first block of data, i.e., the request for an action (read or
	 * write).
	 * 
	 * @param socket
	 *            (socket to read from)
	 * @param buf
	 *            (where to store the read data)
	 * @return socketAddress (the socket address of the client)
	 */
	private InetSocketAddress receiveFrom(DatagramSocket socket, byte[] buf) {
		System.out.println("***Receiving***");
		// Create datagram packet
		DatagramPacket receivePacket = new DatagramPacket(buf, buf.length);

		try {
			// Receive packet
			socket.receive(receivePacket);
			// Get client address and port from the packet
			socketAddress = new InetSocketAddress(receivePacket.getAddress(), receivePacket.getPort());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return socketAddress;
	}

	/**
	 * Parses the request in buf to retrieve the type of request and
	 * requestedFile
	 * 
	 * @param buf
	 *            (received request)
	 * @param requestedFile
	 *            (name of file to read/write)
	 * @return opcode (request type: RRQ or WRQ)
	 */
	private int ParseRQ(byte[] buf, StringBuffer requestedFile) {
		// See "TFTP Formats" in TFTP specification for the RRQ/WRQ request
		// contents
		ByteBuffer wrap = ByteBuffer.wrap(buf);
		short opcode = wrap.getShort();
		System.out.println("\nOPCODE: " + opcode);
		// index is the number of bytes read into the byte array
		int index = 1; 
		boolean loop = true;
		while (loop) {
			index++;
			if (buf[index] == 0) {
				loop = false;
			}
		}
		// Parse the request message for opcode and requested file as:
		requestedFile.append(new String(buf, 2, index - 2));
		System.out.println("REQUESTED FILE: " + requestedFile);

		// Start reading the transfer mode
		int start = index + 1;

		for (int i = start; i < buf.length; i++) {
			// Found end of transfer mode
			if (buf[i] == 0) {
				// End of transfer mode at i - start
				transferMode = new String(buf, start, i - start);
				System.out.println("TRANSFER MODE: " + transferMode);
				if (!transferMode.equals("octet")) {
					System.err.println("Invalid mode");
					System.exit(0);
				}
				return opcode;
			}
		}
		System.err.println("No mode found");
		System.exit(0);
		return 0;
	}

	/**
	 * Handles RRQ and WRQ requests
	 * 
	 * @param sendSocket
	 *            (socket used to send/receive packets)
	 * @param requestedFile
	 *            (name of file to read/write)
	 * @param opcode
	 *            (RRQ or WRQ)
	 */
	private void HandleRQ(DatagramSocket socket, String requestedFile, int opcode) throws IOException {

		if (opcode == OP_RRQ) {

			try {

				boolean result = send_DATA_receive_ACK(socket, requestedFile);
			} catch (FileNotFoundException e) {
				System.err.println("File not Found.");
				send_ERR(socket, 1, "File not Found.");
			}
		} else if (opcode == OP_WRQ) {

			try {
				boolean result = receive_DATA_send_ACK(socket, requestedFile);
			} catch (FileAlreadyExistsException e) {
				System.err.println("File already exists.");
				send_ERR(socket, 6, "File already exists.");
			}
		} else {
			System.err.println("Invalid request. Sending an error packet.");
			send_ERR(socket, 4, "Illegal TFTP operation.");
			return;
		}
	}

	/**
	 * In this method we start by setting the path of the requested file. We
	 * check if the requested file does not exist. If the files exist we
	 * continuer to the loop. This loop goes until the limit of bad acks is
	 * reached or if an Error occurs, which results with an error message.
	 * Inside this loop is a buffer 516, we read the file, increase the block
	 * (since TFTP sends data block-by-block, with block sizes split into 512
	 * bytes each) and create a data packet with this data. We send the data 
	 * and wait for the ack. If there is a timeout and we have no ack, we try 
	 * again until the limit is reached. If the packet is less than 512 it 
	 * means its the last one. We send an Error when the limit is exceeded.
	 */
	private boolean send_DATA_receive_ACK(DatagramSocket socket, String requestedFile) throws IOException {

		File file = new File(requestedFile);
		//516 - 4 = 512
		byte[] buffer = new byte[BUFSIZE - 4];

		try {
			// Check if file exists, else send Error #1
			if (!file.exists()) {
				System.err.println("File not found.");
				send_ERR(socket, 1, "File not found.");
				return false;
				// Check write and read permissions
			} else if (!file.canWrite() && !file.canRead()) {
				System.err.println("Access violation.");
				send_ERR(socket, 2, "Access violation.");

			} else {

				int block = 0;
				FileInputStream stream = new FileInputStream(file);

				while (true) {
					++block;
					int read = stream.read(buffer);

					ByteBuffer buf = ByteBuffer.allocate(BUFSIZE);
					buf.putShort((short) OP_DAT);
					buf.putShort((short) block);
					buf.put(buffer);

					int sendCounter = 0;
					short blockNum;

					try {
						socket.setSoTimeout(4000);
						do {
							/*
							 * Create the Datagram Packet with buffer and bytes
							 * read
							 */
							DatagramPacket packet = new DatagramPacket(buf.array(), read + 4);
							// Send the packet from the specified socket
							socket.send(packet);
							/* Sent the data and wait for the ack */
							ByteBuffer receive = ByteBuffer.allocate(OP_ACK);
							DatagramPacket ackPack = new DatagramPacket(receive.array(), receive.array().length);
							socket.receive(ackPack);
							// short opcode = OP_RRQ; //uncomment this line for ERR 0
							short opcode = receive.getShort();
							blockNum = receive.getShort();
							// counter for failures
							sendCounter++;
							// System.out.println("Ack opcode: " + opcode);

						} while (sendCounter < 6 && blockNum != block);

					} catch (SocketTimeoutException e) {
						System.err.println("Timeout Exception");
						return false;
					}
					if (read < 512) {
						break;
					}
					// If too many failures, show Error #0
					else if (sendCounter >= 5) {
						System.err.println("Max allowed re-transmission is 5.");
						send_ERR(socket, 0, "Max allowed re-transmission is 5.");
						return false;
					}
				}
				stream.close();
			}
		} catch (IOException ie) {
			try {
				System.err.println("Access violation.");
				send_ERR(socket, 2, "Access violation.");
			} catch (Exception e) {
				e.printStackTrace();
			}
			return false;
		} catch (SecurityException se) {
			se.printStackTrace();
		}
		return true;
	}

	/**
	 * In this method we write a requested file to the server directory "write".
	 * We start by setting the path and checking if the requested file already
	 * exists in the given directory. Then we enter the loop. Inside the loop we
	 * create a packet and wait for the client to respond. We verify if the
	 * received packet is the data packet. Starting from 4, because the first
	 * four bytes belong to packet opcode and block. Now we write the data, send
	 * the ack to the client. Then we check if the received packet is the last
	 * one. We repeat this process until it is the last one, then we break the
	 * loop.
	 */
	private boolean receive_DATA_send_ACK(DatagramSocket socket, String requestedFile) throws IOException {

		File file = new File(requestedFile);
		int timeoutCounter = 0;
		try {
			// If file exists, send Error #6
			if (file.exists()) {
				System.err.println("File already exists.");
				send_ERR(socket, 6, "File already exists.");
				return false;
			} else {
				int blockNumber = 0;
				FileOutputStream output = new FileOutputStream(requestedFile);
				// Send an ACK packet to client
				firstACK(blockNumber, socket);

				while (true) {
					try {
						socket.setSoTimeout(4000);

						byte[] buffer = new byte[BUFSIZE];
						/* Create the Datagram Packet with data */
						DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
						// receive packet - wait for client response
						socket.receive(packet);

						ByteBuffer wrap = ByteBuffer.wrap(packet.getData());
						short opcode = wrap.getShort();
						// Check if the received packet is the data packet
						if (opcode == OP_DAT) {
							byte[] data = Arrays.copyOfRange(packet.getData(), 4, packet.getLength());
							// write data
							output.write(data);
							output.flush();
							// send ACK packet to client with the same block
							dataACK(wrap, socket);
							// check if received packet is the last
							if (data.length < 512) {
								socket.close();
								output.close();
								break;
							}
							// check if Error
						} else if (opcode == OP_ERR) {
							System.err.println("Error code: " + wrap.getShort());
							return false;
						} else {
							System.err.println("Error from Client");
							return false;
						}
					} catch (SocketTimeoutException e) {
						if (++timeoutCounter >= 5) {
							System.err.println("No Response From Client.");
							send_ERR(socket, 0, "No Response From Client");
							return false;
						}
					}
				}
				return true;
			}
		} catch (IOException ie) {
			try {
				System.err.println("Access violation.");
				send_ERR(socket, 2, "Access violation.");
			} catch (Exception e) {
				e.printStackTrace();
			}
			return false;
		}
	}

	private void send_ERR(DatagramSocket socket, int errorID, String errorMessage) throws IOException {
		/*  2 bytes     2 bytes      string    1 byte
         *  -----------------------------------------
         * | Opcode |  ErrorCode |   ErrMsg   |   0  |
         *  -----------------------------------------
         */
		byte[] buf = errorMessage.getBytes();

		ByteBuffer data = ByteBuffer.allocate(errorMessage.length() + OP_ERR);
		data.putShort((short) OP_ERR);
		data.putShort((short) errorID);
		data.put(buf);

		socket.send(new DatagramPacket(data.array(), data.array().length));
	}

	private void firstACK(int blockNumber, DatagramSocket socket) throws IOException {
		ByteBuffer receive = ByteBuffer.allocate(OP_ACK);
		receive.putShort((short) OP_ACK);
		receive.putShort((short) blockNumber);
		DatagramPacket ackPacket = new DatagramPacket(receive.array(), receive.array().length);
		socket.send(ackPacket);
	}

	private void dataACK(ByteBuffer wrap, DatagramSocket socket) throws IOException {
		ByteBuffer receive = ByteBuffer.allocate(OP_ACK);
		receive.putShort((short) OP_ACK);
		receive.putShort(wrap.getShort());
		socket.send(new DatagramPacket(receive.array(), receive.array().length));
	}
}
