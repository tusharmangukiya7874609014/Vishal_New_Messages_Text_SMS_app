package com.texting.sms.messaging_app.activity

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import com.texting.sms.messaging_app.R
import com.texting.sms.messaging_app.database.Const
import com.texting.sms.messaging_app.databinding.ActivityImagePreviewBinding
import com.texting.sms.messaging_app.adapter.ImagePreviewAdapter
import java.io.File
import java.io.FileOutputStream

class ImagePreviewActivity : BaseActivity() {
    private lateinit var binding: ActivityImagePreviewBinding
    private lateinit var selectedImageUris: MutableList<Uri>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_image_preview)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        getImagesUris()
        initClickListener()
    }

    private fun getImagesUris() {
        selectedImageUris = mutableListOf()
        if (intent.hasExtra(Const.THREAD_ID) && intent.hasExtra(Const.SELECTED_IMAGE) && intent.hasExtra(
                Const.CONTACT_NAME
            )
        ) {
            val threadId = intent.getLongExtra(Const.THREAD_ID, 0)
            val selectedImage = intent.getStringExtra(Const.SELECTED_IMAGE)
            val contactName = intent.getStringExtra(Const.CONTACT_NAME)

            binding.txtUserName.text = contactName
            val mmsUri = "content://mms".toUri()
            val mmsProjection = arrayOf("_id", "date", "msg_box", "read")
            val mmsSelection = "thread_id = ?"
            val mmsArgs = arrayOf(threadId.toString())

            contentResolver.query(mmsUri, mmsProjection, mmsSelection, mmsArgs, "date ASC")
                ?.use { cursor ->
                    val idIndex = cursor.getColumnIndexOrThrow("_id")

                    while (cursor.moveToNext()) {
                        val mmsId = cursor.getLong(idIndex)

                        val parts = getMmsParts(this, mmsId)
                        for ((_, mediaUri) in parts) {
                            if (mediaUri != null) {
                                selectedImageUris.add(mediaUri)
                            }
                        }
                    }
                }

            val imagePreviewAdapter =
                ImagePreviewAdapter(selectedImageUris, this@ImagePreviewActivity)
            binding.viewPager.adapter = imagePreviewAdapter

            val startIndex = selectedImageUris.indexOfFirst { it.toString() == selectedImage }
            if (startIndex in selectedImageUris.indices) {
                binding.viewPager.setCurrentItem(startIndex, false)
            }
        }
    }

    private fun getMmsParts(context: Context, mmsId: Long): List<Pair<String, Uri?>> {
        val parts = mutableListOf<Pair<String, Uri?>>()
        val partUri = "content://mms/part".toUri()
        val selection = "mid=?"
        val selectionArgs = arrayOf(mmsId.toString())

        context.contentResolver.query(partUri, null, selection, selectionArgs, null)
            ?.use { cursor ->
                val idIndex = cursor.getColumnIndex("_id")
                val typeIndex = cursor.getColumnIndex("ct")
                val textIndex = cursor.getColumnIndex("text")

                while (cursor.moveToNext()) {
                    val partId = cursor.getString(idIndex)
                    val type = cursor.getString(typeIndex)
                    val text = cursor.getString(textIndex)

                    if (type == "text/plain") {
                        parts.add(Pair(text ?: "", null))
                    } else if (type.startsWith("image/")) {
                        val uri = "content://mms/part/$partId".toUri()
                        parts.add(Pair("[Image]", uri))
                    }
                }
            }

        return parts
    }

    private fun initClickListener() {
        binding.ivBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.ivShare.setOnClickListener {
            shareImage(this, selectedImageUris[binding.viewPager.currentItem])
        }

        binding.ivDownload.setOnClickListener {
            saveImageToGallery(this, selectedImageUris[binding.viewPager.currentItem])
        }
    }

    private fun shareImage(context: Context, imageUri: Uri) {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, imageUri)
            type = "image/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(
            Intent.createChooser(shareIntent, "Share Image via")
        )
    }

    private fun saveImageToGallery(
        context: Context,
        imageUri: Uri,
        fileName: String = "image_${System.currentTimeMillis()}.jpg"
    ) {
        val resolver = context.contentResolver

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }

            val imageCollection =
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val newImageUri = resolver.insert(imageCollection, contentValues)

            if (newImageUri != null) {
                try {
                    resolver.openOutputStream(newImageUri)?.use { outputStream ->
                        resolver.openInputStream(imageUri)?.use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }

                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(newImageUri, contentValues, null, null)
                    showToast(resources.getString(R.string.image_saved_to_gallery))
                } catch (_: Exception) {
                    showToast(resources.getString(R.string.failed_to_save_image))
                }
            }
        } else {
            val picturesDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val imageFile = File(picturesDir, fileName)

            try {
                context.contentResolver.openInputStream(imageUri)?.use { inputStream ->
                    FileOutputStream(imageFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }

                MediaScannerConnection.scanFile(
                    context, arrayOf(imageFile.absolutePath), arrayOf("image/jpeg"), null
                )
                showToast(resources.getString(R.string.image_saved_to_gallery))
            } catch (_: Exception) {
                showToast(resources.getString(R.string.failed_to_save_image))
            }
        }
    }
}