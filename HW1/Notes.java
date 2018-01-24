
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






