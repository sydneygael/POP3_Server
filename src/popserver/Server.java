package popserver;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

import popmail.Mail;

public class Server {

	private boolean run;
	public final int LISTENING_PORT = 110;
	public final int NB_CONNECTIONS= 10;
	private ArrayList<Mail> mails;
	private ServerSocket serverSocket = null;

	protected void initilalisation () throws IOException {

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

		try {
			serverSocket = new ServerSocket(LISTENING_PORT,NB_CONNECTIONS);
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println("............WAINTING FOR CLIENT CONNECTION...............");
		Socket socket = serverSocket.accept();
		Communication com = new Communication(socket,mails);
		com.start();
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

	protected void run() {
		run = true;

		try {
			initilalisation();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		new Server().run();
	}


}
