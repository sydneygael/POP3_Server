package popserver;

import java.util.List;
import java.util.Scanner;
import java.io.File;
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
	protected void initilalisation() throws IOException {
		
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
			System.out.println(m.toString());
			scanner.close();
		}
		
	};
	
	@Override
	public void run() {
		run = true;
		try {
		
			SSLServerSocketFactory ssf = (SSLServerSocketFactory)SSLServerSocketFactory.getDefault();
			List<String> anonList = new ArrayList<>();
	        
			//anon
			// on filtre seulement celles qui ne prennent pas de cerificat
			for (String chipset : ssf.getSupportedCipherSuites()) {
	            if (chipset.contains("anon")) {
	                anonList.add(chipset);
	            }
	        }
			
	        SSLServerSocket ssock = (SSLServerSocket) ssf.createServerSocket(LISTENING_PORT);
	        //imposer les chiper sur la connexion
	        ssock.setEnabledCipherSuites(Arrays.copyOf(anonList.toArray(), anonList.size(), String[].class));

			while (run) {
				this.initilalisation();
				
				System.out.println("Waiting for connection........");
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
