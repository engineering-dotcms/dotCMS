package it.bankit.website.ajax;

import java.io.IOException;

import com.dotmarketing.util.Logger;

public class ScpFrom {

	public static int fileTransfer(String host, String user, int port, String from, String to)  {
		int exitCode = 100;
		
		String command = "scp -o StrictHostKeyChecking=no " + user.trim() + "@" + from.replaceAll(host.trim(), host.trim() + ":") + "   " + to;
		Logger.error(ScpFrom.class, "Command: "+command);
		try {
			Process p = Runtime.getRuntime().exec(command);
			try {
				exitCode = p.waitFor();
				
			} catch (InterruptedException e) {
				Logger.error(ScpFrom.class, "Error on executing scp command", e);
				e.printStackTrace();
			}
		} catch (IOException e) {
			Logger.error(ScpFrom.class, "Error on executing scp command", e);
		}
		return exitCode;

	}
}