package com.bluetooth.bluetoothhelper.ui.connect

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.fragment.app.viewModels
import androidx.lifecycle.*
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.bluetooth.bluetoothhelper.*
import com.bluetooth.bluetoothhelper.databinding.ConnectDeviceFragmentBinding
import com.bluetooth.bluetoothhelper.databinding.ScanDeviceFragmentBinding
import com.bluetooth.bluetoothhelper.ui.scan.ScanDeviceFragment
import com.welie.blessed.ConnectionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.withContext
import timber.log.Timber

class ConnectDeviceFragment : Fragment() {
    private var _binding: ConnectDeviceFragmentBinding? = null
    private val binding
        get() = _binding!!
    private val viewModel: ConnectDeviceViewModel by viewModels()
    private lateinit var bluetoothHelper: BluetoothHelper

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ConnectDeviceFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bluetoothHelper.disconnectDevice()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initBluetooth()
        initObserver()
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            findNavController().popBackStack()
        }
    }

    private fun initView() {
        binding.viewActionBluetoothPair.setOnClickListener {
            val uuid = binding.viewInputUuidPair.text?.toString()
            if (uuid.isNullOrBlank()) {
                toast("uuid????????????")
            } else {
                bluetoothHelper.setBluetoothPair(uuid) {
                    viewLifecycleOwner.lifecycleScope.launchWhenResumed {
                        if (it.isEmpty()) {
                            toast("????????????")
                        } else {
                            toast("????????????")
                        }
                    }
                }
            }
        }
        binding.viewActionRead.setOnClickListener {
            val uuid = binding.viewInputUuidRead.text?.toString()
            if (uuid.isNullOrBlank()) {
                toast("uuid????????????")
            } else {
                bluetoothHelper.readDataFromDevice(uuid) {
                    binding.viewTextReadData.text = it.toJson()
                }
            }
        }

        binding.viewActionWrite.setOnClickListener {
            val uuid = binding.viewInputUuidWrite.text?.toString()
            val data = binding.viewInputDataWrite.text?.toString()
            if (uuid.isNullOrBlank() || data.isNullOrBlank()) {
                toast("uuid?????????????????????????????????")
            } else {
                bluetoothHelper.writeDataToDevice(uuid, data) {
                    viewLifecycleOwner.lifecycleScope.launchWhenResumed {
                        if (it.isEmpty()) {
                            toast("????????????")
                        } else {
                            toast("????????????")
                        }
                    }
                }
            }
        }
        binding.viewActionByteTransformHex.setOnClickListener {
            val byteArrayStr = binding.viewInputByteArray.text?.toString()
            if (byteArrayStr.isNullOrBlank()) {
                toast("????????????????????????")
            } else {
                val byteArray = byteArrayStr.jsonToBean(ByteArray::class.java)
                if (byteArray == null || byteArray.isEmpty()) {
                    toast("???????????????????????????")
                } else {
                    val hexString = byteArray.toHexString()
                    binding.viewTextByteToData.text = hexString
                }
            }
        }
        binding.viewActionByteTransformAscii.setOnClickListener {
            val byteArrayStr = binding.viewInputByteArray.text?.toString()
            if (byteArrayStr.isNullOrBlank()) {
                toast("????????????????????????")
            } else {
                val byteArray = byteArrayStr.jsonToBean(ByteArray::class.java)
                if (byteArray == null || byteArray.isEmpty()) {
                    toast("???????????????????????????")
                } else {
                    val hexString = byteArray.toHexString()
                    val str = hexString.hexToAscii()
                    binding.viewTextByteToData.text = str
                }
            }
        }
        binding.viewActionHexTransformNum.setOnClickListener {
            val hexStr = binding.viewInputHex.text?.toString()
            if (hexStr.isNullOrBlank()) {
                toast("??????????????????????????????")
            } else {
                val num = hexStr.hex2Decimal()
                binding.viewTextHexToData.text = "$num"
            }
        }
        binding.viewActionHexTransformAscii.setOnClickListener {
            val hexStr = binding.viewInputHex.text?.toString()
            if (hexStr.isNullOrBlank()) {
                toast("??????????????????????????????")
            } else {
                val str = hexStr.hexToAscii()
                binding.viewTextHexToData.text = str
            }
        }
        binding.viewActionAsciiTransformHex.setOnClickListener {
            val str = binding.viewInputString.text?.toString()
            if (str.isNullOrBlank()) {
                toast("?????????????????????")
            } else {
                val hex = str.asciiToHexString()
                binding.viewTextAsciiToHex.text = hex
            }
        }
        binding.viewActionMacAddressTransformChipId.setOnClickListener {
            val str = binding.viewInputString.text?.toString()
            if (str.isNullOrBlank()) {
                toast("?????????????????????")
            } else {
                val hex = str.macToChipID()
                binding.viewTextAsciiToHex.text = hex
            }
        }
    }

    private fun initBluetooth() {
        bluetoothHelper = BluetoothHelper.getInstance(requireContext())
        binding.viewTextDeviceAddress.text = bluetoothHelper.getDeviceAddress()
    }

    private fun initObserver() {
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            bluetoothHelper.connectStateFlow
                .flowWithLifecycle(viewLifecycleOwner.lifecycle, Lifecycle.State.STARTED)
                .collect {
                    Timber.d("ConnectionState-->3.${it.first.name}")
                    when (it.first) {
                        ConnectionState.DISCONNECTED -> {
                            findNavController().popBackStack()
                        }
                        else -> {
                        }
                    }
                }
        }
    }
}