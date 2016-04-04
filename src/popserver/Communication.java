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



	public static final String ERR_WRONG_PASSWORD = "-ERR wrong password ";
	//les commandes POP 3
	private static final String APOP = "APOP";
	private static final String LIST = "LIST";
	private static final String QUIT = "QUIT";
	private static final String DELETE = "DELE";
	private static final String STAT = "STAT";
	private static final String RETR = "RETR";

	//les retours clients
	public static final String END_OF_LINE = "\r\n";
	private static final String WRONG_REQUEST = "ERROR THE REQUEST IS WRONG";
	private static final String CLIENT_TRYING_A_CONNECTION = "A client trying a connection.....";
	public static final String ERR_INVALID_USERNAME_OR_PASSWORD = "-ERR invalid username or password";

	private final int NB_APOP_MAX = 5;

	private Socket socket;
	protected States currentState;
	protected BufferedInputStream socketReader;
	protected BufferedOutputStream socketWriter;
	protected StringBuilder responseBuilder;
	


	//les flux 
	protected OutputStreamWriter writer;
	protected BufferedReader reader;


	protected ArrayList<String> users;
	protected ArrayList<String> passwords;

	protected String userName;
	protected String password;
	protected ArrayList<Mail> userMails;
	protected String domain;


	protected enum States {
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

		//on commence par mettre l'état en initialisation
		this.currentState = States.Inititialization_State;
	}

	public Communication(Socket s) {

		System.out.println(CLIENT_TRYING_A_CONNECTION);
		this.socket = s;
		this.initialization();
	}

	public Communication(Socket s, ArrayList<Mail> mails) {

		System.out.println(CLIENT_TRYING_A_CONNECTION);
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

	@Override
	public void run() {

		super.run();
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

		// clear the string builder
		responseBuilder = null;

		//waiting a message of the client
		String request = this.receiveMessage();
		System.out.println("Client send : " + request);

		if (request != null & !request.isEmpty() )
		{
			this.commencerTraitement(request);
		}

		else {
			this.currentState = null;
			System.out.println(WRONG_REQUEST);
		}


	}

	private void commencerTraitement (String request) {

		boolean mailToDelete = false;

		while ( null != currentState) {

			switch (this.currentState) {

			case Authorization_State:

				System.out.println("-------- Authorization State ----------");
				this.authorisationState(request);

				break;
			case Transaction_State :

				System.out.println("------- TransactionState ----------");
				String requete = this.receiveMessage();
				mailToDelete = transactionState(requete);

				break;

			case Update_State :

				System.out.println("--------- UpdateState -----------------");
				updateState(mailToDelete);

				break;

			default:
				this.closeConnection();
				break;
			}
		}
	}

	/**
	 * permet de gérer l'état Authorization_State
	 * @param request la requête à traiter
	 */
	private void authorisationState(String request) {
		boolean authorized = false;
		int nbTest = 0;

		do {
			if(request.startsWith(APOP)) {

				authorized = traiterAPOP(request);
			}

			else if(request.startsWith(QUIT)) {

				traiterQUIT();
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

	/**
	 * permet de traiter l'état update state
	 * @param mailToDelete
	 */
	private void updateState(boolean mailToDelete) {
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

	protected boolean transactionState(String requete) {
		boolean mailToDelete = false;
		System.out.println("Client send : \n" + requete);

		String reponse;
		int cpt = 0;

		int maxAttempts = 3;
		int totalBytes = 0;

		for( Mail m : userMails) {
			totalBytes += m.getNbByte();
		}

		do {
			//handle receive command LIST
			if(requete.startsWith(LIST)) {
				traiterLIST(totalBytes);
			}

			//handle receive command RETR
			else if(requete.startsWith(RETR)) {
				traiterRETR(requete);
			}

			//handle receive command DELETE
			else if(requete.startsWith(DELETE)) {

				mailToDelete = traiterDELETE(requete, mailToDelete);
			}

			//handle receive command STAT
			else if(requete.startsWith(STAT)) {
				reponse = "+OK " + this.userMails.size() + " (" + totalBytes + " Bytes)";

				System.out.println("Server send : " + reponse);
				responseBuilder = new StringBuilder();
				responseBuilder.append(reponse);
				responseBuilder.append(END_OF_LINE);
				//send the reponse
				this.sendMessage(responseBuilder.toString());
			}

			else if( requete == null || requete.isEmpty()) {
				cpt ++;
				if(cpt > maxAttempts) {
					System.out.println("ERROR connection aborted");
					this.closeConnection();
				}
			}

			requete = this.receiveMessage();
			System.out.println("Client send : " + requete);
		}

		while(!requete.startsWith(QUIT));

		if(requete.startsWith(QUIT)) {
			if(!mailToDelete) {
				System.out.println("ERROR client closed connection");
				this.closeConnection();
			}
			else {
				currentState = States.Update_State;
			}
		}
		return mailToDelete;
	}

	protected boolean traiterDELETE(String requete, boolean mailToDelete) {
		String reponse;
		/*int tmp = message.indexOf(" ");
		int tmp2 = message.indexOf("\n");
		int id = Integer.parseInt(message.substring(tmp+1,tmp2));*/
		String[] messages = requete.split(" ");
		int id = Integer.parseInt(messages[1]);
		reponse = "";

		//on cherche le mail à détruire
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
		return mailToDelete;
	}

	protected void traiterUSER (String requete) {

		String [] requestS = requete.split(" ");
		String reponse;
		System.out.println("Server received an USER command");
		String userName = requestS[1];

		if(users.contains(userName)){

			System.out.println("An user matching the input username");

			responseBuilder = new StringBuilder();
			reponse = "+OK waiting for " + userName + "'s password";
			responseBuilder.append(reponse);
			responseBuilder.append(END_OF_LINE);

			//attente de commande pass
		}

		else{
			reponse = "-ERR , sorry user : " + userName + " not found";
			reponse = "+OK waiting for " + userName + "'s password";
			responseBuilder.append(reponse);
			responseBuilder.append(END_OF_LINE);
		}
	}

	protected void traiterPASS (String requete) {

		System.out.println("Server receive a PASS command");
		String params [] = requete.split(" ");
		password = params[1];

		if (checkPassword(password)) {

			responseBuilder = new StringBuilder();
			responseBuilder.append("+OK users maildrop has "+userMails.size()+" messages");
			responseBuilder.append(END_OF_LINE);
			this.sendMessage(responseBuilder.toString());

			currentState = States.Transaction_State;
		}
		else {
			responseBuilder = new StringBuilder();
			responseBuilder.append(ERR_WRONG_PASSWORD);
			responseBuilder.append(END_OF_LINE);
			this.sendMessage(responseBuilder.toString());
			this.currentState= null; // on termine alors
		}

	}

	protected void traiterRETR(String requete) {
		String reponse;
		/*int tmp = message.indexOf(" ");
		int tmp2 = message.indexOf("\n");
		int id = Integer.parseInt(message.substring(tmp+1,tmp2));*/
		String[] messages = requete.split(" ");
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

	protected void traiterLIST(int totalBytes) {
		String reponse;
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

	protected void traiterQUIT() {

		System.out.println("client want to close connection");

		this.closeConnection();
	}

	protected boolean traiterAPOP(String request) {
		boolean authorized;

		String [] requestS = request.split(" ");


		userName = requestS[1];
		password = requestS[2];

		authorized = checkUser(userName, password);

		if (authorized) {


			responseBuilder = new StringBuilder();
			responseBuilder.append("+OK users maildrop has "+userMails.size()+" messages");
			responseBuilder.append(END_OF_LINE);
			this.sendMessage(responseBuilder.toString());

			currentState = States.Transaction_State;
		}

		if(!authorized) { // wrong password 

			System.out.println("Server send : -ERR Wrong authentication");
			responseBuilder = new StringBuilder();
			responseBuilder.append(ERR_INVALID_USERNAME_OR_PASSWORD);
			responseBuilder.append(END_OF_LINE);
			this.sendMessage(responseBuilder.toString());
		}
		return authorized;
	}

	protected void closeConnection() {
		try {
			socket.close();
			this.currentState=null;
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

	protected boolean checkUser(String user , String mdp) {

		for ( int i =0 ; i < users.size() ; i++) {
			if ( user.equals(users.get(i)) ) {
				if ( mdp.equals(passwords.get(i))) {
					return true;
				}
			}
		}
		return false;
	}

	protected boolean checkPassword(String password) {

		for ( int i =0 ; i < users.size() ; i++) {
			if ( password.equals(passwords.get(i))) {
				return true;
			}
		}
		return false;
	}
}