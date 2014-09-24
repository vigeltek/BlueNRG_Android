package com.st.bluenrg;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

public class DeviceScanActivity extends ListActivity {
	public static boolean accesso = true;
	private LeDeviceListAdapter mLeDeviceListAdapter;
	private BluetoothAdapter mBluetoothAdapter;
	private boolean mScanning;
	private Handler mHandler;

	private static final int REQUEST_ENABLE_BT = 1;

	private static final long SCAN_PERIOD = 10000;
	private static final int SHOW_DETAILS = 100;

	public static ArrayList<visualLog> log = new ArrayList<visualLog>();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getActionBar().setTitle(R.string.title_devices);


		mHandler = new Handler();

		if (!getPackageManager().hasSystemFeature(
				PackageManager.FEATURE_BLUETOOTH_LE)) {
			Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT)
					.show();
			finish();
		}

		final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = bluetoothManager.getAdapter();

		if (mBluetoothAdapter == null) {
			Toast.makeText(this, R.string.error_bluetooth_not_supported,
					Toast.LENGTH_SHORT).show();
			finish();
			return;
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		if (!mScanning) {
			menu.findItem(R.id.menu_stop).setVisible(false);
			menu.findItem(R.id.menu_scan).setVisible(true);
			menu.findItem(R.id.menu_refresh).setActionView(null);
		} else {
			menu.findItem(R.id.menu_stop).setVisible(true);
			menu.findItem(R.id.menu_scan).setVisible(false);
			menu.findItem(R.id.menu_refresh).setActionView(
					R.layout.actionbar_indeterminate_progress);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_scan:
			mLeDeviceListAdapter.clear();
			scanLeDevice(true);
			break;
		case R.id.menu_stop:
			scanLeDevice(false);
			break;
		case R.id.menu_info:
			Intent intent = new Intent(this, AboutActivity.class);
			startActivity(intent);
			break;
		}
		return true;
	}

	@Override
	protected void onResume() {
		super.onResume();

		invalidateOptionsMenu();

		mLeDeviceListAdapter = new LeDeviceListAdapter();
		setListAdapter(mLeDeviceListAdapter);
		mLeDeviceListAdapter.notifyDataSetChanged();


		Thread enableThread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					while (!mBluetoothAdapter.isEnabled()) {
						mBluetoothAdapter.enable();
						Log.d("resume", "enable...");
						Thread.sleep(500);
					}
				} catch (Exception e) {
					e.getLocalizedMessage();
				}
				Log.d("resume",
						"bluetooth enabled: " + mBluetoothAdapter.isEnabled()
								+ "; scan...");
				scanLeDevice(true);
			}
		});
		enableThread.start();
		Log.d("method", "onresume");
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (requestCode == SHOW_DETAILS
				&& resultCode == DetailsActivity.BOARD_DISCONNECTED) {
			final AlertDialog alertDialog = new AlertDialog.Builder(
					DeviceScanActivity.this).create();
			alertDialog.setTitle("Error");
			alertDialog.setMessage("Peripheral disconnected!");
			alertDialog.setIcon(R.drawable.fail);
			alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							alertDialog.dismiss();
						}

					});
			alertDialog.setCancelable(false);
			alertDialog.show();
		} else if (requestCode == SHOW_DETAILS
				&& resultCode == DetailsActivity.WRONG_FIRMWARE) {
			final AlertDialog alertDialog = new AlertDialog.Builder(
					DeviceScanActivity.this).create();
			alertDialog.setTitle("Error!");
			alertDialog.setCancelable(false);
			alertDialog
					.setMessage("No valid characteristics found. Wrong firmware? Please check instruction on how to setup the BlueNRG board");
			alertDialog.setIcon(R.drawable.fail);
			alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Instructions",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							Intent webIntent = new Intent(
									Intent.ACTION_VIEW,
									Uri.parse("http://www.st.com/web/catalog/tools/FM116/SC1075/PF259562"));
							startActivity(webIntent);
							alertDialog.dismiss();
						}
					});
			alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							alertDialog.dismiss();
						}
					});
			alertDialog.show();
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	protected void onPause() {
		super.onPause();
		scanLeDevice(false);
		mLeDeviceListAdapter.clear();
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
		if (device == null)
			return;

		if (device != null && device.getName() != null
				&& device.getName().contains("BlueNRG")) {
			final Intent intent = new Intent(this, DetailsActivity.class);
			intent.putExtra(DetailsActivity.EXTRAS_DEVICE_NAME,
					device.getName());
			intent.putExtra(DetailsActivity.EXTRAS_DEVICE_ADDRESS,
					device.getAddress());

			if (mScanning) {
				mBluetoothAdapter.stopLeScan(mLeScanCallback);
				mScanning = false;
			}
			startActivityForResult(intent, SHOW_DETAILS);
		} else
			return;
	}

	private void scanLeDevice(final boolean enable) {

		if (enable) {

			String currentTime = new SimpleDateFormat("HH:mm:ss")
					.format(new Date());
			DeviceScanActivity.log.add(new visualLog(currentTime,
					"Scan for bluetooth devices..."));
			mHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					mScanning = false;
					mBluetoothAdapter.stopLeScan(mLeScanCallback);
					invalidateOptionsMenu();
				}
			}, SCAN_PERIOD);

			mScanning = true;
			mBluetoothAdapter.startLeScan(mLeScanCallback);
		} else {
			mScanning = false;
			mBluetoothAdapter.stopLeScan(mLeScanCallback);
		}
		invalidateOptionsMenu();
	}

	private class LeDeviceListAdapter extends BaseAdapter {
		private ArrayList<BluetoothDevice> mLeDevices;
		private LayoutInflater mInflator;

		public LeDeviceListAdapter() {
			super();
			mLeDevices = new ArrayList<BluetoothDevice>();
			mInflator = DeviceScanActivity.this.getLayoutInflater();
		}

		public void addDevice(BluetoothDevice device) {
			if (!mLeDevices.contains(device)) {
				mLeDevices.add(device);
				sort();
			}
		}

		public void sort() {
			Collections.sort(mLeDevices, new Comparator<BluetoothDevice>() {
				@Override
				public int compare(BluetoothDevice item1, BluetoothDevice item2) {
					if (item1 != null && item1.getName() != null
							&& item1.getName().equalsIgnoreCase("BlueNRG"))
						return -1;
					return item1.getName().compareTo(item2.getName());
				};
			});
		}

		public BluetoothDevice getDevice(int position) {
			return mLeDevices.get(position);
		}

		public void clear() {
			mLeDevices.clear();
		}

		@Override
		public int getCount() {
			return mLeDevices.size();
		}

		@Override
		public Object getItem(int i) {
			return mLeDevices.get(i);
		}

		@Override
		public long getItemId(int i) {
			return i;
		}

		@Override
		public View getView(int i, View view, ViewGroup viewGroup) {
			int theType = 0;
			ViewHolder viewHolder;

			if (view == null) {
				view = mInflator.inflate(R.layout.listitem_device, null);
				viewHolder = new ViewHolder();
				viewHolder.deviceAddress = (TextView) view
						.findViewById(R.id.device_address);
				viewHolder.deviceName = (TextView) view
						.findViewById(R.id.device_name);
				viewHolder.deviceLogo = (ImageView) view
						.findViewById(R.id.device_logo);

				view.setTag(viewHolder);
			} else {
				viewHolder = (ViewHolder) view.getTag();
			}

			BluetoothDevice device = mLeDevices.get(i);
			final String deviceName = device.getName();
			if (deviceName != null && deviceName.length() > 0) {

				viewHolder.deviceName.setText(deviceName);
			} else
				viewHolder.deviceName.setText(R.string.unknown_device);
			viewHolder.deviceAddress.setText(device.getAddress());

			if (deviceName != null && deviceName.equalsIgnoreCase("BlueNRG")) {
				viewHolder.deviceLogo.setVisibility(View.VISIBLE);
				viewHolder.deviceLogo.setImageDrawable(getResources()
						.getDrawable(R.drawable.st_logo));
			} else {
				viewHolder.deviceLogo.setVisibility(View.GONE);
			}

			return view;
		}
	}


	private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

		@Override
		public void onLeScan(final BluetoothDevice device, int rssi,
				byte[] scanRecord) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					mLeDeviceListAdapter.addDevice(device);
					DeviceScanActivity.this.invalidateOptionsMenu();

					mLeDeviceListAdapter.notifyDataSetChanged();
				}
			});
		}
	};

	static class ViewHolder {
		TextView deviceName;
		TextView deviceAddress;
		ImageView deviceLogo;
	}

}