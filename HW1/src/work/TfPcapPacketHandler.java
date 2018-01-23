package work;

import org.jnetpcap.PcapPktHdr;
import org.jnetpcap.packet.PcapPacket;
import org.jnetpcap.packet.PcapPacketHandler;
import org.jnetpcap.protocol.lan.Ethernet;
import org.jnetpcap.protocol.network.Ip4;
import org.jnetpcap.protocol.tcpip.Tcp;

import java.util.*;

import static java.lang.System.exit;

/**
 * Created by Amos on 2018/1/22.
 */
public class TfPcapPacketHandler implements PcapPacketHandler<Map<String, TreeMap<Long, Session>>> {

    private Ethernet eth = new Ethernet();
    private Ip4 ip4 = new Ip4();
    private Tcp tcp = new Tcp();
    private  Set<Integer> supportedPorts;
    public TfPcapPacketHandler() {
        this.supportedPorts = new HashSet<>();
        supportedPorts.add(21);
        supportedPorts.add(23);
        supportedPorts.add(80);
    }



    @Override
    public void nextPacket(PcapPacket packet, Map<String, TreeMap<Long, Session>> map) {
        // Do something on the received packet

        // This shows everything [Header / Formatting / Payload] in a byte-oriented way
        // Show each part of data from 1st bit to last
        System.out.println(packet.toString());
        // IP.type ref: https://en.wikipedia.org/wiki/List_of_IP_protocol_numbers



        // Step 0: Some Checkings
        // Check Existence of Ethernet IP TCP headers
        if (!packet.hasHeader(eth) || !packet.hasHeader(ip4) || !packet.hasHeader(tcp)) {
            return;
        }
        // Check for CheckSum
        if (!ip4.checksumDescription().equals("correct") || !tcp.checksumDescription().equals("correct")) {
            return;
        }
        // Check Supported ports: 23 for Telnet 21 for FTP 80 for HTTP
        if (!supportedPorts.contains(tcp.destination()) && !supportedPorts.contains(tcp.source())) {
            return;
        }

        // Step 1: Generate Key [sourceIP:port destIP:port]
        String[] keyArray = {String.valueOf(ip4.destinationToInt()) + ":" + tcp.destination(), String.valueOf(ip4.sourceToInt()) + ":" + tcp.source()};
        Arrays.sort(keyArray);

        String connectionKey = keyArray[0] + " " + keyArray[1];

        // Step 2: Add packet to Corresponding Session
        if (tcp.flags() == 2) {
            // A SYN packet, which is the start of a session
            Long connectionTimestamp = packet.getCaptureHeader().timestampInMillis();

            TreeMap<Long, Session> sessionsForConnection;
            if(map.containsKey(connectionKey)) {
                sessionsForConnection = map.get(connectionKey);
            } else {
                sessionsForConnection = new TreeMap<>();
            }
            sessionsForConnection.put(connectionTimestamp, new Session(packet));
            map.put(connectionKey, sessionsForConnection);

        } else {
            // Other Packets, add to corresponding position
            Long connectionTimestamp = packet.getCaptureHeader().timestampInMillis();
            TreeMap<Long, Session> sessionsForConnection = map.get(connectionKey);
            if(sessionsForConnection == null) {
                System.err.println("Error");
//                return;
                exit(1);
            }
            Long sessionStartTimestamp = sessionsForConnection.floorKey(connectionTimestamp);
            if(sessionStartTimestamp == null) {
                System.err.println("Error");
//                return;
//                exit(1);
            }
            sessionsForConnection.get(sessionStartTimestamp).addLast(packet);

        }

        System.out.println("Finished Processing 1 packet");
//        System.out.printf("Received at %s caplen=%-4d len=%-4d %s\n",
//            new Date(packet.getCaptureHeader().timestampInMillis()),
//            packet.getCaptureHeader().caplen(), // Length actually captured
//            packet.getCaptureHeader().wirelen(), // Original length
//            t.toString() // User supplied object
//        );
    }
}
