package edu.tamu.amos.hw1;

import org.jnetpcap.packet.Payload;
import org.jnetpcap.packet.PcapPacket;
import org.jnetpcap.protocol.application.Html;
import org.jnetpcap.protocol.lan.Ethernet;
import org.jnetpcap.protocol.network.Ip4;
import org.jnetpcap.protocol.tcpip.Http;
import org.jnetpcap.protocol.tcpip.Tcp;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Amos on 2018/1/23.
 *
 * Timestamp is in micro seconds
 */
public class Session {
    // server: positive     client: negative
    private List<Integer> operationsFlag;
    private List<String> operationsHeadersList;
    private List<String> operationsList;

    private List<PcapPacket> packets;

    private byte[] serverIP;
    private byte[] clientIP;
    private int serverPort;
    private int clientPort;
    private byte[] serverMacAddress;
    private byte[] clientMacAddress;

    private long sessionStartTimestamp;
    private long sessionEndTimestamp;
    private String applicationType;

    private int clientPacketNumber;
    private int serverPacketNumber;
    private boolean connectionEstablished;
    private boolean[] connectionEstablishingFlags = new boolean[2];

    // HTTP parser useful fields
    private Map<String, String> clientHTTPFields;
    private Map<String, String> serverHTTPFields;
    private String serverCharset;
    private ByteArrayOutputStream HTTPPayloadBuffer;

    Session(PcapPacket head, Tcp tcp, Ip4 ip4 , Ethernet eth) {
        this.operationsList = new ArrayList<>();
        this.operationsFlag = new ArrayList<>();
        this.operationsHeadersList = new ArrayList<>();
        this.packets = new ArrayList<>();
        packets.add(head);
        // HTTP
        this.clientHTTPFields = new HashMap<>();
        this.serverHTTPFields = new HashMap<>();
        this.serverCharset = null;
        this.HTTPPayloadBuffer = new ByteArrayOutputStream();

        // Physical Info
        this.clientIP = ip4.source();
        this.clientPort = tcp.source();
        this.clientMacAddress = eth.source();
        this.serverIP = ip4.destination();
        this.serverPort = tcp.destination();
        this.serverMacAddress = eth.destination();
        // TimeStamps
        this.sessionStartTimestamp = head.getCaptureHeader().timestampInMillis();
        this.sessionEndTimestamp = sessionStartTimestamp;
        // Application Type
        this.applicationType = Util.supportedApplicationTypes.get(clientPort) == null ?
                Util.supportedApplicationTypes.get(serverPort) : Util.supportedApplicationTypes.get(clientPort);

        this.clientPacketNumber = 1;
        this.serverPacketNumber = 0;
        this.connectionEstablished = false;

        // Generate HTTP operation for head packet
        if (applicationType.equals("HTTP")) {
            generateOperationForHTTP( head, tcp);
        }

    }

    // Helpers
    public void addPacket(PcapPacket element, Tcp tcp) {
        // Global updates
        sessionEndTimestamp = Math.max(element.getCaptureHeader().timestampInMillis(), sessionEndTimestamp);
        if (tcp.source() == serverPort) serverPacketNumber++;
        else clientPacketNumber++;

        packets.add(element);
        // HTTP separate
//        if (applicationType.equals("HTTP")) {
//            generateOperationForHTTP(element, tcp);
//            return;
//        }
//
//        if (!connectionEstablished) {
//            if (tcp.flags() == 18) {
//                connectionEstablishingFlags[0] = true;
//            } else if (tcp.flags() == 16) {
//                connectionEstablishingFlags[1] = true;
//            }
//            connectionEstablished = connectionEstablishingFlags[0] && connectionEstablishingFlags[1];
//        } else {
//            if(applicationType.equals("FTP")) {
//                generateOperationForFTP(element, tcp);
//            } else if (applicationType.equals("TELNET")) {
//                generateOperationForTELNET(element, tcp);
//            }
//        }
    }

    public void generateOperations() {
        packets.sort(new Comparator<PcapPacket>() {
            @Override
            public int compare(PcapPacket o1, PcapPacket o2) {
                Tcp tcp1 = new Tcp();
                Tcp tcp2 = new Tcp();
                o1.hasHeader(tcp1);
                o2.hasHeader(tcp2);

                Long seq1, seq2;
                if (tcp1.source() == serverPort) {
                    seq1 = tcp1.ack();
                } else {
                    seq1 = tcp1.seq();
                }
                if (tcp2.source() == serverPort) {
                    seq2 = tcp2.ack();
                } else {
                    seq2 = tcp2.seq();
                }


                return Long.compare(seq1, seq2);
            }
        });
        for (PcapPacket element : packets) {
            Tcp tcp = new Tcp();
            if (element.hasHeader(tcp)) {
                switch (applicationType) {
                    case "HTTP":
                        generateOperationForHTTP(element, tcp);
                        break;
                    case "FTP":
                        generateOperationForFTP(element, tcp);

                        break;
                    case "TELNET":
                        generateOperationForTELNET(element, tcp);

                        break;
                }
            }
        }

    }





    private void generateOperationForTELNET(PcapPacket element, Tcp tcp) {
        // Client-side message is terminated by '\rNUL'
        Payload payload = new Payload();
        if (element.hasHeader(payload) || tcp.flags() == 24) {
            // A PSH+ACK packet
            StringBuilder operationHeader = generateHeaderForOperation(element, tcp);

            int[] data = Util.byteToDec(payload.data());
            // Convert the data into readable String
            StringBuilder operation = new StringBuilder();
            // Used to add essential space between text data & the commands
            boolean pureTextMode = true;
            // Set if next char is a IAC command
            boolean nextCharCommand = false;
            // 1 if next char is normal option, 2 if next char is a sub option (With "SB" proceed)
            int nextCharOption = 0;
            // Set to the optionNumber of a sub option, which will be used to parse this specific subOption syntax
            int optionNumber = -1;
            // Set if next char is the first char of a sub option data fields
            boolean firstSubOption = false;
            // Index of the raw data of this frame
            int i = 0;
            while (i < data.length) {
                int step = data[i];
                if (nextCharCommand) {
                    String command = Util.telnetCommands[step];
                    operation.append(command);
                    operation.append(' ');
                    nextCharCommand = false;
                    if ("SB".equals(command)) {
                        nextCharOption = 2;
                    } else if ("SE".equals(command)) {
                        nextCharOption = 0;
                    } else {
                        nextCharOption = 1;
                    }
                    pureTextMode = false;
                    // Always update
                    optionNumber = -1;
                } else if (nextCharOption == 2) {
                    // Encounter SB
                    String option = Util.telnetOptions[step];
                    operation.append(option);
                    operation.append(' ');
                    nextCharOption = 0;
                    // Record the operation number here
                    optionNumber = data[i];
                    firstSubOption = true;
                } else if (nextCharOption == 1) {
                    // Normal Option comes here
                    String option = Util.telnetOptions[step];
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
                        operation.append(Util.telnetCommands[255]);
                        operation.append(' ');
                        // Next is Command
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
                                // Other cases can also be supported if needed

                            } else if ( Util.telnetSpecialCharacters.containsKey(step)) {
                                operation.append(Util.telnetSpecialCharacters.get(step));
                            } else {
                                operation.append((char) step);
                            }
                            // This is actually a subCommand
                        } else if (optionNumber == 7 || optionNumber == 17 || optionNumber == 20 || optionNumber == 26) {
                            //  NAME 8-bit-values IAC SE
                            operation.append(Util.byteToHexString(payload.data()[i]));
                        } else if (optionNumber == 19 || optionNumber == 27 || optionNumber == 34
                                || optionNumber == 36 || optionNumber == 37 || optionNumber == 38
                                || optionNumber == 40 || optionNumber == 42 || optionNumber == 44) {
                            // Too Complex Just append Decimal encoding
                            operation.append(Util.byteToDecString(payload.data()[i]));
                        }else if (optionNumber == 10 || optionNumber == 11 || optionNumber == 12 ||
                                optionNumber == 13 || optionNumber == 14 || optionNumber == 15 ||
                                optionNumber == 16  ) {
                            // NAME DS/DR 8-bit-values IAC SE
                            // NAOCRD NAOHTS NAOHTD NAOFFD NAOVTS NAOVTD NAOLFD modes,
                            if (firstSubOption) {
                                if (step == 0) {
                                    operation.append("DR ");
                                } else if (step == 1) {
                                    operation.append("DS ");
                                } else {
                                    operation.append("ERROR-FORMATTED-NAOCRD/NAOHTS/NAOHTD/NAOFFD/NAOVTS/NAOVTD/NAOLFD");
                                }
                            } else {
                                operation.append(Util.byteToHexString(payload.data()[i]));
                            }
                        } else if (optionNumber == 22) {
                            // SUPDUP-OUTPUT
                            if (firstSubOption) {
                                if (step == 1) {
                                    operation.append("1 ");
                                } else if (step == 2) {
                                    operation.append("2 ");
                                } else {
                                    operation.append("ERROR-FORMATTED-SUPDUP-OUTPUT");
                                }
                            } else {
                                operation.append(Util.byteToHexString(payload.data()[i]));
                            }
                        } else if (optionNumber == 23) {
                            // SEND-LOCATION
                            operation.append((char) step);
                        } else if (optionNumber == 28){
                            // TTYLOC <format> <TTYLOC number with IAC doubling> IAC SE
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
                                if (step == 0) {
                                    operation.append("IS ");
                                } else if (step == 1) {
                                    operation.append("ARE ");
                                } else {
                                    operation.append("ERROR-FORMATTED-3270-REGIME");
                                }
                            } else {
                                // REGIME normal ASCII char
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
                                        operation.append("ERROR-FORMATTED-X.3-PAD");
                                        break;
                                }
                            } else {
                                // <param1> <value1>
                                operation.append(step);
                                operation.append(" ");
                            }
                        } else if (optionNumber == 31) {
                            // NAWS <16-bit value> <16-bit value> IAC SE
                            if(i + 4 >= data.length) {
                                operation.append("ERROR-FORMATTED-NAWS");
                            } else {
                                int widthLow = data[++i];
                                int heightHigh = data[++i];
                                int heightLow = data[++i];

                                int width = step * 256 + widthLow;
                                int height = heightHigh * 256 + heightLow;
                                operation.append(String.format("WIDTH[%d] HEIGHT[%d]", width, height));
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
                        } else if (optionNumber == 35) {
                            // X-DISPLAY-LOCATION
                            if (firstSubOption) {
                                if (step == 0) {
                                    operation.append("IS ");
                                } else if (step == 1) {
                                    operation.append("SEND");
                                } else {
                                    operation.append("ERROR-FORMATTED-X-DISPLAY-LOCATION");
                                }
                            } else {
                                // <host>:<dispnum> ASCII char
                                operation.append((char) step);
                            }
                        }
                        else if (optionNumber == 39) {
                            // NEW-ENVIRON complex model recursive
                            // Default number routing here
                            if (firstSubOption) {
                                switch (step) {
                                    case 0:
                                        operation.append("IS");
                                        break;
                                    case 1:
                                        operation.append("SEND");
                                        break;
                                    case 2:
                                        operation.append("INFO");
                                        break;
                                    default:
                                        operation.append("ERROR-FORMATTED-NEW-ENVIRON");
                                        break;
                                }
                            } else {
                                switch (step) {
                                    case 0:
                                        operation.append(" VAR ");
                                        break;
                                    case 1:
                                        operation.append(" VALUE ");
                                        break;
                                    case 2:
                                        operation.append(" ESC ");
                                        break;
                                    case 3:
                                        operation.append(" USERVAR ");
                                        break;
                                    default:
                                        operation.append((char) step);
                                        break;
                                }
                            }
                        } else if (optionNumber == 47) {
                            // KERMIT
                            if (firstSubOption) {
                                switch (step) {
                                    case 0:
                                        operation.append("START-SERVER ");
                                        break;
                                    case 1:
                                        operation.append("STOP-SERVER ");
                                        break;
                                    case 2:
                                        operation.append("REQ-START-SERVER ");
                                        break;
                                    case 3:
                                        operation.append("REQ-STOP-SERVER ");
                                        break;
                                    case 4:
                                        operation.append("SOP ");
                                        break;
                                    case 8:
                                        operation.append("RESP-START-SERVER ");
                                        break;
                                    case 9:
                                        operation.append("RESP-STOP-SERVER ");
                                        break;
                                    default:
                                        operation.append("ERROR-FORMATTED-KERMIT");
                                        break;
                                }
                            } else {
                                operation.append(Util.byteToHexString(payload.data()[i]));
                            }
                        } else{
                            // NAME IS/SEND (ASCII-DATA) IAC SE (Default) [5 24 32]
                            if (firstSubOption) {
                                String command = Util.telnetCommands[step];
                                operation.append(command);
                            } else {
                                operation.append((char) step);
                            }
                        }
                        // Update Flags on next Char
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

            operationsHeadersList.add(operationHeader.toString());
            operationsList.add(operation.toString());
            operationsFlag.add(tcp.source() == serverPort ? 1 : -1);
        }
    }
    private StringBuilder generateHeaderForOperation(PcapPacket element, Tcp tcp) {
        StringBuilder sb = new StringBuilder();
        if (tcp.source() == serverPort) {
            sb.append("\nSERVER Timestamp: ");
        } else {
            sb.append("\nCLIENT Timestamp: ");
        }
        sb.append(new Date(element.getCaptureHeader().timestampInMillis()).toString());
        sb.append(" Context: \n");
        return sb;
    }

    private void generateOperationForFTP(PcapPacket element, Tcp tcp) {
        Payload payload = new Payload();
        if (element.hasHeader(payload) || tcp.flags() == 24) {
            // A PSH+ACK packet
            StringBuilder sb = generateHeaderForOperation(element, tcp);
            try {
                sb.append(Util.unEscapeExceptNT(new String(payload.data(), "ISO-8859-1")));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            operationsList.add(sb.toString());
        }
    }

    private void generateOperationForHTTP(PcapPacket element, Tcp tcp) {
        Payload payload = new Payload();
        Http http = new Http();
        Html html = new Html();

        boolean qualified = false;
        if (element.hasHeader(payload)) qualified = true;
        if (element.hasHeader(http)) qualified = true;
        if (element.hasHeader(html)) qualified = true;

        if (qualified) {
            // A PSH+ACK packet
            StringBuilder operation = generateHeaderForOperation(element, tcp);

            if (http.getLength() > 0 ) {
                String messageType = http.getMessageType().name();
                String messageContent = http.header();
                operation.append(String.format("MessageType: %s\n", messageType));
                operation.append(Util.unEscapeExceptNT(messageContent));
                if (messageType.equals("REQUEST")) {
                    for (Http.Request field : Http.Request.values()) {
                        if (http.fieldValue(field) != null) {
                            clientHTTPFields.put(field.name(), http.fieldValue(field));
                        }
                    }
                } else if (messageType.equals("RESPONSE")) {
                    for (Http.Response field : Http.Response.values()) {
                        if (http.fieldValue(field) != null) {
                            serverHTTPFields.put(field.name(), http.fieldValue(field));
                        }
                    }
                    if ( serverHTTPFields.get("Content_Type") != null) {
                        Pattern pattern = Pattern.compile("^\\s*charset=(.*)");
                        for (String possibleCharset : serverHTTPFields.get("Content_Type").split(";")) {
                            Matcher matcher = pattern.matcher(possibleCharset);
                            if (matcher.find()) {
                                serverCharset = matcher.group(1);
                                break;
                            }
                        }
                    }
                    if (serverCharset == null) {
                        // Default charset
                        serverCharset = "ISO-8859-1";
                    }
                }
                if (html.getLength() > 0) {
                    try {
                        HTTPPayloadBuffer.write(html.page().getBytes(serverCharset), 0, html.page().getBytes(serverCharset).length);
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }

                operationsList.add(operation.toString());
            }
            if (payload.getLength() > 0) {
                HTTPPayloadBuffer.write(payload.data(), 0, payload.data().length);
            }

        }
    }

    private int unionSingleOperationForTelnet(int oldCursor, int i, List<String> result, boolean isServer) {
        StringBuilder step = new StringBuilder();
        String operation = operationsList.get(i);
        step.append(operationsList.get(i));
        int newServerCursor = oldCursor;

        if (operation.length() == 1) {
            // Start unionning
            int j = i + 1;
            while (j < operationsList.size()) {
                if ((isServer && operationsFlag.get(j) > 0) ||
                        (!isServer && operationsFlag.get(j) < 0)) {
                    step.append(operationsList.get(j));
                    if ( (isServer && operationsList.get(j).equals("\r\n")) ||
                            (!isServer && operationsList.get(j).equals("\rNUL")) ) {
                        newServerCursor = j;
                        break;
                    }
                }
                j++;
            }
        } else {
            int j = i + 1;
            while ( (j < operationsList.size()) &&
                    ( (isServer && operationsFlag.get(j) > 0) || (!isServer && operationsFlag.get(j) < 0) )  ) {
                step.append(operationsList.get(j));
                newServerCursor = j;
                j++;
            }
        }

        result.add(operationsHeadersList.get(i) + Util.unEscapeExceptNT(step.toString()));
        return newServerCursor;
    }

    /**
     * Comprise consequent and echo messages during Telnet transmission for higher level view purposes
     * @return Union Operations of Telnet session
     */
    public List<String> unionOperationsForTelnet() {
        List<String> result = new ArrayList<>();
        int clientCursor = -1;  int serverCursor = -1;

        for (int i=0; i<operationsList.size(); i++) {
            if (this.operationsFlag.get(i) > 0) {
                // Server
                if (i > serverCursor) serverCursor = unionSingleOperationForTelnet(serverCursor, i, result, true);
            } else {
                // client
                if (i > clientCursor) clientCursor = unionSingleOperationForTelnet(clientCursor, i, result, false);
            }
        }

        return result;
    }

    /**
     * Use Algorithm to learn what options have been adopted in the Session
     * @return Options adopted for this Telnet session
     */
    public String learnOptionsForTelnet() {
        StringBuilder res = new StringBuilder();

        // WONT series are not necessary
        Set<String> clientEnables = new HashSet<>();
        Set<String> clientDemandEnables = new HashSet<>();
        Set<String> clientDemandWontEnables = new HashSet<>();
        Set<String> serverEnables = new HashSet<>();
        Set<String> serverDemandEnables = new HashSet<>();
        Set<String> serverDemandWontEnables = new HashSet<>();

        for( int i=0; i<operationsList.size(); i++) {
            String operation = operationsList.get(i);
            boolean isServer = operationsFlag.get(i) > 0;

            if (operation.length() > 3 && operation.substring(0,3).equals("IAC")) {
                // Enter decoding mode
                if (isServer) {
                    buildTelnetOptionsSet(serverEnables, serverDemandEnables, serverDemandWontEnables, operation);
                } else {
                    buildTelnetOptionsSet(clientEnables, clientDemandEnables, clientDemandWontEnables, operation);
                }
            }
        }

        res.append(buildTelnetOptionsString(clientEnables, serverDemandEnables, serverDemandWontEnables, false ));
        res.append("\n");
        res.append(buildTelnetOptionsString(serverEnables, clientDemandEnables, clientDemandWontEnables, true ));
        return res.toString();
    }

    private String buildTelnetOptionsString(Set<String> selfWant, Set<String> otherDemand, Set<String> otherDemandWont, boolean isServer) {
        if (selfWant == null || otherDemand == null || otherDemandWont == null ) {    return ""; }
        Set<String> selfEnables = new HashSet<>();
        // Intersect serverWill with clientDemand, then exclude clientDemandWont
        for(String option : selfWant) {
            if (otherDemand.contains(option) && !otherDemandWont.contains(option)) {
                selfEnables.add(option);
            }
        }
        StringBuilder selfOptions = new StringBuilder();

        selfOptions.append(isServer ? "Server " : "Client ").append("Enabled Options:\n");
        for (String str: selfEnables) {
            selfOptions.append("\t").append(str).append("\n");
        }
        return selfOptions.deleteCharAt(selfOptions.length()-1).toString();
    }

    private void buildTelnetOptionsSet (Set<String> self, Set<String> demand, Set<String> demandWont, String operation) {
        if (self == null || demand == null || demandWont == null || operation == null) {    return; }
        String[] operators = operation.split(" ");
        int j=0;
        while (j<operators.length) {
            if (operators[j].equals("IAC")) {
                if (j+2 >= operators.length) {
                    j ++;
                    continue;
                }
                j++;
                switch (operators[j]) {
                    case "WILL":
                        self.add(operators[++j]);
                        break;
                    case "DO":
                        demand.add(operators[++j]);
                        break;
                    case "DONT":
                        demandWont.add(operators[++j]);
                        break;
                    default:
                        break;
                }
            }
            j++;
        }
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
        return String.format("Client Physical Information:\n\tMAC Address: %s IP Address: %s Port Number: %d",
                Util.byteToHexString(clientMacAddress),
                Util.byteToDecString(clientIP), clientPort);
    }

    public String getServerPhysicalInformation() {
        return String.format("Server Physical Information:\n\tMAC Address: %s IP Address: %s Port Number: %d",
                Util.byteToHexString(serverMacAddress),
                Util.byteToDecString(serverIP), serverPort);
    }

    public String getServerCharset() {
        return serverCharset;
    }

    public ByteArrayOutputStream getHTTPPayloadBuffer() {
        return HTTPPayloadBuffer;
    }

    public List<String> getOperationsList() {
        return operationsList;
    }

    public Map<String, String> getClientHTTPFields() {
        return clientHTTPFields;
    }

    public Map<String, String> getServerHTTPFields() {
        return serverHTTPFields;
    }

    @Override
    public String toString() {
        return "Session: " + this.hashCode();
    }

}
