package work;

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
import static work.Util.telnetCommands;
import static work.Util.telnetOptions;

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

    // 1st Version: Don'telnetCommands check ACK & SEQ numbers


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
                    sb.append( new Date(element.getCaptureHeader().timestampInMillis()).toString());
                    sb.append(" Context: \n");

                    int[] data = Util.byteToDec(payload.data());

                    // Convert the data into String
                    StringBuilder operation = new StringBuilder();
                    boolean pureTextMode = false; boolean nextCharCommand = false;
                    int nextCharOption = 0;
                    int operationNumber = -1; boolean firstSubOption = false;
                    for (int i = 0; i < data.length; i++) {
                        int step = data[i];
                        if (nextCharCommand) {

                            String command = telnetCommands[step];
                            operation.append(command);
                            operation.append(' ');
                            nextCharCommand = false;
                            switch (command) {
                                case "SB":
                                    nextCharOption = 2;
                                    break;
                                case "SE":
                                    nextCharOption = 0;
                                    break;
                                default:
                                    nextCharOption = 1;
                                    break;
                            }
                            pureTextMode = false;
                        } else if (nextCharOption == 2) {
                            String option = telnetOptions[step];
                            operation.append(option);
                            operation.append(' ');
                            nextCharOption = 11;
                        } else if (nextCharOption == 1) {
                            // Normal Option comes here
                            String option = telnetOptions[step];
                            operation.append(option);
                            operation.append(' ');
                            nextCharOption = 0;
                        } else if (nextCharOption == 11) {
                            // Separate subCommand "SB" case here
                            String command = telnetCommands[step];
                            operation.append(command);
                            operation.append(' ');
                            nextCharOption = 0;
                            // Record the operation number here
                            operationNumber = data[i-1];
                            firstSubOption = true;
                        } else {
                            if (step == 255) {
                                // IAC encountered
                                if (pureTextMode) {
                                    operation.append(' ');
                                }
                                operation.append(telnetCommands[255]);
                                operation.append(' ');
                                nextCharCommand = true;
                                pureTextMode = false;
                            } else {
                                // Normal char
                                if (operationNumber == 10 || operationNumber == 11 || operationNumber == 12 ||
                                        operationNumber == 13 || operationNumber == 14 || operationNumber == 15 ||
                                        operationNumber == 16 || operationNumber == 17 || operationNumber == 26) {
                                    // Those modes just need to convert to Hex
                                    // NAOCRD NAOHTS NAOHTD NAOFFD NAOVTS NAOVTD NAOLFD EXTEND-ASCII TUID modes,
                                    // should append this 8-bit value instead of converting to char
                                    operation.append(Util.byteToHexString(payload.data()[i]));
                                } else if (operationNumber == 28){
                                    // Those modes separate the first character & rest
                                    if (firstSubOption) {
                                        // TTYLOC fist is <format>
                                        operation.append( step);
                                    } else {
                                        operation.append(Util.byteToHexString(payload.data()[i]));
                                    }

                                    firstSubOption = false;
                                } else{
                                    if (step <= 4) {
                                        // Use a map to get the special char
                                        switch (operationNumber) {

                                        }
                                    } else {
                                        operation.append((char) step);
                                    }
                                }
                                nextCharCommand = false;
                                pureTextMode = true;
                            }
                        }
                    }
                    operationsList.add(operation.toString());


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
                Util.byteToHexString(clientMacAddress),
                Util.byteToDecString(clientIP), clientPort);
    }
    public String getServerPhysicalInformation() {
        return String.format("Server Physical Information:\nMAC Address: %s IP Address: %s Port Number: %d",
                Util.byteToHexString(serverMacAddress),
                Util.byteToDecString(serverIP), serverPort);
    }



}
