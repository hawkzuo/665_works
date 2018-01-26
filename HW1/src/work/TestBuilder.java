package work;

import org.jnetpcap.Pcap;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by Amos on 2018/1/22.
 */
public class TestBuilder {


    public static void main(String[] args) {
        // Start point of jNetPCap is class Pcap
        // Pcap stands for Pcapture ?
        // Main Static Methods:
        // openLive: Live Network
        // openOffline: From File
        // openDead: Dummy Pcap Session

        final StringBuilder errorBuffer = new StringBuilder(); // For any error msgs

//        final String fileName = "HW1/test/tfsession.pcap";
        final String fileName = "HW1/test/httpsession.pcap";
//        final String fileName = "HW1/test/test-ipreassembly.pcap";

        final Pcap pcap = Pcap.openOffline(fileName, errorBuffer);

        // Error Checking
        if (pcap == null) {
            System.err.print("Error while opening device for capture: "
                    + errorBuffer.toString());
            return;
        }

        TfPcapPacketHandler packetHandler = new TfPcapPacketHandler();
        Map<String, TreeMap<Long, Session>> holder = new HashMap<>();

        try {
            Util.showNotes();

            // statusCode:
            // -1 on ERROR
            // 0 on cnt exhausted
            // -2 on pcap_breakloop() call
            int statusCode = pcap.loop(Integer.MAX_VALUE, packetHandler, holder);
            if (statusCode != 0) {
                System.err.print("Error while processing packets");
            }

            for(String connectionPairs : holder.keySet()) {
                TreeMap<Long, Session> connectionsMap = holder.get(connectionPairs);

                for(Long sessionStartTime: connectionsMap.keySet()) {
                    Session instance = connectionsMap.get(sessionStartTime);
                    switch (instance.getApplicationType()) {
                        case "FTP":
                            Util.prettyPrintFTPSession(instance);
                            break;
                        case "TELNET":
                            Util.prettyPrintTelnetSession(instance);
                            instance.learnOptionsForTelnet();
                            break;
                        default:
                            String data = Util.decodeHTTPPayloadToPrintable(instance);
                            System.out.println("Successfully opened local file: " +fileName);

                            break;
                    }
                }
            }

        } finally {
            pcap.close();
        }

//        System.out.println("Successfully opened local file: " +fileName);
    }
}
