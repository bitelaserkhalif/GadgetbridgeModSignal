/*  Copyright (C) 2017-2021 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package blk.freeyourgadget.gadgetbridge.service.devices.huami.operations;

import android.text.format.DateUtils;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.GregorianCalendar;

import androidx.annotation.NonNull;

import blk.freeyourgadget.gadgetbridge.GBApplication;
import blk.freeyourgadget.gadgetbridge.Logging;
import blk.freeyourgadget.gadgetbridge.R;
import blk.freeyourgadget.gadgetbridge.database.DBHandler;
import blk.freeyourgadget.gadgetbridge.devices.huami.amazfitbip.AmazfitBipService;
import blk.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import blk.freeyourgadget.gadgetbridge.export.ActivityTrackExporter;
import blk.freeyourgadget.gadgetbridge.export.GPXExporter;
import blk.freeyourgadget.gadgetbridge.model.ActivityKind;
import blk.freeyourgadget.gadgetbridge.model.ActivityTrack;
import blk.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import blk.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import blk.freeyourgadget.gadgetbridge.service.devices.huami.AbstractHuamiActivityDetailsParser;
import blk.freeyourgadget.gadgetbridge.service.devices.huami.HuamiSupport;
import blk.freeyourgadget.gadgetbridge.service.devices.huami.HuamiActivityDetailsParser;
import blk.freeyourgadget.gadgetbridge.util.CheckSums;
import blk.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import blk.freeyourgadget.gadgetbridge.util.FileUtils;
import blk.freeyourgadget.gadgetbridge.util.GB;

/**
 * An operation that fetches activity data. For every fetch, a new operation must
 * be created, i.e. an operation may not be reused for multiple fetches.
 */
public class FetchSportsDetailsOperation extends AbstractFetchOperation {
    private static final Logger LOG = LoggerFactory.getLogger(FetchSportsDetailsOperation.class);
    private final AbstractHuamiActivityDetailsParser detailsParser;
    private final BaseActivitySummary summary;
    private final String lastSyncTimeKey;

    private ByteArrayOutputStream buffer;

    FetchSportsDetailsOperation(@NonNull BaseActivitySummary summary,
                                @NonNull AbstractHuamiActivityDetailsParser detailsParser,
                                @NonNull HuamiSupport support,
                                @NonNull String lastSyncTimeKey,
                                int fetchCount) {
        super(support);
        setName("fetching sport details");
        this.summary = summary;
        this.detailsParser = detailsParser;
        this.lastSyncTimeKey = lastSyncTimeKey;
        this.fetchCount = fetchCount;
    }

    @Override
    protected void startFetching(TransactionBuilder builder) {
        LOG.info("start " + getName());
        buffer = new ByteArrayOutputStream(1024);
        GregorianCalendar sinceWhen = getLastSuccessfulSyncTime();
        startFetching(builder, AmazfitBipService.COMMAND_ACTIVITY_DATA_TYPE_SPORTS_DETAILS, sinceWhen);
    }

    @Override
    protected boolean handleActivityFetchFinish(boolean success) {
        LOG.info(getName() + " has finished round " + fetchCount);

        boolean parseSuccess = true;

        if (success && buffer.size() > 0) {
            if (detailsParser instanceof HuamiActivityDetailsParser) {
                ((HuamiActivityDetailsParser) detailsParser).setSkipCounterByte(false); // is already stripped
            }
            try {
                ActivityTrack track = detailsParser.parse(buffer.toByteArray());
                ActivityTrackExporter exporter = createExporter();
                String trackType = "track";
                switch (summary.getActivityKind()) {
                    case ActivityKind.TYPE_CYCLING:
                        trackType = getContext().getString(R.string.activity_type_biking);
                        break;
                    case ActivityKind.TYPE_RUNNING:
                        trackType = getContext().getString(R.string.activity_type_running);
                        break;
                    case ActivityKind.TYPE_WALKING:
                        trackType = getContext().getString(R.string.activity_type_walking);
                        break;
                    case ActivityKind.TYPE_HIKING:
                        trackType = getContext().getString(R.string.activity_type_hiking);
                        break;
                    case ActivityKind.TYPE_CLIMBING:
                        trackType = getContext().getString(R.string.activity_type_climbing);
                        break;
                    case ActivityKind.TYPE_SWIMMING:
                        trackType = getContext().getString(R.string.activity_type_swimming);
                        break;
                }
                final String rawBytesPath = saveRawBytes();

                String fileName = FileUtils.makeValidFileName("gadgetbridge-" + trackType.toLowerCase() + "-" + DateTimeUtils.formatIso8601(summary.getStartTime()) + ".gpx");
                File targetFile = new File(FileUtils.getExternalFilesDir(), fileName);

                try {
                    exporter.performExport(track, targetFile);

                    try (DBHandler dbHandler = GBApplication.acquireDB()) {
                        summary.setGpxTrack(targetFile.getAbsolutePath());
                        if (rawBytesPath != null) {
                            summary.setRawDetailsPath(rawBytesPath);
                        }
                        dbHandler.getDaoSession().getBaseActivitySummaryDao().update(summary);
                    }
                } catch (ActivityTrackExporter.GPXTrackEmptyException ex) {
                    GB.toast(getContext(), "This activity does not contain GPX tracks.", Toast.LENGTH_LONG, GB.ERROR, ex);
                }
            } catch (Exception ex) {
                GB.toast(getContext(), "Error getting activity details: " + ex.getMessage(), Toast.LENGTH_LONG, GB.ERROR, ex);
                parseSuccess = false;
            }
        }

        final boolean superSuccess = super.handleActivityFetchFinish(success);

        if (success && parseSuccess) {
            // Always increment the sync timestamp on success, even if we did not get data
            GregorianCalendar endTime = BLETypeConversions.createCalendar();
            endTime.setTime(summary.getEndTime());
            saveLastSyncTimestamp(endTime);

            if (needsAnotherFetch(endTime)) {
                FetchSportsSummaryOperation nextOperation = new FetchSportsSummaryOperation(getSupport(), fetchCount);
                try {
                    nextOperation.perform();
                } catch (IOException ex) {
                    LOG.error("Error starting another round of fetching activity data", ex);
                }
            }
        }


        return superSuccess && parseSuccess;
    }

    private boolean needsAnotherFetch(GregorianCalendar lastSyncTimestamp) {
        // We have 2 operations per fetch round: summary + details
        if (fetchCount > 10) {
            LOG.warn("Already have 5 fetch rounds, not doing another one.");
            return false;
        }

        if (DateUtils.isToday(lastSyncTimestamp.getTimeInMillis())) {
            LOG.info("Hopefully no further fetch needed, last synced timestamp is from today.");
            return false;
        }
        if (lastSyncTimestamp.getTimeInMillis() > System.currentTimeMillis()) {
            LOG.warn("Not doing another fetch since last synced timestamp is in the future: {}", DateTimeUtils.formatDateTime(lastSyncTimestamp.getTime()));
            return false;
        }
        LOG.info("Doing another fetch since last sync timestamp is still too old: {}", DateTimeUtils.formatDateTime(lastSyncTimestamp.getTime()));
        return true;
    }

    @Override
    protected boolean validChecksum(int crc32) {
        return crc32 == CheckSums.getCRC32(buffer.toByteArray());
    }

    private ActivityTrackExporter createExporter() {
        GPXExporter exporter = new GPXExporter();
        exporter.setCreator(GBApplication.app().getNameAndVersion());
        return exporter;
    }

    /**
     * Method to handle the incoming activity data.
     * There are two kind of messages we currently know:
     * - the first one is 11 bytes long and contains metadata (how many bytes to expect, when the data starts, etc.)
     * - the second one is 20 bytes long and contains the actual activity data
     * <p/>
     * The first message type is parsed by this method, for every other length of the value param, bufferActivityData is called.
     *
     * @param value
     */
    @Override
    protected void handleActivityNotif(byte[] value) {
        LOG.warn("sports details: " + Logging.formatBytes(value));

        if (!isOperationRunning()) {
            LOG.error("ignoring sports details notification because operation is not running. Data length: " + value.length);
            getSupport().logMessageContent(value);
            return;
        }

        if (value.length < 2) {
            LOG.error("unexpected sports details data length: " + value.length);
            getSupport().logMessageContent(value);
            return;
        }

        if ((byte) (lastPacketCounter + 1) == value[0]) {
            lastPacketCounter++;
            bufferActivityData(value);
        } else {
            GB.toast("Error " + getName() + ", invalid package counter: " + value[0] + ", last was: " + lastPacketCounter, Toast.LENGTH_LONG, GB.ERROR);
            handleActivityFetchFinish(false);
        }
    }

    /**
     * Buffers the given activity summary data. If the total size is reached,
     * it is converted to an object and saved in the database.
     *
     * @param value
     */
    @Override
    protected void bufferActivityData(byte[] value) {
        buffer.write(value, 1, value.length - 1); // skip the counter
    }

    @Override
    protected String getLastSyncTimeKey() {
        return lastSyncTimeKey;
    }

    protected GregorianCalendar getLastSuccessfulSyncTime() {
        GregorianCalendar calendar = BLETypeConversions.createCalendar();
        calendar.setTime(summary.getStartTime());
        return calendar;
    }

    private String saveRawBytes() {
        final String fileName = FileUtils.makeValidFileName(String.format("%s.bin", DateTimeUtils.formatIso8601(summary.getStartTime())));
        FileOutputStream outputStream = null;

        try {
            final File targetFolder = new File(FileUtils.getExternalFilesDir(), "rawDetails");
            targetFolder.mkdirs();
            final File targetFile = new File(targetFolder, fileName);
            outputStream = new FileOutputStream(targetFile);
            outputStream.write(buffer.toByteArray());
            outputStream.close();
            return targetFile.getAbsolutePath();
        } catch (final IOException e) {
            LOG.error("Failed to save raw bytes", e);
        }

        return null;
    }
}
