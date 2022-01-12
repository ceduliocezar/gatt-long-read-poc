package com.ceduliocezar.longreadpoc.ui.dashboard

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.os.Bundle
import android.os.ParcelUuid
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.ceduliocezar.longreadpoc.MainActivity
import com.ceduliocezar.longreadpoc.databinding.FragmentDashboardBinding
import com.ceduliocezar.longreadpoc.ui.ByteArrayHelper
import java.util.Arrays

private const val TAG = "DashboardFragment"

class DashboardFragment : Fragment() {

    private val advertiseCallback: AdvertiseCallback = object : AdvertiseCallback() {}
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var gattServer: BluetoothGattServer
    private val callback: BluetoothGattServerCallback = object : BluetoothGattServerCallback() {
        override fun onCharacteristicReadRequest(
            device: BluetoothDevice?,
            requestId: Int,
            offset: Int,
            characteristic: BluetoothGattCharacteristic?
        ) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic)

            Log.d(
                TAG,
                "onCharacteristicReadRequest device:${device?.name}, requestId:$requestId, offset:$offset, characteristic:${characteristic?.uuid}"
            )

            val slicedArray = ByteArrayHelper.skipBytes(characteristic!!.value, offset)

            //val slicedArray = ByteArrayHelper.chopBeginAndEnd(characteristic, offset)

            Log.d(
                TAG,
                "Value returned: ${String(slicedArray)}"
            )

            gattServer.sendResponse(
                device,
                requestId,
                BluetoothGatt.GATT_SUCCESS,
                offset,
                slicedArray
            )
        }

    }

    private fun copyOf(source: ByteArray, offset: Int, maxSize: Int): ByteArray {
        if (source.size > maxSize) {
            val chunkSize = Math.min(source.size - offset, maxSize)
            return Arrays.copyOfRange(source, offset, offset + chunkSize)
        }
        return Arrays.copyOf(source, source.size)
    }


    private lateinit var dashboardViewModel: DashboardViewModel
    private var _binding: FragmentDashboardBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dashboardViewModel =
            ViewModelProvider(this).get(DashboardViewModel::class.java)

        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.startServer.setOnClickListener {
            startServer()
        }

        binding.addService.setOnClickListener {
            addService()
        }

        binding.startAdvertising.setOnClickListener {
            startAdvertising()
        }
        return root
    }

    /**
     * Begin advertising over Bluetooth that this device is connectable
     * and supports the Current Time Service.
     */
    private fun startAdvertising() {
        val bluetoothLeAdvertiser: BluetoothLeAdvertiser? =
            bluetoothManager.adapter.bluetoothLeAdvertiser

        bluetoothLeAdvertiser?.let {
            val settings = AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setConnectable(true)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .build()

            val data = AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .setIncludeTxPowerLevel(false)
                .addServiceUuid(ParcelUuid(MainActivity.SERVICE_ID))
                .build()

            it.startAdvertising(settings, data, advertiseCallback)
        } ?: Log.w(TAG, "Failed to create advertiser")
    }

    private fun startServer() {
        bluetoothManager =
            requireContext().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

        gattServer = bluetoothManager.openGattServer(requireContext(), callback)
    }

    private fun addService() {
        val bluetoothGattService = BluetoothGattService(
            MainActivity.SERVICE_ID,
            BluetoothGattService.SERVICE_TYPE_PRIMARY
        )

        val bluetoothGattCharacteristic = BluetoothGattCharacteristic(
            MainActivity.CHARACTERISTIC_ID,
            BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED
        )

        bluetoothGattCharacteristic.value = MainActivity.CHARACTERISTIC_VALUE.toByteArray()
        bluetoothGattService.addCharacteristic(bluetoothGattCharacteristic)

        gattServer.addService(bluetoothGattService)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}