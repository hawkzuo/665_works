package work;

import org.jnetpcap.packet.PcapPacket;
import org.jnetpcap.protocol.lan.Ethernet;
import org.jnetpcap.protocol.network.Ip4;
import org.jnetpcap.protocol.tcpip.Tcp;

import java.util.ArrayList;
import java.util.List;

import static work.Util.supportedApplicationTypes;

/**
 * Created by Amos on 2018/1/23.
 *
 * Timestamp is in micro seconds
 */
public class Session {


    private byte[] serverIP;
    private byte[] clientIP;
    private int serverPort;
    private int clientPort;

    private long sessionStartTimestamp;
    private long sessionEndTimestamp;
    private String applicationType;

    private List<PcapPacket> serverPackets;
    private List<PcapPacket> clientPackets;

    Session(PcapPacket head, Tcp tcp, Ip4 ip4 , Ethernet eth) {
        this.clientPackets = new ArrayList<>();
        this.serverPackets = new ArrayList<>();

        this.sessionStartTimestamp = head.getCaptureHeader().timestampInMicros();

        this.clientIP = ip4.source();
        this.clientPort = tcp.source();
        this.serverIP = ip4.destination();
        this.serverPort = tcp.destination();

//        supportedApplicationTypes
        this.applicationType = supportedApplicationTypes.get(clientPort) == null ?
                supportedApplicationTypes.get(serverPort) : supportedApplicationTypes.get(clientPort);

        this.clientPackets.add(head);
    }


    public int addPacket(PcapPacket element, Tcp tcp, Ip4 ip4 , Ethernet eth) {

        if (tcp.source() == serverPort) {
            // This is a packet from the server
            serverPackets.add(element);


            return serverPackets.size();
        } else {
            clientPackets.add(element);


            return clientPackets.size();
        }

    }



    @Override
    public String toString() {
        return "ServerPackets size: " + serverPackets.size() + "ClientPackets size: " + clientPackets.size();
    }

    public byte[] getServerIP() {
        return serverIP;
    }

    public byte[] getClientIP() {
        return clientIP;
    }
}
