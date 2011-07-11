package ca.kscheme.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import ca.kscheme.data.KSchemeException;

/**
 * Implementations for the 'system and 'getenv features in slib.
 */
public class System {

	static class StreamGobbler extends Thread
	{
	    InputStream is;
	    String type;
	    
	    StreamGobbler(InputStream is, String type)
	    {
	        this.is = is;
	        this.type = type;
	    }
	    
	    public void run()
	    {
	        try
	        {
	            InputStreamReader isr = new InputStreamReader(is);
	            BufferedReader br = new BufferedReader(isr);
	            String line=null;
	            while ( (line = br.readLine()) != null)
	                java.lang.System.out.println(type + ">" + line);    
	            } catch (IOException ioe)
	              {
	                ioe.printStackTrace();  
	              }
	    }
	}

	public static int system(String cmd) throws KSchemeException {
		Runtime rt = Runtime.getRuntime();
		try {
			Process process = rt.exec(cmd);
			StreamGobbler errorGobbler = new StreamGobbler(process.getErrorStream(),"ERR");
			StreamGobbler outputGobbler = new StreamGobbler(process.getInputStream(),"OUT");
			errorGobbler.start();
			outputGobbler.start();
			try {
				return process.waitFor();
			} catch (InterruptedException e) {
				e.printStackTrace();
				return -1;
			}
		} catch (IOException e) {
			e.printStackTrace();
			return -1;
		}
		
	}
	
}
