package com.appevolutionng.bluetooth.ui

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.RecyclerView
import com.appevolutionng.bluetooth.R
import com.appevolutionng.bluetooth.bluetooth.BluetoothController
import com.appevolutionng.bluetooth.bluetooth.BluetoothDiscoveryDeviceListener

class DeviceRecyclerViewAdapter(private val context: Context,private val listener: ListInteractionListener<BluetoothDevice>) :
        RecyclerView.Adapter<DeviceRecyclerViewAdapter.ViewHolder>(),
    BluetoothDiscoveryDeviceListener {

        private val devices: MutableList<BluetoothDevice> = mutableListOf()
        private var bluetooth: BluetoothController? = null
    var mItem: BluetoothDevice? = null


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.list_device_item, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val device = devices[position]
            holder.bind(device)
            holder.itemView.setOnClickListener {
                listener.onItemClick(device)
            }
        }

        override fun getItemCount(): Int = devices.size



    override fun onDeviceDiscovered(device: BluetoothDevice) {
        listener.endLoading(true)
        devices.add(device)
        notifyDataSetChanged()
    }

    override fun onDeviceDiscoveryStarted() {
            cleanView()
            listener.startLoading()
        }

        fun cleanView() {
            devices.clear()
            notifyDataSetChanged()
        }

        override fun setBluetoothController(bluetooth: BluetoothController) {
            this.bluetooth = bluetooth
        }

        override fun onDeviceDiscoveryEnd() {
            listener.endLoading(false)
        }

        override fun onBluetoothStatusChanged() {
            bluetooth?.onBluetoothStatusChanged()
        }

        override fun onBluetoothTurningOn() {
            listener.startLoading()
        }

        override fun onDevicePairingEnded() {
            bluetooth?.let { bluetooth ->
                if (bluetooth.isPairingInProgress()) {
                    val device = bluetooth.getBoundingDevice()
                    when (bluetooth.getPairingDeviceStatus()) {
                        BluetoothDevice.BOND_BONDING -> {
                            // Still pairing, do nothing.
                        }

                        BluetoothDevice.BOND_BONDED -> {
                            // Successfully paired.
                            if (device != null) {
                                listener.endLoadingWithDialog(false, device)
                            }
                            notifyDataSetChanged()
                        }

                        BluetoothDevice.BOND_NONE -> {
                            // Failed pairing.
                            if (device != null) {
                                listener.endLoadingWithDialog(true, device)
                            }
                        }
                    }
                }
            }
        }

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val imageView: ImageView = itemView.findViewById(R.id.device_icon)
            private val deviceNameView: TextView = itemView.findViewById(R.id.device_name)
            private val deviceAddressView: TextView = itemView.findViewById(R.id.device_address)

            fun bind(device: BluetoothDevice) {
                imageView.setImageResource(getDeviceIcon(device))
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return
                }
                deviceNameView.text = device.name
                deviceAddressView.text = device.address
            }


//            override fun toString(): String {
//                return super.toString() + " '" + bluetooth.deviceToString(context,mItem) + "'"
//            }

        }

        private fun getDeviceIcon(device: BluetoothDevice): Int {
            return if (bluetooth?.isAlreadyPaired(device) == true) {
                R.drawable.ic_bluetooth_connected_black_24dp
            } else {
                R.drawable.ic_bluetooth_black_24dp
            }
        }

}