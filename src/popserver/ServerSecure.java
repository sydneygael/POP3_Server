package popserver;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import popmail.Mail;

public class ServerSecure {
	
	public static final String sharedSecret = "azerty";
	private boolean run;
	public final int LISTENING_PORT = 110;
	private ArrayList<Mail> mails;
	
	public void initialization() throws FileNotFoundException {
		this.mails = new ArrayList<Mail>();
		
		ArrayList<String> mailsPaths = new ArrayList<String>();
		String filePath = "C:\\Users\\Sydney\\workspace\\pop3\\Mails";		
		mailsPaths = listerRepertoire(new File(filePath), mailsPaths);
		
		for(String s : mailsPaths) {
			Scanner scanner = new Scanner(new File (filePath + "/" + s));
			scanner.useDelimiter(";");
			
			ArrayList<String> line = new ArrayList<String>();
			while (scanner.hasNext()) {
				line.add(scanner.next());
			}
			Mail m = new Mail(Integer.parseInt(line.get(0)),line.get(1),line.get(2),line.get(3),line.get(4));
			mails.add(m);
			//System.out.println(m.toString());
			scanner.close();
		}
		
	}
	
	public ArrayList<String> listerRepertoire(File repertoire, ArrayList<String> mailsPaths){ 

		String [] listefichiers; 
		int i; 
		listefichiers=repertoire.list(); 
		for(i=0;i<listefichiers.length;i++){ 
			if(listefichiers[i].endsWith(".csv")){ 
				mailsPaths.add(listefichiers[i].substring(0,listefichiers[i].length()));
			}
		}
		return mailsPaths;
	}
	
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
				this.initialization();
				new Communication(ssock.accept(),mails).start();
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
