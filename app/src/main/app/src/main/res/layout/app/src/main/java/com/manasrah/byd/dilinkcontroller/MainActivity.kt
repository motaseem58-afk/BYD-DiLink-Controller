package com.manasrah.byd.dilinkcontroller

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import rikka.shizuku.Shizuku

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var btnConnectShizuku: Button
    private lateinit var btnUnlockCar: Button
    private lateinit var btnDiLinkSettings: Button

    private val REQUEST_CODE_SHIZUKU = 1001

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        checkShizukuPermission()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        tvStatus.text = "Shizuku Binder Died"
        tvStatus.setTextColor(getColor(android.R.color.holo_red_dark))
        disableButtons()
    }

    private val permissionResultListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == REQUEST_CODE_SHIZUKU) {
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                updateConnectedState()
            } else {
                Toast.makeText(this, "تم رفض صلاحية Shizuku", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        btnConnectShizuku = findViewById(R.id.btnConnectShizuku)
        btnUnlockCar = findViewById(R.id.btnUnlockCar)
        btnDiLinkSettings = findViewById(R.id.btnDiLinkSettings)

        Shizuku.addBinderReceivedListener(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        Shizuku.addRequestPermissionResultListener(permissionResultListener)

        btnConnectShizuku.setOnClickListener {
            requestShizukuPermission()
        }

        btnUnlockCar.setOnClickListener {
            executeBydCommand("doorlock")
        }

        btnDiLinkSettings.setOnClickListener {
            executeBydCommand("dilink")
        }

        if (Shizuku.pingBinder()) {
            checkShizukuPermission()
        }
    }

    private fun requestShizukuPermission() {
        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            updateConnectedState()
        } else {
            Shizuku.requestPermission(REQUEST_CODE_SHIZUKU)
        }
    }

    private fun checkShizukuPermission() {
        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            updateConnectedState()
        }
    }

    private fun updateConnectedState() {
        tvStatus.text = "Shizuku Status: Connected & Active"
        tvStatus.setTextColor(getColor(android.R.color.holo_green_dark))
        btnUnlockCar.isEnabled = true
        btnDiLinkSettings.isEnabled = true
        Toast.makeText(this, "تم الاتصال بنجاح عبر Shizuku وصلاحيات النظام!", Toast.LENGTH_SHORT).show()
    }

    private fun disableButtons() {
        btnUnlockCar.isEnabled = false
        btnDiLinkSettings.isEnabled = false
    }

    private fun executeBydCommand(type: String) {
        try {
            val uid = Shizuku.getUid()
            if (uid == 0 || uid == 2000) {
                when (type) {
                    "doorlock" -> {
                        Toast.makeText(this, "جاري إرسال أمر فتح الأبواب...", Toast.LENGTH_SHORT).show()
                        execRemoteShell("service call android.hardware.bydauto.doorlock.BYDAutoDoorLockDevice 1")
                    }
                    "dilink" -> {
                        Toast.makeText(this, "جاري تعديل إعدادات DiLink...", Toast.LENGTH_SHORT).show()
                        execRemoteShell("am broadcast -a android.intent.action.BYD_DILINK_SETTING")
                    }
                }
            } else {
                Toast.makeText(this, "لا توجد صلاحيات كافية لتنفيذ الأمر عبر Shizuku", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "خطأ في تنفيذ الأمر: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun execRemoteShell(command: String) {
        try {
            val process = Shizuku.newProcess(arrayOf("sh", "-c", command), null, null)
            process.waitFor()
            Toast.makeText(this, "تم تنفيذ الأمر بنجاح!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeBinderReceivedListener(binderReceivedListener)
        Shizuku.removeBinderDeadListener(binderDeadListener)
        Shizuku.removeRequestPermissionResultListener(permissionResultListener)
    }
}
