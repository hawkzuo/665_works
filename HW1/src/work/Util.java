package work;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;



/**
 * Created by Amos on 2018/1/23.
 */
public class Util {

    public static Map<Integer, String> supportedApplicationTypes;

    static {
        supportedApplicationTypes = new HashMap<>();
        supportedApplicationTypes.put(21, "FTP");
        supportedApplicationTypes.put(23, "TELNET");
        supportedApplicationTypes.put(80, "HTTP");
    }

    public static String byteToHex(byte[] input) {
        StringBuilder sb = new StringBuilder();
        for(byte b: input) {
            sb.append(String.format("%02X:", b));
        }
        sb.deleteCharAt(sb.length()-1);
        return sb.toString();
    }

    public static String byteToDec(byte[] input) {
        StringBuilder sb = new StringBuilder();
        for(byte b: input) {
            sb.append(String.format("%2d:", b));
        }
        sb.deleteCharAt(sb.length()-1);
        return sb.toString();
    }

    public static String unEscapeString(String s){
        StringBuilder sb = new StringBuilder();
        for (int i=0; i<s.length(); i++)
            switch (s.charAt(i)){
                case '\t': sb.append("\\t"); break;
                case '\b': sb.append("\\b"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\f': sb.append("\\f"); break;
                case '\'': sb.append("\\'"); break;
                // ... rest of escape characters
                default: sb.append(s.charAt(i));
            }
        return sb.toString();
    }

    public static void prettyPrintSession(Session session) {
        // Print out some information
        if (session == null) {
            return;
        }
        System.out.println("********************************************");
        System.out.println("*************New Session Details************");
        System.out.println("********************************************");

        System.out.println("Session Application Type: " + session.getApplicationType());
        System.out.println("Session Start Time: " + new Date(session.getSessionStartTimestamp()).toString());
        System.out.println("Session End Time: " + new Date(session.getSessionEndTimestamp()).toString());
        System.out.println("Total Packets Transferred: " + session.getTotalPacketNumber());
        System.out.println("Client-side Packets: " + session.getClientPacketNumber());
        System.out.println("Server-side Packets: " + session.getServerPacketNumber());
        System.out.println(session.getClientPhysicalInformation());
        System.out.println(session.getServerPhysicalInformation());


        System.out.println("Application-level Contents:");
        session.getOperationsList().forEach(System.out::print);
        System.out.println();
        System.out.println();
    }


}
