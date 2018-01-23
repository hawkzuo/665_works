package examples;
/*
  Created by Amos on 2018/1/22.
  Sample Output:
  Network devices found:
 #0: \Device\NPF_{BF953A1A-3058-4FB7-9B8D-026621F9234E} [Realtek PCIe GBE Family Controller]
 #1: \Device\NPF_{4D799157-E162-41BE-A8E6-F3FC3B7E0668} [Microsoft]
 #2: \Device\NPF_{3D9510CC-DFAC-4646-BABE-1CDE6AC4930F} [Microsoft]

 Choosing 'Realtek PCIe GBE Family Controller' on your behalf:
 Received packet at Mon Jan 22 17:15:54 CST 2018 caplen=1067 len=1067 jNetPcap rocks!
 Received packet at Mon Jan 22 17:15:54 CST 2018 caplen=88   len=88   jNetPcap rocks!
 Received packet at Mon Jan 22 17:15:54 CST 2018 caplen=88   len=88   jNetPcap rocks!
 Received packet at Mon Jan 22 17:15:54 CST 2018 caplen=68   len=68   jNetPcap rocks!
 Received packet at Mon Jan 22 17:15:54 CST 2018 caplen=68   len=68   jNetPcap rocks!
 Received packet at Mon Jan 22 17:15:54 CST 2018 caplen=216  len=216  jNetPcap rocks!
 Received packet at Mon Jan 22 17:15:54 CST 2018 caplen=305  len=305  jNetPcap rocks!
 Received packet at Mon Jan 22 17:15:54 CST 2018 caplen=66   len=66   jNetPcap rocks!
 Received packet at Mon Jan 22 17:15:54 CST 2018 caplen=92   len=92   jNetPcap rocks!
 Received packet at Mon Jan 22 17:15:55 CST 2018 caplen=60   len=60   jNetPcap rocks!

 */

import org.jnetpcap.Pcap;
import org.jnetpcap.PcapIf;
import org.jnetpcap.packet.PcapPacket;
import org.jnetpcap.packet.PcapPacketHandler;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ClassicPcapExample {

    /**
     * Main startup method
     *
     * @param args
     *          ignored
     */
    public static void main(String[] args) {
        List<PcapIf> alldevs = new ArrayList<PcapIf>(); // Will be filled with NICs
        StringBuilder errbuf = new StringBuilder(); // For any error msgs

        /***************************************************************************
         * First get a list of devices on this system
         **************************************************************************/
        int r = Pcap.findAllDevs(alldevs, errbuf);
        if (r == Pcap.NOT_OK || alldevs.isEmpty()) {
            System.err.printf("Can't read list of devices, error is %s", errbuf
                    .toString());
            return;
        }

        System.out.println("Network devices found:");

        int i = 0;
        for (PcapIf device : alldevs) {
            String description =
                    (device.getDescription() != null) ? device.getDescription()
                            : "No description available";
            System.out.printf("#%d: %s [%s]\n", i++, device.getName(), description);
        }

        PcapIf device = alldevs.get(0); // We know we have atleast 1 device
        System.out
                .printf("\nChoosing '%s' on your behalf:\n",
                        (device.getDescription() != null) ? device.getDescription()
                                : device.getName());

        /***************************************************************************
         * Second we open up the selected device
         **************************************************************************/
        int snaplen = 64 * 1024;           // Capture all packets, no trucation
        int flags = Pcap.MODE_PROMISCUOUS; // capture all packets
        int timeout = 10 * 1000;           // 10 seconds in millis
        Pcap pcap =
                Pcap.openLive(device.getName(), snaplen, flags, timeout, errbuf);

        if (pcap == null) {
            System.err.printf("Error while opening device for capture: "
                    + errbuf.toString());
            return;
        }

        /***************************************************************************
         * Third we create a packet handler which will receive packets from the
         * libpcap loop.
         **************************************************************************/
        PcapPacketHandler<String> jpacketHandler = new PcapPacketHandler<String>() {

            public void nextPacket(PcapPacket packet, String user) {

                System.out.printf("Received packet at %s caplen=%-4d len=%-4d %s\n",
                        new Date(packet.getCaptureHeader().timestampInMicros()),
                        packet.getCaptureHeader().caplen(),  // Length actually captured
                        packet.getCaptureHeader().wirelen(), // Original length
                        user                                 // User supplied object
                );
            }
        };

        /***************************************************************************
         * Fourth we enter the loop and tell it to capture 10 packets. The loop
         * method does a mapping of pcap.datalink() DLT value to JProtocol ID, which
         * is needed by JScanner. The scanner scans the packet buffer and decodes
         * the headers. The mapping is done automatically, although a variation on
         * the loop method exists that allows the programmer to sepecify exactly
         * which protocol ID to use as the data link type for this pcap interface.
         **************************************************************************/
        pcap.loop(10, jpacketHandler, "jNetPcap rocks!");

        /***************************************************************************
         * Last thing to do is close the pcap handle
         **************************************************************************/
        pcap.close();
    }
}

