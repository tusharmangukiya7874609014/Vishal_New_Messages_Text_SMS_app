package com.texting.sms.messaging_app.activity

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.texting.sms.messaging_app.R
import com.texting.sms.messaging_app.adapter.BackupHistoryAdapter
import com.texting.sms.messaging_app.ads.BannerAdHelper
import com.texting.sms.messaging_app.ads.BannerType
import com.texting.sms.messaging_app.ads.NativeAdHelper
import com.texting.sms.messaging_app.database.Const
import com.texting.sms.messaging_app.database.SharedPreferencesHelper
import com.texting.sms.messaging_app.databinding.ActivityBackupAndRestoreBinding
import com.texting.sms.messaging_app.databinding.DialogBackupsHistoryBinding
import com.texting.sms.messaging_app.listener.OnBackupClickInterface
import com.texting.sms.messaging_app.model.SmsBackupInfo
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupAndRestoreActivity : BaseActivity(), OnBackupClickInterface {
    private lateinit var binding: ActivityBackupAndRestoreBinding
    private lateinit var backupHistoryAdapter: BackupHistoryAdapter
    private lateinit var backupFilesList: List<SmsBackupInfo>
    private lateinit var dialog: Dialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_backup_and_restore)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        dialog = Dialog(this)
        runAdsCampion()
        initView()
        initClickListener()
    }

    private fun runAdsCampion() {
        lifecycleScope.launch(Dispatchers.IO) {
            delay(300)

            val isAppAdsShowing = SharedPreferencesHelper.getBoolean(
                this@BackupAndRestoreActivity, Const.IS_ADS_ENABLED, false
            )

            if (!isAppAdsShowing) return@launch

            val isAppNativeAdsEnabled = SharedPreferencesHelper.getBoolean(
                this@BackupAndRestoreActivity, Const.IS_NATIVE_ENABLED, false
            )

            val isAppBannerAdsEnabled = SharedPreferencesHelper.getBoolean(
                this@BackupAndRestoreActivity, Const.IS_BANNER_ENABLED, false
            )

            val retrievedAdsJson = SharedPreferencesHelper.getJsonFromPreferences(
                this@BackupAndRestoreActivity, Const.MESSAGE_MANAGER_RESPONSE
            )

            val getAdsPageResponse = retrievedAdsJson.optJSONObject("activities")
                ?.optJSONObject("BackupAndRestoreActivity") ?: return@launch

            val isCurrentPageAdsEnabled = getAdsPageResponse.optBoolean("isAdsShowing")

            if (!isCurrentPageAdsEnabled) return@launch

            val isCurrentPageBannerAdsEnabled =
                getAdsPageResponse.optBoolean("isBannerAdsShowing") && isAppBannerAdsEnabled

            val isCurrentPageNativeAdsEnabled =
                getAdsPageResponse.optBoolean("isNativeAdsShowing") && isAppNativeAdsEnabled

            val nativeAdsType = getAdsPageResponse.optString("isNativeAdsType").ifEmpty {
                SharedPreferencesHelper.getString(
                    this@BackupAndRestoreActivity,
                    Const.IS_NATIVE_ADS_TYPE_DEFAULT,
                    Const.STRING_DEFAULT_VALUE
                )
            } ?: ""

            val currentPageNativeAdsId = getAdsPageResponse.optString("nativeAdsID").ifEmpty {
                SharedPreferencesHelper.getString(
                    this@BackupAndRestoreActivity, Const.NATIVE_ID, Const.STRING_DEFAULT_VALUE
                )
            } ?: ""

            val currentPageBannerAdsID = getAdsPageResponse.optString("bannerAdsID").ifEmpty {
                SharedPreferencesHelper.getString(
                    this@BackupAndRestoreActivity, Const.BANNER_ID, Const.STRING_DEFAULT_VALUE
                )
            } ?: ""

            val bannerAdsType = getAdsPageResponse.optString("bannerAdsType").ifEmpty {
                "adaptive"
            } ?: "adaptive"

            withContext(Dispatchers.Main) {
                if (isFinishing || isDestroyed) return@withContext

                if (isCurrentPageNativeAdsEnabled && !isCurrentPageBannerAdsEnabled) {
                    runNativeAds(
                        nativeAdsType = nativeAdsType, nativeAdsId = currentPageNativeAdsId
                    )
                } else if (isCurrentPageBannerAdsEnabled && !isCurrentPageNativeAdsEnabled) {
                    runBannerAds(
                        bannerAdsId = currentPageBannerAdsID, bannerAdsType = bannerAdsType
                    )
                }
            }
        }
    }

    private fun runNativeAds(
        nativeAdsType: String, nativeAdsId: String
    ) {
        binding.nativeAdContainer.visibility = View.VISIBLE
        NativeAdHelper.loadNativeAd(
            this, binding.nativeAdContainer, nativeAdsType, nativeAdsId = nativeAdsId
        )
    }

    private fun runBannerAds(
        bannerAdsId: String, bannerAdsType: String
    ) {
        val bannerType = when (bannerAdsType) {
            "medium_rectangle" -> {
                BannerType.MEDIUM_RECTANGLE
            }

            else -> {
                BannerType.ADAPTIVE
            }
        }

        when (bannerAdsType) {
            "medium_rectangle" -> {
                binding.bannerAdContainer.iAdaptiveShimmer.root.visibility = View.GONE
                binding.bannerAdContainer.iMediumRectangleShimmer.root.visibility = View.VISIBLE
            }

            else -> {
                binding.bannerAdContainer.iMediumRectangleShimmer.root.visibility = View.GONE
                binding.bannerAdContainer.iAdaptiveShimmer.root.visibility = View.VISIBLE
            }
        }
        binding.bannerAdContainer.root.visibility = View.VISIBLE
        BannerAdHelper.loadBannerAd(
            this,
            binding.bannerAdContainer.bannerAdView,
            binding.bannerAdContainer.shimmerViewContainer,
            bannerType = bannerType,
            view = binding.bannerAdContainer.view,
            bannerAdsId = bannerAdsId
        )
    }

    private fun lastBackup() {
        backupFilesList = getAllBackupFiles()
        if (backupFilesList.isNotEmpty()) {
            binding.txtLastBackup.text = backupFilesList[0].backupTime
        } else {
            binding.txtLastBackup.text = getString(R.string.never)
        }
    }

    private fun hasStoragePermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            true
        } else {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestStoragePermission(activity: Activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                Const.PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun createDownloadSubfolder(): Boolean {
        val baseDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val folder = File(baseDir, "sms/messages/Backups")

        return if (!folder.exists()) {
            val created = folder.mkdirs()
            created
        } else {
            false
        }
    }

    private fun initView() {
        if (hasStoragePermission(this)) {
            val folderCreatedNow = createDownloadSubfolder()
            if (folderCreatedNow) {
                showToast(getString(R.string.messages_backup_folder_created))
            }
            lastBackup()
        } else {
            requestStoragePermission(this)
        }
    }

    private fun initClickListener() {
        binding.ivBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.rvRestoreBackup.setOnClickListener {
            showRestoreBackupDialog()
        }

        binding.rvBackupNow.setOnClickListener {
            binding.progressBar.fadeIn()
            binding.rvBackingUpMessage.fadeIn()
            binding.txtProgressMessages.text = getString(R.string.backing_up_messages)
            binding.txtBackUpProcess.text = getString(R.string.in_progress)

            CoroutineScope(Dispatchers.IO).launch {
                val smsList = getAllSmsMessages(this@BackupAndRestoreActivity)

                withContext(Dispatchers.Main) {
                    val file = saveSmsBackupAsJsonObject(smsList)
                    if (file != null) {
                        lastBackup()
                        Handler(Looper.getMainLooper()).postDelayed({
                            binding.txtBackUpProcess.text = getString(R.string.finished)
                            binding.progressBar.fadeOut()
                        }, 1500)
                    } else {
                        showToast(getString(R.string.failed_to_backup_sms))
                    }
                }
            }
        }
    }

    private fun saveSmsBackupAsJsonObject(smsList: List<SmsMessage>): File? {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
            val timestamp = sdf.format(Date())

            val backup = SmsBackup(
                totalMessages = smsList.size, backupTime = timestamp, messages = smsList
            )

            val json = GsonBuilder().setPrettyPrinting().create().toJson(backup)

            val fileName = "Backups_${
                SimpleDateFormat(
                    "yyyyMMdd_HHmmss", Locale.ENGLISH
                ).format(Date())
            }.json"
            val backupDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "sms/messages/Backups"
            )
            if (!backupDir.exists()) backupDir.mkdirs()

            val file = File(backupDir, fileName)
            file.writeText(json)

            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    data class SmsMessage(
        val id: String,
        val address: String,
        val body: String,
        val date: Long,
        val type: Int,
        val read: Int,
        val isMms: Boolean = false,
        val imageUriString: String = ""
    ) {
        val imageUri: Uri?
            get() = if (imageUriString.isNotEmpty()) imageUriString.toUri() else null
    }

    data class SmsBackup(
        val totalMessages: Int, val backupTime: String, val messages: List<SmsMessage>
    )

    private fun getAllSmsMessages(context: Context): List<SmsMessage> {
        val messages = mutableListOf<SmsMessage>()

        // Fetch SMS
        val smsUri = "content://sms".toUri()
        val smsProjection = arrayOf("_id", "address", "body", "date", "type", "read")
        context.contentResolver.query(smsUri, smsProjection, null, null, "date DESC")
            ?.use { cursor ->
                while (cursor.moveToNext()) {
                    messages.add(
                        SmsMessage(
                            id = cursor.getString(0),
                            address = cursor.getString(1) ?: "",
                            body = cursor.getString(2) ?: "",
                            date = cursor.getLong(3),
                            type = cursor.getInt(4),
                            read = cursor.getInt(5),
                            isMms = false
                        )
                    )
                }
            }

        // Fetch MMS
        val mmsUri = "content://mms".toUri()
        val mmsCursor = context.contentResolver.query(mmsUri, null, null, null, "date DESC")
        mmsCursor?.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getString(cursor.getColumnIndexOrThrow("_id"))
                val date = cursor.getLong(cursor.getColumnIndexOrThrow("date")) * 1000
                val read = cursor.getInt(cursor.getColumnIndexOrThrow("read"))
                val type = cursor.getInt(cursor.getColumnIndexOrThrow("msg_box"))

                val addrCursor = context.contentResolver.query(
                    "content://mms/$id/addr".toUri(), null, "type=137", null, null
                )

                var address = ""
                addrCursor?.use {
                    if (it.moveToFirst()) {
                        address = it.getString(it.getColumnIndexOrThrow("address")) ?: ""
                    }
                }

                val partCursor = context.contentResolver.query(
                    "content://mms/part".toUri(), null, "mid=?", arrayOf(id), null
                )

                var body = ""
                var imageUri: String? = null

                partCursor?.use {
                    while (it.moveToNext()) {
                        val partId = it.getString(it.getColumnIndexOrThrow("_id"))
                        val ct = it.getString(it.getColumnIndexOrThrow("ct"))
                        when {
                            ct == "text/plain" -> {
                                val data = it.getString(it.getColumnIndexOrThrow("_data"))
                                body = if (data != null) {
                                    getMmsText(context, partId)
                                } else {
                                    it.getString(it.getColumnIndexOrThrow("text")) ?: ""
                                }
                            }

                            ct.startsWith("image/") -> {
                                imageUri = "content://mms/part/$partId"
                            }
                        }
                    }
                }

                if (address.isNotEmpty() && !address.equals("Unknown", ignoreCase = true)) {
                    messages.add(
                        SmsMessage(
                            id = id,
                            address = address,
                            body = body,
                            date = date,
                            type = type,
                            read = read,
                            isMms = true,
                            imageUriString = imageUri ?: ""
                        )
                    )
                }
            }
        }

        return messages.sortedByDescending { it.date }
    }

    private fun getMmsText(context: Context, id: String): String {
        val uri = "content://mms/part/$id".toUri()
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            inputStream?.bufferedReader()?.use { it.readText() } ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    private fun getAllBackupFiles(): List<SmsBackupInfo> {
        val result = mutableListOf<SmsBackupInfo>()
        val backupDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "sms/messages/Backups"
        )

        if (backupDir.exists() && backupDir.isDirectory) {
            val jsonFiles =
                backupDir.listFiles { file -> file.extension == "json" } ?: return emptyList()

            for (file in jsonFiles) {
                try {
                    val json = file.readText()
                    val backup = Gson().fromJson(json, SmsBackup::class.java)

                    // Format backupTime
                    val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
                    val outputFormat = SimpleDateFormat("M/d/yyyy hh:mm:ss a", Locale.ENGLISH)
                    val formattedTime = try {
                        outputFormat.format(inputFormat.parse(backup.backupTime)!!)
                    } catch (_: Exception) {
                        "Invalid Time"
                    }

                    val sizeInKb = file.length() / 1024.0
                    val fileSize = if (sizeInKb > 1024) {
                        String.format(Locale.ENGLISH, "%.2f MB", sizeInKb / 1024)
                    } else {
                        String.format(Locale.ENGLISH, "%.2f KB", sizeInKb)
                    }

                    result.add(
                        SmsBackupInfo(
                            fileName = file.name,
                            backupTime = formattedTime,
                            totalMessages = backup.totalMessages,
                            fileSize = fileSize,
                            filePath = file.absolutePath
                        )
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        return result.sortedByDescending { it.fileName }
    }

    private fun showRestoreBackupDialog() {
        dialog = Dialog(this)
        val dialogBackupAndRestoreBinding: DialogBackupsHistoryBinding =
            DialogBackupsHistoryBinding.inflate(LayoutInflater.from(this))
        dialog.setContentView(dialogBackupAndRestoreBinding.root)

        dialog.window?.let { window ->
            window.setBackgroundDrawableResource(R.color.transparent)
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            window.setDimAmount(0.6f)

            val metrics = resources.displayMetrics
            window.attributes = window.attributes.apply {
                width = (metrics.widthPixels * 0.9f).toInt()
                height = WindowManager.LayoutParams.WRAP_CONTENT
            }
        }

        val layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(this)
        dialogBackupAndRestoreBinding.rvBackupHistory.setLayoutManager(layoutManager)
        backupHistoryAdapter =
            BackupHistoryAdapter(backupFilesList, this, this@BackupAndRestoreActivity)
        dialogBackupAndRestoreBinding.rvBackupHistory.adapter = backupHistoryAdapter

        if (backupFilesList.isNotEmpty()) {
            dialogBackupAndRestoreBinding.txtNoBackups.visibility = View.GONE
            dialogBackupAndRestoreBinding.rvBackupHistory.fadeIn()
        } else {
            dialogBackupAndRestoreBinding.rvBackupHistory.visibility = View.GONE
            dialogBackupAndRestoreBinding.txtNoBackups.fadeIn()
        }

        dialogBackupAndRestoreBinding.ivCloseDialog.setOnClickListener {
            dialog.dismiss()
        }

        if (!isFinishing && !isDestroyed) dialog.show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == Const.PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            if (createDownloadSubfolder()) {
                showToast(getString(R.string.messages_backup_folder_created))
            }
        } else {
            showToast(getString(R.string.storage_permission_denied))
        }
    }

    override fun onSelectBackupClick(backupName: SmsBackupInfo) {
        dialog.dismiss()
        binding.progressBar.fadeIn()
        binding.rvBackingUpMessage.fadeIn()
        binding.txtProgressMessages.text = getString(R.string.restoring_messages)
        binding.txtBackUpProcess.text = getString(R.string.in_progress)

        CoroutineScope(Dispatchers.IO).launch {
            val file = File(backupName.filePath)
            val backup = readBackupFromFile(file)
            if (backup != null) {
                restoreSmsBackup(this@BackupAndRestoreActivity, backup)
            } else {
                showToast(getString(R.string.failed_to_read_backup))
            }

            withContext(Dispatchers.Main) {
                binding.txtBackUpProcess.text = getString(R.string.finished)
                binding.progressBar.fadeOut()
            }
        }
    }

    private fun readBackupFromFile(file: File): SmsBackup? {
        return try {
            val json = file.readText()
            Gson().fromJson(json, SmsBackup::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun restoreSmsBackup(context: Context, backup: SmsBackup) {
        val contentResolver = context.contentResolver
        val smsUri = "content://sms".toUri()

        backup.messages.forEach { message ->
            if (!message.isMms) {
                val selection = "address=? AND date=? AND body=?"
                val selectionArgs = arrayOf(message.address, message.date.toString(), message.body)

                val isDuplicate =
                    contentResolver.query(smsUri, null, selection, selectionArgs, null)
                        ?.use { it.moveToFirst() } == true

                if (!isDuplicate) {
                    val values = ContentValues().apply {
                        put("address", message.address)
                        put("body", message.body)
                        put("date", message.date)
                        put("type", message.type)
                        put("read", message.read)
                    }
                    contentResolver.insert(smsUri, values)
                }
            }
        }

        try {
            backup.messages.forEach { message ->
                if (message.isMms) {
                    val threadId = getThreadIdFromPhoneNumber(context, message.address)

                    val isIncoming = message.type == 1

                    val mmsValues = ContentValues().apply {
                        put("thread_id", threadId)
                        put("date", message.date / 1000)
                        put("read", message.read)
                        put("msg_box", message.type)
                        put("m_type", if (isIncoming) 132 else 128)
                        put("ct_t", "application/vnd.wap.multipart.related")
                        put("sub", "")
                    }

                    val mmsUri = context.contentResolver.insert("content://mms".toUri(), mmsValues)
                        ?: throw Exception("Failed to insert MMS")
                    val messageId = ContentUris.parseId(mmsUri)

                    // Add address
                    val addrValues = ContentValues().apply {
                        put("address", message.address)
                        put("type", if (isIncoming) 137 else 151)
                        put("charset", 106)
                    }

                    context.contentResolver.insert(
                        "content://mms/$messageId/addr".toUri(), addrValues
                    )

                    // Add empty text part
                    val textPartValues = ContentValues().apply {
                        put("mid", messageId)
                        put("ct", "text/plain")
                        put("text", message.body)
                        put("cid", "<text>")
                        put("cl", "text.txt")
                        put("seq", 0)
                    }

                    context.contentResolver.insert(
                        "content://mms/$messageId/part".toUri(), textPartValues
                    )

                    // Insert image part
                    val inputStream = message.imageUri?.let {
                        context.contentResolver.openInputStream(it)
                    } ?: throw Exception("Cannot open URI: ${message.imageUri}")

                    val bytes = inputStream.readBytes()
                    inputStream.close()

                    val mimeType = message.imageUri?.let { context.contentResolver.getType(it) }
                        ?: "image/jpeg"

                    val partValues = ContentValues().apply {
                        put("mid", messageId)
                        put("ct", mimeType)
                        put("cid", "<image1>")
                        put("cl", "image1.jpg")
                        put("name", "image1.jpg")
                        put("seq", 1)
                    }

                    val partUri = context.contentResolver.insert(
                        "content://mms/$messageId/part".toUri(), partValues
                    ) ?: throw Exception("Failed to insert MMS image part")

                    context.contentResolver.openOutputStream(partUri)?.use { outputStream ->
                        outputStream.write(bytes)
                        outputStream.flush()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MMSRestore", "Failed to restore MMS: ${e.message}", e)
        }
    }

    private fun getThreadIdFromPhoneNumber(context: Context, phoneNumber: String): Long {
        val uri = "content://mms-sms/threadID".toUri().buildUpon()
            .appendQueryParameter("recipient", phoneNumber).build()

        context.contentResolver.query(uri, arrayOf("_id"), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val threadId = cursor.getLong(cursor.getColumnIndexOrThrow("_id"))
                return threadId
            }
        }
        throw IllegalStateException("Unable to get thread_id for address: $phoneNumber")
    }

    override fun onResume() {
        if (!isDefaultSmsApp(this)) {
            startActivity(Intent(this, HomeActivity::class.java))
            finishAffinity()
        } else {
            lastBackup()
        }
        super.onResume()
    }
}