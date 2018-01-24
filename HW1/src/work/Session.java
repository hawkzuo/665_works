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

    // server: positive     client: negative
    private List<Integer> operationsFlag;

    private List<PcapPacket> packets;
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
        this.operationsFlag = new ArrayList<>();
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



        this.packets = new ArrayList<>();

    }

    public int addPacket(PcapPacket element, Tcp tcp, Ip4 ip4 , Ethernet eth) {
        packets.add(element);
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
                        sb.append(Util.unEscapeExceptNT(new String(payload.data(), "UTF-8")));
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    operationsList.add(sb.toString());
                }
            } else if (applicationType.equals("TELNET")) {
                // Client-side message is terminated by '\r' ?
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
                    // Used to add essential space between text data & the commands
                    boolean pureTextMode = true;
                    // Set if next char is a IAC command
                    boolean nextCharCommand = false;
                    // 1 if next char is normal option, 2 if next char is a sub option (With "SB" proceed)
                    int nextCharOption = 0;
                    // Set to the optionNumber of a sub option, which will be used to parse this specific option type
                    // With format "IAC SB optionNumber ? <data> IAC SE"
                    int optionNumber = -1;
                    // Set if next char is the first char of a sub option data fields
                    boolean firstSubOption = false;
                    int i = 0;
                    while (i < data.length) {
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
                            // Always update
                            optionNumber = -1;
                        } else if (nextCharOption == 2) {
                            // Change strategy, let "IS SEND" done by parsing text
                            String option = telnetOptions[step];
                            operation.append(option);
                            operation.append(' ');
                            nextCharOption = 0;
                            // Record the operation number here
                            optionNumber = data[i];
                            firstSubOption = true;

                        } else if (nextCharOption == 1) {
                            // Normal Option comes here
                            String option = telnetOptions[step];
                            operation.append(option);
                            operation.append(' ');
                            nextCharOption = 0;

                            optionNumber = -1;
                        } else {
                            // Double IAC should be taken care of

                            if (step == 255 && (i+1 < data.length) && data[i+1] != 255) {
                                // IAC encountered & not doubled
                                if (pureTextMode && i != 0) {
                                    operation.append(' ');
                                }
                                operation.append(telnetCommands[255]);
                                operation.append(' ');
                                nextCharCommand = true;
                                pureTextMode = false;
                            } else {
                                // Normal char

                                // First check if optionNumber is -1 to distinguish either SB or real normal char
                                if (optionNumber == -1) {
                                    // This is a real normal char
                                    if (step == 27) {
                                        // we encountered a ESC char, escape
                                        i ++;
                                        StringBuilder sequence = new StringBuilder();
                                        while (i < data.length) {
                                            if (Util.telnetEscapeCharacters.contains(data[i])) {
                                                sequence.append((char) data[i]);
                                                i++;
                                            } else {
                                                sequence.append((char) data[i]);
                                                break;
                                            }
                                        }
                                        // Reference: http://ascii-table.com/ansi-escape-sequences.php
                                        // Already detected escape sequences, but due to beautiful layouts, omit showing
                                        if (sequence.toString().equals("[H")) {
                                            operation.append("MOVE-CURSOR-TOP-LEFT ");
                                        } else if (sequence.toString().equals("[2J")) {
                                            operation.append("ERASE-DISPLAY ");
                                        }
                                    } else if ( Util.telnetSpecialCharacters.containsKey(step)) {
                                        operation.append(Util.telnetSpecialCharacters.get(step));
                                    } else {
                                        operation.append((char) step);
                                    }




                                } else if (optionNumber == 10 || optionNumber == 11 || optionNumber == 12 ||
                                        optionNumber == 13 || optionNumber == 14 || optionNumber == 15 ||
                                        optionNumber == 16 || optionNumber == 17 || optionNumber == 26) {
                                    // Those modes just need to convert to Hex
                                    // NAOCRD NAOHTS NAOHTD NAOFFD NAOVTS NAOVTD NAOLFD EXTEND-ASCII TUID modes,
                                    // should append this 8-bit value instead of converting to char
                                    operation.append(Util.byteToHexString(payload.data()[i]));
                                } else if (optionNumber == 28){
                                    // TTYLOC fist is <format> <64-bit>
                                    if (firstSubOption) {
                                        // Format Bit
                                        operation.append(String.format("Format: %d ", step));
                                        // Rest 64-bits
                                        if(i + 8 >= data.length) {
                                            operation.append("ERROR-FORMATTED-TTYLOC");
                                        } else {
                                            int counter = 0;
                                            while (counter < 8) {
                                                operation.append(Util.byteToHexString(payload.data()[++i]));
                                                counter ++;
                                            }
                                        }
                                    }
                                } else if (optionNumber == 29) {
                                    // 3270-REGIME
                                    if (firstSubOption) {
                                        switch (step) {
                                            case 0:
                                                operation.append("IS ");
                                                break;
                                            case 1:
                                                operation.append("ARE ");
                                                break;
                                            default:
                                                operation.append("ERROR-FORMATTED-3270-REGIME");
                                                break;
                                        }
                                    } else {
                                        // REGIME normal char
                                        operation.append((char) step);
                                    }
                                } else if (optionNumber == 30) {
                                    // X.3-PAD
                                    if (firstSubOption) {
                                        switch (step) {
                                            case 0:
                                                operation.append("SET ");
                                                break;
                                            case 1:
                                                operation.append("RESPONSE-SET ");
                                                break;
                                            case 2:
                                                operation.append("IS ");
                                                break;
                                            case 3:
                                                operation.append("RESPONSE-IS ");
                                                break;
                                            case 4:
                                                operation.append("SEND ");
                                                break;
                                            default:
                                                operation.append("ERROR-FORMATTED-3270-REGIME");
                                                break;
                                        }
                                    } else {
                                        // <param1> <value1>
                                        operation.append(step);
                                        operation.append(" ");
                                    }
                                } else if (optionNumber == 31) {
                                    // IAC SB NAWS <16-bit value> <16-bit value> IAC SE
                                    if(i + 4 >= data.length) {
                                        operation.append("ERROR-FORMATTED-NAWS");
                                    } else {
                                        int widthHigh = step;
                                        int widthLow = data[++i];
                                        int heightHigh = data[++i];
                                        int heightLow = data[++i];

                                        int width = widthHigh * 256 + widthLow;
                                        int height = heightHigh * 256 + heightLow;
                                        operation.append(String.format("WIDTH-%d HEIGHT-%d", width, height));
                                    }
                                } else if (optionNumber == 32) {
                                    // TERMINAL-SPEED
                                    if (firstSubOption) {
                                        switch (step) {
                                            case 0:
                                                operation.append("IS ");
                                                break;
                                            case 1:
                                                operation.append("SEND ");
                                                break;
                                            default:
                                                operation.append("ERROR-FORMATTED-TERMINAL-SPEED");
                                                break;
                                        }
                                    } else {
                                        // ASCII char
                                        operation.append((char) step);
                                    }
                                } else if (optionNumber == 33) {
                                    // TOGGLE-FLOW-CONTROL
                                    // Only 1 byte
                                    if (firstSubOption) {
                                        switch (step) {
                                            case 0:
                                                operation.append("OFF");
                                                break;
                                            case 1:
                                                operation.append("ON");
                                                break;
                                            case 2:
                                                operation.append("RESTART-ANY");
                                                break;
                                            case 3:
                                                operation.append("RESTART-XON");
                                                break;
                                            default:
                                                operation.append("ERROR-FORMATTED-TOGGLE-FLOW-CONTROL");
                                                break;
                                        }
                                    }
                                } else if (optionNumber == 34) {
                                    // LINEMODE complex

                                } else if (optionNumber == 35) {
                                    // X-DISPLAY-LOCATION
                                    if (firstSubOption) {
                                        switch (step) {
                                            case 0:
                                                operation.append("IS ");
                                                break;
                                            case 1:
                                                operation.append("SEND");
                                                break;
                                            default:
                                                operation.append("ERROR-FORMATTED-X-DISPLAY-LOCATION");
                                                break;
                                        }
                                    } else {
                                        // <host>:<dispnum> ASCII char
                                        operation.append((char) step);
                                    }
                                } else if (optionNumber == 36) {
                                    // ENVIRON complex

                                } else if (optionNumber == 37) {
                                    // AUTHENTICATION complex

                                } else if (optionNumber == 38) {
                                    // ENCRYPT complex model

                                }
//                                else if (optionNumber == 39) {
//                                    // NEW-ENVIRON complex model recursive
//
//                                }
                                else if (optionNumber == 40) {
                                    // TN3270 complex model recursive

                                } else if (optionNumber == 42) {
                                    // CHARSET complex but easier

                                } else if (optionNumber == 44) {
                                    // COM-PORT-OPTION
                                } else if (optionNumber == 47) {
                                    // KERMIT
                                }
                                else{
                                    // Default number routing here
                                    if (firstSubOption) {
                                        String command = telnetCommands[step];
                                        operation.append(command);
                                    } else {
                                        operation.append((char) step);
                                    }
                                }
                                nextCharCommand = false;
                                pureTextMode = true;
                                // Always set this to false
                                firstSubOption = false;

                                // Normal char needs to take care of Double IAC case
                                if (step == 255) {
                                    // Skip 1 IAC
                                    i++;
                                }

                            }
                        }
                        i++;
                    }
                    operationsList.add(operation.toString());
                    if (tcp.source() == serverPort) {
                        operationsFlag.add(1);
                    } else {
                        operationsFlag.add(-1);
                    }

//                    System.out.println();
                }
//                System.out.println();
            } else {
//                System.out.println();
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

    public List<String> unionOperationsForTelnet() {
        List<String> result = new ArrayList<>();

        int clientCursor = -1;
        int serverCursor = -1;

        for (int i=0; i<operationsList.size(); i++) {
            String operation = operationsList.get(i);
            StringBuilder step = new StringBuilder();

            if (this.operationsFlag.get(i) > 0) {
                // Server
                if (i > serverCursor) {
                    step.append(operation);
                    if (operation.length() == 1) {
                        // Start unionning
                        int j = i + 1;
                        while (j < operationsList.size()) {
                            if (operationsFlag.get(j) > 0) {
                                step.append(operationsList.get(j));
                                if (operationsList.get(j).equals("\r\n")) {
                                    serverCursor = j;
                                    break;
                                }
                            }
                            j++;
                        }
                    } else {
                        int j = i + 1;
                        while (j < operationsList.size() && operationsFlag.get(j) > 0) {
                            step.append(operationsList.get(j));
                            serverCursor = j;
                            j++;
                        }
                    }
                    result.add("\nSERVER:\n" + Util.unEscapeExceptNT(step.toString()));
                }
            } else {
                // client
                if (i > clientCursor) {
                    step.append(operation);
                    if (operation.length() == 1) {
                        // Start unionning
                        int j = i + 1;
                        while (j < operationsList.size()) {
                            if (operationsFlag.get(j) < 0) {
                                step.append(operationsList.get(j));
                                if (operationsList.get(j).equals("\rNUL")) {
                                    clientCursor = j;
                                    break;
                                }
                            }
                            j++;
                        }
                    } else {
                        int j = i + 1;
                        while (j < operationsList.size() && operationsFlag.get(j) < 0) {
                            step.append(operationsList.get(j));
                            clientCursor = j;
                            j++;
                        }
                    }
                    result.add("\nCLIENT:\n" + Util.unEscapeExceptNT(step.toString()));
                }
            }
        }



//        for (int i=0; i<this.operationsList.size(); i++) {
//            String operation = operationsList.get(i);
//            StringBuilder step = new StringBuilder();
//            if (this.operationsFlag.get(i) > 0) {
//                // Server
//                if (i <= serverCursor) {
//                    continue;
//                }
//                step.append("\nSERVER:\n");
//                if (operation.length() == 1) {
//                    // Start unionning
//                    step.append(operation);
//                    int j = i + 1;
//                    while (j < operationsList.size()) {
//                        if (operationsFlag.get(j) > 0) {
//                            step.append(operationsList.get(j));
//                            if (operationsList.get(j).equals("\r\n")) {
//                                serverCursor = j;
//                                break;
//                            }
//                        }
//                        j++;
//                    }
//                } else {
//                    step.append(operation);
//                }
//                result.add(step.toString());
//            } else {
//                // Client
//                if (i <= clientCursor) {
//                    continue;
//                }
//                step.append("\nCLIENT:\n");
//                if (operation.length() == 1) {
//                    // Start unionning
//                    step.append(operation);
//                    int j = i + 1;
//                    while (j < operationsList.size()) {
//                        if (operationsFlag.get(j) < 0) {
//                            step.append(operationsList.get(j));
//                            if (operationsList.get(j).equals("\rNUL")) {
//                                clientCursor = j;
//                                break;
//                            }
//                        }
//                        j++;
//                    }
//                } else {
//                    step.append(operation);
//                }
//                result.add(step.toString());
//            }
//        }








        return result;
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
