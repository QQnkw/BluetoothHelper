package com.bluetooth.bluetoothhelper.ui.scan

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.NonNull
import androidx.recyclerview.widget.RecyclerView
import com.bluetooth.bluetoothhelper.databinding.ItemDeviceInfoBinding

class ScanDeviceAdapter(
    private val deviceList: MutableList<Triple<String, String, String>>,
    private val onClickItemListener: (String) -> Unit
) :
    RecyclerView.Adapter<ScanDeviceAdapter.ScanDeviceViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScanDeviceViewHolder {
        val itemBinding =
            ItemDeviceInfoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ScanDeviceViewHolder(itemBinding)
    }

    override fun onBindViewHolder(holder: ScanDeviceViewHolder, position: Int) {
        with(holder) {
            with(deviceList[position]) {
                itemBinding.viewTextName.text = first
                itemBinding.viewTextMac.text = second
                itemBinding.viewTextRssi.text = third
                itemBinding.root.setOnClickListener {
                    onClickItemListener.invoke(second)
                }
            }
        }
    }

    override fun getItemCount(): Int = deviceList.size

    inner class ScanDeviceViewHolder(val itemBinding: ItemDeviceInfoBinding) :
        RecyclerView.ViewHolder(itemBinding.root) {

    }
}