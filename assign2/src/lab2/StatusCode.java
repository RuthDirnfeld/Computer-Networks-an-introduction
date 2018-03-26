package lab2;

public enum StatusCode {
	OK(200,"OK"),
	Found(302,"Found In "+"<a href=\"http://localhost:4950//redirected//homer.jpg\">here</a>"),
	BadRequest(400,"Bad Request"),
	Forbidden(403,"Forbidden"),
	NotFound(404,"Not Found"),
	InternalServerError(500,"Internal Server Error"),
	Unknown(520, "Unknown Error");

	private final int code;
	private final String message;
	
	
	// Constructor for enum status code
	StatusCode(int code,String message){
		this.code = code;
		this.message = message;
	}
	
	@Override
	//Returns HTTP message in string.
	public String toString(){
		return "HTTP/1.1 " + code + " " + message + "\r\n";
		// HTTP/1.1 200 OK
	}
	public int getCode() {
		return code;
	}
	public String getMessage() {
		return message;
	}
	public byte[] getBytes(){
		return this.toString().getBytes();
	}
}
