package com.st.bluenrg;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class BluetoothLeService extends Service {
    private final static String TAG = BluetoothLeService.class.getSimpleName();
    
    private Queue<BluetoothGattCharacteristic> characteristicReadQueue = new LinkedList<BluetoothGattCharacteristic>();

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;

    public final static String ACTION_GATT_CONNECTED =
            "com.st.bluenrg.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.st.bluenrg.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.st.bluenrg.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.st.bluenrg.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.st.bluenrg.EXTRA_DATA";    
    
    public final static UUID UUID_ACCELERATION=
            UUID.fromString("340a1b80-cf4b-11e1-ac36-0002a5d5c51b");
    public final static UUID UUID_FREE_FALL =
            UUID.fromString("e23e78a0-cf4a-11e1-8ffc-0002a5d5c51b");
    public final static UUID UUID_TEMPERATURE =
            UUID.fromString("a32e5520-e477-11e2-a9e3-0002a5d5c51b");
    public final static UUID UUID_PRESSURE =
            UUID.fromString("cd20c480-e48b-11e2-840b-0002a5d5c51b");
    public final static UUID UUID_HUMIDITY =
            UUID.fromString("01c50b60-e48c-11e2-a073-0002a5d5c51b");
    public final static UUID CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    public final static UUID CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID1 =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fc");

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.");

                Log.i(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                
                Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);
                mBluetoothAdapter.disable();
                mBluetoothAdapter.enable();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        	Log.d("ble", "stefano108onServicesDiscovered");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
        	Log.d("ble", "stefano104onCharacteristicRead");
        	characteristicReadQueue.remove();
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }else{
        		Log.d(TAG, "onCharacteristicRead error: " + status);
    		}
            if(characteristicReadQueue.size() > 0)
        		mBluetoothGatt.readCharacteristic(characteristicReadQueue.element());
        		
        }
        
        @Override
        public void onReadRemoteRssi (BluetoothGatt gatt, int rssi, int status){
        	Log.d("ble", "stefano105onReadRemoteRssi");
        	if (status == BluetoothGatt.GATT_SUCCESS) {
        		final Intent intent = new Intent(ACTION_DATA_AVAILABLE);
                intent.putExtra(EXTRA_DATA, "5;"+rssi);
                sendBroadcast(intent);
            }else{
        		Log.d(TAG, "onReadRemoteRssi error: " + status);
    		}
        }
        
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) { 
        	Log.d("ble", "stefano106onDescriptorWrite");        
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Callback: Wrote GATT Descriptor successfully.");           
            }           
            else{
                Log.d(TAG, "Callback: Error writing GATT Descriptor: "+ status);
            }

            if(characteristicReadQueue.size() > 0)
                mBluetoothGatt.readCharacteristic(characteristicReadQueue.element());
        };

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
        	Log.d("ble", "stefano107onCharacteristicChanged");      
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }
    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        if (UUID_ACCELERATION.equals(characteristic.getUuid())) {
        	short x,y,z;
        	final byte[] data = characteristic.getValue();
        	if (data.length == 6){
            	x = ByteBuffer.wrap(data, 0, 2).order(java.nio.ByteOrder.LITTLE_ENDIAN).getShort();
            	y = ByteBuffer.wrap(data, 2, 2).order(java.nio.ByteOrder.LITTLE_ENDIAN).getShort();
            	z = ByteBuffer.wrap(data, 4, 2).order(java.nio.ByteOrder.LITTLE_ENDIAN).getShort();
            	Log.d(TAG, "__Motion: "+x+";"+y+";"+z);
            	intent.putExtra(EXTRA_DATA, "0;"+x+";"+y+";"+z);
        	}
        	
        }else if (UUID_TEMPERATURE.equals(characteristic.getUuid())) {
            float temperatura =(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT16,0)).floatValue();
            temperatura /= 10.0f;
            Log.d(TAG, String.format("__Temperatura: %,2.1f", temperatura));
            
            intent.putExtra(EXTRA_DATA, String.valueOf("1;"+temperatura));
        } else if (UUID_PRESSURE.equals(characteristic.getUuid())) {
        	byte [] tm = characteristic.getValue();
        	long val = 0;
        	for (int i = 0; i < tm.length; i++)
        	{
        	   val += ((long) tm[i] & 0xffL) << (i*8);
        	}
            float pressione = val/100.0f;
            Log.d(TAG, String.format("__Pressione: %,2.2f", pressione));
            intent.putExtra(EXTRA_DATA, String.valueOf("2;"+pressione));
            
        } else if (UUID_HUMIDITY.equals(characteristic.getUuid())) {
        	float umidita = (characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16,0)).floatValue();
            umidita /= 10.0f;
            Log.d(TAG, String.format("__Umidità: %,2.1f", umidita));
            intent.putExtra(EXTRA_DATA, String.valueOf("3;"+umidita));
        } else if (UUID_FREE_FALL.equals(characteristic.getUuid())) {
            Log.d(TAG, "FREE_FALL");
            intent.putExtra(EXTRA_DATA, String.valueOf("4; "));
        }else {
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                Log.d(TAG, "Caratteristica generica");
                for(byte byteChar : data)
                    stringBuilder.append(String.format("%02X ", byteChar));
                intent.putExtra(EXTRA_DATA, new String(data) + "\n" + stringBuilder.toString());
            }
        }
        
        sendBroadcast(intent);
    }
    

    public class LocalBinder extends Binder {
        BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        close();
        if (mBluetoothGatt!=null) mBluetoothGatt.close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
      
        characteristicReadQueue.add(characteristic);
        if(characteristicReadQueue.size() == 1)
        	System.out.println("lettura_");
            mBluetoothGatt.readCharacteristic(characteristic); 
    }
    
    public boolean readRemoteRssi (){
    	return mBluetoothGatt.readRemoteRssi();
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled If true, enable notification.  False otherwise.
     */
    public boolean setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return false;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
        
        if (UUID_FREE_FALL.equals(characteristic.getUuid())) {
        	BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID);
            descriptor.setValue(enabled ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : new byte[] { 0x00, 0x00 });
            return mBluetoothGatt.writeDescriptor(descriptor); //descriptor write operation successfully started? 
        } else if (UUID_ACCELERATION.equals(characteristic.getUuid())) {
        	Log.d("stefano", "UUID_ACCELERATION setCharacteristicNotification");
        	BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID);
            descriptor.setValue(enabled ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : new byte[] { 0x00, 0x00 });
            return mBluetoothGatt.writeDescriptor(descriptor); //descriptor write operation successfully started? 
        }
        return false;
    }
    

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }
}
