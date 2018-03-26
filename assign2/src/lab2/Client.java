package lab2;

import lab2.HTTPResponse.ResponseType;
import lab2.HTTPRequest.MethodType;
import lab2.HTTPException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;

/*
 * Class for server connection that reads and writes concurrently
 */
public class Client implements Runnable {

	private final int byteArray = 8*1024;
	private Socket connection;
	private byte[] buffer = new byte[byteArray];
	private HTTPResponse response = new HTTPResponse();
	private final int TIME_OUT = 1000000;
	
	public Client(Socket socket) {
		connection = socket;
	}

	private void clearBuffer() {
		for (int i = 0; i < byteArray; i++)
			buffer[i] = 0;
	}

	@Override
	public void run() {
		try {
			InputStream input = connection.getInputStream();
			OutputStream out = connection.getOutputStream();
			boolean DEBUG = true;
			do {
				clearBuffer();
				try {													
					/* Set the timeout time first and then parses the Client request.
					 * Once the request is parsed it continues to the response which returns
					 * the appropriate response. 
					 */
					BufferedReader temp = new BufferedReader(new InputStreamReader(connection.getInputStream()));
					connection.setSoTimeout(TIME_OUT);
					HTTPRequest request = new HTTPRequest(temp);
					String filePath = request.getUrl(request.getPath());
		
					// Checks the method of the request
					if (request.getMethod() == MethodType.GET) {
						// Set / update the response status code and file path
						response.setResponse(StatusCode.OK);
						response.setFile(filePath);
						// Generate a response with header. Write to output
						write(out, response.getResponse(
								new ResponseType[] { ResponseType.RESPOND, ResponseType.DATE, ResponseType.SERVER, 
										ResponseType.CONTENT_TYPE, ResponseType.CONTENT_LENGTH }));
						/*
						 * This part reads the file in bytes and sends the content
						 * of the file. Throws HTTPException if internal error occurs
						 */
						try {
							File file = new File(filePath);
							FileInputStream fileIn = new FileInputStream(file);
							
							// Wait until file is available
							while (fileIn.available() == 0) {
							}
							while (fileIn.available() != 0) {
								clearBuffer();
								fileIn.read(buffer);
								out.write(buffer);
							}
							fileIn.close();

						} catch (FileNotFoundException e) {
							throw new HTTPException(StatusCode.NotFound);
						} catch (IOException e) {
							throw new HTTPException(StatusCode.InternalServerError); 
						}
						out.flush();
					} else {
						// If the command is not recognized, throws bad request
						throw new HTTPException(StatusCode.BadRequest);
					}
				} catch (IllegalArgumentException | SocketTimeoutException e) {
					out.write(StatusCode.BadRequest.getBytes());
					DEBUG = false;
				} catch (HTTPException http) {
					// Writes an appropriate error according to data in exception
					writeError(http.getStatusCode(), out); 
					out.flush();
					DEBUG = false;
				}
			} while (DEBUG);
			connection.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}	
	
	private void write(OutputStream out, String str) throws IOException {
		out.write(str.getBytes());
	}

	/*
	 * Generates an HTML page which shows the error message
	 */
	private void writeError(StatusCode code, OutputStream out) { 
		
		StringBuilder format = new StringBuilder();
		HTTPResponse response = new HTTPResponse();

		format.append("<html><body><h1> " + code.getCode() + " - " + code.getMessage() + "</h1></body></html>");

		response.setResponse(code);
		response.setContentLength(format.toString().length());
		response.setContentType("text/html");
		try {
			write(out, response
			.getResponse(new ResponseType[] { ResponseType.RESPOND, ResponseType.DATE, ResponseType.SERVER, 
					ResponseType.CONTENT_TYPE, ResponseType.CONTENT_LENGTH }));
			out.write(format.toString().getBytes());

		} catch (IOException e) {
		//	System.out.println("No error :P");
			Thread.currentThread().interrupt();
		}

	}
}
