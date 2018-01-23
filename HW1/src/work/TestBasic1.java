package work;

import org.jnetpcap.Pcap;
import org.jnetpcap.PcapIf;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Amos on 2018/1/22.
 */
public class TestBasic1 {

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

//        final String fileName = "HW1/test/tfsession.pcap";
        final String fileName = "HW1/test/test-l2tp.pcap";

        final Pcap pcap = Pcap.openOffline(fileName, errorBuffer);

        // Error Checking
        if (pcap == null) {
            System.err.print("Error while opening device for capture: "
                    + errorBuffer.toString());
            return;
        }

        TfPcapPacketHandler<String> packetHandler = new TfPcapPacketHandler<>();

        try {
            // statusCode:
            // -1 on ERROR
            // 0 on cnt exhausted
            // -2 on pcap_breakloop() call
            int statusCode = pcap.loop(10000, packetHandler, "testing");
            System.out.println("StatusCode: "+ statusCode);
        } finally {
            /***************************************************************************
             * Last thing to do is close the pcap handle
             **************************************************************************/
            pcap.close();
        }




        System.out.println("Successfully opened local file: " +fileName);

    }


}
