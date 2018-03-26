package lab2;

public class HTTPException extends Exception {
	private static final long serialVersionUID = 1L;
	private StatusCode code;
	
	// Constructor
	public HTTPException(StatusCode code) {
		this.code = code;
	}

	public StatusCode getStatusCode() {
		return code;
	}
}
