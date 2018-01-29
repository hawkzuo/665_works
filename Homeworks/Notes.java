
-Install WinPCap, Java(>=8)
-Install jnetpcap 1.3
-Copy file jnetpcap.dll to PATH folder 'WIN/system32'
-Copy CSCE665_work.jar file to a folder and copy test .pcap files to a sub-folder named "data"
Then run the program via command:
    java -jar CSCE665_workspace.jar data/tfsession.pcap
    java -jar CSCE665_workspace.jar data/httpsession.pcap
The output will be in sub-folder "data", with filename pattern
    pcapFilename.txt

 Linux:
 -Install libpcap-dev, Java(>=8)
   [sudo apt-get install libpcap-dev]
-Copy file libjnetpcap.so to /usr/lib/libjnetpcap.so
  Or Copy jnetpcap.jar file to /usr/share/java/jnetpcap.jar
Then run the program via command:
  java -jar CSCE665_workspace.jar data/tfsession.pcap
  java -jar CSCE665_workspace.jar data/httpsession.pcap
The output will be in sub-folder "data", with filename pattern
  pcapFilename.txt

Note: Version 1.3 of jnetpcap library is required



 Jnetpcap:
    Just filter for each packet, divide them into serveral headers.
    http://jnetpcap.com/docs/javadoc/jnetpcap-1.3/index.html

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
    Some Options should be read as raw bytes in the subCommand case, so for those subOptions,
    a link to the RFC is offered in the parsed result.


FTP:    Server always port 21, data is sent through port 20
    Data is Sent through PSH+ACK packet all the time
    Either check TCP.flags == 24 or check packet.hasHeader(payload)
    Details:    http://www.pcvr.nl/tcpip/ftp_file.htm

General:
    ASCII table:
        https://www.cs.cmu.edu/~pattis/15-1XX/common/handouts/ascii.html
    ASCII escape sequence:
        Parsing: https://stackoverflow.com/questions/9913342/byte-to-character-conversion-for-a-telnet-stream
        Meaning: http://ascii-table.com/ansi-escape-sequences.php




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