package com.chat.sms_text.messages.activity

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowInsetsController
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.chat.sms_text.messages.R
import com.chat.sms_text.messages.database.Const
import com.chat.sms_text.messages.database.SharedPreferencesHelper
import com.chat.sms_text.messages.databinding.ActivityPreviewWallpaperBinding
import com.chat.sms_text.messages.utils.getColorFromAttr
import jp.wasabeef.glide.transformations.BlurTransformation

class PreviewWallpaperActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPreviewWallpaperBinding
    private lateinit var imageWallpaper: Any

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_preview_wallpaper)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        initView()
        initClickListener()
    }

    private fun initView() {
        if (intent.hasExtra("imageUri")) {
            imageWallpaper = intent.getStringExtra("imageUri").toString()
            loadOriginalImage()
        }
    }

    private fun loadOriginalImage() {
        Glide.with(this).load(imageWallpaper).diskCacheStrategy(DiskCacheStrategy.ALL)
            .skipMemoryCache(false).into(binding.ivWallpaper)
    }

    private fun loadBlurredImage() {
        Glide.with(this).load(imageWallpaper)
            .apply(RequestOptions.bitmapTransform(BlurTransformation(10, 2)))
            .diskCacheStrategy(DiskCacheStrategy.ALL).skipMemoryCache(false)
            .into(binding.ivWallpaper)
    }

    private fun initClickListener() {
        binding.switchBlur.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                loadBlurredImage()
                binding.switchBlur.setBackColor(ColorStateList.valueOf(getColor(R.color.app_theme_color)))
            } else {
                loadOriginalImage()
                binding.switchBlur.setBackColor(ColorStateList.valueOf(getColorFromAttr(R.attr.switchColor)))
            }
        }

        binding.ivBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.btnSetWallpaper.setOnClickListener {
            SharedPreferencesHelper.saveBoolean(
                this,
                Const.IS_WALLPAPER_BLUR,
                binding.switchBlur.isChecked
            )
            SharedPreferencesHelper.saveString(
                this,
                Const.CHAT_WALLPAPER,
                imageWallpaper.toString()
            )
            SharedPreferencesHelper.saveString(this, Const.WALLPAPER_TYPE, "Gallary")

            Toast.makeText(this, getString(R.string.wallpaper_set_successfully), Toast.LENGTH_SHORT)
                .show()

            Handler(Looper.getMainLooper()).postDelayed({
                onBackPressedDispatcher.onBackPressed()
            }, 1000)
        }
    }

    override fun onResume() {
        enableImmersiveMode(this)
        super.onResume()
    }

    private fun enableImmersiveMode(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val controller = activity.window.insetsController
            if (controller != null) {
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            activity.window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN
        }
    }
}