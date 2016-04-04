package popserver;

import java.util.List;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

import popmail.Mail;

public class ServerSecure  extends Server {
	
	public static final String sharedSecret = "azerty";
	private boolean run;
	public final int LISTENING_PORT = 110;
	private ArrayList<Mail> mails;
	
	@Override
	public void run() {
		run = true;
		try {
			System.out.println("Waiting for connection........");
		
			SSLServerSocketFactory ssf = (SSLServerSocketFactory)SSLServerSocketFactory.getDefault();
			List<String> anonList = new ArrayList<>();
	        
			for (String string : ssf.getSupportedCipherSuites()) {
	            if (string.contains("anon")) {
	                anonList.add(string);
	            }
	        }
	        SSLServerSocket ssock = (SSLServerSocket) ssf.createServerSocket(LISTENING_PORT);
	        ssock.setEnabledCipherSuites(Arrays.copyOf(anonList.toArray(), anonList.size(), String[].class));

			while (run) {
				this.initilalisation();
				new CommunicationSecure(ssock.accept(),mails).start();
			}
			ssock.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	public static void main(String[] args) {
		new ServerSecure().run();
	}
}
