package com.example.filearchiver.ui.main

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.filearchiver.databinding.ActivityMainBinding
import com.example.filearchiver.ui.base.BaseActivity


/**
 * Created by bggRGjQaUbCoE on 2024/5/25
 */
class MainActivity : BaseActivity<ActivityMainBinding>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkPermission()

    }

    private var storagePermissionRequest: ActivityResultLauncher<Intent> =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            if (SDK_INT >= 30) {
                if (!Environment.isExternalStorageManager()) {
                    checkPermission()
                }
            }
        }

    private fun checkPermission() {
        if (SDK_INT >= 30) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.addCategory("android.intent.category.DEFAULT")
                    intent.setData(
                        Uri.parse(String.format("package:%s", applicationContext.packageName))
                    )
                    storagePermissionRequest.launch(intent)
                } catch (e: Exception) {
                    try {
                        val intent = Intent()
                        intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                        storagePermissionRequest.launch(intent)
                    } catch (e: Exception) {
                        Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            }
        } else {
            requestPermission()
        }
    }

    private fun requestPermission(
        checkPermission: String = Manifest.permission.READ_EXTERNAL_STORAGE,
        requestPermission: Array<String> = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    ) {
        if (ContextCompat.checkSelfPermission(this, checkPermission) != 0) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, checkPermission)) {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                ActivityCompat.requestPermissions(this, requestPermission, 1)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (grantResults[0] != 0) {
                checkPermission()
            }
        }
    }

}