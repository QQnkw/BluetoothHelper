package com.bluetooth.bluetoothhelper.ui.scan

import android.Manifest
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.lifecycle.*
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bluetooth.bluetoothhelper.BluetoothHelper
import com.bluetooth.bluetoothhelper.R
import com.bluetooth.bluetoothhelper.databinding.ScanDeviceFragmentBinding
import com.welie.blessed.BluetoothCentralManager
import com.welie.blessed.ConnectionState
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.consume
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.*
import timber.log.Timber
import java.util.*

class ScanDeviceFragment : Fragment() {

    private val viewModel: ScanDeviceViewModel by viewModels()
    private lateinit var adapter: ScanDeviceAdapter
    private lateinit var bluetoothHelper: BluetoothHelper
    private var _binding: ScanDeviceFragmentBinding? = null
    private val deviceList = mutableListOf<Triple<String, String, String>>()

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding
        get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ScanDeviceFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bluetoothHelper.stopScanning()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bluetoothHelper = BluetoothHelper.getInstance(requireContext())
        initView()
        initObserver()
        requireActivity().registerReceiver(
            locationServiceStateReceiver,
            IntentFilter(LocationManager.MODE_CHANGED_ACTION)
        )
    }

    private fun initView() {
        binding.viewActionStartScan.setOnClickListener {
            if (deviceList.isNotEmpty()) {
                adapter.notifyItemRangeRemoved(0, deviceList.size)
                deviceList.clear()
            }
            val name = binding.viewInputFilter.text.toString()
            val uppercase = name.uppercase(Locale.getDefault())
            bluetoothHelper.startScanning(uppercase)
        }
        binding.viewActionStopScan.setOnClickListener {
            bluetoothHelper.stopScanning()
        }
        binding.viewRecyclerList.layoutManager = LinearLayoutManager(requireContext())
        binding.viewRecyclerList.setHasFixedSize(true)
        adapter = ScanDeviceAdapter(deviceList) {
            viewLifecycleOwner.lifecycleScope.launchWhenResumed {
                bluetoothHelper.stopScanning()
                bluetoothHelper.connectDevice(it)
            }
        }
        binding.viewRecyclerList.adapter = adapter
    }

    private fun openConnectFragment() {
        findNavController().navigate(
            R.id.action_scanDeviceFragment_to_connectDeviceFragment
        )
    }

    private fun initObserver() {
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            bluetoothHelper.scanDeviceFlow
                .filter { device ->
                    val item = deviceList.find {
                        it.second == device.second
                    }
                    item == null
                }.collect {
                    Timber.d("device:${it.first}--${it.second}--${it.third}")
                    deviceList.add(it)
                    adapter.notifyItemInserted(deviceList.size - 1)
                }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            bluetoothHelper.connectStateFlow
                .flowWithLifecycle(viewLifecycleOwner.lifecycle, Lifecycle.State.STARTED)
                .collect {
                    Timber.d("ConnectionState-->2.${it.first.name}")
                    when (it.first) {
                        ConnectionState.CONNECTED -> {
                            openConnectFragment()
                        }
                        else -> {
                        }
                    }
                }
        }
    }

    private val locationServiceStateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action != null && action == LocationManager.MODE_CHANGED_ACTION) {
                val isEnabled = areLocationServicesEnabled()
                Timber.d("Location service state changed to: %s", if (isEnabled) "on" else "off")
                checkPermissions()
            }
        }
    }

    private fun areLocationServicesEnabled(): Boolean {
        val locationManager =
            requireActivity().application.getSystemService(AppCompatActivity.LOCATION_SERVICE) as LocationManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            locationManager.isLocationEnabled
        } else {
            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetworkEnabled =
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            isGpsEnabled || isNetworkEnabled
        }
    }

    private val requiredPermissions: Array<String>
        get() {
            val targetSdkVersion = requireActivity().applicationInfo.targetSdkVersion
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && targetSdkVersion >= Build.VERSION_CODES.S) {
                arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && targetSdkVersion >= Build.VERSION_CODES.Q) {
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
            } else arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

    private fun checkPermissions() {
        val missingPermissions = getMissingPermissions(requiredPermissions)
        if (missingPermissions.isNotEmpty()) {
            requestPermissions(missingPermissions, ACCESS_LOCATION_REQUEST)
        } else {
            permissionsGranted()
        }
    }

    private fun getMissingPermissions(requiredPermissions: Array<String>): Array<String> {
        val missingPermissions: MutableList<String> = ArrayList()
        for (requiredPermission in requiredPermissions) {
            if (requireActivity().applicationContext.checkSelfPermission(requiredPermission) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(requiredPermission)
            }
        }
        return missingPermissions.toTypedArray()
    }

    private fun permissionsGranted() {
        // Check if Location services are on because they are required to make scanning work for SDK < 31
        val targetSdkVersion = requireActivity().applicationInfo.targetSdkVersion
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && targetSdkVersion < Build.VERSION_CODES.S) {
            checkLocationServices()
        }
    }

    private fun checkLocationServices(): Boolean {
        return if (!areLocationServicesEnabled()) {
            AlertDialog.Builder(requireContext())
                .setTitle("Location services are not enabled")
                .setMessage("Scanning for Bluetooth peripherals requires locations services to be enabled.") // Want to enable?
                .setPositiveButton("Enable") { dialogInterface, _ ->
                    dialogInterface.cancel()
                    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    // if this button is clicked, just close
                    // the dialog box and do nothing
                    dialog.cancel()
                }
                .create()
                .show()
            false
        } else {
            true
        }
    }

    private val bluetoothManager by lazy {
        requireActivity().applicationContext
            .getSystemService(AppCompatActivity.BLUETOOTH_SERVICE)
                as BluetoothManager
    }

    private val isBluetoothEnabled: Boolean
        get() {
            val bluetoothAdapter = bluetoothManager.adapter ?: return false
            return bluetoothAdapter.isEnabled
        }

    override fun onResume() {
        super.onResume()
        if (bluetoothManager.adapter != null) {
            if (!isBluetoothEnabled) {
                askToEnableBluetooth()
            } else {
                checkPermissions()
            }
        } else {
            Timber.d("This device has no Bluetooth hardware")
        }
    }

    private val enableBluetoothRequest =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == AppCompatActivity.RESULT_OK) {
                // Bluetooth has been enabled
                checkPermissions()
            } else {
                // Bluetooth has not been enabled, try again
                askToEnableBluetooth()
            }
        }

    private fun askToEnableBluetooth() {
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        enableBluetoothRequest.launch(enableBtIntent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // Check if all permission were granted
        var allGranted = true
        for (result in grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                allGranted = false
                break
            }
        }
        if (allGranted) {
            permissionsGranted()
        } else {
            AlertDialog.Builder(requireContext())
                .setTitle("Location permission is required for scanning Bluetooth peripherals")
                .setMessage("Please grant permissions")
                .setPositiveButton("Retry") { dialogInterface, _ ->
                    dialogInterface.cancel()
                    checkPermissions()
                }
                .create()
                .show()
        }
    }

    companion object {
        private const val REQUEST_ENABLE_BT = 1
        private const val ACCESS_LOCATION_REQUEST = 2
    }
}