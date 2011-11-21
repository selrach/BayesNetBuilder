package selrach.bnetbuilder.model.dao;

/**
 * Thrown if the file format is incorrect.
 * 
 * @author <a href="mailto:charleswrobertson@gmail.com">Charles Robertson</a>
 * 
 */
public class IncorrectFileFormatException extends Exception {

	private static final long serialVersionUID = 8729596738419781886L;

	private final static String message = "File format was incorrect.";

	public IncorrectFileFormatException() {
		super(message);
	}

	public IncorrectFileFormatException(String message) {
		super(message);
	}

	public IncorrectFileFormatException(Throwable cause) {
		super(message, cause);
	}

	public IncorrectFileFormatException(String message, Throwable cause) {
		super(message, cause);
	}
}
