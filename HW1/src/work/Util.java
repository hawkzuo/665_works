package work;

import java.util.*;


/**
 * Created by Amos on 2018/1/23.
 */
public class Util {

    public static Map<Integer, String> supportedApplicationTypes;
    public static String[] telnetOptions;
    public static String[] telnetCommands;

    public static Map<Integer, String[]> telnetSubOptions;

    public static Set<Integer> telnetEscapeCharacters;
    public static Map<Integer, String> telnetSpecialCharacters;


    static {
        supportedApplicationTypes = new HashMap<>();
        supportedApplicationTypes.put(21, "FTP");
        supportedApplicationTypes.put(23, "TELNET");
        supportedApplicationTypes.put(80, "HTTP");

        telnetOptions = new String[256];
        telnetOptions[0] = "TRANSMIT-BINARY"; telnetOptions[1] = "ECHO"; telnetOptions[2] = "RECONNECT";
        telnetOptions[3] = "SUPPRESS-GO-AHEAD"; telnetOptions[4] = "Approx-Message-Size-Negotiation"; telnetOptions[5] = "STATUS";
        telnetOptions[6] = "TIMING-MARK"; telnetOptions[7] = "RCTE"; telnetOptions[8] = "Output-Line-Width";
        telnetOptions[9] = "Output-Page-Size"; telnetOptions[10] = "NAOCRD"; telnetOptions[11] = "NAOHTS";
        telnetOptions[12] = "NAOHTD"; telnetOptions[13] = "NAOFFD"; telnetOptions[14] = "NAOVTS";
        telnetOptions[15] = "NAOVTD"; telnetOptions[16] = "NAOLFD"; telnetOptions[17] = "EXTEND-ASCII";
        telnetOptions[18] = "LOGOUT"; telnetOptions[19] = "BM"; telnetOptions[20] = "DET";
        telnetOptions[21] = "SUPDUP"; telnetOptions[22] = "SUPDUP"; telnetOptions[23] = "SEND-LOCATION";
        telnetOptions[24] = "TERMINAL-TYPE"; telnetOptions[25] = "END-OF-RECORD"; telnetOptions[26] = "TUID";
        telnetOptions[27] = "OUTMRK"; telnetOptions[28] = "TTYLOC"; telnetOptions[29] = "3270-REGIME";
        telnetOptions[30] = "X.3-PAD"; telnetOptions[31] = "NAWS"; telnetOptions[32] = "TERMINAL-SPEED";
        telnetOptions[33] = "TOGGLE-FLOW-CONTROL"; telnetOptions[34] = "LINEMODE"; telnetOptions[35] = "X-DISPLAY-LOCATION";
        telnetOptions[36] = "ENVIRON"; telnetOptions[37] = "AUTHENTICATION"; telnetOptions[38] = "ENCRYPT";
        telnetOptions[39] = "NEW-ENVIRON"; telnetOptions[40] = "TN3270E";
        telnetOptions[42] = "CHARSET"; telnetOptions[44] = "COM-PORT-OPTION";
        telnetOptions[47] = "KERMIT"; telnetOptions[255] = "EXTENDED-OPTIONS-LIST";

        // Taking care of telnet Sub Options
        telnetSubOptions = new HashMap<>();
        // 7 -> RCTE should be extra taken care of, which here will not focus
        // 19 -> BM complex too
        // 20 -> DET
        // 22 -> SUPDUP-OUTPUT
        // 27 -> OUTMRK

        telnetEscapeCharacters = new HashSet<>();
        telnetEscapeCharacters.add(91);    telnetEscapeCharacters.add(59);
        for(int i=48; i<=57; i++) {
            telnetEscapeCharacters.add(i);
        }
        telnetSpecialCharacters = new HashMap<>();
        telnetSpecialCharacters.put(0, "NUL");
        telnetSpecialCharacters.put(7, "BEL");
        telnetSpecialCharacters.put(11, "VT");


        telnetCommands = new String[256];
        telnetCommands[0] = "IS";   telnetCommands[1] = "SEND"; telnetCommands[2] = "INFO";
        telnetCommands[236] = "EOF"; telnetCommands[237] = "SUSP"; telnetCommands[238] = "ABORT"; telnetCommands[239] = "EOR";
        telnetCommands[240] = "SE"; telnetCommands[241] = "NOP"; telnetCommands[242] = "DM"; telnetCommands[243] = "BRK";
        telnetCommands[244] = "IP"; telnetCommands[245] = "AO"; telnetCommands[246] = "AYT"; telnetCommands[247] = "EC";
        telnetCommands[248] = "EL"; telnetCommands[249] = "GA"; telnetCommands[250] = "SB"; telnetCommands[251] = "WILL";
        telnetCommands[252] = "WONT"; telnetCommands[253] = "DO"; telnetCommands[254] = "DONT"; telnetCommands[255] = "IAC";
    }

    public static String byteToHexString(byte input) {
        return String.format("0x%02X:", input);
    }

    public static String byteToHexString(byte[] input) {
        StringBuilder sb = new StringBuilder();
        for(byte b: input) {
            sb.append(String.format("%02X:", b));
        }
        sb.deleteCharAt(sb.length()-1);
        return sb.toString();
    }

    public static String byteToDecString(byte[] input) {
        StringBuilder sb = new StringBuilder();
        for(byte b: input) {
            sb.append(String.format("%2d:", b & 0xff));
        }
        sb.deleteCharAt(sb.length()-1);
        return sb.toString();
    }

    public static int[] byteToDec(byte[] input) {
        int[] res = new int[input.length];
        for(int i=0; i<res.length; i++) {
            res[i] = input[i] & 0xff;
        }
        return res;
    }

    public static String unEscapeExceptNT(String s){
        StringBuilder sb = new StringBuilder();
        for (int i=0; i<s.length(); i++)
            switch (s.charAt(i)){
                case '\b': sb.append("\\b"); break;
                case '\n': sb.append("\\n\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\f': sb.append("\\f"); break;
                case '\'': sb.append("\\'"); break;
                // ... rest of escape characters
//                case '\t': sb.append("\\t"); break;
                default: sb.append(s.charAt(i));
            }
        return sb.toString();
    }



    /* Add the Telnet Parsing Function Here in Util */
    public static void prettyPrintTelnetSession(Session session) {
        if (session == null) {
            return;
        }
        // Print out some information
        System.out.println("******************************************************");
        System.out.println("******************New Session Details*****************");
        System.out.println("******************************************************");

        System.out.println("Session Application Type: " + session.getApplicationType());
        System.out.println("Session Start Time: " + new Date(session.getSessionStartTimestamp()).toString());
        System.out.println("Session End Time: " + new Date(session.getSessionEndTimestamp()).toString());
        System.out.println("Total Packets Transferred: " + session.getTotalPacketNumber());
        System.out.println("Client-side Packets: " + session.getClientPacketNumber());
        System.out.println("Server-side Packets: " + session.getServerPacketNumber());
        System.out.println(session.getClientPhysicalInformation());
        System.out.println(session.getServerPhysicalInformation());

        for (String option : session.learnOptionsForTelnet()) {
            System.out.println(option);
        }

        System.out.println("Application-level Contents:");
        for (String str: session.unionOperationsForTelnet()) {
            System.out.print(str);
        }
        System.out.println();
        System.out.println();
    }

    public static void prettyPrintFTPSession(Session session) {
        if (session == null) {
            return;
        }
        // Print out some information
        System.out.println("******************************************************");
        System.out.println("****************  New Session Details  ***************");
        System.out.println("******************************************************");

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

    public static void showNotes() {
        System.out.println("Some Notes on this program: ");
        System.out.println("\tTelnet Special Characters are defined as follows: 0 -> NUL, 7 -> BEL, 11 -> VT");
        System.out.println("\tOther Special Characters are ASCII normal characters, which will be in explicit form.");
        System.out.println("\tFor example, TAB will be printed as '\\t' and CR will be printed as '\\r', etc.");
        System.out.println("\tActually, for better layouts, the LF is replaced by '\\n' plus a real line feed.");
        System.out.println("\tOtherwise, the data will be squeezed into only 1 line.");
        System.out.println("\tThis becomes severe on TELNET/HTTP application scenarios");
        System.out.println("\tBesides, this program is able to catch the escaped sequences used in Telnet application");
        System.out.println("\tHowever, those are used to adjust font properties, which would influence the general");
        System.out.println("\tlayout of the messages. Thus, they are omitted mostly. ");
        System.out.println("\tThere's only 1 valid parsing for it, which is at the end of TELNET session.");
    }
}
