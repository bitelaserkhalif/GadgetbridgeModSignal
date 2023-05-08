package blk.freeyourgadget.gadgetbridge.devices;

import java.io.Serializable;
import java.util.Map;

public class DataWrapper implements Serializable {
    private Map map;

    public DataWrapper(Map dataMap) {
        this.map = dataMap;
    }

    public Map getData() {
        return this.map;
    }

}
