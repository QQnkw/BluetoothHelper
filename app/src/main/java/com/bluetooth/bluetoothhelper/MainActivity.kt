package com.bluetooth.bluetoothhelper

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.bluetooth.bluetoothhelper.databinding.MainActivityBinding
import com.bluetooth.bluetoothhelper.ui.scan.ScanDeviceFragment
import timber.log.Timber
import java.util.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
    }
}