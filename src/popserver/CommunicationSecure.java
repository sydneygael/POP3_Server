package popserver;

import java.net.Socket;
import java.util.ArrayList;

import popmail.Mail;

public class CommunicationSecure extends Communication {

	public CommunicationSecure(Socket s, ArrayList<Mail> mails) {
		super(s, mails);
	}
	
	@Override
	protected boolean traiterAPOP(String request) {
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
		return false;
	}

}
