package blk.freeyourgadget.gadgetbridge.devices.pinetime;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import blk.freeyourgadget.gadgetbridge.devices.AbstractSampleProvider;
import blk.freeyourgadget.gadgetbridge.entities.DaoSession;
import blk.freeyourgadget.gadgetbridge.entities.PineTimeActivitySample;
import blk.freeyourgadget.gadgetbridge.entities.PineTimeActivitySampleDao;
import blk.freeyourgadget.gadgetbridge.impl.GBDevice;

public class PineTimeActivitySampleProvider extends AbstractSampleProvider<PineTimeActivitySample> {
    private GBDevice mDevice;
    private DaoSession mSession;

    public PineTimeActivitySampleProvider(GBDevice device, DaoSession session) {
        super(device, session);

        mSession = session;
        mDevice = device;
    }

    @Override
    public AbstractDao<PineTimeActivitySample, ?> getSampleDao() {
        return getSession().getPineTimeActivitySampleDao();
    }

    @Nullable
    @Override
    protected Property getRawKindSampleProperty() {
        return PineTimeActivitySampleDao.Properties.RawKind;
    }

    @NonNull
    @Override
    protected Property getTimestampSampleProperty() {
        return PineTimeActivitySampleDao.Properties.Timestamp;
    }

    @NonNull
    @Override
    protected Property getDeviceIdentifierSampleProperty() {
        return PineTimeActivitySampleDao.Properties.DeviceId;
    }

    @Override
    public int normalizeType(int rawType) {
        return rawType;
    }

    @Override
    public int toRawActivityKind(int activityKind) {
        return activityKind;
    }

    @Override
    public float normalizeIntensity(int rawIntensity) {
        return rawIntensity;
    }

    /**
     * Factory method to creates an empty sample of the correct type for this sample provider
     *
     * @return the newly created "empty" sample
     */
    @Override
    public PineTimeActivitySample createActivitySample() {
        return new PineTimeActivitySample();
    }
}
