package it.eng.bankit.app;

public class ImportException extends Exception {
	private static final long serialVersionUID = 7171472971979493311L;
	
	public ImportException( String message, Exception e ) {
		super(message,e);
	}

	public ImportException( String message ) {
		super(message);
	}

	public ImportException( Exception e ) {
		super(e);
	}
	

}
