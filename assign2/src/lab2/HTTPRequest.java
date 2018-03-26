package lab2;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;

public class HTTPRequest {

	private static final String CRLF = "\r\n";
	private MethodType method;
	private String path;
	private String protocol;
	private long contentLength = 0;

	public enum MethodType {
		GET, HEAD, POST, PUT, DELETE, TRACE, OPTIONS, CONNECT, PATCH;
	}

	/*
	 * Constructor that parses the header
	 * throws HTTPException if request is invalid
	 */
	public HTTPRequest(BufferedReader reader) throws HTTPException, IOException {
		
		try {
			parseHeader(readHeader(reader));
		} catch (ArrayIndexOutOfBoundsException | IllegalArgumentException
				| NullPointerException | IOException e) {
			throw new HTTPException(StatusCode.InternalServerError); //throws Internal Server Error
		}
	}

	/*
	 * This method parses the request header. 
	 * throws HTTPException.
	 */
	private void parseHeader(String head) throws HTTPException {

		try {
			// Split the String by tabs and new line
			String[] parts = head.split(CRLF);
			String firstLine = parts[0];
			// Separate the first line by space
			String[] request = firstLine.split("\\s");
			//If length is not 3, throws bad request
			if (request.length != 3) {
				throw new HTTPException(StatusCode.BadRequest); //Bad Request
			}
			// Update method, path and protocol 
			this.method = MethodType.valueOf(request[0]);
			this.path = request[1];
			this.protocol = request[2];
			// check if it is 1.1
			if (!request[2].equals("HTTP/1.1")) {
				throw new HTTPException(StatusCode.BadRequest);
			}
		} catch (Exception e) {
			throw new HTTPException(StatusCode.BadRequest);
		}
	}

	/*
	 * Reads the header and stops when the line is null or empty. After each header property a new
	 * tab and new line is added.
	 */
	private String readHeader(BufferedReader reader) throws IOException, NumberFormatException, HTTPException { // inspired by the method from 2DV610

		StringBuilder line = new StringBuilder();
		
	//	if (line.toString().equals("")) throw new IOException(); //uncomment for Internal Server Error
	
		while (true) {
			String lineRead = reader.readLine();

			if (lineRead == null || lineRead.equals(CRLF) || lineRead.isEmpty() || lineRead.equals("")) {
				break;
			}

			line.append(lineRead);
			line.append(CRLF);

			//Substring is 16 because the "Content-Length: ".length() = 16.
			if (lineRead.startsWith("Content-Length")) {
				contentLength = Integer.parseInt(line.substring(16));
			}
		}
		return line.toString();
	}

	/*
	 * Changes url into absolute path of the file in server
	 * throws HTTPException if the file does not exist
	 */
	public String getUrl(String path) throws HTTPException {
		String[] nope = {"NOPE"};

		if (path == null)
			throw new HTTPException(StatusCode.BadRequest);

		String allPath = Server.contentPath + path;
		
		// If the User enters images/homer, he will get redirected to the redirected directory containing homer
		if (allPath.contains("images/homer"))
			throw new HTTPException(StatusCode.Found); 
		
		// If the User enters //redirected or //images he will get the option of redirection
		if (new File(allPath).isDirectory()) {
			if (allPath.charAt(allPath.length() - 1) != '/') {
				throw new HTTPException(StatusCode.Found); 
			}
		// Makes it load the main page
			String temp = allPath + "index.htm";
			if (new File(temp).exists())
				return temp;
			temp = allPath + "index.html";
			if (new File(temp).exists())
				return temp;
		}
		if (new File(allPath).exists()) {
			return allPath;
		}
		// If the User enters /NOPE he will find that it is a forbidden request
		for (String str : nope) {
			if (path.contains(str)) {
				throw new HTTPException(StatusCode.Forbidden); //Forbidden
			}
		}
		throw new HTTPException(StatusCode.NotFound);
	}

	/*
	 * Returns the method used for request as enum RequestType
	 */
	public MethodType getMethod() {
		return method;
	}

	public String getPath() {
		return path;
	}
}
