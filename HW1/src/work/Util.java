package work;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Amos on 2018/1/23.
 */
public class Util {

    public static Map<Integer, String> supportedApplicationTypes;

    static {
        supportedApplicationTypes = new HashMap<>();
        supportedApplicationTypes.put(21, "FTP");
        supportedApplicationTypes.put(23, "TELNET");
        supportedApplicationTypes.put(80, "HTTP");
    }



}
