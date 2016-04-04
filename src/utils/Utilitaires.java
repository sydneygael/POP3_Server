package utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
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
		String uniqueID = UUID.randomUUID().toString();
		String stamp = date.toString()+uniqueID;
		
		MessageDigest m;
		try {
			m = MessageDigest.getInstance("MD5");
			m.update(stamp.getBytes(),0,stamp.length());
			stamp = new BigInteger(1,m.digest()).toString(16);
			return stamp;
		} catch (NoSuchAlgorithmException e) {
			
		}
		return null;
	}
    
public static boolean LireAuthentificationMD5(String identifiant, String motDePasse, String timbre) {
		
		String filePath = new File("").getAbsolutePath();
		filePath += "/Fichiers/authentifications.txt";
		
		try {
			BufferedReader buff = new BufferedReader(new FileReader(filePath));
		 
			try {
				String line;
				String[] parts;
				while ((line = buff.readLine()) != null) {
					parts = line.split(";");
					
					MessageDigest m;
					try {
						m = MessageDigest.getInstance("MD5");
						parts[1] = timbre+parts[1];
						m.update(parts[1].getBytes(),0,parts[1].length());
						parts[1] = new BigInteger(1,m.digest()).toString(16);
					} catch (NoSuchAlgorithmException e) {
						System.out.println("Erreur MD5");
					}
					
					if(parts[0].equals(identifiant) && parts[1].equals(motDePasse))
						return true;
				}
			} finally {
				buff.close();
			}
		} catch (FileNotFoundException fnfe) { System.out.println("Fichier d'authentification introuvable");
		} catch (IOException e) { System.out.println("Erreur IO --" + e.toString()); }
		
		return false;
	}
    
}
