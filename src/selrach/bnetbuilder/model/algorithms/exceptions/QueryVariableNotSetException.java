package selrach.bnetbuilder.model.algorithms.exceptions;

/**
 * Exception to be thrown if no query variables were set up.
 * 
 * @author <a href="mailto:charleswrobertson@gmail.com">Charles Robertson</a>
 * 
 */
public class QueryVariableNotSetException extends Exception {
	private static final long serialVersionUID = 6762806728954288230L;

	public QueryVariableNotSetException(String message) {
		super(message);
	}

	public QueryVariableNotSetException(String message, Throwable cause) {
		super(message, cause);
	}

	public QueryVariableNotSetException(Throwable cause) {
		super(cause);
	}

	public QueryVariableNotSetException() {
		super("Query Variables were not set");
	}
}
