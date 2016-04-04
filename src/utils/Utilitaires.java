package utils;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.UUID;

public class Utilitaires {
	
    public static String getDomain() {
        String hostname;
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch(UnknownHostException e) {
            hostname = "localhost";
        }
        
        return "<" + Thread.currentThread().getId() + "." +
                System.currentTimeMillis() + "@" +
                hostname + ">";
    }
    
    /**
     * 
     * @return timbre à date
     */
    public static String generateStamp() {

		Date date = new Date();
		String id = UUID.randomUUID().toString();
		String stamp = date.toString()+id;
		
		MessageDigest m;
		try {
			m = MessageDigest.getInstance("MD5");
			m.update(stamp.getBytes(),0,stamp.length());
			stamp = new BigInteger(1,m.digest()).toString(16);
			return stamp;
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return stamp;
	}
    
}
