package work;

import org.jnetpcap.nio.JBuffer;
import org.jnetpcap.packet.Payload;
import org.jnetpcap.packet.PcapPacket;
import org.jnetpcap.protocol.lan.Ethernet;
import org.jnetpcap.protocol.network.Ip4;
import org.jnetpcap.protocol.tcpip.Tcp;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static work.Util.supportedApplicationTypes;

/**
 * Created by Amos on 2018/1/23.
 *
 * Timestamp is in micro seconds
 */
public class Session {

    private List<String> operationsList;
    private byte[] serverIP;
    private byte[] clientIP;
    private int serverPort;
    private int clientPort;
    private byte[] serverMacAddress;
    private byte[] clientMacAddress;

    private int clientPacketNumber;
    private int serverPacketNumber;



    // Application level fields
    private long sessionStartTimestamp;
    private long sessionEndTimestamp;
    private String applicationType;

    private boolean connectionEstablished;
    private boolean[] flags = new boolean[2];





    Session(PcapPacket head, Tcp tcp, Ip4 ip4 , Ethernet eth) {
        this.operationsList = new ArrayList<>();
        this.connectionEstablished = false;

        // This field is for Application Level usage
        this.sessionStartTimestamp = head.getCaptureHeader().timestampInMillis();
        this.sessionEndTimestamp = sessionStartTimestamp;

        this.clientIP = ip4.source();
        this.clientPort = tcp.source();
        this.clientMacAddress = eth.source();
        this.serverIP = ip4.destination();
        this.serverPort = tcp.destination();
        this.serverMacAddress = eth.destination();

//        supportedApplicationTypes
        this.applicationType = supportedApplicationTypes.get(clientPort) == null ?
                supportedApplicationTypes.get(serverPort) : supportedApplicationTypes.get(clientPort);

        this.clientPacketNumber = 1;
        this.serverPacketNumber = 0;
    }

    // 1st Version: Don't check ACK & SEQ numbers


    public int addPacket(PcapPacket element, Tcp tcp, Ip4 ip4 , Ethernet eth) {
        sessionEndTimestamp = Math.max(element.getCaptureHeader().timestampInMillis(), sessionEndTimestamp);
        if (tcp.source() == serverPort) {
            // This is a packet from the server
            serverPacketNumber ++;
        } else {
            // This is a packet from client
            clientPacketNumber ++;
        }

        if (!connectionEstablished) {
            if (tcp.flags() == 18) {
                flags[0] = true;
            } else if (tcp.flags() == 16) {
                flags[1] = true;
            }
            connectionEstablished = flags[0] && flags[1];
        } else {
            if(applicationType.equals("FTP")) {
                Payload payload = new Payload();
//                element.hasHeader(payload);
                if (element.hasHeader(payload) || tcp.flags() == 24) {
                    // A PSH+ACK packet
                    StringBuilder sb = new StringBuilder();

                    if (tcp.source() == serverPort) {
                        // This is a packet from the server
                        sb.append("\nSERVER Timestamp: ");
                    } else {
                        // This is a packet from client
                        sb.append("\nCLIENT Timestamp: ");
                    }
                    sb.append( new Date(element.getCaptureHeader().timestampInMillis()).toString());
                    sb.append(" Context: \n");
                    try {
                        sb.append(Util.unEscapeString(new String(payload.data(), "UTF-8")));
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    operationsList.add(sb.toString());
                }
            } else if (applicationType.equals("TELNET")) {
                Payload payload = new Payload();
                if (element.hasHeader(payload) || tcp.flags() == 24) {
                    // A PSH+ACK packet
                    StringBuilder sb = new StringBuilder();
                    if (tcp.source() == serverPort) {
                        // This is a packet from the server
                        sb.append("\nSERVER Timestamp: ");
                    } else {
                        // This is a packet from client
                        sb.append("\nCLIENT Timestamp: ");
                    }

                    System.out.println();

                }


                System.out.println();
            } else {
                System.out.println();

            }
        }

        return  0;
    }



    @Override
    public String toString() {
        return "Session: " + this.hashCode();
    }

    public byte[] getServerIP() {
        return serverIP;
    }

    public byte[] getClientIP() {
        return clientIP;
    }


    public List<String> getOperationsList() {
        return operationsList;
    }

    public String getApplicationType() {
        return applicationType;
    }

    public long getSessionStartTimestamp() {
        return sessionStartTimestamp;
    }

    public long getSessionEndTimestamp() {
        return sessionEndTimestamp;
    }

    public int getClientPacketNumber() {
        return clientPacketNumber;
    }

    public int getServerPacketNumber() {
        return serverPacketNumber;
    }

    public int getTotalPacketNumber() {
        return clientPacketNumber + serverPacketNumber;
    }

    public String getClientPhysicalInformation() {
        return String.format("Client Physical Information:\nMAC Address: %s IP Address: %s Port Number: %d",
                Util.byteToHex(clientMacAddress),
                Util.byteToDec(clientIP), clientPort);
    }
    public String getServerPhysicalInformation() {
        return String.format("Server Physical Information:\nMAC Address: %s IP Address: %s Port Number: %d",
                Util.byteToHex(serverMacAddress),
                Util.byteToDec(serverIP), serverPort);
    }



}
