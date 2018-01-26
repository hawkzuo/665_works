package work;

import org.jnetpcap.packet.PcapPacket;
import org.jnetpcap.packet.PcapPacketHandler;
import org.jnetpcap.protocol.lan.Ethernet;
import org.jnetpcap.protocol.network.Ip4;
import org.jnetpcap.protocol.tcpip.Tcp;

import java.util.*;

import static java.lang.System.exit;
import static work.Util.supportedApplicationTypes;

/**
 * Created by Amos on 2018/1/22.
 */
public class TfPcapPacketHandler implements PcapPacketHandler<Map<String, TreeMap<Long, Session>>> {

    private Ethernet eth = new Ethernet();
    private Ip4 ip4 = new Ip4();
    private Tcp tcp = new Tcp();

    private static int global = 0;


    private void processForHTTP(PcapPacket packet, Map<String, TreeMap<Long, Session>> map) {
        if (packet == null) {   return; }
        // Step 1: Generate Key [sourceIP:port destIP:port]
        String[] keyArray = {String.valueOf(ip4.destinationToInt()) + ":" + tcp.destination(), String.valueOf(ip4.sourceToInt()) + ":" + tcp.source()};
        Arrays.sort(keyArray);
        String connectionKey = keyArray[0] + " " + keyArray[1];

        // Step 2: We don't distinguish different sessions between two connection Pairs
        // Client: 145.254.160.237  // Server: 65.208.228.223
        Long connectionTimestamp = packet.getCaptureHeader().timestampInMicros();
        TreeMap<Long, Session> sessionsForConnection;
        if(map.containsKey(connectionKey)) {
            sessionsForConnection = map.get(connectionKey);
        } else {
            sessionsForConnection = new TreeMap<>();
        }
        Long sessionStartTimestamp = sessionsForConnection.floorKey(connectionTimestamp);
        if (sessionStartTimestamp == null) {
            // Beginning of a new session
            sessionsForConnection.put(connectionTimestamp, new Session(packet, tcp, ip4, eth));
        } else {
            // Add packet to existing session
            sessionsForConnection.get(sessionStartTimestamp).addPacket(packet, tcp, ip4, eth);
        }
        map.put(connectionKey, sessionsForConnection);
    }

    @Override
    public void nextPacket(PcapPacket packet, Map<String, TreeMap<Long, Session>> map) {
        global ++;
        System.out.println("Global: " + global);
        // Do something on the received packet

        // This shows everything [Header / Formatting / Payload] in a byte-oriented way
        // Show each part of data from 1st bit to last
        // System.out.println(packet.toString());
        // IP.type ref: https://en.wikipedia.org/wiki/List_of_IP_protocol_numbers

        // Step 0: Some Checkings
        // Check Existence of Ethernet IP TCP headers
        if (packet == null || !packet.hasHeader(eth) || !packet.hasHeader(ip4) || !packet.hasHeader(tcp)) {
            return;
        }
        // Check for CheckSum
        if (!ip4.checksumDescription().equals("correct") || !tcp.checksumDescription().equals("correct")) {
            return;
        }
        // Check Supported ports: 23 for Telnet 21 for FTP 80 for HTTP
        if (!supportedApplicationTypes.containsKey(tcp.destination()) && !supportedApplicationTypes.containsKey(tcp.source())) {
            return;
        }

        if (tcp.destination() == 80 || tcp.source() == 80) {
            // HTTP
            processForHTTP(packet, map);
            return;
        }

        // Step 1: Generate Key [sourceIP:port destIP:port]
        String[] keyArray = {String.valueOf(ip4.destinationToInt()) + ":" + tcp.destination(), String.valueOf(ip4.sourceToInt()) + ":" + tcp.source()};
        Arrays.sort(keyArray);

        String connectionKey = keyArray[0] + " " + keyArray[1];

        // Step 2: Add packet to Corresponding Session
        if (tcp.flags() == 2) {
            // A SYN packet, which is the start of a session
            Long connectionTimestamp = packet.getCaptureHeader().timestampInMicros();

            TreeMap<Long, Session> sessionsForConnection;
            if(map.containsKey(connectionKey)) {
                sessionsForConnection = map.get(connectionKey);
            } else {
                sessionsForConnection = new TreeMap<>();
            }
            sessionsForConnection.put(connectionTimestamp, new Session(packet, tcp, ip4, eth));
            map.put(connectionKey, sessionsForConnection);

        } else {
            // Other Packets, add to corresponding position
            Long connectionTimestamp = packet.getCaptureHeader().timestampInMicros();
            TreeMap<Long, Session> sessionsForConnection = map.get(connectionKey);
            if(sessionsForConnection == null) {
                System.err.println("Error");
                return;
//                exit(1);
            }
            Long sessionStartTimestamp = sessionsForConnection.floorKey(connectionTimestamp);
            if(sessionStartTimestamp == null) {
                System.err.println("Error");
                return;
//                exit(1);
            }
            sessionsForConnection.get(sessionStartTimestamp).addPacket(packet, tcp, ip4, eth);

        }

//        System.out.println("Finished Processing 1 packet");
    }
}
