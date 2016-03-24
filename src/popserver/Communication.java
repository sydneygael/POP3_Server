package popserver;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import popmail.Mail;
import utils.Utilitaires;

/**
 * cette classe permet de lancer une session de communication
 * tous les états sont représentés
 * @author Sydney
 *
 */
public class Communication extends Thread {

	//les commandes POP 3
	private static final String APOP = "APOP";
	private static final String LIST = "LIST";
	private static final String QUIT = "QUIT";
	private static final String DELETE = "DELE";
	private static final String STAT = "STAT";
	private static final String RETR = "RETR";
	public static final String END_OF_LINE = "\r\n";

	private final int NB_APOP_MAX = 5;

	private Socket socket;
	private States currentState;
	protected BufferedInputStream socketReader;
	protected BufferedOutputStream socketWriter;
	protected StringBuilder responseBuilder;


	//les flux 
	private OutputStreamWriter writer;
	private BufferedReader reader;


	private ArrayList<String> users;
	private ArrayList<String> passwords;

	private String userName;
	private String password;
	private ArrayList<Mail> userMails;
	private String domain;


	private enum States {
		Inititialization_State,
		Authorization_State,
		Transaction_State,
		Update_State;
	}

	public void initialization() {

		this.userName = "";
		this.password = "";

		//creation users and passwords
		users = new ArrayList<String>();
		users.add("sydney");
		users.add("bruno");
		users.add("arnaud");

		passwords = new ArrayList<String>();
		passwords.add("sydney");
		passwords.add("titi");
		passwords.add("tata");

		//on commence par mettre l'�tat en initialisation
		this.currentState = States.Inititialization_State;
	}

	public Communication(Socket s) {

		System.out.println("A client trying a connection.....");
		this.socket = s;
		this.initialization();
	}

	public Communication(Socket s, ArrayList<Mail> mails) {

		System.out.println("A client trying a connection.....");

		this.socket = s;
		this.userMails = mails;
		initialization();
	}

	/**
	 * permet d'envoyer un message
	 * @param message
	 */
	public void sendMessage(String message) {
		try {
			this.writer = new OutputStreamWriter(socket.getOutputStream());
			this.writer.write(message+"\n");
			this.writer.write("\n");
			this.writer.flush();

		} catch (IOException e) {

			e.printStackTrace();
		}
	}

	/**
	 * permet de recevoir un message
	 * @return
	 */
	public String receiveMessage() {
		String response = "";
		try {
			this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			String line = "";
			line = reader.readLine();
			System.out.println(line);
			response = line;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return response;
	}

	public void sendResponse(String response)
			throws IOException
	{
		ByteArrayOutputStream dataStream= new ByteArrayOutputStream();
		DataOutputStream dataWriter = new DataOutputStream(dataStream);

		try
		{
			// Transform the response into a byte array
			dataWriter.writeBytes(response);

			// Then, send the response to the client
			socketWriter.write(dataStream.toByteArray());
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public void run() {

		super.run();

		boolean mailToDelete = false;
		////////////////////////////////
		//start Inititialization_State
		//////////////////////////////

		// Build the response
		responseBuilder = new StringBuilder();

		System.out.println("------- Inititialization State-------------------");

		this.domain = Utilitaires.getDomain();
		String welcomeMessage = "+OK Server POP3 ready" ;
		responseBuilder.append(END_OF_LINE);
		responseBuilder.append(" "+ domain);
		this.sendMessage(welcomeMessage);
		currentState = States.Authorization_State;

		System.out.println("-------- Authorization State ----------");

		// clear the string builder
		responseBuilder = null;

		//waiting a message of the client
		String request = this.receiveMessage();
		System.out.println("Client send : " + request);

		if (request != null & !request.isEmpty() )
		{
			while ( null != currentState) {


				if(States.Authorization_State == currentState) {

					boolean authorized = false;
					int nbTest = 0;

					do {
						if(request.startsWith(APOP)) {

							//on lance un controle MD5
							//int tmp = request.indexOf(" ");
							//int tmp2 = request.lastIndexOf(" ");
							//this.userName = request.substring(tmp + 1, tmp2);
							//this.password = request.substring(tmp2 + 1,request.length()-1);

							String [] requestS = request.split(" ");


							userName = requestS[1];
							password = requestS[2];

							authorized = verifyUser(userName, password);

							if (authorized) {


								responseBuilder = new StringBuilder();
								responseBuilder.append("+OK users maildrop has "+userMails.size()+" messages");
								responseBuilder.append(END_OF_LINE);
								this.sendMessage(responseBuilder.toString());

								currentState = States.Transaction_State;
							}
							/*int userRow = -1;

							String correctPassword = "";

							for(int i=0;i<users.size();i++) {
								if(0==users.get(i).compareTo(userName)) {
									correctPassword = users.get(i);
									userRow = i;
									break;
								}
							}

							/*MessageDigest md;
							String cryptedPassword ="";
							try {
								md = MessageDigest.getInstance("MD5");
								final byte[] messageDigest = md.digest((this.domain + correctPassword).getBytes());
								final BigInteger number = new BigInteger(1, messageDigest);
								cryptedPassword = String.format("%032x", number);
							} catch (NoSuchAlgorithmException e) {
								e.printStackTrace();
							}

							if(userRow >= 0) {
								if( (0==users.get(userRow).compareTo(this.userName)) 
										&& (0==cryptedPassword.compareTo(this.password)) ) {
									authorized = true;
									System.out.println(this.userName + " Authozired");
									break;
								}
							}*/

							if(!authorized) { // wrong password 

								System.out.println("Server send : -ERR Wrong authentication");
								responseBuilder = new StringBuilder();
								responseBuilder.append("-ERR Wrong authentication");
								responseBuilder.append(END_OF_LINE);
								this.sendMessage(responseBuilder.toString());
							}
						}

						else if(request.startsWith(QUIT)) {
							System.out.println("client want to close connection");
							this.closeConnection();
						}

						else {
							System.out.println("Server send : -ERR Retry " + (this.NB_APOP_MAX-nbTest) + " remaining attempts");

							responseBuilder = new StringBuilder();
							responseBuilder.append("-ERR Retry " + (this.NB_APOP_MAX-nbTest) + " remaining attempts");
							responseBuilder.append(END_OF_LINE);
							this.sendMessage(responseBuilder.toString());
						}
						nbTest++;
					}

					while(!authorized && nbTest < NB_APOP_MAX);
				}

				////////////////////////////////
				//start Transaction_State
				//////////////////////////////

				else if(States.Transaction_State == currentState) {

					System.out.println("------- TransactionState ----------");
					String message = this.receiveMessage();
					System.out.println("Client send : \n" + message);

					String reponse;
					int cpt = 0;

					int maxAttempts = 3;
					int totalBytes = 0;

					for( Mail m : userMails) {
						totalBytes += m.getNbByte();
					}

					do {
						//handle receive command LIST
						if(message.startsWith(LIST)) {
							reponse = "+OK " + this.userMails.size() + " (" + totalBytes + " Bytes)";

							for(Mail m : this.userMails) {
								reponse += "ID " + m.getId() + " " + m.getSubject() + " (" + m.getNbByte() + " Bytes)";
							}

							System.out.println("Server send : " + reponse);
							responseBuilder = new StringBuilder();
							responseBuilder.append(reponse);
							responseBuilder.append(END_OF_LINE);
							//send the reponse
							this.sendMessage(responseBuilder.toString());
						}

						//handle receive command RETR
						else if(message.startsWith(RETR)) {
							/*int tmp = message.indexOf(" ");
							int tmp2 = message.indexOf("\n");
							int id = Integer.parseInt(message.substring(tmp+1,tmp2));*/
							String[] messages = message.split(" ");
							int id = Integer.parseInt(messages[1]);
							reponse = "";
							String stringToSend2 = "";
							for(Mail m : this.userMails) {
								if(m.getId() == id) {
									reponse = "+OK " + m.getNbByte();
									stringToSend2 = m.generateMail();
									break;
								}
							}

							System.out.println("Server send : " + reponse);
							responseBuilder = new StringBuilder();
							responseBuilder.append(reponse);
							responseBuilder.append(END_OF_LINE);
							//send the reponse
							this.sendMessage(responseBuilder.toString());

							System.out.println("Server send : " + stringToSend2);
							responseBuilder = new StringBuilder();
							responseBuilder.append(stringToSend2);
							responseBuilder.append(END_OF_LINE);
							//send the reponse
							this.sendMessage(responseBuilder.toString());

						}

						//handle receive command DELETE
						else if(message.startsWith(DELETE)) {
							/*int tmp = message.indexOf(" ");
							int tmp2 = message.indexOf("\n");
							int id = Integer.parseInt(message.substring(tmp+1,tmp2));*/
							String[] messages = message.split(" ");
							int id = Integer.parseInt(messages[1]);
							reponse = "";
							
							//on cherche le mail � d�truire
							for(Mail m : this.userMails) {
								if(m.getId() == id) {
									m.setToDelete(true);
									mailToDelete = true;
								}
							}

							if(!mailToDelete) {
								reponse = "-ERR message " + id + " invalid";
							}
							else {
								reponse = "+OK message "+ id+ " deleted";
							}

							System.out.println("Server send : " + reponse);

							responseBuilder = new StringBuilder();
							responseBuilder.append(reponse);
							responseBuilder.append(END_OF_LINE);
							//send the reponse
							this.sendMessage(responseBuilder.toString());
						}

						//handle receive command STAT
						else if(message.startsWith(STAT)) {
							reponse = "+OK " + this.userMails.size() + " (" + totalBytes + " Bytes)";

							System.out.println("Server send : " + reponse);
							responseBuilder = new StringBuilder();
							responseBuilder.append(reponse);
							responseBuilder.append(END_OF_LINE);
							//send the reponse
							this.sendMessage(responseBuilder.toString());
						}

						else if( message == null || message.isEmpty()) {
							cpt ++;
							if(cpt > maxAttempts) {
								System.out.println("ERROR connection aborted");
								this.closeConnection();
							}
						}

						message = this.receiveMessage();
						System.out.println("Client send : " + message);
					}

					while(!message.startsWith(QUIT));

					if(message.startsWith(QUIT)) {
						if(!mailToDelete) {
							System.out.println("ERROR client closed connection");
							this.closeConnection();
						}
						else {
							currentState = States.Update_State;
						}
					}
				}

				////////////////////////////////
				//start Update_State
				//////////////////////////////

				else if(States.Update_State == currentState) {

					System.out.println("--------- UpdateState -----------------");
					String reponse;

					if(mailToDelete) {
						
						for(Mail m : this.userMails) {
							if(m.getToDelete()) {
								this.userMails.remove(m);
							}
						}
						reponse = "+OK " + this.userMails.size() + " messages left";
					}
					else {
						reponse = "-ERR some messages not removed";
					}

					System.out.println("Server send : " + reponse);
					responseBuilder = new StringBuilder();
					responseBuilder.append(reponse);
					responseBuilder.append(END_OF_LINE);
					//send the reponse
					this.sendMessage(responseBuilder.toString());

					currentState = States.Inititialization_State;
				}

			}
		}

	}

	private void closeConnection() {
		try {
			socket.close();
			System.out.println("connextion closed........");
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	// getters and setters   
	public Socket getSocket() {
		return this.socket;
	}

	public void setSocket(Socket s) {
		this.socket = s;
	}

	public boolean verifyUser(String user , String mdp) {

		for ( int i =0 ; i < users.size() ; i++) {
			if ( user.equals(users.get(i)) ) {
				if ( mdp.equals(passwords.get(i))) {
					return true;
				}
			}
		}
		return false;
	}
}