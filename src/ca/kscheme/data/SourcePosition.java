package ca.kscheme.data;

import java.net.URL;

public class SourcePosition {

	private URL sourceFile;
	private int line;
	private int col;

	public SourcePosition(URL sourceFile, int line, int col) {
		this.sourceFile = sourceFile;
		this.line = line;
		this.col = col;
	}
	
	@Override
	public String toString() {
		if (sourceFile!=null)
			return "line: "+line+" col: "+col+ " in "+sourceFile;
		else
			return "line: "+line+" "+" col: "+col;
	}

	public URL getSourceURL() {
		return sourceFile;
	}

}
