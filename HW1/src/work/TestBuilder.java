package work;

import org.jnetpcap.Pcap;
import org.jnetpcap.PcapIf;

import java.util.*;

/**
 * Created by Amos on 2018/1/22.
 */
public class TestBuilder {

    public static void references() {
        // Fetch all Internet Interfaces into List ifs
        StringBuilder errbuf = new StringBuilder();
        List<PcapIf> ifs = new ArrayList<PcapIf>();
        int statusCode = Pcap.findAllDevs(ifs, errbuf);
        if (statusCode != Pcap.OK) {
            System.out.println("Error occured: " + errbuf.toString());
            return;
        }

        // Select one of the interface
        for (int i = 0; i < ifs.size(); i++) {
            System.out.println("#" + i + ": " + ifs.get(i).getName());
        }
        Integer i = 0;
        PcapIf netInterface = ifs.get(i);


    }



    public static void main(String[] args) {
        // Start point of jNetPCap is class Pcap
        // Pcap stands for Pcapture ?
        // Main Static Methods:
        // openLive: Live Network
        // openOffline: From File
        // openDead: Dummy Pcap Session


        final StringBuilder errorBuffer = new StringBuilder(); // For any error msgs

        final String fileName = "HW1/test/tfsession.pcap";
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
                    Util.prettyPrintSession(instance);

                }

            }

        } finally {
            /***************************************************************************
             * Last thing to do is close the pcap handle
             **************************************************************************/
            pcap.close();
        }




//        System.out.println("Successfully opened local file: " +fileName);

    }


}
