package work;

import org.jnetpcap.PcapPktHdr;
import org.jnetpcap.packet.PcapPacket;
import org.jnetpcap.packet.PcapPacketHandler;

import java.util.Date;

/**
 * Created by Amos on 2018/1/22.
 */
public class TfPcapPacketHandler<T> implements PcapPacketHandler<T> {

    @Override
    public void nextPacket(PcapPacket packet, T t) {
        // Do something on the received packet

        // This shows everything [Header / Formatting / Payload]
        System.out.println(packet.toString());

        System.out.printf("Received at %s caplen=%-4d len=%-4d %s\n",
            new Date(packet.getCaptureHeader().timestampInMillis()),
            packet.getCaptureHeader().caplen(), // Length actually captured
            packet.getCaptureHeader().wirelen(), // Original length
            t.toString() // User supplied object
        );
    }
}
