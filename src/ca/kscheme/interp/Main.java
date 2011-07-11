package ca.kscheme.interp;

import ca.kscheme.KScheme;



public class Main {
	
	public static void main(String[] args) throws Exception {
		KScheme kscheme = CoreInterpreter.getDefault();
		for (int i = 0; i < args.length; i++) {
			System.out.println("will load : "+args[i]);
			kscheme.load(args[i]);
		}
		kscheme.readEvalPrintLoop();
	}

}
