package com.example.filearchiver

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.filearchiver.databinding.ActivityMainBinding
import com.google.android.material.color.DynamicColors
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date


/**
 * Created by bggRGjQaUbCoE on 2024/5/25
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DynamicColors.applyToActivityIfAvailable(this)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        checkPermission()
        initButton()

    }

    private fun initButton() {
        binding.run.setOnClickListener {
            try {
                val list = File(binding.editText.text.toString()).listFiles()
                if (!list.isNullOrEmpty()) {
                    list.forEachIndexed { index, file ->
                        if (!file.isDirectory) {
                            val fromFolder = binding.editText.text.toString()
                            val lastModifiedTime = getFileLastModifiedTime(file)
                            val targetFolder =
                                "${fromFolder}${if (fromFolder.substring(fromFolder.lastIndex) == "/") "" else "/"}$lastModifiedTime"
                            val targetFolderFile = File(targetFolder)
                            if (!targetFolderFile.exists()) {
                                targetFolderFile.mkdirs()
                            }
                            val targetFile = File(targetFolder, file.name)
                            file.renameTo(targetFile)
                        }

                        if (index == list.lastIndex) {
                            Toast.makeText(this, "DONE", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(this, "Folder is null or empty", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, e.message ?: "unknown issue", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun getFileLastModifiedTime(file: File): String {
        val time = file.lastModified()
        return SimpleDateFormat("yyyy-MM-dd").format(Date(time))
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