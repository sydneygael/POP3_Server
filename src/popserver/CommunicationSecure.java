package popserver;

import java.math.BigInteger;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import popmail.Mail;
import utils.Utilitaires;

public class CommunicationSecure extends Communication {

	private String stamp; // timbre à date
	public CommunicationSecure(Socket s, ArrayList<Mail> mails) {
		super(s, mails);
		this.stamp = Utilitaires.generateStamp();
	}
	
	@Override
	protected boolean traiterAPOP(String request) {
		boolean authorized;

		String [] requestS = request.split(" ");


		userName = requestS[1];
		password = requestS[2];

		authorized = verifyMD5(userName, password);

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
	
	private boolean verifyMD5 (String user , String password) {
		
		MessageDigest md;
		String cryptedPassword = null ;
		int userRow=-1;
		
		String correctPassword = "";
		for(int i=0;i<users.size();i++) {
			if(0==users.get(i).compareTo(this.userName)) {
				correctPassword = users.get(i);
				userRow = i;
				break;
			}
		}
		
		try {
			md = MessageDigest.getInstance("MD5");
			final byte[] messageDigest = md.digest((this.stamp + correctPassword).getBytes());
	        final BigInteger number = new BigInteger(1, messageDigest);
	        cryptedPassword = String.format("%032x", number);
		} 
		catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		
		if ( userRow >= 0) {
			if( (users.get(userRow).equals(this.userName)) 
					&& (cryptedPassword.equals(this.password)) )
				return true;
			else return false;
		}
		
		else return false;
	}

}
