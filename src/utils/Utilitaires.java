package utils;

import java.net.InetAddress;
import java.net.UnknownHostException;

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
}
