package work;

import org.jnetpcap.packet.PcapPacket;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Amos on 2018/1/23.
 */
public class Session {

    private List<PcapPacket> packets;

    Session(PcapPacket head) {
        this.packets = new ArrayList<>();
        this.packets.add(head);
    }
    Session() {
        this.packets = new ArrayList<>();
    }

    public int addLast(PcapPacket element) {
        packets.add(element);
        return packets.size();
    }



    @Override
    public String toString() {
        return "Packets size: " + packets.size();
    }
}
