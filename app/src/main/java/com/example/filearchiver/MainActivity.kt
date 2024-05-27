package com.example.filearchiver

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Resources
import android.graphics.Color
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.Q
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.filearchiver.databinding.ActivityMainBinding
import com.google.android.material.color.DynamicColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator


/**
 * Created by bggRGjQaUbCoE on 2024/5/25
 */
class MainActivity : AppCompatActivity() {

    private val Number.dp get() = (toFloat() * Resources.getSystem().displayMetrics.density).toInt()

    private lateinit var binding: ActivityMainBinding
    private var dialog: AlertDialog? = null
    private lateinit var indicator: LinearProgressIndicator
    private lateinit var textView: TextView
    private lateinit var serviceIntent: Intent

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
        initBroadCast()

    }

    private fun initBroadCast() {
        val filter = IntentFilter()
        filter.addAction("android.intent.action.MOVING")
        when {
            SDK_INT >= 33 -> registerReceiver(mReceiver, filter, RECEIVER_NOT_EXPORTED)
            SDK_INT < 33 -> registerReceiver(mReceiver, filter)
        }
    }

    private var mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        @SuppressLint("SetTextI18n")
        override fun onReceive(context: Context, intent: Intent) {
            val progress = intent.getIntExtra("progress", 0)
            val max = intent.getIntExtra("max", 0)
            if (progress == max) {
                dialog?.dismiss()
                dialog = null
                stopService(serviceIntent)
            } else {
                if (dialog == null) {
                    showProgressDialog(max)
                } else {
                    indicator.progress = progress
                    textView.text = "$progress/$max"
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(mReceiver)
    }

    @SuppressLint("SetTextI18n")
    private fun showProgressDialog(count: Int) {
        val linearLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20.dp, 10.dp, 20.dp, 20.dp)
        }
        indicator = LinearProgressIndicator(this).apply {
            progress = 0
            max = count
        }
        textView = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.END
                setMargins(0, 10.dp, 0, 0)
            }
            text = "0/$count"
        }
        linearLayout.addView(indicator)
        linearLayout.addView(textView)
        dialog = MaterialAlertDialogBuilder(this).apply {
            setCancelable(false)
            setTitle("Progress")
            setView(linearLayout)
        }.create()
        dialog?.show()
    }

    @SuppressLint("SetTextI18n")
    private fun initButton() {
        binding.run.setOnClickListener {
            serviceIntent = Intent(this, MService::class.java)
            serviceIntent.putExtra("fromFolder", binding.editText.text.toString())
            startService(serviceIntent)
        }
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

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        if (SDK_INT >= Q) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }
    }

}