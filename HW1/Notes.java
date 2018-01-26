
Install WinPCap
Copy file jnetpcap.dll to PATH folder 'WIN/system32'

Jnetpcap:
    Just filter for each packet, divide them into serveral headers.

TCP Flags Refer:
        http://www.freesoft.org/CIE/Course/Section4/8.htm
        https://www.manitonetworks.com/flow-management/2016/10/16/decoding-tcp-flags
        https://www.keycdn.com/support/tcp-flags/
    Especially:
        2 - URG
        24 - ACK+PSH
        18 - SYN+ACK
        16 - ACK
        8 - PSH
        4 - RST
        2 - SYN
        1 - FIN

Telnet:
    Details: http://www.pcvr.nl/tcpip/telnet.htm#26_0
    Options: https://www.iana.org/assignments/telnet-options/telnet-options.xhtml#telnet-options-4
    Wiki: https://en.wikipedia.org/wiki/Telnet
    RFC: https://tools.ietf.org/html/rfc854 {Tomorrow will look at it in details}

    Key Points:
        - Translate Commands & Combine Messages
        - Design A DS to filter out the Options the session is on
        


FTP:    Server always port 21, data is sent through port 20
    Data is Sent through PSH+ACK packet all the time
    Either check TCP.flags == 24 or check packet.hasHeader(payload)
    Details:    http://www.pcvr.nl/tcpip/ftp_file.htm





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

        try {
        ByteArrayInputStream bis = new ByteArrayInputStream(html.page().getBytes("ISO-8859-1"));
        GZIPInputStream gis;
        gis = new GZIPInputStream(bis);
        BufferedReader br = new BufferedReader(new InputStreamReader(gis, "ISO-8859-1"));
        StringBuilder sb1 = new StringBuilder();
        String line;
        while((line = br.readLine()) != null) {
        sb1.append(line);
        }
        br.close();
        gis.close();
        bis.close();
        System.out.println();
        } catch (IOException e) {
        e.printStackTrace();
        }