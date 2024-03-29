/*  Copyright (C) 2015-2020 Andreas Shimokawa, boun, Carsten Pfeiffer, Daniel
    Dakhno, Daniele Gobbetti, JohnnySun, jonnsoft, Lem Dulfo, Taavi Eomäe,
    Uwe Hermann

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
package blk.freeyourgadget.gadgetbridge.activities;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.os.Parcelable;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Pair;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import blk.freeyourgadget.gadgetbridge.GBApplication;
import blk.freeyourgadget.gadgetbridge.R;
import blk.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsActivity;
import blk.freeyourgadget.gadgetbridge.adapter.DeviceCandidateAdapter;
import blk.freeyourgadget.gadgetbridge.adapter.SpinnerWithIconAdapter;
import blk.freeyourgadget.gadgetbridge.adapter.SpinnerWithIconItem;
import blk.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import blk.freeyourgadget.gadgetbridge.impl.GBDevice;
import blk.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import blk.freeyourgadget.gadgetbridge.model.DeviceType;
import blk.freeyourgadget.gadgetbridge.util.AndroidUtils;
import blk.freeyourgadget.gadgetbridge.util.BondingInterface;
import blk.freeyourgadget.gadgetbridge.util.BondingUtil;
import blk.freeyourgadget.gadgetbridge.util.DeviceHelper;
import blk.freeyourgadget.gadgetbridge.util.GB;
import blk.freeyourgadget.gadgetbridge.util.Prefs;

import static blk.freeyourgadget.gadgetbridge.util.GB.toast;


public class DiscoveryActivity extends AbstractGBActivity implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener, BondingInterface {
    private static final Logger LOG = LoggerFactory.getLogger(DiscoveryActivity.class);
    private static final long SCAN_DURATION = 30000; // 30s
    private final Handler handler = new Handler();
    private final ArrayList<GBDeviceCandidate> deviceCandidates = new ArrayList<>();
    int CallbackType = android.bluetooth.le.ScanSettings.CALLBACK_TYPE_ALL_MATCHES;
    int MatchMode = android.bluetooth.le.ScanSettings.MATCH_MODE_STICKY;
    private ScanCallback newBLEScanCallback = null;
    /**
     * If already bonded devices are to be ignored when scanning
     */
    private boolean ignoreBonded = true;
    private boolean discoverUnsupported = false;
    private ProgressBar bluetoothProgress;
    private ProgressBar bluetoothLEProgress;
    private DeviceCandidateAdapter deviceCandidateAdapter;
    private GBDeviceCandidate deviceTarget;
    private BluetoothAdapter adapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private Button startButton;
    private boolean scanning;
    private long selectedUnsupportedDeviceKey = DebugActivity.SELECT_DEVICE;
    private final Runnable stopRunnable = new Runnable() {
        @Override
        public void run() {
            stopDiscovery();
            LOG.info("Discovery stopped by thread timeout.");
        }
    };
    private Set<BTUUIDPair> foundCandidates = new HashSet<>();
    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (Objects.requireNonNull(intent.getAction())) {
                case BluetoothAdapter.ACTION_DISCOVERY_STARTED: {
                    LOG.debug("ACTION_DISCOVERY_STARTED");
                    break;
                }
                case BluetoothAdapter.ACTION_STATE_CHANGED: {
                    LOG.debug("ACTION_STATE_CHANGED ");
                    bluetoothStateChanged(intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF));
                    break;
                }
                case BluetoothDevice.ACTION_FOUND: {
                    LOG.debug("ACTION_FOUND");
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    handleDeviceFound(device, intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, GBDevice.RSSI_UNKNOWN));
                    break;
                }
                case BluetoothDevice.ACTION_UUID: {
                    LOG.debug("ACTION_UUID");
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    short rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, GBDevice.RSSI_UNKNOWN);
                    Parcelable[] uuids = intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID);
                    ParcelUuid[] uuids2 = AndroidUtils.toParcelUuids(uuids);
                    addToCandidateListIfNotAlreadyProcessed(device, rssi, uuids2);
                    break;
                }
                case BluetoothDevice.ACTION_BOND_STATE_CHANGED: {
                    LOG.debug("ACTION_BOND_STATE_CHANGED");
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (device != null) {
                        int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE);
                        LOG.debug(String.format(Locale.ENGLISH, "Bond state: %d", bondState));

                        if (bondState == BluetoothDevice.BOND_BONDED) {
                            BondingUtil.handleDeviceBonded((BondingInterface) context, getCandidateFromMAC(device));
                        }
                    }
                    break;
                }
            }
        }
    };

    public void logMessageContent(byte[] value) {
        if (value != null) {
            LOG.warn("DATA: " + GB.hexdump(value, 0, value.length));
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        BondingUtil.handleActivityResult(this, requestCode, resultCode, data);
    }

    private GBDeviceCandidate getCandidateFromMAC(BluetoothDevice device) {
        for (GBDeviceCandidate candidate : deviceCandidates) {
            if (candidate.getMacAddress().equals(device.getAddress())) {
                return candidate;
            }
        }
        LOG.warn(String.format("This shouldn't happen unless the list somehow emptied itself, device MAC: %1$s", device.getAddress()));
        return null;
    }

    private ScanCallback getScanCallback() {
        if (newBLEScanCallback != null) {
            return newBLEScanCallback;
        }

        newBLEScanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                try {
                    ScanRecord scanRecord = result.getScanRecord();
                    ParcelUuid[] uuids = null;
                    if (scanRecord != null) {
                        //logMessageContent(scanRecord.getBytes());
                        List<ParcelUuid> serviceUuids = scanRecord.getServiceUuids();
                        if (serviceUuids != null) {
                            uuids = serviceUuids.toArray(new ParcelUuid[0]);
                        }
                    }
                    LOG.warn(result.getDevice().getName() + ": " +
                            ((scanRecord != null) ? scanRecord.getBytes().length : -1));
                    addToCandidateListIfNotAlreadyProcessed(result.getDevice(), (short) result.getRssi(), uuids);

                } catch (NullPointerException e) {
                    LOG.warn("Error handling scan result", e);
                }
            }
        };

        return newBLEScanCallback;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        loadSettingsValues();
        setContentView(R.layout.activity_discovery);
        startButton = findViewById(R.id.discovery_start);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onStartButtonClick(startButton);
            }
        });

        Button settingsButton = findViewById(R.id.discovery_preferences);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent enableIntent = new Intent(DiscoveryActivity.this, DiscoveryPairingPreferenceActivity.class);
                startActivity(enableIntent);
            }
        });

        bluetoothProgress = findViewById(R.id.discovery_progressbar);
        bluetoothProgress.setProgress(0);
        bluetoothProgress.setIndeterminate(true);
        bluetoothProgress.setVisibility(View.GONE);
        ListView deviceCandidatesView = findViewById(R.id.discovery_device_candidates_list);

        bluetoothLEProgress = findViewById(R.id.discovery_ble_progressbar);
        bluetoothLEProgress.setProgress(0);
        bluetoothLEProgress.setIndeterminate(true);
        bluetoothLEProgress.setVisibility(View.GONE);

        deviceCandidateAdapter = new DeviceCandidateAdapter(this, deviceCandidates);
        deviceCandidatesView.setAdapter(deviceCandidateAdapter);
        deviceCandidatesView.setOnItemClickListener(this);
        deviceCandidatesView.setOnItemLongClickListener(this);

        registerBroadcastReceivers();

        checkAndRequestLocationPermission();

        startDiscovery();
    }

    public void onStartButtonClick(View button) {
        LOG.debug("Start button clicked");
        if (isScanning()) {
            stopDiscovery();
        } else {
            deviceCandidates.clear();
            deviceCandidateAdapter.notifyDataSetChanged();
            startDiscovery();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList("deviceCandidates", deviceCandidates);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        ArrayList<Parcelable> restoredCandidates = savedInstanceState.getParcelableArrayList("deviceCandidates");
        if (restoredCandidates != null) {
            deviceCandidates.clear();
            for (Parcelable p : restoredCandidates) {
                deviceCandidates.add((GBDeviceCandidate) p);
            }
        }
    }

    @Override
    protected void onDestroy() {
        unregisterBroadcastReceivers();
        stopDiscovery();
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        unregisterBroadcastReceivers();
        stopDiscovery();
        super.onStop();
    }

    @Override
    protected void onPause() {
        unregisterBroadcastReceivers();
        stopDiscovery();
        super.onPause();
    }

    @Override
    protected void onResume() {
        loadSettingsValues();
        registerBroadcastReceivers();
        super.onResume();
    }

    private void handleDeviceFound(BluetoothDevice device, short rssi) {
        if (device.getName() != null) {
            if (handleDeviceFound(device, rssi, null)) {
                LOG.info("found supported device " + device.getName() + " without scanning services, skipping service scan.");
                return;
            }
        }
        ParcelUuid[] uuids = device.getUuids();
        if (uuids == null) {
            if (device.fetchUuidsWithSdp()) {
                return;
            }
        }

        addToCandidateListIfNotAlreadyProcessed(device, rssi, uuids);
    }

    private void addToCandidateListIfNotAlreadyProcessed(BluetoothDevice device, short rssi, ParcelUuid[] uuids) {
        BTUUIDPair btuuidPair = new BTUUIDPair(device, uuids);
        if (foundCandidates.contains(btuuidPair)) {
//                        LOG.info("candidate already processed, skipping");
            return;
        }

        if (handleDeviceFound(device, rssi, uuids)) {
            //device was considered a candidate, do not process it again unless something changed
            foundCandidates.add(btuuidPair);
        }
    }

    private boolean handleDeviceFound(BluetoothDevice device, short rssi, ParcelUuid[] uuids) {
        LOG.debug("found device: " + device.getName() + ", " + device.getAddress());
        if (LOG.isDebugEnabled()) {
            if (uuids != null && uuids.length > 0) {
                for (ParcelUuid uuid : uuids) {
                    LOG.debug("  supports uuid: " + uuid.toString());
                }
            }
        }

        if (device.getBondState() == BluetoothDevice.BOND_BONDED && ignoreBonded) {
            return true; // Ignore already bonded devices
        }

        GBDeviceCandidate candidate = new GBDeviceCandidate(device, rssi, uuids);
        DeviceType deviceType = DeviceHelper.getInstance().getSupportedType(candidate);
        if (deviceType.isSupported() || discoverUnsupported) {
            candidate.setDeviceType(deviceType);
            LOG.info("Recognized device: " + candidate);
            int index = deviceCandidates.indexOf(candidate);
            if (index >= 0) {
                deviceCandidates.set(index, candidate); // replace
            } else {
                deviceCandidates.add(candidate);
            }
            deviceCandidateAdapter.notifyDataSetChanged();
            return true;
        }
        return false;
    }

    private void startDiscovery() {
        if (isScanning()) {
            LOG.warn("Not starting discovery, because already scanning.");
            return;
        }

        LOG.info("Starting discovery");
        startButton.setText(getString(R.string.discovery_stop_scanning));
        if (ensureBluetoothReady()) {
            if (GB.supportsBluetoothLE()) {
                startBTLEDiscovery();
                startBTDiscovery();
            } else {
                startBTDiscovery();
            }
            setScanning(true);
        } else {
            toast(DiscoveryActivity.this, getString(R.string.discovery_enable_bluetooth), Toast.LENGTH_SHORT, GB.ERROR);
        }
    }

    private void stopDiscovery() {
        LOG.info("Stopping discovery");
        stopBTDiscovery();
        stopBLEDiscovery();
        setScanning(false);
        handler.removeMessages(0, stopRunnable);
    }

    private boolean isScanning() {
        return scanning;
    }

    public void setScanning(boolean scanning) {
        this.scanning = scanning;
        if (scanning) {
            startButton.setText(getString(R.string.discovery_stop_scanning));
        } else {
            startButton.setText(getString(R.string.discovery_start_scanning));
            bluetoothProgress.setVisibility(View.GONE);
            bluetoothLEProgress.setVisibility(View.GONE);
        }
    }

    private void startBTLEDiscovery() {
        LOG.info("Starting BLE discovery");

        handler.removeMessages(0, stopRunnable);
        handler.sendMessageDelayed(getPostMessage(stopRunnable), SCAN_DURATION);

        // Filters being non-null would be a very good idea with background scan, but in this case,
        // not really required.
        adapter.getBluetoothLeScanner().startScan(null, getScanSettings(), getScanCallback());

        LOG.debug("Bluetooth LE discovery started successfully");
        bluetoothLEProgress.setVisibility(View.VISIBLE);
        setScanning(true);
    }

    private void stopBLEDiscovery() {
        if (adapter == null) {
            return;
        }

        BluetoothLeScanner bluetoothLeScanner = adapter.getBluetoothLeScanner();
        if (bluetoothLeScanner == null) {
            LOG.warn("Could not get BluetoothLeScanner()!");
            return;
        }
        if (newBLEScanCallback == null) {
            LOG.warn("newLeScanCallback == null!");
            return;
        }
        try {
            bluetoothLeScanner.stopScan(newBLEScanCallback);
        } catch (NullPointerException e) {
            LOG.warn("Internal NullPointerException when stopping the scan!");
            return;
        }

        LOG.debug("Stopped BLE discovery");
    }

    /**
     * Starts a regular Bluetooth scan
     */
    private void startBTDiscovery() {
        LOG.info("Starting BT discovery");
        try {
            // LineageOS quirk, can't start scan properly,
            // if scan has been started by something else
            stopBTDiscovery();
        } catch (Exception ignored) {
        }
        handler.removeMessages(0, stopRunnable);
        handler.sendMessageDelayed(getPostMessage(stopRunnable), SCAN_DURATION);
        if (adapter.startDiscovery()) {
            LOG.debug("Discovery started successfully");
            bluetoothProgress.setVisibility(View.VISIBLE);
        } else {
            LOG.error("Discovery starting failed");
        }
    }

    private void stopBTDiscovery() {
        if (adapter != null) {
            adapter.cancelDiscovery();
            LOG.info("Stopped BT discovery");
        }
    }

    private void bluetoothStateChanged(int newState) {
        if (newState == BluetoothAdapter.STATE_ON) {
            this.adapter = BluetoothAdapter.getDefaultAdapter();
            startButton.setEnabled(true);
        } else {
            this.adapter = null;
            startButton.setEnabled(false);
            bluetoothProgress.setVisibility(View.GONE);
            bluetoothLEProgress.setVisibility(View.GONE);
        }
    }

    private boolean checkBluetoothAvailable() {
        BluetoothManager bluetoothService = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        if (bluetoothService == null) {
            LOG.warn("No bluetooth service available");
            this.adapter = null;
            return false;
        }
        BluetoothAdapter adapter = bluetoothService.getAdapter();
        if (adapter == null) {
            LOG.warn("No bluetooth adapter available");
            this.adapter = null;
            return false;
        }
        if (!adapter.isEnabled()) {
            LOG.warn("Bluetooth not enabled");
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBtIntent);
            this.adapter = null;
            return false;
        }
        this.adapter = adapter;
        if (GB.supportsBluetoothLE())
            this.bluetoothLeScanner = adapter.getBluetoothLeScanner();
        return true;
    }

    private boolean ensureBluetoothReady() {
        boolean available = checkBluetoothAvailable();
        startButton.setEnabled(available);
        if (available) {
            adapter.cancelDiscovery();
            // must not return the result of cancelDiscovery()
            // appears to return false when currently not scanning
            return true;
        }
        return false;
    }

    private ScanSettings getScanSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return new ScanSettings.Builder()
                    .setCallbackType(CallbackType)
                    .setScanMode(android.bluetooth.le.ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .setMatchMode(MatchMode)
                    .setPhy(android.bluetooth.le.ScanSettings.PHY_LE_ALL_SUPPORTED)
                    .setNumOfMatches(android.bluetooth.le.ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
                    .build();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return new ScanSettings.Builder()
                    .setCallbackType(CallbackType)
                    .setScanMode(android.bluetooth.le.ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .setMatchMode(MatchMode)
                    .setNumOfMatches(android.bluetooth.le.ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
                    .build();
        } else {
            return new ScanSettings.Builder()
                    .setScanMode(android.bluetooth.le.ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build();
        }
    }

    private List<ScanFilter> getScanFilters() {
        List<ScanFilter> allFilters = new ArrayList<>();
        for (DeviceCoordinator coordinator : DeviceHelper.getInstance().getAllCoordinators()) {
            allFilters.addAll(coordinator.createBLEScanFilters());
        }
        return allFilters;
    }

    private Message getPostMessage(Runnable runnable) {
        Message message = Message.obtain(handler, runnable);
        message.obj = runnable;
        return message;
    }

    private void checkAndRequestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            LOG.error("No permission to access coarse location!");
            toast(DiscoveryActivity.this, getString(R.string.error_no_location_access), Toast.LENGTH_SHORT, GB.ERROR);
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
        }
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            LOG.error("No permission to access fine location!");
            toast(DiscoveryActivity.this, getString(R.string.error_no_location_access), Toast.LENGTH_SHORT, GB.ERROR);
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                LOG.error("No permission to access background location!");
                toast(DiscoveryActivity.this, getString(R.string.error_no_location_access), Toast.LENGTH_SHORT, GB.ERROR);
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, 0);
            }
        }

        LocationManager locationManager = (LocationManager) DiscoveryActivity.this.getSystemService(Context.LOCATION_SERVICE);
        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                // Do nothing
                LOG.debug("Some location provider is enabled, assuming location is enabled");
            } else {
                toast(DiscoveryActivity.this, getString(R.string.require_location_provider), Toast.LENGTH_LONG, GB.ERROR);
                DiscoveryActivity.this.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                // We can't be sure location was enabled, cancel scan start and wait for new user action
                toast(DiscoveryActivity.this, getString(R.string.error_location_enabled_mandatory), Toast.LENGTH_SHORT, GB.ERROR);
                return;
            }
        } catch (Exception ex) {
            LOG.error("Exception when checking location status: ", ex);
        }
        LOG.info("Permissions seems to be fine for scanning");
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        GBDeviceCandidate deviceCandidate = deviceCandidates.get(position);
        if (deviceCandidate == null) {
            LOG.error("Device candidate clicked, but item not found");
            return;
        }
        if (!deviceCandidate.getDeviceType().isSupported()) {
            LOG.error("Unsupported device candidate");
            ArrayList deviceDetails = new ArrayList<>();
            deviceDetails.add(deviceCandidate.getName());
            deviceDetails.add(deviceCandidate.getMacAddress());
            try {
                for (ParcelUuid uuid : deviceCandidate.getServiceUuids()) {
                    deviceDetails.add(uuid.getUuid().toString());
                }
            } catch (Exception e) {
                LOG.error("Error collecting device uuids: " + e);
            }
            String clipboardData = TextUtils.join(", ", deviceDetails);
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText(deviceCandidate.getName(), clipboardData);
            clipboard.setPrimaryClip(clip);
            toast(this, "Device details copied to clipboard", Toast.LENGTH_SHORT, GB.INFO);
            return;
        }

        stopDiscovery();
        DeviceCoordinator coordinator = DeviceHelper.getInstance().getCoordinator(deviceCandidate);
        LOG.info("Using device candidate " + deviceCandidate + " with coordinator: " + coordinator.getClass());

        if (coordinator.getBondingStyle() == DeviceCoordinator.BONDING_STYLE_REQUIRE_KEY) {
            SharedPreferences sharedPrefs = GBApplication.getDeviceSpecificSharedPrefs(deviceCandidate.getMacAddress());

            String authKey = sharedPrefs.getString("authkey", null);
            if (authKey == null || authKey.isEmpty()) {
                toast(DiscoveryActivity.this, getString(R.string.discovery_need_to_enter_authkey), Toast.LENGTH_LONG, GB.WARN);
                return;
            } else if (authKey.getBytes().length < 34 || !authKey.startsWith("0x")) {
                toast(DiscoveryActivity.this, getString(R.string.discovery_entered_invalid_authkey), Toast.LENGTH_LONG, GB.WARN);
                return;
            }
        }

        Class<? extends Activity> pairingActivity = coordinator.getPairingActivity();
        if (pairingActivity != null) {
            Intent intent = new Intent(this, pairingActivity);
            intent.putExtra(DeviceCoordinator.EXTRA_DEVICE_CANDIDATE, deviceCandidate);
            startActivity(intent);
        } else {
            if (coordinator.getBondingStyle() == DeviceCoordinator.BONDING_STYLE_NONE ||
                    coordinator.getBondingStyle() == DeviceCoordinator.BONDING_STYLE_LAZY) {
                LOG.info("No bonding needed, according to coordinator, so connecting right away");
                BondingUtil.connectThenComplete(this, deviceCandidate);
                return;
            }

            try {
                this.deviceTarget = deviceCandidate;
                BondingUtil.initiateCorrectBonding(this, deviceCandidate);
            } catch (Exception e) {
                LOG.error("Error pairing device: " + deviceCandidate.getMacAddress());
            }
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
        stopDiscovery();
        final GBDeviceCandidate deviceCandidate = deviceCandidates.get(position);
        if (deviceCandidate == null) {
            LOG.error("Device candidate clicked, but item not found");
            return true;
        }
        if (!deviceCandidate.getDeviceType().isSupported()) {
            LOG.error("Unsupported device candidate");
            LinkedHashMap<String, Pair<Long, Integer>> allDevices;
            allDevices = DebugActivity.getAllSupportedDevices(getApplicationContext());

            final LinearLayout linearLayout = new LinearLayout(DiscoveryActivity.this);
            linearLayout.setOrientation(LinearLayout.VERTICAL);

            final LinearLayout macLayout = new LinearLayout(DiscoveryActivity.this);
            macLayout.setOrientation(LinearLayout.HORIZONTAL);
            macLayout.setPadding(20, 0, 20, 0);

            final Spinner deviceListSpinner = new Spinner(DiscoveryActivity.this);
            ArrayList<SpinnerWithIconItem> deviceListArray = new ArrayList<>();
            for (Map.Entry<String, Pair<Long, Integer>> item : allDevices.entrySet()) {
                deviceListArray.add(new SpinnerWithIconItem(item.getKey(), item.getValue().first, item.getValue().second));
            }
            final SpinnerWithIconAdapter deviceListAdapter = new SpinnerWithIconAdapter(DiscoveryActivity.this,
                    R.layout.spinner_with_image_layout, R.id.spinner_item_text, deviceListArray);
            deviceListSpinner.setAdapter(deviceListAdapter);
            addListenerOnSpinnerDeviceSelection(deviceListSpinner);

            linearLayout.addView(deviceListSpinner);
            linearLayout.addView(macLayout);

            new AlertDialog.Builder(DiscoveryActivity.this)
                    .setCancelable(true)
                    .setTitle(R.string.add_test_device)
                    .setView(linearLayout)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (selectedUnsupportedDeviceKey != DebugActivity.SELECT_DEVICE) {
                                DebugActivity.createTestDevice(DiscoveryActivity.this, selectedUnsupportedDeviceKey, deviceCandidate.getMacAddress());
                                finish();
                            }
                        }
                    })
                    .setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
                    .show();

            return true;
        }

        DeviceCoordinator coordinator = DeviceHelper.getInstance().getCoordinator(deviceCandidate);
        GBDevice device = DeviceHelper.getInstance().toSupportedDevice(deviceCandidate);
        if (coordinator.getSupportedDeviceSpecificSettings(device) == null) {
            return true;
        }

        Intent startIntent;
        startIntent = new Intent(this, DeviceSettingsActivity.class);
        startIntent.putExtra(GBDevice.EXTRA_DEVICE, device);
        if (coordinator.getBondingStyle() == DeviceCoordinator.BONDING_STYLE_REQUIRE_KEY) {
            startIntent.putExtra(DeviceSettingsActivity.MENU_ENTRY_POINT, DeviceSettingsActivity.MENU_ENTRY_POINTS.AUTH_SETTINGS);
        } else {
            startIntent.putExtra(DeviceSettingsActivity.MENU_ENTRY_POINT, DeviceSettingsActivity.MENU_ENTRY_POINTS.DEVICE_SETTINGS);
        }
        startActivity(startIntent);
        return true;
    }

    private void addListenerOnSpinnerDeviceSelection(Spinner spinner) {
        spinner.setOnItemSelectedListener(new CustomOnDeviceSelectedListener());
    }

    public void onBondingComplete(boolean success) {
        finish();
    }

    public GBDeviceCandidate getCurrentTarget() {
        return this.deviceTarget;
    }

    public void unregisterBroadcastReceivers() {
        AndroidUtils.safeUnregisterBroadcastReceiver(this, bluetoothReceiver);
    }

    public void registerBroadcastReceivers() {
        IntentFilter bluetoothIntents = new IntentFilter();
        bluetoothIntents.addAction(BluetoothDevice.ACTION_FOUND);
        bluetoothIntents.addAction(BluetoothDevice.ACTION_UUID);
        bluetoothIntents.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        bluetoothIntents.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        bluetoothIntents.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);

        registerReceiver(bluetoothReceiver, bluetoothIntents);
    }

    private void loadSettingsValues() {
        Prefs prefs = GBApplication.getPrefs();
        ignoreBonded = prefs.getBoolean("ignore_bonded_devices", true);
        discoverUnsupported = prefs.getBoolean("discover_unsupported_devices", false);
        int level = prefs.getInt("scanning_intensity", 1);
        switch (level) {
            case 0:
                CallbackType = android.bluetooth.le.ScanSettings.CALLBACK_TYPE_FIRST_MATCH;
                MatchMode = android.bluetooth.le.ScanSettings.MATCH_MODE_STICKY;
                break;
            case 1:
                CallbackType = android.bluetooth.le.ScanSettings.CALLBACK_TYPE_FIRST_MATCH;
                MatchMode = android.bluetooth.le.ScanSettings.MATCH_MODE_AGGRESSIVE;
                break;
            case 2:
                CallbackType = android.bluetooth.le.ScanSettings.CALLBACK_TYPE_ALL_MATCHES;
                MatchMode = android.bluetooth.le.ScanSettings.MATCH_MODE_STICKY;
                break;
            case 3:
                CallbackType = android.bluetooth.le.ScanSettings.CALLBACK_TYPE_ALL_MATCHES;
                MatchMode = android.bluetooth.le.ScanSettings.MATCH_MODE_AGGRESSIVE;
                break;
        }
        LOG.debug("Device discovery - scanning level: " + level + " " + CallbackType + " " + MatchMode);
    }

    @Override
    public Context getContext() {
        return this;
    }

    public class CustomOnDeviceSelectedListener implements AdapterView.OnItemSelectedListener {

        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            SpinnerWithIconItem selectedItem = (SpinnerWithIconItem) parent.getItemAtPosition(pos);
            selectedUnsupportedDeviceKey = selectedItem.getId();
        }

        @Override
        public void onNothingSelected(AdapterView<?> arg0) {
            // TODO Auto-generated method stub
        }

    }

    private class BTUUIDPair {
        private final BluetoothDevice bluetoothDevice;
        private final ParcelUuid[] parcelUuid;

        public BTUUIDPair(BluetoothDevice bluetoothDevice, ParcelUuid[] parcelUuid) {
            this.bluetoothDevice = bluetoothDevice;
            this.parcelUuid = parcelUuid;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BTUUIDPair that = (BTUUIDPair) o;
            return bluetoothDevice.equals(that.bluetoothDevice) && Arrays.equals(parcelUuid, that.parcelUuid);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(bluetoothDevice);
            result = 31 * result + Arrays.hashCode(parcelUuid);
            return result;
        }
    }


}
