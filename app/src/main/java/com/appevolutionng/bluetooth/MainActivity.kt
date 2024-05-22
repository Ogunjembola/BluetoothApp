package com.appevolutionng.bluetooth
import android.Manifest
import android.app.AlertDialog
import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.appevolutionng.bluetooth.bluetooth.BluetoothController
import com.appevolutionng.bluetooth.ui.DeviceRecyclerViewAdapter
import com.appevolutionng.bluetooth.ui.ListInteractionListener
import com.appevolutionng.bluetooth.ui.RecyclerViewProgressEmptySupport
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity(), ListInteractionListener<BluetoothDevice> {
    private val TAG = "MainActivity"
    private lateinit var bluetooth: BluetoothController
    private lateinit var fab: FloatingActionButton
    private var bondingProgressDialog: ProgressDialog? = null
    private lateinit var recyclerViewAdapter: DeviceRecyclerViewAdapter
    private lateinit var recyclerView: RecyclerViewProgressEmptySupport
    private lateinit var adContainer: LinearLayout

    // Permission request code
    private val REQUEST_BLUETOOTH_SCAN_PERMISSION = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        recyclerViewAdapter = DeviceRecyclerViewAdapter(this, this)
        recyclerView = findViewById(R.id.list)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.emptyView = findViewById(R.id.empty_list)
        recyclerView.progressView = findViewById<ProgressBar>(R.id.progressBar)
        recyclerView.adapter = recyclerViewAdapter

        val hasBluetooth = packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)
        if (!hasBluetooth) {
            val dialog = AlertDialog.Builder(this).create()
            dialog.setTitle(getString(R.string.bluetooth_not_available_title))
            dialog.setMessage(getString(R.string.bluetooth_not_available_message))
            dialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK") { _, _ -> finish() }
            dialog.setCancelable(false)
            dialog.show()
        }
        bluetooth =
            BluetoothController(this, BluetoothAdapter.getDefaultAdapter(), recyclerViewAdapter)

        fab = findViewById(R.id.fab)
        fab.setOnClickListener { view ->
            if (!bluetooth.isBluetoothEnabled()) {
                Snackbar.make(view, R.string.enabling_bluetooth, Snackbar.LENGTH_SHORT).show()
                bluetooth.turnOnBluetoothAndScheduleDiscovery()
            } else {
                if (!bluetooth.isDiscovering()) {
                    Snackbar.make(
                        view,
                        R.string.device_discovery_started,
                        Snackbar.LENGTH_SHORT
                    )
                        .show()
                    bluetooth.startDiscovery()
                } else {
                    Snackbar.make(
                        view,
                        R.string.device_discovery_stopped,
                        Snackbar.LENGTH_SHORT
                    )
                        .show()
                    bluetooth.cancelDiscovery()
                }
            }
        }


        // Check and request Bluetooth scan permission
        checkBluetoothScanPermission()
    }

    private fun checkBluetoothScanPermission() {
        val permission1 =
            ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        val permission2 =
            ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
        if (permission1 != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                this,
                PERMISSIONS_STORAGE,
                1
            );
        } else if (permission2 != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(
                this,
                PERMISSIONS_LOCATION,
                1
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_BLUETOOTH_SCAN_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with Bluetooth scanning
                // You can start Bluetooth scanning here if needed
            } else {
                // Permission denied, handle accordingly
                Toast.makeText(
                    this,
                    "Bluetooth scan permission denied. Cannot perform Bluetooth scanning.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    override fun onItemClick(item: BluetoothDevice) {
        // Log.d(TAG, "Item clicked : ${BluetoothController.deviceToString(item)}")
        if (bluetooth.isAlreadyPaired(item)) {
            Log.d(TAG, "Device already paired!")
            Toast.makeText(this, R.string.device_already_paired, Toast.LENGTH_SHORT).show()
        } else {
            Log.d(TAG, "Device not paired. Pairing.")
            val outcome = bluetooth.pair(item)
            val deviceName = bluetooth.getDeviceName(this, item)
            if (outcome) {
                bondingProgressDialog =
                    ProgressDialog.show(this, "", "Pairing with device $deviceName...", true, false)
            } else {
                Log.d(TAG, "Error while pairing with device $deviceName!")
                Toast.makeText(
                    this,
                    "Error while pairing with device $deviceName!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun startLoading() {
        recyclerView.startLoading()
        fab.setImageResource(R.drawable.ic_bluetooth_searching_white_24dp)
    }

    override fun endLoading(partialResults: Boolean) {
        recyclerView.endLoading()
        if (!partialResults) {
            fab.setImageResource(R.drawable.ic_bluetooth_white_24dp)
        }
    }

    override fun endLoadingWithDialog(error: Boolean, device: BluetoothDevice) {
        bondingProgressDialog?.let {
            val view = findViewById<View>(R.id.main_content)
            val message = if (error) {
                "Failed pairing with device ${bluetooth.getDeviceName(this,device)}!"
            } else {
                "Successfully paired with device ${bluetooth.getDeviceName(this,device)}!"
            }
            it.dismiss()
            Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show()
        }

    }

    override fun onDestroy() {
        bluetooth.close()
        super.onDestroy()
    }

    override fun onRestart() {
        super.onRestart()
        bluetooth.cancelDiscovery()
        recyclerViewAdapter.cleanView()
    }

    override fun onStop() {
        super.onStop()
        bluetooth.cancelDiscovery()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE || newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {

        }
    }

    private val PERMISSIONS_STORAGE = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS,
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_PRIVILEGED
    )
    private val PERMISSIONS_LOCATION = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS,
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_PRIVILEGED
    )
}
















