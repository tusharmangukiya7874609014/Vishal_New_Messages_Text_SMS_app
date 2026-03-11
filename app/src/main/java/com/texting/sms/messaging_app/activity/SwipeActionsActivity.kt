package com.texting.sms.messaging_app.activity

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.texting.sms.messaging_app.R
import com.texting.sms.messaging_app.ads.NativeAdHelper
import com.texting.sms.messaging_app.database.Const
import com.texting.sms.messaging_app.database.SharedPreferencesHelper
import com.texting.sms.messaging_app.databinding.ActivitySwipeActionsBinding
import com.texting.sms.messaging_app.databinding.DialogSwipeActionsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SwipeActionsActivity : BaseActivity() {
    private lateinit var binding: ActivitySwipeActionsBinding
    private var leftSwipeAction: SwipeAction = SwipeAction.NONE
    private var rightSwipeAction: SwipeAction = SwipeAction.NONE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_swipe_actions)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        runAdsCampion()
        initView()
        initClickListener()
    }

    private fun runAdsCampion() {
        lifecycleScope.launch(Dispatchers.IO) {
            delay(300)

            val isAppAdsShowing = SharedPreferencesHelper.getBoolean(
                this@SwipeActionsActivity, Const.IS_ADS_ENABLED, false
            )

            if (!isAppAdsShowing) return@launch

            val isAppNativeAdsEnabled = SharedPreferencesHelper.getBoolean(
                this@SwipeActionsActivity, Const.IS_NATIVE_ENABLED, false
            )

            val retrievedAdsJson = SharedPreferencesHelper.getJsonFromPreferences(
                this@SwipeActionsActivity, Const.MESSAGE_MANAGER_RESPONSE
            )

            val getAdsPageResponse =
                retrievedAdsJson.optJSONObject("activities")?.optJSONObject("SwipeActionsActivity")
                    ?: return@launch

            val isCurrentPageAdsEnabled = getAdsPageResponse.optBoolean("isAdsShowing")

            if (!isCurrentPageAdsEnabled) return@launch

            val isCurrentPageNativeAdsEnabled =
                getAdsPageResponse.optBoolean("isNativeAdsShowing") && isAppNativeAdsEnabled

            val nativeAdsType = getAdsPageResponse.optString("isNativeAdsType").ifEmpty {
                SharedPreferencesHelper.getString(
                    this@SwipeActionsActivity,
                    Const.IS_NATIVE_ADS_TYPE_DEFAULT,
                    Const.STRING_DEFAULT_VALUE
                )
            } ?: ""

            val currentPageNativeAdsId = getAdsPageResponse.optString("nativeAdsID").ifEmpty {
                SharedPreferencesHelper.getString(
                    this@SwipeActionsActivity, Const.NATIVE_ID, Const.STRING_DEFAULT_VALUE
                )
            } ?: ""

            withContext(Dispatchers.Main) {
                if (isFinishing || isDestroyed) return@withContext

                if (isCurrentPageNativeAdsEnabled) {
                    runNativeAds(
                        nativeAdsType = nativeAdsType, nativeAdsId = currentPageNativeAdsId
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

    private fun initView() {
        rightSwipeAction = SwipeAction.fromStoredValue(
            SharedPreferencesHelper.getString(
                this,
                Const.RIGHT_SWIPE_ACTIONS,
                SwipeAction.DELETE.name
            )
        ) ?: SwipeAction.DELETE

        leftSwipeAction = SwipeAction.fromStoredValue(
            SharedPreferencesHelper.getString(
                this,
                Const.LEFT_SWIPE_ACTIONS,
                SwipeAction.ARCHIVED.name
            )
        ) ?: SwipeAction.ARCHIVED

        binding.txtLeftSideSelected.text = getString(leftSwipeAction.resId)
        binding.txtRightSideSelected.text = getString(rightSwipeAction.resId)

        applySwipeAction(
            action = leftSwipeAction,
            container = binding.rvSwipeLeftSideView,
            imageView = binding.ivLeftSelectedSwipe
        )

        applySwipeAction(
            action = rightSwipeAction,
            container = binding.rvSwipeRightSideView,
            imageView = binding.ivRightSelectedSwipe
        )
    }

    private fun applySwipeAction(
        action: SwipeAction,
        container: View,
        imageView: AppCompatImageView
    ) {
        when (action) {
            SwipeAction.NONE -> {
                container.visibility = View.GONE
            }

            SwipeAction.ARCHIVED -> {
                imageView.setImageResource(R.drawable.ic_archive_swipe)
                container.fadeIn()
            }

            SwipeAction.DELETE -> {
                imageView.setImageResource(R.drawable.ic_delete_swipe)
                container.fadeIn()
            }

            SwipeAction.CALL -> {
                imageView.setImageResource(R.drawable.ic_swipe_call)
                container.fadeIn()
            }

            SwipeAction.MARK_AS_READ -> {
                imageView.setImageResource(R.drawable.ic_read)
                container.fadeIn()
            }

            SwipeAction.MARK_AS_UNREAD -> {
                imageView.setImageResource(R.drawable.ic_unread_messages)
                container.fadeIn()
            }

            SwipeAction.ADD_TO_PRIVATE_CHAT -> {
                imageView.setImageResource(R.drawable.ic_add_private_chat)
                container.fadeIn()
            }
        }
    }

    private fun initClickListener() {
        binding.ivBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.rvRightSwipe.setOnClickListener {
            showChangeSwipeActions("Right")
        }

        binding.rvLeftSwipe.setOnClickListener {
            showChangeSwipeActions("Left")
        }
    }

    enum class SwipeAction(val resId: Int) {
        NONE(R.string.none),
        ARCHIVED(R.string.archive),
        DELETE(R.string.delete),
        CALL(R.string.call),
        MARK_AS_READ(R.string.mark_read),
        MARK_AS_UNREAD(R.string.mark_unread),
        ADD_TO_PRIVATE_CHAT(R.string.add_to_private_chat);

        companion object {
            fun fromStoredValue(value: String?): SwipeAction? =
                runCatching { value?.let { valueOf(it) } }.getOrDefault(NONE)
        }
    }

    private fun showChangeSwipeActions(swipeSide: String) {
        val dialog = Dialog(this)
        val bindingDialog = DialogSwipeActionsBinding.inflate(layoutInflater)
        dialog.setContentView(bindingDialog.root)

        dialog.window?.apply {
            setBackgroundDrawableResource(R.color.transparent)
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setDimAmount(0.6f)
            val metrics = resources.displayMetrics
            setLayout((metrics.widthPixels * 0.9f).toInt(), WindowManager.LayoutParams.WRAP_CONTENT)
        }

        val currentAction =
            if (swipeSide == "Left") leftSwipeAction else rightSwipeAction

        bindingDialog.txtTitle.text = getString(
            if (swipeSide == "Left") R.string.left_swipe else R.string.right_swipe
        )

        when (currentAction) {
            SwipeAction.NONE -> bindingDialog.rgSwipeAction.check(R.id.rbNone)
            SwipeAction.ARCHIVED -> bindingDialog.rgSwipeAction.check(R.id.rbArchive)
            SwipeAction.DELETE -> bindingDialog.rgSwipeAction.check(R.id.rbDelete)
            SwipeAction.CALL -> bindingDialog.rgSwipeAction.check(R.id.rbCall)
            SwipeAction.MARK_AS_READ -> bindingDialog.rgSwipeAction.check(R.id.rbMarkRead)
            SwipeAction.MARK_AS_UNREAD -> bindingDialog.rgSwipeAction.check(R.id.rbMarkUnread)
            SwipeAction.ADD_TO_PRIVATE_CHAT -> bindingDialog.rgSwipeAction.check(R.id.rbAddToPrivateChat)
        }

        bindingDialog.rgSwipeAction.setOnCheckedChangeListener { _, checkedId ->
            val selectedAction = when (checkedId) {
                R.id.rbNone -> SwipeAction.NONE
                R.id.rbArchive -> SwipeAction.ARCHIVED
                R.id.rbDelete -> SwipeAction.DELETE
                R.id.rbCall -> SwipeAction.CALL
                R.id.rbMarkRead -> SwipeAction.MARK_AS_READ
                R.id.rbMarkUnread -> SwipeAction.MARK_AS_UNREAD
                R.id.rbAddToPrivateChat -> SwipeAction.ADD_TO_PRIVATE_CHAT
                else -> SwipeAction.NONE
            }

            if (swipeSide == "Left") {
                leftSwipeAction = selectedAction
            } else {
                rightSwipeAction = selectedAction
            }
        }

        bindingDialog.btnCancel.setOnClickListener { dialog.dismiss() }

        bindingDialog.btnApply.setOnClickListener {
            val action = if (swipeSide == "Left") leftSwipeAction else rightSwipeAction
            val iconView =
                if (swipeSide == "Left") binding.ivLeftSelectedSwipe else binding.ivRightSelectedSwipe
            val container =
                if (swipeSide == "Left") binding.rvSwipeLeftSideView else binding.rvSwipeRightSideView
            val label =
                if (swipeSide == "Left") binding.txtLeftSideSelected else binding.txtRightSideSelected

            SharedPreferencesHelper.saveString(
                this,
                if (swipeSide == "Left") Const.LEFT_SWIPE_ACTIONS else Const.RIGHT_SWIPE_ACTIONS,
                action.name
            )

            label.text = getString(action.resId)

            when (action) {
                SwipeAction.NONE -> container.visibility = View.GONE
                SwipeAction.ARCHIVED -> iconView.setImageResource(R.drawable.ic_archive_swipe)
                SwipeAction.DELETE -> iconView.setImageResource(R.drawable.ic_delete_swipe)
                SwipeAction.CALL -> iconView.setImageResource(R.drawable.ic_swipe_call)
                SwipeAction.MARK_AS_READ -> iconView.setImageResource(R.drawable.ic_read)
                SwipeAction.MARK_AS_UNREAD -> iconView.setImageResource(R.drawable.ic_unread_messages)
                SwipeAction.ADD_TO_PRIVATE_CHAT -> iconView.setImageResource(R.drawable.ic_add_private_chat)
            }

            if (action != SwipeAction.NONE) container.fadeIn()
            dialog.dismiss()
        }

        if (!isFinishing && !isDestroyed) dialog.show()
    }
}