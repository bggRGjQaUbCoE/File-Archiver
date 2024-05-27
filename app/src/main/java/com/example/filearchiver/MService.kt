package com.example.filearchiver

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date


/**
 * Created by bggRGjQaUbCoE on 2024/5/28
 */
class MService : Service() {

    private var serviceJob = Job()
    private var serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    @SuppressLint("SetTextI18n")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.getStringExtra("fromFolder")?.let {
            serviceScope.launch {
                try {
                    val list = File(it).listFiles()
                    if (!list.isNullOrEmpty()) {
                        list.forEachIndexed { index, file ->
                            sendProgressBroadcast(index + 1, list.size)

                            if (!file.isDirectory) {
                                val lastModifiedTime = getFileLastModifiedTime(file)
                                val targetFolder =
                                    "$it${if (it.substring(it.lastIndex) == "/") "" else "/"}$lastModifiedTime"
                                val targetFolderFile = File(targetFolder)
                                if (!targetFolderFile.exists()) {
                                    targetFolderFile.mkdirs()
                                }
                                val targetFile = File(targetFolder, file.name)
                                file.renameTo(targetFile)
                            }

                            if (index == list.lastIndex) {
                                withContext(Dispatchers.Main) {
                                    makeToast("DONE")
                                }
                            }
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            makeToast("Folder is null or empty")
                        }
                    }
                } catch (e: Exception) {
                    sendProgressBroadcast(0, 0)
                    withContext(Dispatchers.Main) {
                        makeToast(e.message ?: "unknown issue")
                    }
                }
                stopSelf()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun sendProgressBroadcast(progress: Int, max: Int) {
        val intent = Intent()
        intent.setAction("android.intent.action.MOVING")
        intent.putExtra("progress", progress)
        intent.putExtra("max", max)
        sendBroadcast(intent)
    }

    private fun makeToast(text: String) {
        Toast.makeText(applicationContext, text, Toast.LENGTH_SHORT).show()
    }

    @SuppressLint("SimpleDateFormat")
    private fun getFileLastModifiedTime(file: File): String {
        val time = file.lastModified()
        return SimpleDateFormat("yyyy-MM-dd").format(Date(time))
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

}