package com.texting.sms.messaging_app.adapter

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.PorterDuff
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.telephony.PhoneNumberUtils
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.format.DateUtils
import android.text.method.LinkMovementMethod
import android.text.style.BackgroundColorSpan
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.util.Patterns
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.toColorInt
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.texting.sms.messaging_app.R
import com.texting.sms.messaging_app.activity.PersonalChatActivity
import com.texting.sms.messaging_app.database.Const
import com.texting.sms.messaging_app.database.SharedPreferencesHelper
import com.texting.sms.messaging_app.databinding.ItemChatBubbleEightBinding
import com.texting.sms.messaging_app.databinding.ItemChatBubbleFiveBinding
import com.texting.sms.messaging_app.databinding.ItemChatBubbleFourBinding
import com.texting.sms.messaging_app.databinding.ItemChatBubbleOneBinding
import com.texting.sms.messaging_app.databinding.ItemChatBubbleSevenBinding
import com.texting.sms.messaging_app.databinding.ItemChatBubbleSixBinding
import com.texting.sms.messaging_app.databinding.ItemChatBubbleThreeBinding
import com.texting.sms.messaging_app.databinding.ItemChatBubbleTwoBinding
import com.texting.sms.messaging_app.databinding.ItemMessageFeaturesBottomViewPopupBinding
import com.texting.sms.messaging_app.databinding.ItemMessageFeaturesTopViewPopupBinding
import com.texting.sms.messaging_app.databinding.ItemMessageTimeHeaderBinding
import com.texting.sms.messaging_app.listener.OnClickPreviewImageInterface
import com.texting.sms.messaging_app.listener.OnOpenFullChatInterface
import com.texting.sms.messaging_app.listener.OnSelectedMessageFeatureClick
import com.texting.sms.messaging_app.model.ChatModel
import com.texting.sms.messaging_app.utils.StarCategory
import com.texting.sms.messaging_app.utils.getColorFromAttr
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class PersonalChatAdapter(
    private var context: Context,
    private var threadId: Long,
    private var chatList: MutableList<ChatModel>,
    private var chatBoxColor: Int,
    private var isMeChatBoxColor: Boolean,
    private var selectedThreadIDList: ArrayList<String>,
    private val onClickPreviewImageInterface: OnClickPreviewImageInterface,
    private val onSelectedMessageFeatureClick: OnSelectedMessageFeatureClick,
    private val onOpenFullChatInterface: OnOpenFullChatInterface,
    private val isFromMiniPopup: Boolean = false
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var translateMessage = ""
    private var searchQuery = ""
    private var isLongClicked = false
    private var contactAddress: String? = null

    init {
        setHasStableIds(true)
    }

    companion object {
        private const val VIEW_TYPE_TIME_HEADER = 0
        private const val VIEW_TYPE_MESSAGE = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (chatList[position]) {
            is ChatModel.Header -> VIEW_TYPE_TIME_HEADER
            is ChatModel.MessageItem -> {
                val style =
                    SharedPreferencesHelper.getInt(context, Const.CHAT_BOX_STYLE_POSITION, 0)
                VIEW_TYPE_MESSAGE + style
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_TIME_HEADER -> {
                val binding =
                    ItemMessageTimeHeaderBinding.inflate(
                        LayoutInflater.from(parent.context), parent, false
                    )
                HeaderViewHolder(binding)
            }

            in VIEW_TYPE_MESSAGE..VIEW_TYPE_MESSAGE + 7 -> {
                val chatBoxStylePosition = viewType - VIEW_TYPE_MESSAGE
                val inflater = LayoutInflater.from(parent.context)
                when (chatBoxStylePosition) {
                    0 -> {
                        val binding =
                            ItemChatBubbleOneBinding.inflate(
                                inflater, parent, false
                            )
                        MessageViewOneHolder(binding)
                    }

                    1 -> {
                        val binding =
                            ItemChatBubbleTwoBinding.inflate(
                                inflater, parent, false
                            )
                        MessageViewTwoHolder(binding)
                    }

                    2 -> {
                        val binding =
                            ItemChatBubbleThreeBinding.inflate(
                                inflater, parent, false
                            )
                        MessageViewThreeHolder(binding)
                    }

                    3 -> {
                        val binding =
                            ItemChatBubbleFourBinding.inflate(
                                inflater, parent, false
                            )
                        MessageViewFourHolder(binding)
                    }

                    4 -> {
                        val binding =
                            ItemChatBubbleFiveBinding.inflate(
                                inflater, parent, false
                            )
                        MessageViewFiveHolder(binding)
                    }

                    5 -> {
                        val binding =
                            ItemChatBubbleSixBinding.inflate(
                                inflater, parent, false
                            )
                        MessageViewSixHolder(binding)
                    }

                    6 -> {
                        val binding =
                            ItemChatBubbleSevenBinding.inflate(
                                inflater, parent, false
                            )
                        MessageViewSevenHolder(binding)
                    }

                    7 -> {
                        val binding =
                            ItemChatBubbleEightBinding.inflate(
                                inflater, parent, false
                            )
                        MessageViewEightHolder(binding)
                    }

                    else -> {
                        val binding =
                            ItemChatBubbleOneBinding.inflate(
                                inflater, parent, false
                            )
                        MessageViewOneHolder(binding)
                    }
                }
            }

            else -> throw IllegalArgumentException("Unknown viewType")
        }
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemCount(): Int = chatList.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = chatList[position]) {
            is ChatModel.Header -> (holder as HeaderViewHolder).bind(item)
            is ChatModel.MessageItem -> {
                val chatBoxStylePosition =
                    SharedPreferencesHelper.getInt(context, Const.CHAT_BOX_STYLE_POSITION, 0)
                when (chatBoxStylePosition) {
                    0 -> {
                        (holder as MessageViewOneHolder).bind(item)
                    }

                    1 -> {
                        (holder as MessageViewTwoHolder).bind(item)
                    }

                    2 -> {
                        (holder as MessageViewThreeHolder).bind(item)
                    }

                    3 -> {
                        (holder as MessageViewFourHolder).bind(item)
                    }

                    4 -> {
                        (holder as MessageViewFiveHolder).bind(item)
                    }

                    5 -> {
                        (holder as MessageViewSixHolder).bind(item)
                    }

                    6 -> {
                        (holder as MessageViewSevenHolder).bind(item)
                    }

                    7 -> {
                        (holder as MessageViewEightHolder).bind(item)
                    }
                }
            }
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isNotEmpty()) {
            when (val item = chatList[position]) {
                is ChatModel.MessageItem -> {
                    val chatBoxStylePosition =
                        SharedPreferencesHelper.getInt(context, Const.CHAT_BOX_STYLE_POSITION, 0)
                    when (chatBoxStylePosition) {
                        0 -> {
                            if (holder is MessageViewOneHolder && payloads.contains("update_statement")) {
                                item.message = translateMessage
                                holder.updateTranslationState(item)
                            } else if (holder is MessageViewOneHolder && payloads.contains("update_chat_box_color")) {
                                holder.updateChatBoxColor()
                            } else if (holder is MessageViewOneHolder && payloads.contains("partialClear")) {
                                holder.clearSelectionView()
                            } else if (holder is MessageViewOneHolder && payloads.contains("partialUpdateProfile")) {
                                holder.updateProfileSection(item)
                            } else {
                                if (holder is MessageViewOneHolder && payloads.contains("highlight_of_view")) {
                                    holder.highlightOfSelectionView(item)
                                }
                            }
                        }

                        1 -> {
                            if (holder is MessageViewTwoHolder && payloads.contains("update_statement")) {
                                item.message = translateMessage
                                holder.updateTranslationState(item)
                            } else if (holder is MessageViewTwoHolder && payloads.contains("update_chat_box_color")) {
                                holder.updateChatBoxColor()
                            } else if (holder is MessageViewTwoHolder && payloads.contains("partialClear")) {
                                holder.clearSelectionView()
                            } else if (holder is MessageViewTwoHolder && payloads.contains("partialUpdateProfile")) {
                                holder.updateProfileSection(item)
                            } else {
                                if (holder is MessageViewTwoHolder && payloads.contains("highlight_of_view")) {
                                    holder.highlightOfSelectionView(item)
                                }
                            }
                        }

                        2 -> {
                            if (holder is MessageViewThreeHolder && payloads.contains("update_statement")) {
                                item.message = translateMessage
                                holder.updateTranslationState(item)
                            } else if (holder is MessageViewThreeHolder && payloads.contains("update_chat_box_color")) {
                                holder.updateChatBoxColor()
                            } else if (holder is MessageViewThreeHolder && payloads.contains("partialClear")) {
                                holder.clearSelectionView()
                            } else if (holder is MessageViewThreeHolder && payloads.contains("partialUpdateProfile")) {
                                holder.updateProfileSection(item)
                            } else {
                                if (holder is MessageViewThreeHolder && payloads.contains("highlight_of_view")) {
                                    holder.highlightOfSelectionView(item)
                                }
                            }
                        }

                        3 -> {
                            if (holder is MessageViewFourHolder && payloads.contains("update_statement")) {
                                item.message = translateMessage
                                holder.updateTranslationState(item)
                            } else if (holder is MessageViewFourHolder && payloads.contains("update_chat_box_color")) {
                                holder.updateChatBoxColor()
                            } else if (holder is MessageViewFourHolder && payloads.contains("partialClear")) {
                                holder.clearSelectionView()
                            } else if (holder is MessageViewFourHolder && payloads.contains("partialUpdateProfile")) {
                                holder.updateProfileSection(item)
                            } else {
                                if (holder is MessageViewFourHolder && payloads.contains("highlight_of_view")) {
                                    holder.highlightOfSelectionView(item)
                                }
                            }
                        }

                        4 -> {
                            if (holder is MessageViewFiveHolder && payloads.contains("update_statement")) {
                                item.message = translateMessage
                                holder.updateTranslationState(item)
                            } else if (holder is MessageViewFiveHolder && payloads.contains("update_chat_box_color")) {
                                holder.updateChatBoxColor()
                            } else if (holder is MessageViewFiveHolder && payloads.contains("partialClear")) {
                                holder.clearSelectionView()
                            } else if (holder is MessageViewFiveHolder && payloads.contains("partialUpdateProfile")) {
                                holder.updateProfileSection(item)
                            } else {
                                if (holder is MessageViewFiveHolder && payloads.contains("highlight_of_view")) {
                                    holder.highlightOfSelectionView(item)
                                }
                            }
                        }

                        5 -> {
                            if (holder is MessageViewSixHolder && payloads.contains("update_statement")) {
                                item.message = translateMessage
                                holder.updateTranslationState(item)
                            } else if (holder is MessageViewSixHolder && payloads.contains("update_chat_box_color")) {
                                holder.updateChatBoxColor()
                            } else if (holder is MessageViewSixHolder && payloads.contains("partialClear")) {
                                holder.clearSelectionView()
                            } else if (holder is MessageViewSixHolder && payloads.contains("partialUpdateProfile")) {
                                holder.updateProfileSection(item)
                            } else {
                                if (holder is MessageViewSixHolder && payloads.contains("highlight_of_view")) {
                                    holder.highlightOfSelectionView(item)
                                }
                            }
                        }

                        6 -> {
                            if (holder is MessageViewSevenHolder && payloads.contains("update_statement")) {
                                item.message = translateMessage
                                holder.updateTranslationState(item)
                            } else if (holder is MessageViewSevenHolder && payloads.contains("update_chat_box_color")) {
                                holder.updateChatBoxColor()
                            } else if (holder is MessageViewSevenHolder && payloads.contains("partialClear")) {
                                holder.clearSelectionView()
                            } else if (holder is MessageViewSevenHolder && payloads.contains("partialUpdateProfile")) {
                                holder.updateProfileSection(item)
                            } else {
                                if (holder is MessageViewSevenHolder && payloads.contains("highlight_of_view")) {
                                    holder.highlightOfSelectionView(item)
                                }
                            }
                        }

                        7 -> {
                            if (holder is MessageViewEightHolder && payloads.contains("update_statement")) {
                                item.message = translateMessage
                                holder.updateTranslationState(item)
                            } else if (holder is MessageViewEightHolder && payloads.contains("update_chat_box_color")) {
                                holder.updateChatBoxColor()
                            } else if (holder is MessageViewEightHolder && payloads.contains("partialClear")) {
                                holder.clearSelectionView()
                            } else if (holder is MessageViewEightHolder && payloads.contains("partialUpdateProfile")) {
                                holder.updateProfileSection(item)
                            } else {
                                if (holder is MessageViewEightHolder && payloads.contains("highlight_of_view")) {
                                    holder.highlightOfSelectionView(item)
                                }
                            }
                        }
                    }
                }

                is ChatModel.Header -> {

                }
            }
        } else {
            onBindViewHolder(holder, position)
        }
    }

    inner class HeaderViewHolder(private val binding: ItemMessageTimeHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ChatModel.Header) {
            binding.txtTime.text = formatTimestamp(item.title)
        }
    }

    inner class MessageViewOneHolder(private val binding: ItemChatBubbleOneBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ChatModel.MessageItem) {
            val isTranslateVisible = SharedPreferencesHelper.getBoolean(
                context,
                Const.IS_TRANSLATE_VISIBLE_OR_NOT,
                false
            )
            var isFromDetectOtp = ""
            var isToDetectOtp = ""

            if (isTranslateVisible) {
                binding.llTranslateMessage.visibility = View.VISIBLE
                binding.llToTranslateMessage.visibility = View.VISIBLE
            } else {
                binding.llTranslateMessage.visibility = View.GONE
                binding.llToTranslateMessage.visibility = View.GONE
            }

            if (item.isFromMe) {
                if (item.mediaUri != null) {
                    binding.rvToImages.visibility = View.VISIBLE
                    Glide.with(context)
                        .load(item.mediaUri)
                        .into(binding.ivToMessage)
                } else {
                    binding.rvToImages.visibility = View.GONE
                }

                binding.rvFromImages.visibility = View.GONE
                binding.cvMessageUserProfile.visibility = View.GONE

                if (item.message.isNotEmpty() && !item.message.startsWith("[Image]")) {
                    val detectedOtpFromMessage = extractOtpFromText(item.message)
                    isToDetectOtp = detectedOtpFromMessage.toString()
                    binding.txtToMessage.text =
                        highlightPatterns(
                            context = context,
                            item.message,
                            searchQuery,
                            true,
                            binding.txtToMessage
                        )
                    binding.txtToMessage.movementMethod = LinkMovementMethod.getInstance()
                    binding.rvFromMessage.visibility = View.GONE
                    if (!detectedOtpFromMessage.isNullOrEmpty()) {
                        binding.txtToCode.text =
                            context.getString(R.string.code_copy, detectedOtpFromMessage)
                        binding.rvToCopyCode.visibility = View.VISIBLE
                    } else {
                        binding.rvToCopyCode.visibility = View.GONE
                    }
                    binding.rvToMessage.visibility = View.VISIBLE
                    val messageTime = formatTimeFromTimestamp(item.timestamp)
                    binding.txtToMessageTime.text = messageTime
                } else {
                    binding.rvFromMessage.visibility = View.GONE
                    binding.rvToMessage.visibility = View.GONE
                }
            } else {
                if (item.mediaUri != null) {
                    binding.rvFromImages.visibility = View.VISIBLE
                    Glide.with(context)
                        .load(item.mediaUri)
                        .into(binding.ivFromMessage)
                } else {
                    binding.rvFromImages.visibility = View.GONE
                }

                binding.rvToImages.visibility = View.GONE
                binding.cvMessageUserProfile.visibility = View.VISIBLE

                if (item.message.isNotEmpty() && !item.message.startsWith("[Image]")) {
                    val detectedOtpFromMessage = extractOtpFromText(item.message)
                    isFromDetectOtp = detectedOtpFromMessage.toString()
                    binding.txtFromMessage.text =
                        highlightPatterns(
                            context = context,
                            item.message,
                            searchQuery,
                            anchorView = binding.txtFromMessage
                        )
                    binding.txtFromMessage.movementMethod = LinkMovementMethod.getInstance()
                    binding.rvToMessage.visibility = View.GONE
                    if (!detectedOtpFromMessage.isNullOrEmpty()) {
                        binding.txtFromCode.text =
                            context.getString(R.string.code_copy, detectedOtpFromMessage)
                        binding.rvFromCopyCode.visibility = View.VISIBLE
                    } else {
                        binding.rvFromCopyCode.visibility = View.GONE
                    }
                    binding.rvFromMessage.visibility = View.VISIBLE
                    val messageTime = formatTimeFromTimestamp(item.timestamp)
                    binding.txtFromMessageTime.text = messageTime

                    binding.userContactAddress = contactAddress
                } else {
                    binding.rvToMessage.visibility = View.GONE
                    binding.rvFromMessage.visibility = View.GONE
                    binding.cvMessageUserProfile.visibility = View.GONE
                }
            }

            if (isMeChatBoxColor) {
                binding.rvFromMessageColor.backgroundTintList = null
                binding.rvToMessageColor.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(context, chatBoxColor))
            } else {
                binding.rvToMessageColor.backgroundTintList = null
                binding.rvFromMessageColor.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(context, chatBoxColor))
            }

            val messageID = if (item.mediaUri != null) {
                val lastNumber = item.mediaUri.lastPathSegment ?: ""
                "$lastNumber (Images)"
            } else item.smsId.toString()

            if (item.isTimeVisible) {
                if (item.isFromMe) {
                    if (binding.rvToMessageColor.isVisible) binding.txtToMessageTime.visibility =
                        View.VISIBLE else binding.txtToMessageTime.visibility = View.GONE
                    binding.txtFromMessageTime.visibility = View.GONE
                } else {
                    if (binding.rvFromMessageColor.isVisible) binding.txtFromMessageTime.visibility =
                        View.VISIBLE else binding.txtFromMessageTime.visibility = View.GONE
                    binding.txtToMessageTime.visibility = View.GONE
                }
            } else {
                binding.txtToMessageTime.visibility = View.GONE
                binding.txtFromMessageTime.visibility = View.GONE
            }

            if (selectedThreadIDList.contains(messageID) && messageID.isNotEmpty()) {
                if (item.isFromMe) {
                    binding.rvSelectedToView.setBackgroundColor(context.getColorFromAttr(R.attr.itemSelectedColor))
                } else {
                    binding.rvSelectedFromView.setBackgroundColor(context.getColorFromAttr(R.attr.itemSelectedColor))
                }
            } else {
                if (messageID.isNotEmpty()) {
                    if (item.isFromMe) {
                        binding.rvSelectedToView.background = null
                    } else {
                        binding.rvSelectedFromView.background = null
                    }
                }
            }

            if (areValidMessageIds(messageID)) {
                if (isContainsLink(item.message)) {
                    if (SharedPreferencesHelper.isMessageStarred(
                            context,
                            messageID,
                            StarCategory.LINK,
                            threadId.toString()
                        )
                    ) {
                        if (item.isFromMe) {
                            binding.ivToStarMessage.visibility = View.VISIBLE
                            binding.ivFromStarMessage.visibility = View.GONE
                        } else {
                            binding.ivFromStarMessage.visibility = View.VISIBLE
                            binding.ivToStarMessage.visibility = View.GONE
                        }
                    } else {
                        binding.ivFromStarMessage.visibility = View.GONE
                        binding.ivToStarMessage.visibility = View.GONE
                    }
                } else {
                    if (SharedPreferencesHelper.isMessageStarred(
                            context,
                            messageID,
                            StarCategory.TEXT_ONLY,
                            threadId.toString()
                        )
                    ) {
                        if (item.isFromMe) {
                            binding.ivToStarMessage.visibility = View.VISIBLE
                            binding.ivFromStarMessage.visibility = View.GONE
                        } else {
                            binding.ivFromStarMessage.visibility = View.VISIBLE
                            binding.ivToStarMessage.visibility = View.GONE
                        }
                    } else {
                        binding.ivFromStarMessage.visibility = View.GONE
                        binding.ivToStarMessage.visibility = View.GONE
                    }
                }
            } else {
                if (SharedPreferencesHelper.isMessageStarred(
                        context,
                        messageID,
                        StarCategory.IMAGE,
                        threadId.toString()
                    )
                ) {
                    if (item.isFromMe) {
                        binding.ivToStarImages.visibility = View.VISIBLE
                        binding.ivFromStarImages.visibility = View.GONE
                    } else {
                        binding.ivFromStarImages.visibility = View.VISIBLE
                        binding.ivToStarImages.visibility = View.GONE
                    }
                } else {
                    binding.ivToStarImages.visibility = View.GONE
                    binding.ivFromStarImages.visibility = View.GONE
                }
            }

            binding.rvToImages.setOnLongClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnLongClickListener true
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                    true
                } else {
                    isLongClicked = true
                    updateSelectedView(item)
                    true
                }
            }

            binding.rvToImages.setOnClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                } else {
                    if (isLongClicked) {
                        updateSelectedView(item)
                    } else {
                        item.mediaUri?.let { uri ->
                            onClickPreviewImageInterface.onItemImagePreviewClick(
                                uri
                            )
                        }
                    }
                }
            }

            binding.rvFromImages.setOnLongClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnLongClickListener true
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                    true
                } else {
                    isLongClicked = true
                    updateSelectedView(item)
                    true
                }
            }

            binding.rvFromImages.setOnClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                } else {
                    if (isLongClicked) {
                        updateSelectedView(item)
                    } else {
                        item.mediaUri?.let { uri ->
                            onClickPreviewImageInterface.onItemImagePreviewClick(
                                uri
                            )
                        }
                    }
                }
            }

            binding.rvFromCopyCode.setOnLongClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnLongClickListener true
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                    true
                } else {
                    isLongClicked = true
                    updateSelectedView(item)
                    true
                }
            }

            binding.rvFromCopyCode.setOnClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                } else {
                    if (isLongClicked) {
                        updateSelectedView(item)
                    } else {
                        if (isFromDetectOtp.isNotEmpty()) {
                            copyToClipboard(context, isFromDetectOtp)
                        }
                    }
                }
            }

            binding.rvToCopyCode.setOnLongClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnLongClickListener true
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                    true
                } else {
                    isLongClicked = true
                    updateSelectedView(item)
                    true
                }
            }

            binding.rvToCopyCode.setOnClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                } else {
                    if (isLongClicked) {
                        updateSelectedView(item)
                    } else {
                        if (isToDetectOtp.isNotEmpty()) {
                            copyToClipboard(context, isToDetectOtp)
                        }
                    }
                }
            }

            binding.llTranslateMessage.setOnLongClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnLongClickListener true
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                    true
                } else {
                    isLongClicked = true
                    updateSelectedView(item)
                    true
                }
            }

            binding.llTranslateMessage.setOnClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                } else {
                    if (isLongClicked) {
                        updateSelectedView(item)
                    } else {
                        onClickPreviewImageInterface.onItemTranslateClick(
                            item,
                            position = absoluteAdapterPosition
                        )
                    }
                }
            }

            binding.llToTranslateMessage.setOnLongClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnLongClickListener true
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                    true
                } else {
                    isLongClicked = true
                    updateSelectedView(item)
                    true
                }
            }

            binding.llToTranslateMessage.setOnClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                } else {
                    if (isLongClicked) {
                        updateSelectedView(item)
                    } else {
                        onClickPreviewImageInterface.onItemTranslateClick(
                            item,
                            position = absoluteAdapterPosition
                        )
                    }
                }
            }

            binding.txtFromMessage.setOnLongClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnLongClickListener true
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                    true
                } else {
                    isLongClicked = true
                    updateSelectedView(item)
                    true
                }
            }

            binding.txtFromMessage.setOnClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                } else {
                    if (isLongClicked) {
                        updateSelectedView(item)
                    } else {
                        if (item.isTimeVisible) {
                            if (item.isFromMe) {
                                binding.txtToMessageTime.visibility = View.GONE
                            } else {
                                binding.txtFromMessageTime.visibility = View.GONE
                            }
                            item.isTimeVisible = false
                        } else {
                            if (item.isFromMe) {
                                binding.txtToMessageTime.visibility = View.VISIBLE
                            } else {
                                binding.txtFromMessageTime.visibility = View.VISIBLE
                            }
                            item.isTimeVisible = true
                        }
                    }
                }
            }

            binding.txtToMessage.setOnLongClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnLongClickListener true
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                    true
                } else {
                    isLongClicked = true
                    updateSelectedView(item)
                    true
                }
            }

            binding.txtToMessage.setOnClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                } else {
                    if (isLongClicked) {
                        updateSelectedView(item)
                    } else {
                        if (item.isTimeVisible) {
                            if (item.isFromMe) {
                                binding.txtToMessageTime.visibility = View.GONE
                            } else {
                                binding.txtFromMessageTime.visibility = View.GONE
                            }
                            item.isTimeVisible = false
                        } else {
                            if (item.isFromMe) {
                                binding.txtToMessageTime.visibility = View.VISIBLE
                            } else {
                                binding.txtFromMessageTime.visibility = View.VISIBLE
                            }
                            item.isTimeVisible = true
                        }
                    }
                }
            }

            itemView.setOnLongClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnLongClickListener true
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                    true
                } else {
                    isLongClicked = true
                    updateSelectedView(item)
                    true
                }
            }

            itemView.setOnClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                } else {
                    if (isLongClicked) {
                        updateSelectedView(item)
                    } else {
                        if (item.isTimeVisible) {
                            if (item.isFromMe) {
                                binding.txtToMessageTime.visibility = View.GONE
                            } else {
                                binding.txtFromMessageTime.visibility = View.GONE
                            }
                            item.isTimeVisible = false
                        } else {
                            if (item.isFromMe) {
                                binding.txtToMessageTime.visibility = View.VISIBLE
                            } else {
                                binding.txtFromMessageTime.visibility = View.VISIBLE
                            }
                            item.isTimeVisible = true
                        }
                    }
                }
            }
        }

        fun updateSelectedView(item: ChatModel.MessageItem) {
            val messageID = if (item.mediaUri != null) {
                val lastNumber = item.mediaUri.lastPathSegment ?: ""
                "$lastNumber (Images)"
            } else item.smsId.toString()

            if (isLongClicked && messageID.isNotEmpty()) {
                if (selectedThreadIDList.contains(messageID)) {
                    selectedThreadIDList.remove(messageID)
                    if (item.isFromMe) {
                        binding.rvSelectedToView.background = null
                    } else {
                        binding.rvSelectedFromView.background = null
                    }
                } else {
                    selectedThreadIDList.add(messageID)
                    if (item.isFromMe) {
                        binding.rvSelectedToView.setBackgroundColor(context.getColorFromAttr(R.attr.itemSelectedColor))
                    } else {
                        binding.rvSelectedFromView.setBackgroundColor(context.getColorFromAttr(R.attr.itemSelectedColor))
                    }
                }

                SharedPreferencesHelper.saveArrayList(
                    context,
                    Const.SELECTED_MESSAGE_IDS,
                    selectedThreadIDList
                )
                (context as PersonalChatActivity).updateSelectedCount(selectedThreadIDList.size)
            }
        }

        fun clearSelectionView() {
            binding.rvSelectedToView.background = null
            binding.rvSelectedFromView.background = null
        }

        fun highlightOfSelectionView(item: ChatModel.MessageItem) {
            val targetView = if (item.isFromMe) {
                binding.rvSelectedToView
            } else {
                binding.rvSelectedFromView
            }
            blinkView(targetView)
        }

        fun updateTranslationState(item: ChatModel.MessageItem) {
            if (item.isFromMe) {
                binding.txtToMessage.text =
                    highlightPatterns(
                        context,
                        item.message,
                        searchQuery,
                        true,
                        anchorView = binding.txtToMessage
                    )
            } else {
                binding.txtFromMessage.text = highlightPatterns(
                    context,
                    item.message,
                    searchQuery,
                    anchorView = binding.txtFromMessage
                )
            }
        }

        fun updateChatBoxColor() {
            if (isMeChatBoxColor) {
                binding.rvToMessageColor.backgroundTintList =
                    ContextCompat.getColorStateList(context, chatBoxColor)
                binding.rvFromMessageColor.backgroundTintList = null
            } else {
                binding.rvFromMessageColor.backgroundTintList =
                    ContextCompat.getColorStateList(context, chatBoxColor)
                binding.rvToMessageColor.backgroundTintList = null
            }
        }

        fun updateProfileSection(item: ChatModel.MessageItem) {
            if (!item.isFromMe) {
                if (item.message.isNotEmpty() && !item.message.startsWith("[Image]")) {
                    binding.userContactAddress = contactAddress
                } else {
                    binding.rvToMessage.visibility = View.GONE
                    binding.rvFromMessage.visibility = View.GONE
                    binding.cvMessageUserProfile.visibility = View.GONE
                }
            } else {
                binding.cvMessageUserProfile.visibility = View.GONE
            }
        }
    }

    inner class MessageViewTwoHolder(private val binding: ItemChatBubbleTwoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ChatModel.MessageItem) {
            val isTranslateVisible = SharedPreferencesHelper.getBoolean(
                context,
                Const.IS_TRANSLATE_VISIBLE_OR_NOT,
                false
            )
            var isFromDetectOtp = ""
            var isToDetectOtp = ""

            if (isTranslateVisible) {
                binding.llTranslateMessage.visibility = View.VISIBLE
                binding.llToTranslateMessage.visibility = View.VISIBLE
            } else {
                binding.llTranslateMessage.visibility = View.GONE
                binding.llToTranslateMessage.visibility = View.GONE
            }

            if (item.isFromMe) {
                if (item.mediaUri != null) {
                    binding.rvToImages.visibility = View.VISIBLE
                    Glide.with(context)
                        .load(item.mediaUri)
                        .into(binding.ivToMessage)
                } else {
                    binding.rvToImages.visibility = View.GONE
                }
                binding.rvFromImages.visibility = View.GONE
                binding.cvMessageUserProfile.visibility = View.GONE

                if (item.message.isNotEmpty() && !item.message.startsWith("[Image]")) {
                    val detectedOtpFromMessage = extractOtpFromText(item.message)
                    isToDetectOtp = detectedOtpFromMessage.toString()
                    binding.txtToMessage.text =
                        highlightPatterns(
                            context = context,
                            item.message,
                            searchQuery,
                            true,
                            anchorView = binding.txtToMessage
                        )
                    binding.txtToMessage.movementMethod = LinkMovementMethod.getInstance()
                    binding.rvFromMessage.visibility = View.GONE
                    if (!detectedOtpFromMessage.isNullOrEmpty()) {
                        binding.txtToCode.text =
                            context.getString(R.string.code_copy, detectedOtpFromMessage)
                        binding.rvToCopyCode.visibility = View.VISIBLE
                    } else {
                        binding.rvToCopyCode.visibility = View.GONE
                    }
                    binding.rvToMessage.visibility = View.VISIBLE
                    val messageTime = formatTimeFromTimestamp(item.timestamp)
                    binding.txtToMessageTime.text = messageTime
                } else {
                    binding.rvFromMessage.visibility = View.GONE
                    binding.rvToMessage.visibility = View.GONE
                }
            } else {
                if (item.mediaUri != null) {
                    binding.rvFromImages.visibility = View.VISIBLE
                    Glide.with(context)
                        .load(item.mediaUri)
                        .into(binding.ivFromMessage)
                } else {
                    binding.rvFromImages.visibility = View.GONE
                }

                binding.rvToImages.visibility = View.GONE
                binding.cvMessageUserProfile.visibility = View.VISIBLE

                if (item.message.isNotEmpty() && !item.message.startsWith("[Image]")) {
                    val detectedOtpFromMessage = extractOtpFromText(item.message)
                    isFromDetectOtp = detectedOtpFromMessage.toString()
                    binding.txtFromMessage.text =
                        highlightPatterns(
                            context = context,
                            item.message,
                            searchQuery,
                            anchorView = binding.txtFromMessage
                        )
                    binding.txtFromMessage.movementMethod = LinkMovementMethod.getInstance()
                    binding.rvToMessage.visibility = View.GONE
                    if (!detectedOtpFromMessage.isNullOrEmpty()) {
                        binding.txtFromCode.text =
                            context.getString(R.string.code_copy, detectedOtpFromMessage)
                        binding.rvFromCopyCode.visibility = View.VISIBLE
                    } else {
                        binding.rvFromCopyCode.visibility = View.GONE
                    }
                    binding.rvFromMessage.visibility = View.VISIBLE
                    val messageTime = formatTimeFromTimestamp(item.timestamp)
                    binding.txtFromMessageTime.text = messageTime

                    binding.userContactAddress = contactAddress
                } else {
                    binding.rvToMessage.visibility = View.GONE
                    binding.rvFromMessage.visibility = View.GONE
                    binding.cvMessageUserProfile.visibility = View.GONE
                }
            }

            if (isMeChatBoxColor) {
                binding.rvFromMessageColor.backgroundTintList = null
                binding.ivLeftCorner.setColorFilter(
                    context.getColorFromAttr(R.attr.chatBoxColor),
                    PorterDuff.Mode.SRC_IN
                )
                binding.rvToMessageColor.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(context, chatBoxColor))
                binding.ivRightCorner.setColorFilter(
                    ContextCompat.getColor(context, chatBoxColor),
                    PorterDuff.Mode.SRC_IN
                )
            } else {
                binding.rvToMessageColor.backgroundTintList = null
                binding.ivRightCorner.setColorFilter(
                    context.getColorFromAttr(R.attr.chatBoxColor),
                    PorterDuff.Mode.SRC_IN
                )
                binding.rvFromMessageColor.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(context, chatBoxColor))
                binding.ivLeftCorner.setColorFilter(
                    ContextCompat.getColor(context, chatBoxColor),
                    PorterDuff.Mode.SRC_IN
                )
            }

            val messageID = if (item.mediaUri != null) {
                val lastNumber = item.mediaUri.lastPathSegment ?: ""
                "$lastNumber (Images)"
            } else item.smsId.toString()

            if (item.isTimeVisible) {
                if (item.isFromMe) {
                    if (binding.rvToMessageColor.isVisible) binding.txtToMessageTime.visibility =
                        View.VISIBLE else binding.txtToMessageTime.visibility = View.GONE
                    binding.txtFromMessageTime.visibility = View.GONE
                } else {
                    if (binding.rvFromMessageColor.isVisible) binding.txtFromMessageTime.visibility =
                        View.VISIBLE else binding.txtFromMessageTime.visibility = View.GONE
                    binding.txtToMessageTime.visibility = View.GONE
                }
            } else {
                binding.txtToMessageTime.visibility = View.GONE
                binding.txtFromMessageTime.visibility = View.GONE
            }

            if (selectedThreadIDList.contains(messageID) && messageID.isNotEmpty()) {
                if (item.isFromMe) {
                    binding.rvSelectedToView.setBackgroundColor(context.getColorFromAttr(R.attr.itemSelectedColor))
                } else {
                    binding.rvSelectedFromView.setBackgroundColor(context.getColorFromAttr(R.attr.itemSelectedColor))
                }
            } else {
                if (messageID.isNotEmpty()) {
                    if (item.isFromMe) {
                        binding.rvSelectedToView.background = null
                    } else {
                        binding.rvSelectedFromView.background = null
                    }
                }
            }

            if (areValidMessageIds(messageID)) {
                if (isContainsLink(item.message)) {
                    if (SharedPreferencesHelper.isMessageStarred(
                            context,
                            messageID,
                            StarCategory.LINK,
                            threadId.toString()
                        )
                    ) {
                        if (item.isFromMe) {
                            binding.ivToStarMessage.visibility = View.VISIBLE
                            binding.ivFromStarMessage.visibility = View.GONE
                        } else {
                            binding.ivFromStarMessage.visibility = View.VISIBLE
                            binding.ivToStarMessage.visibility = View.GONE
                        }
                    } else {
                        binding.ivFromStarMessage.visibility = View.GONE
                        binding.ivToStarMessage.visibility = View.GONE
                    }
                } else {
                    if (SharedPreferencesHelper.isMessageStarred(
                            context,
                            messageID,
                            StarCategory.TEXT_ONLY,
                            threadId.toString()
                        )
                    ) {
                        if (item.isFromMe) {
                            binding.ivToStarMessage.visibility = View.VISIBLE
                            binding.ivFromStarMessage.visibility = View.GONE
                        } else {
                            binding.ivFromStarMessage.visibility = View.VISIBLE
                            binding.ivToStarMessage.visibility = View.GONE
                        }
                    } else {
                        binding.ivFromStarMessage.visibility = View.GONE
                        binding.ivToStarMessage.visibility = View.GONE
                    }
                }
            } else {
                if (SharedPreferencesHelper.isMessageStarred(
                        context,
                        messageID,
                        StarCategory.IMAGE,
                        threadId.toString()
                    )
                ) {
                    if (item.isFromMe) {
                        binding.ivToStarImages.visibility = View.VISIBLE
                        binding.ivFromStarImages.visibility = View.GONE
                    } else {
                        binding.ivFromStarImages.visibility = View.VISIBLE
                        binding.ivToStarImages.visibility = View.GONE
                    }
                } else {
                    binding.ivToStarImages.visibility = View.GONE
                    binding.ivFromStarImages.visibility = View.GONE
                }
            }

            binding.rvToImages.setOnLongClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnLongClickListener true
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                    true
                } else {
                    isLongClicked = true
                    updateSelectedView(item)
                    true
                }
            }

            binding.rvToImages.setOnClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                } else {
                    if (isLongClicked) {
                        updateSelectedView(item)
                    } else {
                        item.mediaUri?.let { uri ->
                            onClickPreviewImageInterface.onItemImagePreviewClick(
                                uri
                            )
                        }
                    }
                }
            }

            binding.rvFromImages.setOnLongClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnLongClickListener true
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                    true
                } else {
                    isLongClicked = true
                    updateSelectedView(item)
                    true
                }
            }

            binding.rvFromImages.setOnClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                } else {
                    if (isLongClicked) {
                        updateSelectedView(item)
                    } else {
                        item.mediaUri?.let { uri ->
                            onClickPreviewImageInterface.onItemImagePreviewClick(
                                uri
                            )
                        }
                    }
                }
            }

            binding.rvFromCopyCode.setOnLongClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnLongClickListener true
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                    true
                } else {
                    isLongClicked = true
                    updateSelectedView(item)
                    true
                }
            }

            binding.rvFromCopyCode.setOnClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                } else {
                    if (isLongClicked) {
                        updateSelectedView(item)
                    } else {
                        if (isFromDetectOtp.isNotEmpty()) {
                            copyToClipboard(context, isFromDetectOtp)
                        }
                    }
                }
            }

            binding.rvToCopyCode.setOnLongClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnLongClickListener true
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                    true
                } else {
                    isLongClicked = true
                    updateSelectedView(item)
                    true
                }
            }

            binding.rvToCopyCode.setOnClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                } else {
                    if (isLongClicked) {
                        updateSelectedView(item)
                    } else {
                        if (isToDetectOtp.isNotEmpty()) {
                            copyToClipboard(context, isToDetectOtp)
                        }
                    }
                }
            }

            binding.llTranslateMessage.setOnLongClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnLongClickListener true
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                    true
                } else {
                    isLongClicked = true
                    updateSelectedView(item)
                    true
                }
            }

            binding.llTranslateMessage.setOnClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                } else {
                    if (isLongClicked) {
                        updateSelectedView(item)
                    } else {
                        onClickPreviewImageInterface.onItemTranslateClick(
                            item,
                            position = absoluteAdapterPosition
                        )
                    }
                }
            }

            binding.llToTranslateMessage.setOnLongClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnLongClickListener true
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                    true
                } else {
                    isLongClicked = true
                    updateSelectedView(item)
                    true
                }
            }

            binding.llToTranslateMessage.setOnClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                } else {
                    if (isLongClicked) {
                        updateSelectedView(item)
                    } else {
                        onClickPreviewImageInterface.onItemTranslateClick(
                            item,
                            position = absoluteAdapterPosition
                        )
                    }
                }
            }

            binding.txtFromMessage.setOnLongClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnLongClickListener true
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                    true
                } else {
                    isLongClicked = true
                    updateSelectedView(item)
                    true
                }
            }

            binding.txtFromMessage.setOnClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                } else {
                    if (isLongClicked) {
                        updateSelectedView(item)
                    } else {
                        if (item.isTimeVisible) {
                            if (item.isFromMe) {
                                binding.txtToMessageTime.visibility = View.GONE
                            } else {
                                binding.txtFromMessageTime.visibility = View.GONE
                            }
                            item.isTimeVisible = false
                        } else {
                            if (item.isFromMe) {
                                binding.txtToMessageTime.visibility = View.VISIBLE
                            } else {
                                binding.txtFromMessageTime.visibility = View.VISIBLE
                            }
                            item.isTimeVisible = true
                        }
                    }
                }
            }

            binding.txtToMessage.setOnLongClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnLongClickListener true
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                    true
                } else {
                    isLongClicked = true
                    updateSelectedView(item)
                    true
                }
            }

            binding.txtToMessage.setOnClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                } else {
                    if (isLongClicked) {
                        updateSelectedView(item)
                    } else {
                        if (item.isTimeVisible) {
                            if (item.isFromMe) {
                                binding.txtToMessageTime.visibility = View.GONE
                            } else {
                                binding.txtFromMessageTime.visibility = View.GONE
                            }
                            item.isTimeVisible = false
                        } else {
                            if (item.isFromMe) {
                                binding.txtToMessageTime.visibility = View.VISIBLE
                            } else {
                                binding.txtFromMessageTime.visibility = View.VISIBLE
                            }
                            item.isTimeVisible = true
                        }
                    }
                }
            }

            itemView.setOnLongClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnLongClickListener true
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                    true
                } else {
                    isLongClicked = true
                    updateSelectedView(item)
                    true
                }
            }

            itemView.setOnClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                } else {
                    if (isLongClicked) {
                        updateSelectedView(item)
                    } else {
                        if (item.isTimeVisible) {
                            if (item.isFromMe) {
                                binding.txtToMessageTime.visibility = View.GONE
                            } else {
                                binding.txtFromMessageTime.visibility = View.GONE
                            }
                            item.isTimeVisible = false
                        } else {
                            if (item.isFromMe) {
                                binding.txtToMessageTime.visibility = View.VISIBLE
                            } else {
                                binding.txtFromMessageTime.visibility = View.VISIBLE
                            }
                            item.isTimeVisible = true
                        }
                    }
                }
            }
        }

        fun updateSelectedView(item: ChatModel.MessageItem) {
            val messageID = if (item.mediaUri != null) {
                val lastNumber = item.mediaUri.lastPathSegment ?: ""
                "$lastNumber (Images)"
            } else item.smsId.toString()

            if (isLongClicked && messageID.isNotEmpty()) {
                if (selectedThreadIDList.contains(messageID)) {
                    selectedThreadIDList.remove(messageID)
                    if (item.isFromMe) {
                        binding.rvSelectedToView.background = null
                    } else {
                        binding.rvSelectedFromView.background = null
                    }
                } else {
                    selectedThreadIDList.add(messageID)
                    if (item.isFromMe) {
                        binding.rvSelectedToView.setBackgroundColor(context.getColorFromAttr(R.attr.itemSelectedColor))
                    } else {
                        binding.rvSelectedFromView.setBackgroundColor(context.getColorFromAttr(R.attr.itemSelectedColor))
                    }
                }

                SharedPreferencesHelper.saveArrayList(
                    context,
                    Const.SELECTED_MESSAGE_IDS,
                    selectedThreadIDList
                )
                (context as PersonalChatActivity).updateSelectedCount(selectedThreadIDList.size)
            }
        }

        fun clearSelectionView() {
            binding.rvSelectedToView.background = null
            binding.rvSelectedFromView.background = null
        }

        fun highlightOfSelectionView(item: ChatModel.MessageItem) {
            val targetView = if (item.isFromMe) {
                binding.rvSelectedToView
            } else {
                binding.rvSelectedFromView
            }
            blinkView(targetView)
        }

        fun updateTranslationState(item: ChatModel.MessageItem) {
            if (item.isFromMe) {
                binding.txtToMessage.text =
                    highlightPatterns(
                        context,
                        item.message,
                        searchQuery,
                        true,
                        anchorView = binding.txtToMessage
                    )
            } else {
                binding.txtFromMessage.text = highlightPatterns(
                    context,
                    item.message,
                    searchQuery,
                    anchorView = binding.txtFromMessage
                )
            }
        }

        fun updateChatBoxColor() {
            if (isMeChatBoxColor) {
                binding.rvFromMessageColor.backgroundTintList = null
                binding.ivLeftCorner.setColorFilter(
                    context.getColorFromAttr(R.attr.chatBoxColor),
                    PorterDuff.Mode.SRC_IN
                )
                binding.rvToMessageColor.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(context, chatBoxColor))
                binding.ivRightCorner.setColorFilter(
                    ContextCompat.getColor(context, chatBoxColor),
                    PorterDuff.Mode.SRC_IN
                )
            } else {
                binding.rvToMessageColor.backgroundTintList = null
                binding.ivRightCorner.setColorFilter(
                    context.getColorFromAttr(R.attr.chatBoxColor),
                    PorterDuff.Mode.SRC_IN
                )
                binding.rvFromMessageColor.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(context, chatBoxColor))
                binding.ivLeftCorner.setColorFilter(
                    ContextCompat.getColor(context, chatBoxColor),
                    PorterDuff.Mode.SRC_IN
                )
            }
        }

        fun updateProfileSection(item: ChatModel.MessageItem) {
            if (!item.isFromMe) {
                if (item.message.isNotEmpty() && !item.message.startsWith("[Image]")) {
                    binding.userContactAddress = contactAddress
                } else {
                    binding.rvToMessage.visibility = View.GONE
                    binding.rvFromMessage.visibility = View.GONE
                    binding.cvMessageUserProfile.visibility = View.GONE
                }
            } else {
                binding.cvMessageUserProfile.visibility = View.GONE
            }
        }
    }

    inner class MessageViewThreeHolder(private val binding: ItemChatBubbleThreeBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ChatModel.MessageItem) {
            val isTranslateVisible = SharedPreferencesHelper.getBoolean(
                context,
                Const.IS_TRANSLATE_VISIBLE_OR_NOT,
                false
            )
            var isFromDetectOtp = ""
            var isToDetectOtp = ""

            if (isTranslateVisible) {
                binding.llTranslateMessage.visibility = View.VISIBLE
                binding.llToTranslateMessage.visibility = View.VISIBLE
            } else {
                binding.llTranslateMessage.visibility = View.GONE
                binding.llToTranslateMessage.visibility = View.GONE
            }

            if (item.isFromMe) {
                if (item.mediaUri != null) {
                    binding.rvToImages.visibility = View.VISIBLE
                    Glide.with(context)
                        .load(item.mediaUri)
                        .into(binding.ivToMessage)
                } else {
                    binding.rvToImages.visibility = View.GONE
                }
                binding.rvFromImages.visibility = View.GONE
                binding.cvMessageUserProfile.visibility = View.GONE

                if (item.message.isNotEmpty() && !item.message.startsWith("[Image]")) {
                    val detectedOtpFromMessage = extractOtpFromText(item.message)
                    isToDetectOtp = detectedOtpFromMessage.toString()
                    binding.txtToMessage.text =
                        highlightPatterns(
                            context = context,
                            item.message,
                            searchQuery,
                            true,
                            anchorView = binding.txtToMessage
                        )
                    binding.txtToMessage.movementMethod = LinkMovementMethod.getInstance()
                    binding.rvFromMessage.visibility = View.GONE
                    if (!detectedOtpFromMessage.isNullOrEmpty()) {
                        binding.txtToCode.text =
                            context.getString(R.string.code_copy, detectedOtpFromMessage)
                        binding.rvToCopyCode.visibility = View.VISIBLE
                    } else {
                        binding.rvToCopyCode.visibility = View.GONE
                    }
                    binding.rvToMessage.visibility = View.VISIBLE
                    val messageTime = formatTimeFromTimestamp(item.timestamp)
                    binding.txtToMessageTime.text = messageTime
                } else {
                    binding.rvFromMessage.visibility = View.GONE
                    binding.rvToMessage.visibility = View.GONE
                }
            } else {
                if (item.mediaUri != null) {
                    binding.rvFromImages.visibility = View.VISIBLE
                    Glide.with(context)
                        .load(item.mediaUri)
                        .into(binding.ivFromMessage)
                } else {
                    binding.rvFromImages.visibility = View.GONE
                }

                binding.rvToImages.visibility = View.GONE
                binding.cvMessageUserProfile.visibility = View.VISIBLE

                if (item.message.isNotEmpty() && !item.message.startsWith("[Image]")) {
                    val detectedOtpFromMessage = extractOtpFromText(item.message)
                    isFromDetectOtp = detectedOtpFromMessage.toString()
                    binding.txtFromMessage.text =
                        highlightPatterns(
                            context = context,
                            item.message,
                            searchQuery,
                            anchorView = binding.txtFromMessage
                        )
                    binding.txtFromMessage.movementMethod = LinkMovementMethod.getInstance()
                    binding.rvToMessage.visibility = View.GONE
                    if (!detectedOtpFromMessage.isNullOrEmpty()) {
                        binding.txtFromCode.text =
                            context.getString(R.string.code_copy, detectedOtpFromMessage)
                        binding.rvFromCopyCode.visibility = View.VISIBLE
                    } else {
                        binding.rvFromCopyCode.visibility = View.GONE
                    }
                    binding.rvFromMessage.visibility = View.VISIBLE
                    val messageTime = formatTimeFromTimestamp(item.timestamp)
                    binding.txtFromMessageTime.text = messageTime

                    binding.userContactAddress = contactAddress
                } else {
                    binding.rvToMessage.visibility = View.GONE
                    binding.rvFromMessage.visibility = View.GONE
                    binding.cvMessageUserProfile.visibility = View.GONE
                }
            }

            if (isMeChatBoxColor) {
                binding.rvToMessageColor.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(context, chatBoxColor))
                binding.rvFromMessageColor.backgroundTintList = null
            } else {
                binding.rvFromMessageColor.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(context, chatBoxColor))
                binding.rvToMessageColor.backgroundTintList = null
            }

            val messageID = if (item.mediaUri != null) {
                val lastNumber = item.mediaUri.lastPathSegment ?: ""
                "$lastNumber (Images)"
            } else item.smsId.toString()

            if (item.isTimeVisible) {
                if (item.isFromMe) {
                    if (binding.rvToMessageColor.isVisible) binding.txtToMessageTime.visibility =
                        View.VISIBLE else binding.txtToMessageTime.visibility = View.GONE
                    binding.txtFromMessageTime.visibility = View.GONE
                } else {
                    if (binding.rvFromMessageColor.isVisible) binding.txtFromMessageTime.visibility =
                        View.VISIBLE else binding.txtFromMessageTime.visibility = View.GONE
                    binding.txtToMessageTime.visibility = View.GONE
                }
            } else {
                binding.txtToMessageTime.visibility = View.GONE
                binding.txtFromMessageTime.visibility = View.GONE
            }

            if (selectedThreadIDList.contains(messageID) && messageID.isNotEmpty()) {
                if (item.isFromMe) {
                    binding.rvSelectedToView.setBackgroundColor(context.getColorFromAttr(R.attr.itemSelectedColor))
                } else {
                    binding.rvSelectedFromView.setBackgroundColor(context.getColorFromAttr(R.attr.itemSelectedColor))
                }
            } else {
                if (messageID.isNotEmpty()) {
                    if (item.isFromMe) {
                        binding.rvSelectedToView.background = null
                    } else {
                        binding.rvSelectedFromView.background = null
                    }
                }
            }

            if (areValidMessageIds(messageID)) {
                if (isContainsLink(item.message)) {
                    if (SharedPreferencesHelper.isMessageStarred(
                            context,
                            messageID,
                            StarCategory.LINK,
                            threadId.toString()
                        )
                    ) {
                        if (item.isFromMe) {
                            binding.ivToStarMessage.visibility = View.VISIBLE
                            binding.ivFromStarMessage.visibility = View.GONE
                        } else {
                            binding.ivFromStarMessage.visibility = View.VISIBLE
                            binding.ivToStarMessage.visibility = View.GONE
                        }
                    } else {
                        binding.ivFromStarMessage.visibility = View.GONE
                        binding.ivToStarMessage.visibility = View.GONE
                    }
                } else {
                    if (SharedPreferencesHelper.isMessageStarred(
                            context,
                            messageID,
                            StarCategory.TEXT_ONLY,
                            threadId.toString()
                        )
                    ) {
                        if (item.isFromMe) {
                            binding.ivToStarMessage.visibility = View.VISIBLE
                            binding.ivFromStarMessage.visibility = View.GONE
                        } else {
                            binding.ivFromStarMessage.visibility = View.VISIBLE
                            binding.ivToStarMessage.visibility = View.GONE
                        }
                    } else {
                        binding.ivFromStarMessage.visibility = View.GONE
                        binding.ivToStarMessage.visibility = View.GONE
                    }
                }
            } else {
                if (SharedPreferencesHelper.isMessageStarred(
                        context,
                        messageID,
                        StarCategory.IMAGE,
                        threadId.toString()
                    )
                ) {
                    if (item.isFromMe) {
                        binding.ivToStarImages.visibility = View.VISIBLE
                        binding.ivFromStarImages.visibility = View.GONE
                    } else {
                        binding.ivFromStarImages.visibility = View.VISIBLE
                        binding.ivToStarImages.visibility = View.GONE
                    }
                } else {
                    binding.ivToStarImages.visibility = View.GONE
                    binding.ivFromStarImages.visibility = View.GONE
                }
            }

            binding.rvToImages.setOnLongClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnLongClickListener true
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                    true
                } else {
                    isLongClicked = true
                    updateSelectedView(item)
                    true
                }
            }

            binding.rvToImages.setOnClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                } else {
                    if (isLongClicked) {
                        updateSelectedView(item)
                    } else {
                        item.mediaUri?.let { uri ->
                            onClickPreviewImageInterface.onItemImagePreviewClick(
                                uri
                            )
                        }
                    }
                }
            }

            binding.rvFromImages.setOnLongClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnLongClickListener true
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                    true
                } else {
                    isLongClicked = true
                    updateSelectedView(item)
                    true
                }
            }

            binding.rvFromImages.setOnClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                } else {
                    if (isLongClicked) {
                        updateSelectedView(item)
                    } else {
                        item.mediaUri?.let { uri ->
                            onClickPreviewImageInterface.onItemImagePreviewClick(
                                uri
                            )
                        }
                    }
                }
            }

            binding.rvFromCopyCode.setOnLongClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnLongClickListener true
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                    true
                } else {
                    isLongClicked = true
                    updateSelectedView(item)
                    true
                }
            }

            binding.rvFromCopyCode.setOnClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                } else {
                    if (isLongClicked) {
                        updateSelectedView(item)
                    } else {
                        if (isFromDetectOtp.isNotEmpty()) {
                            copyToClipboard(context, isFromDetectOtp)
                        }
                    }
                }
            }

            binding.rvToCopyCode.setOnLongClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnLongClickListener true
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                    true
                } else {
                    isLongClicked = true
                    updateSelectedView(item)
                    true
                }
            }

            binding.rvToCopyCode.setOnClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                } else {
                    if (isLongClicked) {
                        updateSelectedView(item)
                    } else {
                        if (isToDetectOtp.isNotEmpty()) {
                            copyToClipboard(context, isToDetectOtp)
                        }
                    }
                }
            }

            binding.llTranslateMessage.setOnLongClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnLongClickListener true
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                    true
                } else {
                    isLongClicked = true
                    updateSelectedView(item)
                    true
                }
            }

            binding.llTranslateMessage.setOnClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                } else {
                    if (isLongClicked) {
                        updateSelectedView(item)
                    } else {
                        onClickPreviewImageInterface.onItemTranslateClick(
                            item,
                            position = absoluteAdapterPosition
                        )
                    }
                }
            }

            binding.llToTranslateMessage.setOnLongClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnLongClickListener true
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                    true
                } else {
                    isLongClicked = true
                    updateSelectedView(item)
                    true
                }
            }

            binding.llToTranslateMessage.setOnClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                } else {
                    if (isLongClicked) {
                        updateSelectedView(item)
                    } else {
                        onClickPreviewImageInterface.onItemTranslateClick(
                            item,
                            position = absoluteAdapterPosition
                        )
                    }
                }
            }

            binding.txtFromMessage.setOnLongClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnLongClickListener true
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                    true
                } else {
                    isLongClicked = true
                    updateSelectedView(item)
                    true
                }
            }

            binding.txtFromMessage.setOnClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                } else {
                    if (isLongClicked) {
                        updateSelectedView(item)
                    } else {
                        if (item.isTimeVisible) {
                            if (item.isFromMe) {
                                binding.txtToMessageTime.visibility = View.GONE
                            } else {
                                binding.txtFromMessageTime.visibility = View.GONE
                            }
                            item.isTimeVisible = false
                        } else {
                            if (item.isFromMe) {
                                binding.txtToMessageTime.visibility = View.VISIBLE
                            } else {
                                binding.txtFromMessageTime.visibility = View.VISIBLE
                            }
                            item.isTimeVisible = true
                        }
                    }
                }
            }

            binding.txtToMessage.setOnLongClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnLongClickListener true
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                    true
                } else {
                    isLongClicked = true
                    updateSelectedView(item)
                    true
                }
            }

            binding.txtToMessage.setOnClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                } else {
                    if (isLongClicked) {
                        updateSelectedView(item)
                    } else {
                        if (item.isTimeVisible) {
                            if (item.isFromMe) {
                                binding.txtToMessageTime.visibility = View.GONE
                            } else {
                                binding.txtFromMessageTime.visibility = View.GONE
                            }
                            item.isTimeVisible = false
                        } else {
                            if (item.isFromMe) {
                                binding.txtToMessageTime.visibility = View.VISIBLE
                            } else {
                                binding.txtFromMessageTime.visibility = View.VISIBLE
                            }
                            item.isTimeVisible = true
                        }
                    }
                }
            }

            itemView.setOnLongClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnLongClickListener true
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                    true
                } else {
                    isLongClicked = true
                    updateSelectedView(item)
                    true
                }
            }

            itemView.setOnClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                } else {
                    if (isLongClicked) {
                        updateSelectedView(item)
                    } else {
                        if (item.isTimeVisible) {
                            if (item.isFromMe) {
                                binding.txtToMessageTime.visibility = View.GONE
                            } else {
                                binding.txtFromMessageTime.visibility = View.GONE
                            }
                            item.isTimeVisible = false
                        } else {
                            if (item.isFromMe) {
                                binding.txtToMessageTime.visibility = View.VISIBLE
                            } else {
                                binding.txtFromMessageTime.visibility = View.VISIBLE
                            }
                            item.isTimeVisible = true
                        }
                    }
                }
            }
        }

        fun updateSelectedView(item: ChatModel.MessageItem) {
            val messageID = if (item.mediaUri != null) {
                val lastNumber = item.mediaUri.lastPathSegment ?: ""
                "$lastNumber (Images)"
            } else item.smsId.toString()

            if (isLongClicked && messageID.isNotEmpty()) {
                if (selectedThreadIDList.contains(messageID)) {
                    selectedThreadIDList.remove(messageID)
                    if (item.isFromMe) {
                        binding.rvSelectedToView.background = null
                    } else {
                        binding.rvSelectedFromView.background = null
                    }
                } else {
                    selectedThreadIDList.add(messageID)
                    if (item.isFromMe) {
                        binding.rvSelectedToView.setBackgroundColor(context.getColorFromAttr(R.attr.itemSelectedColor))
                    } else {
                        binding.rvSelectedFromView.setBackgroundColor(context.getColorFromAttr(R.attr.itemSelectedColor))
                    }
                }

                SharedPreferencesHelper.saveArrayList(
                    context,
                    Const.SELECTED_MESSAGE_IDS,
                    selectedThreadIDList
                )
                (context as PersonalChatActivity).updateSelectedCount(selectedThreadIDList.size)
            }
        }

        fun clearSelectionView() {
            binding.rvSelectedToView.background = null
            binding.rvSelectedFromView.background = null
        }

        fun highlightOfSelectionView(item: ChatModel.MessageItem) {
            val targetView = if (item.isFromMe) {
                binding.rvSelectedToView
            } else {
                binding.rvSelectedFromView
            }
            blinkView(targetView)
        }

        fun updateTranslationState(item: ChatModel.MessageItem) {
            if (item.isFromMe) {
                binding.txtToMessage.text =
                    highlightPatterns(
                        context,
                        item.message,
                        searchQuery,
                        true,
                        anchorView = binding.txtToMessage
                    )
            } else {
                binding.txtFromMessage.text = highlightPatterns(
                    context,
                    item.message,
                    searchQuery,
                    anchorView = binding.txtFromMessage
                )
            }
        }

        fun updateChatBoxColor() {
            if (isMeChatBoxColor) {
                binding.rvToMessageColor.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(context, chatBoxColor))
                binding.rvFromMessageColor.backgroundTintList = null
            } else {
                binding.rvFromMessageColor.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(context, chatBoxColor))
                binding.rvToMessageColor.backgroundTintList = null
            }
        }

        fun updateProfileSection(item: ChatModel.MessageItem) {
            if (!item.isFromMe) {
                if (item.message.isNotEmpty() && !item.message.startsWith("[Image]")) {
                    binding.userContactAddress = contactAddress
                } else {
                    binding.rvToMessage.visibility = View.GONE
                    binding.rvFromMessage.visibility = View.GONE
                    binding.cvMessageUserProfile.visibility = View.GONE
                }
            } else {
                binding.cvMessageUserProfile.visibility = View.GONE
            }
        }
    }

    inner class MessageViewFourHolder(private val binding: ItemChatBubbleFourBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ChatModel.MessageItem) {
            val isTranslateVisible = SharedPreferencesHelper.getBoolean(
                context,
                Const.IS_TRANSLATE_VISIBLE_OR_NOT,
                false
            )
            var isFromDetectOtp = ""
            var isToDetectOtp = ""

            if (isTranslateVisible) {
                binding.llTranslateMessage.visibility = View.VISIBLE
                binding.llToTranslateMessage.visibility = View.VISIBLE
            } else {
                binding.llTranslateMessage.visibility = View.GONE
                binding.llToTranslateMessage.visibility = View.GONE
            }

            if (item.isFromMe) {
                if (item.mediaUri != null) {
                    binding.rvToImages.visibility = View.VISIBLE
                    Glide.with(context)
                        .load(item.mediaUri)
                        .into(binding.ivToMessage)
                } else {
                    binding.rvToImages.visibility = View.GONE
                }
                binding.rvFromImages.visibility = View.GONE
                binding.cvMessageUserProfile.visibility = View.GONE

                if (item.message.isNotEmpty() && !item.message.startsWith("[Image]")) {
                    val detectedOtpFromMessage = extractOtpFromText(item.message)
                    isToDetectOtp = detectedOtpFromMessage.toString()

                    binding.txtToMessage.text =
                        highlightPatterns(
                            context = context,
                            item.message,
                            searchQuery,
                            true,
                            anchorView = binding.txtToMessage
                        )
                    binding.txtToMessage.movementMethod = LinkMovementMethod.getInstance()
                    binding.rvFromMessage.visibility = View.GONE
                    if (!detectedOtpFromMessage.isNullOrEmpty()) {
                        binding.txtToCode.text =
                            context.getString(R.string.code_copy, detectedOtpFromMessage)
                        binding.rvToCopyCode.visibility = View.VISIBLE
                    } else {
                        binding.rvToCopyCode.visibility = View.GONE
                    }
                    binding.rvToMessage.visibility = View.VISIBLE
                    val messageTime = formatTimeFromTimestamp(item.timestamp)
                    binding.txtToMessageTime.text = messageTime
                } else {
                    binding.rvFromMessage.visibility = View.GONE
                    binding.rvToMessage.visibility = View.GONE
                }
            } else {
                if (item.mediaUri != null) {
                    binding.rvFromImages.visibility = View.VISIBLE
                    Glide.with(context)
                        .load(item.mediaUri)
                        .into(binding.ivFromMessage)
                } else {
                    binding.rvFromImages.visibility = View.GONE
                }

                binding.rvToImages.visibility = View.GONE
                binding.cvMessageUserProfile.visibility = View.VISIBLE

                if (item.message.isNotEmpty() && !item.message.startsWith("[Image]")) {
                    val detectedOtpFromMessage = extractOtpFromText(item.message)
                    isFromDetectOtp = detectedOtpFromMessage.toString()
                    binding.txtFromMessage.text =
                        highlightPatterns(
                            context = context,
                            item.message,
                            searchQuery,
                            anchorView = binding.txtFromMessage
                        )
                    binding.txtFromMessage.movementMethod = LinkMovementMethod.getInstance()
                    binding.rvToMessage.visibility = View.GONE
                    if (!detectedOtpFromMessage.isNullOrEmpty()) {
                        binding.txtFromCode.text =
                            context.getString(R.string.code_copy, detectedOtpFromMessage)
                        binding.rvFromCopyCode.visibility = View.VISIBLE
                    } else {
                        binding.rvFromCopyCode.visibility = View.GONE
                    }
                    binding.rvFromMessage.visibility = View.VISIBLE
                    val messageTime = formatTimeFromTimestamp(item.timestamp)
                    binding.txtFromMessageTime.text = messageTime
                    binding.userContactAddress = contactAddress
                } else {
                    binding.rvToMessage.visibility = View.GONE
                    binding.rvFromMessage.visibility = View.GONE
                    binding.cvMessageUserProfile.visibility = View.GONE
                }
            }

            if (isMeChatBoxColor) {
                binding.rvFromMessageColor.backgroundTintList = null
                binding.ivLeftCorner.setColorFilter(
                    context.getColorFromAttr(R.attr.chatBoxColor),
                    PorterDuff.Mode.SRC_IN
                )
                binding.rvToMessageColor.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(context, chatBoxColor))
                binding.ivRightCorner.setColorFilter(
                    ContextCompat.getColor(context, chatBoxColor),
                    PorterDuff.Mode.SRC_IN
                )
            } else {
                binding.rvToMessageColor.backgroundTintList = null
                binding.ivRightCorner.setColorFilter(
                    context.getColorFromAttr(R.attr.chatBoxColor),
                    PorterDuff.Mode.SRC_IN
                )
                binding.rvFromMessageColor.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(context, chatBoxColor))
                binding.ivLeftCorner.setColorFilter(
                    ContextCompat.getColor(context, chatBoxColor),
                    PorterDuff.Mode.SRC_IN
                )
            }

            val messageID = if (item.mediaUri != null) {
                val lastNumber = item.mediaUri.lastPathSegment ?: ""
                "$lastNumber (Images)"
            } else item.smsId.toString()

            if (item.isTimeVisible) {
                if (item.isFromMe) {
                    if (binding.rvToMessageColor.isVisible) binding.txtToMessageTime.visibility =
                        View.VISIBLE else binding.txtToMessageTime.visibility = View.GONE
                    binding.txtFromMessageTime.visibility = View.GONE
                } else {
                    if (binding.rvFromMessageColor.isVisible) binding.txtFromMessageTime.visibility =
                        View.VISIBLE else binding.txtFromMessageTime.visibility = View.GONE
                    binding.txtToMessageTime.visibility = View.GONE
                }
            } else {
                binding.txtToMessageTime.visibility = View.GONE
                binding.txtFromMessageTime.visibility = View.GONE
            }

            if (selectedThreadIDList.contains(messageID) && messageID.isNotEmpty()) {
                if (item.isFromMe) {
                    binding.rvSelectedToView.setBackgroundColor(context.getColorFromAttr(R.attr.itemSelectedColor))
                } else {
                    binding.rvSelectedFromView.setBackgroundColor(context.getColorFromAttr(R.attr.itemSelectedColor))
                }
            } else {
                if (messageID.isNotEmpty()) {
                    if (item.isFromMe) {
                        binding.rvSelectedToView.background = null
                    } else {
                        binding.rvSelectedFromView.background = null
                    }
                }
            }

            if (areValidMessageIds(messageID)) {
                if (isContainsLink(item.message)) {
                    if (SharedPreferencesHelper.isMessageStarred(
                            context,
                            messageID,
                            StarCategory.LINK,
                            threadId.toString()
                        )
                    ) {
                        if (item.isFromMe) {
                            binding.ivToStarMessage.visibility = View.VISIBLE
                            binding.ivFromStarMessage.visibility = View.GONE
                        } else {
                            binding.ivFromStarMessage.visibility = View.VISIBLE
                            binding.ivToStarMessage.visibility = View.GONE
                        }
                    } else {
                        binding.ivFromStarMessage.visibility = View.GONE
                        binding.ivToStarMessage.visibility = View.GONE
                    }
                } else {
                    if (SharedPreferencesHelper.isMessageStarred(
                            context,
                            messageID,
                            StarCategory.TEXT_ONLY,
                            threadId.toString()
                        )
                    ) {
                        if (item.isFromMe) {
                            binding.ivToStarMessage.visibility = View.VISIBLE
                            binding.ivFromStarMessage.visibility = View.GONE
                        } else {
                            binding.ivFromStarMessage.visibility = View.VISIBLE
                            binding.ivToStarMessage.visibility = View.GONE
                        }
                    } else {
                        binding.ivFromStarMessage.visibility = View.GONE
                        binding.ivToStarMessage.visibility = View.GONE
                    }
                }
            } else {
                if (SharedPreferencesHelper.isMessageStarred(
                        context,
                        messageID,
                        StarCategory.IMAGE,
                        threadId.toString()
                    )
                ) {
                    if (item.isFromMe) {
                        binding.ivToStarImages.visibility = View.VISIBLE
                        binding.ivFromStarImages.visibility = View.GONE
                    } else {
                        binding.ivFromStarImages.visibility = View.VISIBLE
                        binding.ivToStarImages.visibility = View.GONE
                    }
                } else {
                    binding.ivToStarImages.visibility = View.GONE
                    binding.ivFromStarImages.visibility = View.GONE
                }
            }

            binding.rvToImages.setOnLongClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnLongClickListener true
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                    true
                } else {
                    isLongClicked = true
                    updateSelectedView(item)
                    true
                }
            }

            binding.rvToImages.setOnClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                } else {
                    if (isLongClicked) {
                        updateSelectedView(item)
                    } else {
                        item.mediaUri?.let { uri ->
                            onClickPreviewImageInterface.onItemImagePreviewClick(
                                uri
                            )
                        }
                    }
                }
            }

            binding.rvFromImages.setOnLongClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnLongClickListener true
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                    true
                } else {
                    isLongClicked = true
                    updateSelectedView(item)
                    true
                }
            }

            binding.rvFromImages.setOnClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                } else {
                    if (isLongClicked) {
                        updateSelectedView(item)
                    } else {
                        item.mediaUri?.let { uri ->
                            onClickPreviewImageInterface.onItemImagePreviewClick(
                                uri
                            )
                        }
                    }
                }
            }

            binding.rvFromCopyCode.setOnLongClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnLongClickListener true
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                    true
                } else {
                    isLongClicked = true
                    updateSelectedView(item)
                    true
                }
            }

            binding.rvFromCopyCode.setOnClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                } else {
                    if (isLongClicked) {
                        updateSelectedView(item)
                    } else {
                        if (isFromDetectOtp.isNotEmpty()) {
                            copyToClipboard(context, isFromDetectOtp)
                        }
                    }
                }
            }

            binding.rvToCopyCode.setOnLongClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnLongClickListener true
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                    true
                } else {
                    isLongClicked = true
                    updateSelectedView(item)
                    true
                }
            }

            binding.rvToCopyCode.setOnClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                } else {
                    if (isLongClicked) {
                        updateSelectedView(item)
                    } else {
                        if (isToDetectOtp.isNotEmpty()) {
                            copyToClipboard(context, isToDetectOtp)
                        }
                    }
                }
            }

            binding.llTranslateMessage.setOnLongClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnLongClickListener true
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                    true
                } else {
                    isLongClicked = true
                    updateSelectedView(item)
                    true
                }
            }

            binding.llTranslateMessage.setOnClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                } else {
                    if (isLongClicked) {
                        updateSelectedView(item)
                    } else {
                        onClickPreviewImageInterface.onItemTranslateClick(
                            item,
                            position = absoluteAdapterPosition
                        )
                    }
                }
            }

            binding.llToTranslateMessage.setOnLongClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnLongClickListener true
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                    true
                } else {
                    isLongClicked = true
                    updateSelectedView(item)
                    true
                }
            }

            binding.llToTranslateMessage.setOnClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                } else {
                    if (isLongClicked) {
                        updateSelectedView(item)
                    } else {
                        onClickPreviewImageInterface.onItemTranslateClick(
                            item,
                            position = absoluteAdapterPosition
                        )
                    }
                }
            }

            binding.txtFromMessage.setOnLongClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnLongClickListener true
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                    true
                } else {
                    isLongClicked = true
                    updateSelectedView(item)
                    true
                }
            }

            binding.txtFromMessage.setOnClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                } else {
                    if (isLongClicked) {
                        updateSelectedView(item)
                    } else {
                        if (item.isTimeVisible) {
                            if (item.isFromMe) {
                                binding.txtToMessageTime.visibility = View.GONE
                            } else {
                                binding.txtFromMessageTime.visibility = View.GONE
                            }
                            item.isTimeVisible = false
                        } else {
                            if (item.isFromMe) {
                                binding.txtToMessageTime.visibility = View.VISIBLE
                            } else {
                                binding.txtFromMessageTime.visibility = View.VISIBLE
                            }
                            item.isTimeVisible = true
                        }
                    }
                }
            }

            binding.txtToMessage.setOnLongClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnLongClickListener true
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                    true
                } else {
                    isLongClicked = true
                    updateSelectedView(item)
                    true
                }
            }

            binding.txtToMessage.setOnClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                } else {
                    if (isLongClicked) {
                        updateSelectedView(item)
                    } else {
                        if (item.isTimeVisible) {
                            if (item.isFromMe) {
                                binding.txtToMessageTime.visibility = View.GONE
                            } else {
                                binding.txtFromMessageTime.visibility = View.GONE
                            }
                            item.isTimeVisible = false
                        } else {
                            if (item.isFromMe) {
                                binding.txtToMessageTime.visibility = View.VISIBLE
                            } else {
                                binding.txtFromMessageTime.visibility = View.VISIBLE
                            }
                            item.isTimeVisible = true
                        }
                    }
                }
            }

            itemView.setOnLongClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnLongClickListener true
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                    true
                } else {
                    isLongClicked = true
                    updateSelectedView(item)
                    true
                }
            }

            itemView.setOnClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                } else {
                    if (isLongClicked) {
                        updateSelectedView(item)
                    } else {
                        if (item.isTimeVisible) {
                            if (item.isFromMe) {
                                binding.txtToMessageTime.visibility = View.GONE
                            } else {
                                binding.txtFromMessageTime.visibility = View.GONE
                            }
                            item.isTimeVisible = false
                        } else {
                            if (item.isFromMe) {
                                binding.txtToMessageTime.visibility = View.VISIBLE
                            } else {
                                binding.txtFromMessageTime.visibility = View.VISIBLE
                            }
                            item.isTimeVisible = true
                        }
                    }
                }
            }
        }

        fun updateSelectedView(item: ChatModel.MessageItem) {
            val messageID = if (item.mediaUri != null) {
                val lastNumber = item.mediaUri.lastPathSegment ?: ""
                "$lastNumber (Images)"
            } else item.smsId.toString()

            if (isLongClicked && messageID.isNotEmpty()) {
                if (selectedThreadIDList.contains(messageID)) {
                    selectedThreadIDList.remove(messageID)
                    if (item.isFromMe) {
                        binding.rvSelectedToView.background = null
                    } else {
                        binding.rvSelectedFromView.background = null
                    }
                } else {
                    selectedThreadIDList.add(messageID)
                    if (item.isFromMe) {
                        binding.rvSelectedToView.setBackgroundColor(context.getColorFromAttr(R.attr.itemSelectedColor))
                    } else {
                        binding.rvSelectedFromView.setBackgroundColor(context.getColorFromAttr(R.attr.itemSelectedColor))
                    }
                }

                SharedPreferencesHelper.saveArrayList(
                    context,
                    Const.SELECTED_MESSAGE_IDS,
                    selectedThreadIDList
                )
                (context as PersonalChatActivity).updateSelectedCount(selectedThreadIDList.size)
            }
        }

        fun clearSelectionView() {
            binding.rvSelectedToView.background = null
            binding.rvSelectedFromView.background = null
        }

        fun highlightOfSelectionView(item: ChatModel.MessageItem) {
            val targetView = if (item.isFromMe) {
                binding.rvSelectedToView
            } else {
                binding.rvSelectedFromView
            }
            blinkView(targetView)
        }

        fun updateTranslationState(item: ChatModel.MessageItem) {
            if (item.isFromMe) {
                binding.txtToMessage.text =
                    highlightPatterns(
                        context,
                        item.message,
                        searchQuery,
                        true,
                        anchorView = binding.txtToMessage
                    )
            } else {
                binding.txtFromMessage.text = highlightPatterns(
                    context,
                    item.message,
                    searchQuery,
                    anchorView = binding.txtFromMessage
                )
            }
        }

        fun updateChatBoxColor() {
            if (isMeChatBoxColor) {
                binding.rvFromMessageColor.backgroundTintList = null
                binding.ivLeftCorner.setColorFilter(
                    context.getColorFromAttr(R.attr.chatBoxColor),
                    PorterDuff.Mode.SRC_IN
                )
                binding.rvToMessageColor.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(context, chatBoxColor))
                binding.ivRightCorner.setColorFilter(
                    ContextCompat.getColor(context, chatBoxColor),
                    PorterDuff.Mode.SRC_IN
                )
            } else {
                binding.rvToMessageColor.backgroundTintList = null
                binding.ivRightCorner.setColorFilter(
                    context.getColorFromAttr(R.attr.chatBoxColor),
                    PorterDuff.Mode.SRC_IN
                )
                binding.rvFromMessageColor.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(context, chatBoxColor))
                binding.ivLeftCorner.setColorFilter(
                    ContextCompat.getColor(context, chatBoxColor),
                    PorterDuff.Mode.SRC_IN
                )
            }
        }

        fun updateProfileSection(item: ChatModel.MessageItem) {
            if (!item.isFromMe) {
                if (item.message.isNotEmpty() && !item.message.startsWith("[Image]")) {
                    binding.userContactAddress = contactAddress
                } else {
                    binding.rvToMessage.visibility = View.GONE
                    binding.rvFromMessage.visibility = View.GONE
                    binding.cvMessageUserProfile.visibility = View.GONE
                }
            } else {
                binding.cvMessageUserProfile.visibility = View.GONE
            }
        }
    }

    inner class MessageViewFiveHolder(private val binding: ItemChatBubbleFiveBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ChatModel.MessageItem) {
            val isTranslateVisible = SharedPreferencesHelper.getBoolean(
                context,
                Const.IS_TRANSLATE_VISIBLE_OR_NOT,
                false
            )
            var isFromDetectOtp = ""
            var isToDetectOtp = ""

            if (isTranslateVisible) {
                binding.llTranslateMessage.visibility = View.VISIBLE
                binding.llToTranslateMessage.visibility = View.VISIBLE
            } else {
                binding.llTranslateMessage.visibility = View.GONE
                binding.llToTranslateMessage.visibility = View.GONE
            }

            if (item.isFromMe) {
                if (item.mediaUri != null) {
                    binding.rvToImages.visibility = View.VISIBLE
                    Glide.with(context)
                        .load(item.mediaUri)
                        .into(binding.ivToMessage)
                } else {
                    binding.rvToImages.visibility = View.GONE
                }
                binding.rvFromImages.visibility = View.GONE
                binding.cvMessageUserProfile.visibility = View.GONE

                if (item.message.isNotEmpty() && !item.message.startsWith("[Image]")) {
                    val detectedOtpFromMessage = extractOtpFromText(item.message)
                    isToDetectOtp = detectedOtpFromMessage.toString()
                    binding.txtToMessage.text =
                        highlightPatterns(
                            context = context,
                            item.message,
                            searchQuery,
                            true,
                            anchorView = binding.txtToMessage
                        )
                    binding.txtToMessage.movementMethod = LinkMovementMethod.getInstance()
                    binding.rvFromMessage.visibility = View.GONE
                    if (!detectedOtpFromMessage.isNullOrEmpty()) {
                        binding.txtToCode.text =
                            context.getString(R.string.code_copy, detectedOtpFromMessage)
                        binding.rvToCopyCode.visibility = View.VISIBLE
                    } else {
                        binding.rvToCopyCode.visibility = View.GONE
                    }
                    binding.rvToMessage.visibility = View.VISIBLE
                    val messageTime = formatTimeFromTimestamp(item.timestamp)
                    binding.txtToMessageTime.text = messageTime
                } else {
                    binding.rvFromMessage.visibility = View.GONE
                    binding.rvToMessage.visibility = View.GONE
                }
            } else {
                if (item.mediaUri != null) {
                    binding.rvFromImages.visibility = View.VISIBLE
                    Glide.with(context)
                        .load(item.mediaUri)
                        .into(binding.ivFromMessage)
                } else {
                    binding.rvFromImages.visibility = View.GONE
                }

                binding.rvToImages.visibility = View.GONE
                binding.cvMessageUserProfile.visibility = View.VISIBLE

                if (item.message.isNotEmpty() && !item.message.startsWith("[Image]")) {
                    val detectedOtpFromMessage = extractOtpFromText(item.message)
                    isFromDetectOtp = detectedOtpFromMessage.toString()
                    binding.txtFromMessage.text =
                        highlightPatterns(
                            context = context,
                            item.message,
                            searchQuery,
                            anchorView = binding.txtFromMessage
                        )
                    binding.txtFromMessage.movementMethod = LinkMovementMethod.getInstance()
                    binding.rvToMessage.visibility = View.GONE
                    if (!detectedOtpFromMessage.isNullOrEmpty()) {
                        binding.txtFromCode.text =
                            context.getString(R.string.code_copy, detectedOtpFromMessage)
                        binding.rvFromCopyCode.visibility = View.VISIBLE
                    } else {
                        binding.rvFromCopyCode.visibility = View.GONE
                    }
                    binding.rvFromMessage.visibility = View.VISIBLE
                    val messageTime = formatTimeFromTimestamp(item.timestamp)
                    binding.txtFromMessageTime.text = messageTime

                    binding.userContactAddress = contactAddress
                } else {
                    binding.rvToMessage.visibility = View.GONE
                    binding.rvFromMessage.visibility = View.GONE
                    binding.cvMessageUserProfile.visibility = View.GONE
                }
            }

            if (isMeChatBoxColor) {
                binding.rvFromMessageColor.backgroundTintList = null
                binding.ivLeftCorner.setColorFilter(
                    context.getColorFromAttr(R.attr.chatBoxColor),
                    PorterDuff.Mode.SRC_IN
                )
                binding.rvToMessageColor.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(context, chatBoxColor))
                binding.ivRightCorner.setColorFilter(
                    ContextCompat.getColor(context, chatBoxColor),
                    PorterDuff.Mode.SRC_IN
                )
            } else {
                binding.rvToMessageColor.backgroundTintList = null
                binding.ivRightCorner.setColorFilter(
                    context.getColorFromAttr(R.attr.chatBoxColor),
                    PorterDuff.Mode.SRC_IN
                )
                binding.rvFromMessageColor.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(context, chatBoxColor))
                binding.ivLeftCorner.setColorFilter(
                    ContextCompat.getColor(context, chatBoxColor),
                    PorterDuff.Mode.SRC_IN
                )
            }

            val messageID = if (item.mediaUri != null) {
                val lastNumber = item.mediaUri.lastPathSegment ?: ""
                "$lastNumber (Images)"
            } else item.smsId.toString()

            if (item.isTimeVisible) {
                if (item.isFromMe) {
                    if (binding.rvToMessageColor.isVisible) binding.txtToMessageTime.visibility =
                        View.VISIBLE else binding.txtToMessageTime.visibility = View.GONE
                    binding.txtFromMessageTime.visibility = View.GONE
                } else {
                    if (binding.rvFromMessageColor.isVisible) binding.txtFromMessageTime.visibility =
                        View.VISIBLE else binding.txtFromMessageTime.visibility = View.GONE
                    binding.txtToMessageTime.visibility = View.GONE
                }
            } else {
                binding.txtToMessageTime.visibility = View.GONE
                binding.txtFromMessageTime.visibility = View.GONE
            }

            if (selectedThreadIDList.contains(messageID) && messageID.isNotEmpty()) {
                if (item.isFromMe) {
                    binding.rvSelectedToView.setBackgroundColor(context.getColorFromAttr(R.attr.itemSelectedColor))
                } else {
                    binding.rvSelectedFromView.setBackgroundColor(context.getColorFromAttr(R.attr.itemSelectedColor))
                }
            } else {
                if (messageID.isNotEmpty()) {
                    if (item.isFromMe) {
                        binding.rvSelectedToView.background = null
                    } else {
                        binding.rvSelectedFromView.background = null
                    }
                }
            }

            if (areValidMessageIds(messageID)) {
                if (isContainsLink(item.message)) {
                    if (SharedPreferencesHelper.isMessageStarred(
                            context,
                            messageID,
                            StarCategory.LINK,
                            threadId.toString()
                        )
                    ) {
                        if (item.isFromMe) {
                            binding.ivToStarMessage.visibility = View.VISIBLE
                            binding.ivFromStarMessage.visibility = View.GONE
                        } else {
                            binding.ivFromStarMessage.visibility = View.VISIBLE
                            binding.ivToStarMessage.visibility = View.GONE
                        }
                    } else {
                        binding.ivFromStarMessage.visibility = View.GONE
                        binding.ivToStarMessage.visibility = View.GONE
                    }
                } else {
                    if (SharedPreferencesHelper.isMessageStarred(
                            context,
                            messageID,
                            StarCategory.TEXT_ONLY,
                            threadId.toString()
                        )
                    ) {
                        if (item.isFromMe) {
                            binding.ivToStarMessage.visibility = View.VISIBLE
                            binding.ivFromStarMessage.visibility = View.GONE
                        } else {
                            binding.ivFromStarMessage.visibility = View.VISIBLE
                            binding.ivToStarMessage.visibility = View.GONE
                        }
                    } else {
                        binding.ivFromStarMessage.visibility = View.GONE
                        binding.ivToStarMessage.visibility = View.GONE
                    }
                }
            } else {
                if (SharedPreferencesHelper.isMessageStarred(
                        context,
                        messageID,
                        StarCategory.IMAGE,
                        threadId.toString()
                    )
                ) {
                    if (item.isFromMe) {
                        binding.ivToStarImages.visibility = View.VISIBLE
                        binding.ivFromStarImages.visibility = View.GONE
                    } else {
                        binding.ivFromStarImages.visibility = View.VISIBLE
                        binding.ivToStarImages.visibility = View.GONE
                    }
                } else {
                    binding.ivToStarImages.visibility = View.GONE
                    binding.ivFromStarImages.visibility = View.GONE
                }
            }

            binding.rvToImages.setOnLongClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnLongClickListener true
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                    true
                } else {
                    isLongClicked = true
                    updateSelectedView(item)
                    true
                }
            }

            binding.rvToImages.setOnClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                } else {
                    if (isLongClicked) {
                        updateSelectedView(item)
                    } else {
                        item.mediaUri?.let { uri ->
                            onClickPreviewImageInterface.onItemImagePreviewClick(
                                uri
                            )
                        }
                    }
                }
            }

            binding.rvFromImages.setOnLongClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnLongClickListener true
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                    true
                } else {
                    isLongClicked = true
                    updateSelectedView(item)
                    true
                }
            }

            binding.rvFromImages.setOnClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                } else {
                    if (isLongClicked) {
                        updateSelectedView(item)
                    } else {
                        item.mediaUri?.let { uri ->
                            onClickPreviewImageInterface.onItemImagePreviewClick(
                                uri
                            )
                        }
                    }
                }
            }

            binding.rvFromCopyCode.setOnLongClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnLongClickListener true
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                    true
                } else {
                    isLongClicked = true
                    updateSelectedView(item)
                    true
                }
            }

            binding.rvFromCopyCode.setOnClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                } else {
                    if (isLongClicked) {
                        updateSelectedView(item)
                    } else {
                        if (isFromDetectOtp.isNotEmpty()) {
                            copyToClipboard(context, isFromDetectOtp)
                        }
                    }
                }
            }

            binding.rvToCopyCode.setOnLongClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnLongClickListener true
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                    true
                } else {
                    isLongClicked = true
                    updateSelectedView(item)
                    true
                }
            }

            binding.rvToCopyCode.setOnClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                } else {
                    if (isLongClicked) {
                        updateSelectedView(item)
                    } else {
                        if (isToDetectOtp.isNotEmpty()) {
                            copyToClipboard(context, isToDetectOtp)
                        }
                    }
                }
            }

            binding.llTranslateMessage.setOnLongClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnLongClickListener true
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                    true
                } else {
                    isLongClicked = true
                    updateSelectedView(item)
                    true
                }
            }

            binding.llTranslateMessage.setOnClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                } else {
                    if (isLongClicked) {
                        updateSelectedView(item)
                    } else {
                        onClickPreviewImageInterface.onItemTranslateClick(
                            item,
                            position = absoluteAdapterPosition
                        )
                    }
                }
            }

            binding.llToTranslateMessage.setOnLongClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnLongClickListener true
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                    true
                } else {
                    isLongClicked = true
                    updateSelectedView(item)
                    true
                }
            }

            binding.llToTranslateMessage.setOnClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                } else {
                    if (isLongClicked) {
                        updateSelectedView(item)
                    } else {
                        onClickPreviewImageInterface.onItemTranslateClick(
                            item,
                            position = absoluteAdapterPosition
                        )
                    }
                }
            }

            binding.txtFromMessage.setOnLongClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnLongClickListener true
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                    true
                } else {
                    isLongClicked = true
                    updateSelectedView(item)
                    true
                }
            }

            binding.txtFromMessage.setOnClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                } else {
                    if (isLongClicked) {
                        updateSelectedView(item)
                    } else {
                        if (item.isTimeVisible) {
                            if (item.isFromMe) {
                                binding.txtToMessageTime.visibility = View.GONE
                            } else {
                                binding.txtFromMessageTime.visibility = View.GONE
                            }
                            item.isTimeVisible = false
                        } else {
                            if (item.isFromMe) {
                                binding.txtToMessageTime.visibility = View.VISIBLE
                            } else {
                                binding.txtFromMessageTime.visibility = View.VISIBLE
                            }
                            item.isTimeVisible = true
                        }
                    }
                }
            }

            binding.txtToMessage.setOnLongClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnLongClickListener true
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                    true
                } else {
                    isLongClicked = true
                    updateSelectedView(item)
                    true
                }
            }

            binding.txtToMessage.setOnClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                } else {
                    if (isLongClicked) {
                        updateSelectedView(item)
                    } else {
                        if (item.isTimeVisible) {
                            if (item.isFromMe) {
                                binding.txtToMessageTime.visibility = View.GONE
                            } else {
                                binding.txtFromMessageTime.visibility = View.GONE
                            }
                            item.isTimeVisible = false
                        } else {
                            if (item.isFromMe) {
                                binding.txtToMessageTime.visibility = View.VISIBLE
                            } else {
                                binding.txtFromMessageTime.visibility = View.VISIBLE
                            }
                            item.isTimeVisible = true
                        }
                    }
                }
            }

            itemView.setOnLongClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnLongClickListener true
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                    true
                } else {
                    isLongClicked = true
                    updateSelectedView(item)
                    true
                }
            }

            itemView.setOnClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                } else {
                    if (isLongClicked) {
                        updateSelectedView(item)
                    } else {
                        if (item.isTimeVisible) {
                            if (item.isFromMe) {
                                binding.txtToMessageTime.visibility = View.GONE
                            } else {
                                binding.txtFromMessageTime.visibility = View.GONE
                            }
                            item.isTimeVisible = false
                        } else {
                            if (item.isFromMe) {
                                binding.txtToMessageTime.visibility = View.VISIBLE
                            } else {
                                binding.txtFromMessageTime.visibility = View.VISIBLE
                            }
                            item.isTimeVisible = true
                        }
                    }
                }
            }
        }

        fun updateSelectedView(item: ChatModel.MessageItem) {
            val messageID = if (item.mediaUri != null) {
                val lastNumber = item.mediaUri.lastPathSegment ?: ""
                "$lastNumber (Images)"
            } else item.smsId.toString()

            if (isLongClicked && messageID.isNotEmpty()) {
                if (selectedThreadIDList.contains(messageID)) {
                    selectedThreadIDList.remove(messageID)
                    if (item.isFromMe) {
                        binding.rvSelectedToView.background = null
                    } else {
                        binding.rvSelectedFromView.background = null
                    }
                } else {
                    selectedThreadIDList.add(messageID)
                    if (item.isFromMe) {
                        binding.rvSelectedToView.setBackgroundColor(context.getColorFromAttr(R.attr.itemSelectedColor))
                    } else {
                        binding.rvSelectedFromView.setBackgroundColor(context.getColorFromAttr(R.attr.itemSelectedColor))
                    }
                }

                SharedPreferencesHelper.saveArrayList(
                    context,
                    Const.SELECTED_MESSAGE_IDS,
                    selectedThreadIDList
                )
                (context as PersonalChatActivity).updateSelectedCount(selectedThreadIDList.size)
            }
        }

        fun clearSelectionView() {
            binding.rvSelectedToView.background = null
            binding.rvSelectedFromView.background = null
        }

        fun highlightOfSelectionView(item: ChatModel.MessageItem) {
            val targetView = if (item.isFromMe) {
                binding.rvSelectedToView
            } else {
                binding.rvSelectedFromView
            }
            blinkView(targetView)
        }

        fun updateTranslationState(item: ChatModel.MessageItem) {
            if (item.isFromMe) {
                binding.txtToMessage.text =
                    highlightPatterns(
                        context,
                        item.message,
                        searchQuery,
                        true,
                        anchorView = binding.txtToMessage
                    )
            } else {
                binding.txtFromMessage.text = highlightPatterns(
                    context,
                    item.message,
                    searchQuery,
                    anchorView = binding.txtFromMessage
                )
            }
        }

        fun updateChatBoxColor() {
            if (isMeChatBoxColor) {
                binding.rvFromMessageColor.backgroundTintList = null
                binding.ivLeftCorner.setColorFilter(
                    context.getColorFromAttr(R.attr.chatBoxColor),
                    PorterDuff.Mode.SRC_IN
                )
                binding.rvToMessageColor.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(context, chatBoxColor))
                binding.ivRightCorner.setColorFilter(
                    ContextCompat.getColor(context, chatBoxColor),
                    PorterDuff.Mode.SRC_IN
                )
            } else {
                binding.rvToMessageColor.backgroundTintList = null
                binding.ivRightCorner.setColorFilter(
                    context.getColorFromAttr(R.attr.chatBoxColor),
                    PorterDuff.Mode.SRC_IN
                )
                binding.rvFromMessageColor.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(context, chatBoxColor))
                binding.ivLeftCorner.setColorFilter(
                    ContextCompat.getColor(context, chatBoxColor),
                    PorterDuff.Mode.SRC_IN
                )
            }
        }

        fun updateProfileSection(item: ChatModel.MessageItem) {
            if (!item.isFromMe) {
                if (item.message.isNotEmpty() && !item.message.startsWith("[Image]")) {
                    binding.userContactAddress = contactAddress
                } else {
                    binding.rvToMessage.visibility = View.GONE
                    binding.rvFromMessage.visibility = View.GONE
                    binding.cvMessageUserProfile.visibility = View.GONE
                }
            } else {
                binding.cvMessageUserProfile.visibility = View.GONE
            }
        }
    }

    inner class MessageViewSixHolder(private val binding: ItemChatBubbleSixBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ChatModel.MessageItem) {
            val isTranslateVisible = SharedPreferencesHelper.getBoolean(
                context,
                Const.IS_TRANSLATE_VISIBLE_OR_NOT,
                false
            )
            var isFromDetectOtp = ""
            var isToDetectOtp = ""

            if (isTranslateVisible) {
                binding.llTranslateMessage.visibility = View.VISIBLE
                binding.llToTranslateMessage.visibility = View.VISIBLE
            } else {
                binding.llTranslateMessage.visibility = View.GONE
                binding.llToTranslateMessage.visibility = View.GONE
            }

            if (item.isFromMe) {
                if (item.mediaUri != null) {
                    binding.rvToImages.visibility = View.VISIBLE
                    Glide.with(context)
                        .load(item.mediaUri)
                        .into(binding.ivToMessage)
                } else {
                    binding.rvToImages.visibility = View.GONE
                }
                binding.rvFromImages.visibility = View.GONE
                binding.cvMessageUserProfile.visibility = View.GONE

                if (item.message.isNotEmpty() && !item.message.startsWith("[Image]")) {
                    val detectedOtpFromMessage = extractOtpFromText(item.message)
                    isToDetectOtp = detectedOtpFromMessage.toString()
                    binding.txtToMessage.text =
                        highlightPatterns(
                            context = context,
                            item.message,
                            searchQuery,
                            true,
                            anchorView = binding.txtToMessage
                        )
                    binding.txtToMessage.movementMethod = LinkMovementMethod.getInstance()
                    binding.rvFromMessage.visibility = View.GONE
                    if (!detectedOtpFromMessage.isNullOrEmpty()) {
                        binding.txtToCode.text =
                            context.getString(R.string.code_copy, detectedOtpFromMessage)
                        binding.rvToCopyCode.visibility = View.VISIBLE
                    } else {
                        binding.rvToCopyCode.visibility = View.GONE
                    }
                    binding.rvToMessage.visibility = View.VISIBLE
                    val messageTime = formatTimeFromTimestamp(item.timestamp)
                    binding.txtToMessageTime.text = messageTime
                } else {
                    binding.rvFromMessage.visibility = View.GONE
                    binding.rvToMessage.visibility = View.GONE
                }
            } else {
                if (item.mediaUri != null) {
                    binding.rvFromImages.visibility = View.VISIBLE
                    Glide.with(context)
                        .load(item.mediaUri)
                        .into(binding.ivFromMessage)
                } else {
                    binding.rvFromImages.visibility = View.GONE
                }

                binding.rvToImages.visibility = View.GONE
                binding.cvMessageUserProfile.visibility = View.VISIBLE

                if (item.message.isNotEmpty() && !item.message.startsWith("[Image]")) {
                    val detectedOtpFromMessage = extractOtpFromText(item.message)
                    isFromDetectOtp = detectedOtpFromMessage.toString()
                    binding.txtFromMessage.text =
                        highlightPatterns(
                            context = context,
                            item.message,
                            searchQuery,
                            anchorView = binding.txtFromMessage
                        )
                    binding.txtFromMessage.movementMethod = LinkMovementMethod.getInstance()
                    binding.rvToMessage.visibility = View.GONE
                    if (!detectedOtpFromMessage.isNullOrEmpty()) {
                        binding.txtFromCode.text =
                            context.getString(R.string.code_copy, detectedOtpFromMessage)
                        binding.rvFromCopyCode.visibility = View.VISIBLE
                    } else {
                        binding.rvFromCopyCode.visibility = View.GONE
                    }
                    binding.rvFromMessage.visibility = View.VISIBLE
                    val messageTime = formatTimeFromTimestamp(item.timestamp)
                    binding.txtFromMessageTime.text = messageTime

                    binding.userContactAddress = contactAddress
                } else {
                    binding.rvToMessage.visibility = View.GONE
                    binding.rvFromMessage.visibility = View.GONE
                    binding.cvMessageUserProfile.visibility = View.GONE
                }
            }

            if (isMeChatBoxColor) {
                binding.rvFromMessageColor.backgroundTintList = null
                binding.ivLeftCorner.setColorFilter(
                    context.getColorFromAttr(R.attr.chatBoxColor),
                    PorterDuff.Mode.SRC_IN
                )
                binding.rvToMessageColor.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(context, chatBoxColor))
                binding.ivRightCorner.setColorFilter(
                    ContextCompat.getColor(context, chatBoxColor),
                    PorterDuff.Mode.SRC_IN
                )
            } else {
                binding.rvToMessageColor.backgroundTintList = null
                binding.ivRightCorner.setColorFilter(
                    context.getColorFromAttr(R.attr.chatBoxColor),
                    PorterDuff.Mode.SRC_IN
                )
                binding.rvFromMessageColor.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(context, chatBoxColor))
                binding.ivLeftCorner.setColorFilter(
                    ContextCompat.getColor(context, chatBoxColor),
                    PorterDuff.Mode.SRC_IN
                )
            }

            val messageID = if (item.mediaUri != null) {
                val lastNumber = item.mediaUri.lastPathSegment ?: ""
                "$lastNumber (Images)"
            } else item.smsId.toString()

            if (item.isTimeVisible) {
                if (item.isFromMe) {
                    if (binding.rvToMessageColor.isVisible) binding.txtToMessageTime.visibility =
                        View.VISIBLE else binding.txtToMessageTime.visibility = View.GONE
                    binding.txtFromMessageTime.visibility = View.GONE
                } else {
                    if (binding.rvFromMessageColor.isVisible) binding.txtFromMessageTime.visibility =
                        View.VISIBLE else binding.txtFromMessageTime.visibility = View.GONE
                    binding.txtToMessageTime.visibility = View.GONE
                }
            } else {
                binding.txtToMessageTime.visibility = View.GONE
                binding.txtFromMessageTime.visibility = View.GONE
            }

            if (selectedThreadIDList.contains(messageID) && messageID.isNotEmpty()) {
                if (item.isFromMe) {
                    binding.rvSelectedToView.setBackgroundColor(context.getColorFromAttr(R.attr.itemSelectedColor))
                } else {
                    binding.rvSelectedFromView.setBackgroundColor(context.getColorFromAttr(R.attr.itemSelectedColor))
                }
            } else {
                if (messageID.isNotEmpty()) {
                    if (item.isFromMe) {
                        binding.rvSelectedToView.background = null
                    } else {
                        binding.rvSelectedFromView.background = null
                    }
                }
            }

            if (areValidMessageIds(messageID)) {
                if (isContainsLink(item.message)) {
                    if (SharedPreferencesHelper.isMessageStarred(
                            context,
                            messageID,
                            StarCategory.LINK,
                            threadId.toString()
                        )
                    ) {
                        if (item.isFromMe) {
                            binding.ivToStarMessage.visibility = View.VISIBLE
                            binding.ivFromStarMessage.visibility = View.GONE
                        } else {
                            binding.ivFromStarMessage.visibility = View.VISIBLE
                            binding.ivToStarMessage.visibility = View.GONE
                        }
                    } else {
                        binding.ivFromStarMessage.visibility = View.GONE
                        binding.ivToStarMessage.visibility = View.GONE
                    }
                } else {
                    if (SharedPreferencesHelper.isMessageStarred(
                            context,
                            messageID,
                            StarCategory.TEXT_ONLY,
                            threadId.toString()
                        )
                    ) {
                        if (item.isFromMe) {
                            binding.ivToStarMessage.visibility = View.VISIBLE
                            binding.ivFromStarMessage.visibility = View.GONE
                        } else {
                            binding.ivFromStarMessage.visibility = View.VISIBLE
                            binding.ivToStarMessage.visibility = View.GONE
                        }
                    } else {
                        binding.ivFromStarMessage.visibility = View.GONE
                        binding.ivToStarMessage.visibility = View.GONE
                    }
                }
            } else {
                if (SharedPreferencesHelper.isMessageStarred(
                        context,
                        messageID,
                        StarCategory.IMAGE,
                        threadId.toString()
                    )
                ) {
                    if (item.isFromMe) {
                        binding.ivToStarImages.visibility = View.VISIBLE
                        binding.ivFromStarImages.visibility = View.GONE
                    } else {
                        binding.ivFromStarImages.visibility = View.VISIBLE
                        binding.ivToStarImages.visibility = View.GONE
                    }
                } else {
                    binding.ivToStarImages.visibility = View.GONE
                    binding.ivFromStarImages.visibility = View.GONE
                }
            }

            binding.rvToImages.setOnLongClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnLongClickListener true
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                    true
                } else {
                    isLongClicked = true
                    updateSelectedView(item)
                    true
                }
            }

            binding.rvToImages.setOnClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                } else {
                    if (isLongClicked) {
                        updateSelectedView(item)
                    } else {
                        item.mediaUri?.let { uri ->
                            onClickPreviewImageInterface.onItemImagePreviewClick(
                                uri
                            )
                        }
                    }
                }
            }

            binding.rvFromImages.setOnLongClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnLongClickListener true
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                    true
                } else {
                    isLongClicked = true
                    updateSelectedView(item)
                    true
                }
            }

            binding.rvFromImages.setOnClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                } else {
                    if (isLongClicked) {
                        updateSelectedView(item)
                    } else {
                        item.mediaUri?.let { uri ->
                            onClickPreviewImageInterface.onItemImagePreviewClick(
                                uri
                            )
                        }
                    }
                }
            }

            binding.rvFromCopyCode.setOnLongClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnLongClickListener true
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                    true
                } else {
                    isLongClicked = true
                    updateSelectedView(item)
                    true
                }
            }

            binding.rvFromCopyCode.setOnClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                } else {
                    if (isLongClicked) {
                        updateSelectedView(item)
                    } else {
                        if (isFromDetectOtp.isNotEmpty()) {
                            copyToClipboard(context, isFromDetectOtp)
                        }
                    }
                }
            }

            binding.rvToCopyCode.setOnLongClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnLongClickListener true
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                    true
                } else {
                    isLongClicked = true
                    updateSelectedView(item)
                    true
                }
            }

            binding.rvToCopyCode.setOnClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                } else {
                    if (isLongClicked) {
                        updateSelectedView(item)
                    } else {
                        if (isToDetectOtp.isNotEmpty()) {
                            copyToClipboard(context, isToDetectOtp)
                        }
                    }
                }
            }

            binding.llTranslateMessage.setOnLongClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnLongClickListener true
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                    true
                } else {
                    isLongClicked = true
                    updateSelectedView(item)
                    true
                }
            }

            binding.llTranslateMessage.setOnClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                } else {
                    if (isLongClicked) {
                        updateSelectedView(item)
                    } else {
                        onClickPreviewImageInterface.onItemTranslateClick(
                            item,
                            position = absoluteAdapterPosition
                        )
                    }
                }
            }

            binding.llToTranslateMessage.setOnLongClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnLongClickListener true
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                    true
                } else {
                    isLongClicked = true
                    updateSelectedView(item)
                    true
                }
            }

            binding.llToTranslateMessage.setOnClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                } else {
                    if (isLongClicked) {
                        updateSelectedView(item)
                    } else {
                        onClickPreviewImageInterface.onItemTranslateClick(
                            item,
                            position = absoluteAdapterPosition
                        )
                    }
                }
            }

            binding.txtFromMessage.setOnLongClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnLongClickListener true
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                    true
                } else {
                    isLongClicked = true
                    updateSelectedView(item)
                    true
                }
            }

            binding.txtFromMessage.setOnClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                } else {
                    if (isLongClicked) {
                        updateSelectedView(item)
                    } else {
                        if (item.isTimeVisible) {
                            if (item.isFromMe) {
                                binding.txtToMessageTime.visibility = View.GONE
                            } else {
                                binding.txtFromMessageTime.visibility = View.GONE
                            }
                            item.isTimeVisible = false
                        } else {
                            if (item.isFromMe) {
                                binding.txtToMessageTime.visibility = View.VISIBLE
                            } else {
                                binding.txtFromMessageTime.visibility = View.VISIBLE
                            }
                            item.isTimeVisible = true
                        }
                    }
                }
            }

            binding.txtToMessage.setOnLongClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnLongClickListener true
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                    true
                } else {
                    isLongClicked = true
                    updateSelectedView(item)
                    true
                }
            }

            binding.txtToMessage.setOnClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                } else {
                    if (isLongClicked) {
                        updateSelectedView(item)
                    } else {
                        if (item.isTimeVisible) {
                            if (item.isFromMe) {
                                binding.txtToMessageTime.visibility = View.GONE
                            } else {
                                binding.txtFromMessageTime.visibility = View.GONE
                            }
                            item.isTimeVisible = false
                        } else {
                            if (item.isFromMe) {
                                binding.txtToMessageTime.visibility = View.VISIBLE
                            } else {
                                binding.txtFromMessageTime.visibility = View.VISIBLE
                            }
                            item.isTimeVisible = true
                        }
                    }
                }
            }

            itemView.setOnLongClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnLongClickListener true
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                    true
                } else {
                    isLongClicked = true
                    updateSelectedView(item)
                    true
                }
            }

            itemView.setOnClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                } else {
                    if (isLongClicked) {
                        updateSelectedView(item)
                    } else {
                        if (item.isTimeVisible) {
                            if (item.isFromMe) {
                                binding.txtToMessageTime.visibility = View.GONE
                            } else {
                                binding.txtFromMessageTime.visibility = View.GONE
                            }
                            item.isTimeVisible = false
                        } else {
                            if (item.isFromMe) {
                                binding.txtToMessageTime.visibility = View.VISIBLE
                            } else {
                                binding.txtFromMessageTime.visibility = View.VISIBLE
                            }
                            item.isTimeVisible = true
                        }
                    }
                }
            }
        }

        fun updateSelectedView(item: ChatModel.MessageItem) {
            val messageID = if (item.mediaUri != null) {
                val lastNumber = item.mediaUri.lastPathSegment ?: ""
                "$lastNumber (Images)"
            } else item.smsId.toString()

            if (isLongClicked && messageID.isNotEmpty()) {
                if (selectedThreadIDList.contains(messageID)) {
                    selectedThreadIDList.remove(messageID)
                    if (item.isFromMe) {
                        binding.rvSelectedToView.background = null
                    } else {
                        binding.rvSelectedFromView.background = null
                    }
                } else {
                    selectedThreadIDList.add(messageID)
                    if (item.isFromMe) {
                        binding.rvSelectedToView.setBackgroundColor(context.getColorFromAttr(R.attr.itemSelectedColor))
                    } else {
                        binding.rvSelectedFromView.setBackgroundColor(context.getColorFromAttr(R.attr.itemSelectedColor))
                    }
                }

                SharedPreferencesHelper.saveArrayList(
                    context,
                    Const.SELECTED_MESSAGE_IDS,
                    selectedThreadIDList
                )
                (context as PersonalChatActivity).updateSelectedCount(selectedThreadIDList.size)
            }
        }

        fun clearSelectionView() {
            binding.rvSelectedToView.background = null
            binding.rvSelectedFromView.background = null
        }

        fun highlightOfSelectionView(item: ChatModel.MessageItem) {
            val targetView = if (item.isFromMe) {
                binding.rvSelectedToView
            } else {
                binding.rvSelectedFromView
            }
            blinkView(targetView)
        }

        fun updateTranslationState(item: ChatModel.MessageItem) {
            if (item.isFromMe) {
                binding.txtToMessage.text =
                    highlightPatterns(
                        context,
                        item.message,
                        searchQuery,
                        true,
                        anchorView = binding.txtToMessage
                    )
            } else {
                binding.txtFromMessage.text = highlightPatterns(
                    context,
                    item.message,
                    searchQuery,
                    anchorView = binding.txtFromMessage
                )
            }
        }

        fun updateChatBoxColor() {
            if (isMeChatBoxColor) {
                binding.rvFromMessageColor.backgroundTintList = null
                binding.ivLeftCorner.setColorFilter(
                    context.getColorFromAttr(R.attr.chatBoxColor),
                    PorterDuff.Mode.SRC_IN
                )
                binding.rvToMessageColor.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(context, chatBoxColor))
                binding.ivRightCorner.setColorFilter(
                    ContextCompat.getColor(context, chatBoxColor),
                    PorterDuff.Mode.SRC_IN
                )
            } else {
                binding.rvToMessageColor.backgroundTintList = null
                binding.ivRightCorner.setColorFilter(
                    context.getColorFromAttr(R.attr.chatBoxColor),
                    PorterDuff.Mode.SRC_IN
                )
                binding.rvFromMessageColor.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(context, chatBoxColor))
                binding.ivLeftCorner.setColorFilter(
                    ContextCompat.getColor(context, chatBoxColor),
                    PorterDuff.Mode.SRC_IN
                )
            }
        }

        fun updateProfileSection(item: ChatModel.MessageItem) {
            if (!item.isFromMe) {
                if (item.message.isNotEmpty() && !item.message.startsWith("[Image]")) {
                    binding.userContactAddress = contactAddress
                } else {
                    binding.rvToMessage.visibility = View.GONE
                    binding.rvFromMessage.visibility = View.GONE
                    binding.cvMessageUserProfile.visibility = View.GONE
                }
            } else {
                binding.cvMessageUserProfile.visibility = View.GONE
            }
        }
    }

    inner class MessageViewSevenHolder(private val binding: ItemChatBubbleSevenBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ChatModel.MessageItem) {
            val isTranslateVisible = SharedPreferencesHelper.getBoolean(
                context,
                Const.IS_TRANSLATE_VISIBLE_OR_NOT,
                false
            )
            var isFromDetectOtp = ""
            var isToDetectOtp = ""

            if (isTranslateVisible) {
                binding.llTranslateMessage.visibility = View.VISIBLE
                binding.llToTranslateMessage.visibility = View.VISIBLE
            } else {
                binding.llTranslateMessage.visibility = View.GONE
                binding.llToTranslateMessage.visibility = View.GONE
            }

            if (item.isFromMe) {
                if (item.mediaUri != null) {
                    binding.rvToImages.visibility = View.VISIBLE
                    Glide.with(context)
                        .load(item.mediaUri)
                        .into(binding.ivToMessage)
                } else {
                    binding.rvToImages.visibility = View.GONE
                }
                binding.rvFromImages.visibility = View.GONE
                binding.cvMessageUserProfile.visibility = View.GONE

                if (item.message.isNotEmpty() && !item.message.startsWith("[Image]")) {
                    val detectedOtpFromMessage = extractOtpFromText(item.message)
                    isToDetectOtp = detectedOtpFromMessage.toString()
                    binding.txtToMessage.text =
                        highlightPatterns(
                            context = context,
                            item.message,
                            searchQuery,
                            true,
                            anchorView = binding.txtToMessage
                        )
                    binding.txtToMessage.movementMethod = LinkMovementMethod.getInstance()
                    binding.rvFromMessage.visibility = View.GONE
                    if (!detectedOtpFromMessage.isNullOrEmpty()) {
                        binding.txtToCode.text =
                            context.getString(R.string.code_copy, detectedOtpFromMessage)
                        binding.rvToCopyCode.visibility = View.VISIBLE
                    } else {
                        binding.rvToCopyCode.visibility = View.GONE
                    }
                    binding.rvToMessage.visibility = View.VISIBLE
                    val messageTime = formatTimeFromTimestamp(item.timestamp)
                    binding.txtToMessageTime.text = messageTime
                } else {
                    binding.rvFromMessage.visibility = View.GONE
                    binding.rvToMessage.visibility = View.GONE
                }
            } else {
                if (item.mediaUri != null) {
                    binding.rvFromImages.visibility = View.VISIBLE
                    Glide.with(context)
                        .load(item.mediaUri)
                        .into(binding.ivFromMessage)
                } else {
                    binding.rvFromImages.visibility = View.GONE
                }

                binding.rvToImages.visibility = View.GONE
                binding.cvMessageUserProfile.visibility = View.VISIBLE

                if (item.message.isNotEmpty() && !item.message.startsWith("[Image]")) {
                    val detectedOtpFromMessage = extractOtpFromText(item.message)
                    isFromDetectOtp = detectedOtpFromMessage.toString()
                    binding.txtFromMessage.text =
                        highlightPatterns(
                            context = context,
                            item.message,
                            searchQuery,
                            anchorView = binding.txtFromMessage
                        )
                    binding.txtFromMessage.movementMethod = LinkMovementMethod.getInstance()
                    binding.rvToMessage.visibility = View.GONE
                    if (!detectedOtpFromMessage.isNullOrEmpty()) {
                        binding.txtFromCode.text =
                            context.getString(R.string.code_copy, detectedOtpFromMessage)
                        binding.rvFromCopyCode.visibility = View.VISIBLE
                    } else {
                        binding.rvFromCopyCode.visibility = View.GONE
                    }
                    binding.rvFromMessage.visibility = View.VISIBLE
                    val messageTime = formatTimeFromTimestamp(item.timestamp)
                    binding.txtFromMessageTime.text = messageTime
                    binding.userContactAddress = contactAddress
                } else {
                    binding.rvToMessage.visibility = View.GONE
                    binding.rvFromMessage.visibility = View.GONE
                    binding.cvMessageUserProfile.visibility = View.GONE
                }
            }

            if (isMeChatBoxColor) {
                binding.rvFromMessageColor.backgroundTintList = null
                binding.ivLeftCorner.setColorFilter(
                    context.getColorFromAttr(R.attr.chatBoxColor),
                    PorterDuff.Mode.SRC_IN
                )
                binding.rvToMessageColor.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(context, chatBoxColor))
                binding.ivRightCorner.setColorFilter(
                    ContextCompat.getColor(context, chatBoxColor),
                    PorterDuff.Mode.SRC_IN
                )
            } else {
                binding.rvToMessageColor.backgroundTintList = null
                binding.ivRightCorner.setColorFilter(
                    context.getColorFromAttr(R.attr.chatBoxColor),
                    PorterDuff.Mode.SRC_IN
                )
                binding.rvFromMessageColor.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(context, chatBoxColor))
                binding.ivLeftCorner.setColorFilter(
                    ContextCompat.getColor(context, chatBoxColor),
                    PorterDuff.Mode.SRC_IN
                )
            }

            val messageID = if (item.mediaUri != null) {
                val lastNumber = item.mediaUri.lastPathSegment ?: ""
                "$lastNumber (Images)"
            } else item.smsId.toString()

            if (item.isTimeVisible) {
                if (item.isFromMe) {
                    if (binding.rvToMessageColor.isVisible) binding.txtToMessageTime.visibility =
                        View.VISIBLE else binding.txtToMessageTime.visibility = View.GONE
                    binding.txtFromMessageTime.visibility = View.GONE
                } else {
                    if (binding.rvFromMessageColor.isVisible) binding.txtFromMessageTime.visibility =
                        View.VISIBLE else binding.txtFromMessageTime.visibility = View.GONE
                    binding.txtToMessageTime.visibility = View.GONE
                }
            } else {
                binding.txtToMessageTime.visibility = View.GONE
                binding.txtFromMessageTime.visibility = View.GONE
            }

            if (selectedThreadIDList.contains(messageID) && messageID.isNotEmpty()) {
                if (item.isFromMe) {
                    binding.rvSelectedToView.setBackgroundColor(context.getColorFromAttr(R.attr.itemSelectedColor))
                } else {
                    binding.rvSelectedFromView.setBackgroundColor(context.getColorFromAttr(R.attr.itemSelectedColor))
                }
            } else {
                if (messageID.isNotEmpty()) {
                    if (item.isFromMe) {
                        binding.rvSelectedToView.background = null
                    } else {
                        binding.rvSelectedFromView.background = null
                    }
                }
            }

            if (areValidMessageIds(messageID)) {
                if (isContainsLink(item.message)) {
                    if (SharedPreferencesHelper.isMessageStarred(
                            context,
                            messageID,
                            StarCategory.LINK,
                            threadId.toString()
                        )
                    ) {
                        if (item.isFromMe) {
                            binding.ivToStarMessage.visibility = View.VISIBLE
                            binding.ivFromStarMessage.visibility = View.GONE
                        } else {
                            binding.ivFromStarMessage.visibility = View.VISIBLE
                            binding.ivToStarMessage.visibility = View.GONE
                        }
                    } else {
                        binding.ivFromStarMessage.visibility = View.GONE
                        binding.ivToStarMessage.visibility = View.GONE
                    }
                } else {
                    if (SharedPreferencesHelper.isMessageStarred(
                            context,
                            messageID,
                            StarCategory.TEXT_ONLY,
                            threadId.toString()
                        )
                    ) {
                        if (item.isFromMe) {
                            binding.ivToStarMessage.visibility = View.VISIBLE
                            binding.ivFromStarMessage.visibility = View.GONE
                        } else {
                            binding.ivFromStarMessage.visibility = View.VISIBLE
                            binding.ivToStarMessage.visibility = View.GONE
                        }
                    } else {
                        binding.ivFromStarMessage.visibility = View.GONE
                        binding.ivToStarMessage.visibility = View.GONE
                    }
                }
            } else {
                if (SharedPreferencesHelper.isMessageStarred(
                        context,
                        messageID,
                        StarCategory.IMAGE,
                        threadId.toString()
                    )
                ) {
                    if (item.isFromMe) {
                        binding.ivToStarImages.visibility = View.VISIBLE
                        binding.ivFromStarImages.visibility = View.GONE
                    } else {
                        binding.ivFromStarImages.visibility = View.VISIBLE
                        binding.ivToStarImages.visibility = View.GONE
                    }
                } else {
                    binding.ivToStarImages.visibility = View.GONE
                    binding.ivFromStarImages.visibility = View.GONE
                }
            }

            binding.rvToImages.setOnLongClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnLongClickListener true
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                    true
                } else {
                    isLongClicked = true
                    updateSelectedView(item)
                    true
                }
            }

            binding.rvToImages.setOnClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                } else {
                    if (isLongClicked) {
                        updateSelectedView(item)
                    } else {
                        item.mediaUri?.let { uri ->
                            onClickPreviewImageInterface.onItemImagePreviewClick(
                                uri
                            )
                        }
                    }
                }
            }

            binding.rvFromImages.setOnLongClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnLongClickListener true
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                    true
                } else {
                    isLongClicked = true
                    updateSelectedView(item)
                    true
                }
            }

            binding.rvFromImages.setOnClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                } else {
                    if (isLongClicked) {
                        updateSelectedView(item)
                    } else {
                        item.mediaUri?.let { uri ->
                            onClickPreviewImageInterface.onItemImagePreviewClick(
                                uri
                            )
                        }
                    }
                }
            }

            binding.rvFromCopyCode.setOnLongClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnLongClickListener true
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                    true
                } else {
                    isLongClicked = true
                    updateSelectedView(item)
                    true
                }
            }

            binding.rvFromCopyCode.setOnClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                } else {
                    if (isLongClicked) {
                        updateSelectedView(item)
                    } else {
                        if (isFromDetectOtp.isNotEmpty()) {
                            copyToClipboard(context, isFromDetectOtp)
                        }
                    }
                }
            }

            binding.rvToCopyCode.setOnLongClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnLongClickListener true
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                    true
                } else {
                    isLongClicked = true
                    updateSelectedView(item)
                    true
                }
            }

            binding.rvToCopyCode.setOnClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                } else {
                    if (isLongClicked) {
                        updateSelectedView(item)
                    } else {
                        if (isToDetectOtp.isNotEmpty()) {
                            copyToClipboard(context, isToDetectOtp)
                        }
                    }
                }
            }

            binding.llTranslateMessage.setOnLongClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnLongClickListener true
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                    true
                } else {
                    isLongClicked = true
                    updateSelectedView(item)
                    true
                }
            }

            binding.llTranslateMessage.setOnClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                } else {
                    if (isLongClicked) {
                        updateSelectedView(item)
                    } else {
                        onClickPreviewImageInterface.onItemTranslateClick(
                            item,
                            position = absoluteAdapterPosition
                        )
                    }
                }
            }

            binding.llToTranslateMessage.setOnLongClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnLongClickListener true
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                    true
                } else {
                    isLongClicked = true
                    updateSelectedView(item)
                    true
                }
            }

            binding.llToTranslateMessage.setOnClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                } else {
                    if (isLongClicked) {
                        updateSelectedView(item)
                    } else {
                        onClickPreviewImageInterface.onItemTranslateClick(
                            item,
                            position = absoluteAdapterPosition
                        )
                    }
                }
            }

            binding.txtFromMessage.setOnLongClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnLongClickListener true
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                    true
                } else {
                    isLongClicked = true
                    updateSelectedView(item)
                    true
                }
            }

            binding.txtFromMessage.setOnClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                } else {
                    if (isLongClicked) {
                        updateSelectedView(item)
                    } else {
                        if (item.isTimeVisible) {
                            if (item.isFromMe) {
                                binding.txtToMessageTime.visibility = View.GONE
                            } else {
                                binding.txtFromMessageTime.visibility = View.GONE
                            }
                            item.isTimeVisible = false
                        } else {
                            if (item.isFromMe) {
                                binding.txtToMessageTime.visibility = View.VISIBLE
                            } else {
                                binding.txtFromMessageTime.visibility = View.VISIBLE
                            }
                            item.isTimeVisible = true
                        }
                    }
                }
            }

            binding.txtToMessage.setOnLongClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnLongClickListener true
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                    true
                } else {
                    isLongClicked = true
                    updateSelectedView(item)
                    true
                }
            }

            binding.txtToMessage.setOnClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                } else {
                    if (isLongClicked) {
                        updateSelectedView(item)
                    } else {
                        if (item.isTimeVisible) {
                            if (item.isFromMe) {
                                binding.txtToMessageTime.visibility = View.GONE
                            } else {
                                binding.txtFromMessageTime.visibility = View.GONE
                            }
                            item.isTimeVisible = false
                        } else {
                            if (item.isFromMe) {
                                binding.txtToMessageTime.visibility = View.VISIBLE
                            } else {
                                binding.txtFromMessageTime.visibility = View.VISIBLE
                            }
                            item.isTimeVisible = true
                        }
                    }
                }
            }

            itemView.setOnLongClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnLongClickListener true
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                    true
                } else {
                    isLongClicked = true
                    updateSelectedView(item)
                    true
                }
            }

            itemView.setOnClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                } else {
                    if (isLongClicked) {
                        updateSelectedView(item)
                    } else {
                        if (item.isTimeVisible) {
                            if (item.isFromMe) {
                                binding.txtToMessageTime.visibility = View.GONE
                            } else {
                                binding.txtFromMessageTime.visibility = View.GONE
                            }
                            item.isTimeVisible = false
                        } else {
                            if (item.isFromMe) {
                                binding.txtToMessageTime.visibility = View.VISIBLE
                            } else {
                                binding.txtFromMessageTime.visibility = View.VISIBLE
                            }
                            item.isTimeVisible = true
                        }
                    }
                }
            }
        }

        fun updateSelectedView(item: ChatModel.MessageItem) {
            val messageID = if (item.mediaUri != null) {
                val lastNumber = item.mediaUri.lastPathSegment ?: ""
                "$lastNumber (Images)"
            } else item.smsId.toString()

            if (isLongClicked && messageID.isNotEmpty()) {
                if (selectedThreadIDList.contains(messageID)) {
                    selectedThreadIDList.remove(messageID)
                    if (item.isFromMe) {
                        binding.rvSelectedToView.background = null
                    } else {
                        binding.rvSelectedFromView.background = null
                    }
                } else {
                    selectedThreadIDList.add(messageID)
                    if (item.isFromMe) {
                        binding.rvSelectedToView.setBackgroundColor(context.getColorFromAttr(R.attr.itemSelectedColor))
                    } else {
                        binding.rvSelectedFromView.setBackgroundColor(context.getColorFromAttr(R.attr.itemSelectedColor))
                    }
                }

                SharedPreferencesHelper.saveArrayList(
                    context,
                    Const.SELECTED_MESSAGE_IDS,
                    selectedThreadIDList
                )
                (context as PersonalChatActivity).updateSelectedCount(selectedThreadIDList.size)
            }
        }

        fun clearSelectionView() {
            binding.rvSelectedToView.background = null
            binding.rvSelectedFromView.background = null
        }

        fun highlightOfSelectionView(item: ChatModel.MessageItem) {
            val targetView = if (item.isFromMe) {
                binding.rvSelectedToView
            } else {
                binding.rvSelectedFromView
            }
            blinkView(targetView)
        }

        fun updateTranslationState(item: ChatModel.MessageItem) {
            if (item.isFromMe) {
                binding.txtToMessage.text =
                    highlightPatterns(
                        context,
                        item.message,
                        searchQuery,
                        true,
                        anchorView = binding.txtToMessage
                    )
            } else {
                binding.txtFromMessage.text = highlightPatterns(
                    context,
                    item.message,
                    searchQuery,
                    anchorView = binding.txtFromMessage
                )
            }
        }

        fun updateChatBoxColor() {
            if (isMeChatBoxColor) {
                binding.rvFromMessageColor.backgroundTintList = null
                binding.ivLeftCorner.setColorFilter(
                    context.getColorFromAttr(R.attr.chatBoxColor),
                    PorterDuff.Mode.SRC_IN
                )
                binding.rvToMessageColor.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(context, chatBoxColor))
                binding.ivRightCorner.setColorFilter(
                    ContextCompat.getColor(context, chatBoxColor),
                    PorterDuff.Mode.SRC_IN
                )
            } else {
                binding.rvToMessageColor.backgroundTintList = null
                binding.ivRightCorner.setColorFilter(
                    context.getColorFromAttr(R.attr.chatBoxColor),
                    PorterDuff.Mode.SRC_IN
                )
                binding.rvFromMessageColor.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(context, chatBoxColor))
                binding.ivLeftCorner.setColorFilter(
                    ContextCompat.getColor(context, chatBoxColor),
                    PorterDuff.Mode.SRC_IN
                )
            }
        }

        fun updateProfileSection(item: ChatModel.MessageItem) {
            if (!item.isFromMe) {
                if (item.message.isNotEmpty() && !item.message.startsWith("[Image]")) {
                    binding.userContactAddress = contactAddress
                } else {
                    binding.rvToMessage.visibility = View.GONE
                    binding.rvFromMessage.visibility = View.GONE
                    binding.cvMessageUserProfile.visibility = View.GONE
                }
            } else {
                binding.cvMessageUserProfile.visibility = View.GONE
            }
        }
    }

    inner class MessageViewEightHolder(private val binding: ItemChatBubbleEightBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ChatModel.MessageItem) {
            val isTranslateVisible = SharedPreferencesHelper.getBoolean(
                context,
                Const.IS_TRANSLATE_VISIBLE_OR_NOT,
                false
            )
            var isFromDetectOtp = ""
            var isToDetectOtp = ""

            if (isTranslateVisible) {
                binding.llTranslateMessage.visibility = View.VISIBLE
                binding.llToTranslateMessage.visibility = View.VISIBLE
            } else {
                binding.llTranslateMessage.visibility = View.GONE
                binding.llToTranslateMessage.visibility = View.GONE
            }

            if (item.isFromMe) {
                if (item.mediaUri != null) {
                    binding.rvToImages.visibility = View.VISIBLE
                    Glide.with(context)
                        .load(item.mediaUri)
                        .into(binding.ivToMessage)
                } else {
                    binding.rvToImages.visibility = View.GONE
                }
                binding.rvFromImages.visibility = View.GONE
                binding.cvMessageUserProfile.visibility = View.GONE

                if (item.message.isNotEmpty() && !item.message.startsWith("[Image]")) {
                    val detectedOtpFromMessage = extractOtpFromText(item.message)
                    isToDetectOtp = detectedOtpFromMessage.toString()
                    binding.txtToMessage.text =
                        highlightPatterns(
                            context = context,
                            item.message,
                            searchQuery,
                            true,
                            anchorView = binding.txtToMessage
                        )
                    binding.txtToMessage.movementMethod = LinkMovementMethod.getInstance()
                    binding.rvFromMessage.visibility = View.GONE
                    if (!detectedOtpFromMessage.isNullOrEmpty()) {
                        binding.txtToCode.text =
                            context.getString(R.string.code_copy, detectedOtpFromMessage)
                        binding.rvToCopyCode.visibility = View.VISIBLE
                    } else {
                        binding.rvToCopyCode.visibility = View.GONE
                    }
                    binding.rvToMessage.visibility = View.VISIBLE
                    val messageTime = formatTimeFromTimestamp(item.timestamp)
                    binding.txtToMessageTime.text = messageTime
                } else {
                    binding.rvFromMessage.visibility = View.GONE
                    binding.rvToMessage.visibility = View.GONE
                }
            } else {
                if (item.mediaUri != null) {
                    binding.rvFromImages.visibility = View.VISIBLE
                    Glide.with(context)
                        .load(item.mediaUri)
                        .into(binding.ivFromMessage)
                } else {
                    binding.rvFromImages.visibility = View.GONE
                }

                binding.rvToImages.visibility = View.GONE
                binding.cvMessageUserProfile.visibility = View.VISIBLE

                if (item.message.isNotEmpty() && !item.message.startsWith("[Image]")) {
                    val detectedOtpFromMessage = extractOtpFromText(item.message)
                    isFromDetectOtp = detectedOtpFromMessage.toString()
                    binding.txtFromMessage.text =
                        highlightPatterns(
                            context = context,
                            item.message,
                            searchQuery,
                            anchorView = binding.txtFromMessage
                        )
                    binding.txtFromMessage.movementMethod = LinkMovementMethod.getInstance()
                    binding.rvToMessage.visibility = View.GONE
                    if (!detectedOtpFromMessage.isNullOrEmpty()) {
                        binding.txtFromCode.text =
                            context.getString(R.string.code_copy, detectedOtpFromMessage)
                        binding.rvFromCopyCode.visibility = View.VISIBLE
                    } else {
                        binding.rvFromCopyCode.visibility = View.GONE
                    }
                    binding.rvFromMessage.visibility = View.VISIBLE
                    val messageTime = formatTimeFromTimestamp(item.timestamp)
                    binding.txtFromMessageTime.text = messageTime
                    binding.userContactAddress = contactAddress
                } else {
                    binding.rvToMessage.visibility = View.GONE
                    binding.rvFromMessage.visibility = View.GONE
                    binding.cvMessageUserProfile.visibility = View.GONE
                }
            }

            if (isMeChatBoxColor) {
                binding.ivFirstCorner.backgroundTintList =
                    ColorStateList.valueOf(context.getColorFromAttr(R.attr.chatBoxColor))
                binding.ivSecondCorner.backgroundTintList =
                    ColorStateList.valueOf(context.getColorFromAttr(R.attr.chatBoxColor))
                binding.rvFromMessageColor.backgroundTintList = null
                binding.ivRightCorner.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(context, chatBoxColor))
                binding.ivSecondRightCorner.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(context, chatBoxColor))
                binding.rvToMessageColor.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(context, chatBoxColor))
            } else {
                binding.ivRightCorner.backgroundTintList =
                    ColorStateList.valueOf(context.getColorFromAttr(R.attr.chatBoxColor))
                binding.ivSecondRightCorner.backgroundTintList =
                    ColorStateList.valueOf(context.getColorFromAttr(R.attr.chatBoxColor))
                binding.rvToMessageColor.backgroundTintList = null
                binding.ivFirstCorner.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(context, chatBoxColor))
                binding.ivSecondCorner.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(context, chatBoxColor))
                binding.rvFromMessageColor.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(context, chatBoxColor))
            }

            val messageID = if (item.mediaUri != null) {
                val lastNumber = item.mediaUri.lastPathSegment ?: ""
                "$lastNumber (Images)"
            } else item.smsId.toString()

            if (item.isTimeVisible) {
                if (item.isFromMe) {
                    if (binding.rvToMessageColor.isVisible) binding.txtToMessageTime.visibility =
                        View.VISIBLE else binding.txtToMessageTime.visibility = View.GONE
                    binding.txtFromMessageTime.visibility = View.GONE
                } else {
                    if (binding.rvFromMessageColor.isVisible) binding.txtFromMessageTime.visibility =
                        View.VISIBLE else binding.txtFromMessageTime.visibility = View.GONE
                    binding.txtToMessageTime.visibility = View.GONE
                }
            } else {
                binding.txtToMessageTime.visibility = View.GONE
                binding.txtFromMessageTime.visibility = View.GONE
            }

            if (selectedThreadIDList.contains(messageID) && messageID.isNotEmpty()) {
                if (item.isFromMe) {
                    binding.rvSelectedToView.setBackgroundColor(context.getColorFromAttr(R.attr.itemSelectedColor))
                } else {
                    binding.rvSelectedFromView.setBackgroundColor(context.getColorFromAttr(R.attr.itemSelectedColor))
                }
            } else {
                if (messageID.isNotEmpty()) {
                    if (item.isFromMe) {
                        binding.rvSelectedToView.background = null
                    } else {
                        binding.rvSelectedFromView.background = null
                    }
                }
            }

            if (areValidMessageIds(messageID)) {
                if (isContainsLink(item.message)) {
                    if (SharedPreferencesHelper.isMessageStarred(
                            context,
                            messageID,
                            StarCategory.LINK,
                            threadId.toString()
                        )
                    ) {
                        if (item.isFromMe) {
                            binding.ivToStarMessage.visibility = View.VISIBLE
                            binding.ivFromStarMessage.visibility = View.GONE
                        } else {
                            binding.ivFromStarMessage.visibility = View.VISIBLE
                            binding.ivToStarMessage.visibility = View.GONE
                        }
                    } else {
                        binding.ivFromStarMessage.visibility = View.GONE
                        binding.ivToStarMessage.visibility = View.GONE
                    }
                } else {
                    if (SharedPreferencesHelper.isMessageStarred(
                            context,
                            messageID,
                            StarCategory.TEXT_ONLY,
                            threadId.toString()
                        )
                    ) {
                        if (item.isFromMe) {
                            binding.ivToStarMessage.visibility = View.VISIBLE
                            binding.ivFromStarMessage.visibility = View.GONE
                        } else {
                            binding.ivFromStarMessage.visibility = View.VISIBLE
                            binding.ivToStarMessage.visibility = View.GONE
                        }
                    } else {
                        binding.ivFromStarMessage.visibility = View.GONE
                        binding.ivToStarMessage.visibility = View.GONE
                    }
                }
            } else {
                if (SharedPreferencesHelper.isMessageStarred(
                        context,
                        messageID,
                        StarCategory.IMAGE,
                        threadId.toString()
                    )
                ) {
                    if (item.isFromMe) {
                        binding.ivToStarImages.visibility = View.VISIBLE
                        binding.ivFromStarImages.visibility = View.GONE
                    } else {
                        binding.ivFromStarImages.visibility = View.VISIBLE
                        binding.ivToStarImages.visibility = View.GONE
                    }
                } else {
                    binding.ivToStarImages.visibility = View.GONE
                    binding.ivFromStarImages.visibility = View.GONE
                }
            }

            binding.rvToImages.setOnLongClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnLongClickListener true
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                    true
                } else {
                    isLongClicked = true
                    updateSelectedView(item)
                    true
                }
            }

            binding.rvToImages.setOnClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                } else {
                    if (isLongClicked) {
                        updateSelectedView(item)
                    } else {
                        item.mediaUri?.let { uri ->
                            onClickPreviewImageInterface.onItemImagePreviewClick(
                                uri
                            )
                        }
                    }
                }
            }

            binding.rvFromImages.setOnLongClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnLongClickListener true
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                    true
                } else {
                    isLongClicked = true
                    updateSelectedView(item)
                    true
                }
            }

            binding.rvFromImages.setOnClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                } else {
                    if (isLongClicked) {
                        updateSelectedView(item)
                    } else {
                        item.mediaUri?.let { uri ->
                            onClickPreviewImageInterface.onItemImagePreviewClick(
                                uri
                            )
                        }
                    }
                }
            }

            binding.rvFromCopyCode.setOnLongClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnLongClickListener true
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                    true
                } else {
                    isLongClicked = true
                    updateSelectedView(item)
                    true
                }
            }

            binding.rvFromCopyCode.setOnClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                } else {
                    if (isLongClicked) {
                        updateSelectedView(item)
                    } else {
                        if (isFromDetectOtp.isNotEmpty()) {
                            copyToClipboard(context, isFromDetectOtp)
                        }
                    }
                }
            }

            binding.rvToCopyCode.setOnLongClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnLongClickListener true
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                    true
                } else {
                    isLongClicked = true
                    updateSelectedView(item)
                    true
                }
            }

            binding.rvToCopyCode.setOnClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                } else {
                    if (isLongClicked) {
                        updateSelectedView(item)
                    } else {
                        if (isToDetectOtp.isNotEmpty()) {
                            copyToClipboard(context, isToDetectOtp)
                        }
                    }
                }
            }

            binding.llTranslateMessage.setOnLongClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnLongClickListener true
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                    true
                } else {
                    isLongClicked = true
                    updateSelectedView(item)
                    true
                }
            }

            binding.llTranslateMessage.setOnClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                } else {
                    if (isLongClicked) {
                        updateSelectedView(item)
                    } else {
                        onClickPreviewImageInterface.onItemTranslateClick(
                            item,
                            position = absoluteAdapterPosition
                        )
                    }
                }
            }

            binding.llToTranslateMessage.setOnLongClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnLongClickListener true
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                    true
                } else {
                    isLongClicked = true
                    updateSelectedView(item)
                    true
                }
            }

            binding.llToTranslateMessage.setOnClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                } else {
                    if (isLongClicked) {
                        updateSelectedView(item)
                    } else {
                        onClickPreviewImageInterface.onItemTranslateClick(
                            item,
                            position = absoluteAdapterPosition
                        )
                    }
                }
            }

            binding.txtFromMessage.setOnLongClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnLongClickListener true
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                    true
                } else {
                    isLongClicked = true
                    updateSelectedView(item)
                    true
                }
            }

            binding.txtFromMessage.setOnClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                } else {
                    if (isLongClicked) {
                        updateSelectedView(item)
                    } else {
                        if (item.isTimeVisible) {
                            if (item.isFromMe) {
                                binding.txtToMessageTime.visibility = View.GONE
                            } else {
                                binding.txtFromMessageTime.visibility = View.GONE
                            }
                            item.isTimeVisible = false
                        } else {
                            if (item.isFromMe) {
                                binding.txtToMessageTime.visibility = View.VISIBLE
                            } else {
                                binding.txtFromMessageTime.visibility = View.VISIBLE
                            }
                            item.isTimeVisible = true
                        }
                    }
                }
            }

            binding.txtToMessage.setOnLongClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnLongClickListener true
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                    true
                } else {
                    isLongClicked = true
                    updateSelectedView(item)
                    true
                }
            }

            binding.txtToMessage.setOnClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                } else {
                    if (isLongClicked) {
                        updateSelectedView(item)
                    } else {
                        if (item.isTimeVisible) {
                            if (item.isFromMe) {
                                binding.txtToMessageTime.visibility = View.GONE
                            } else {
                                binding.txtFromMessageTime.visibility = View.GONE
                            }
                            item.isTimeVisible = false
                        } else {
                            if (item.isFromMe) {
                                binding.txtToMessageTime.visibility = View.VISIBLE
                            } else {
                                binding.txtFromMessageTime.visibility = View.VISIBLE
                            }
                            item.isTimeVisible = true
                        }
                    }
                }
            }

            itemView.setOnLongClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnLongClickListener true
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                    true
                } else {
                    isLongClicked = true
                    updateSelectedView(item)
                    true
                }
            }

            itemView.setOnClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                if (isFromMiniPopup) {
                    onOpenFullChatInterface.onItemClick()
                } else {
                    if (isLongClicked) {
                        updateSelectedView(item)
                    } else {
                        if (item.isTimeVisible) {
                            if (item.isFromMe) {
                                binding.txtToMessageTime.visibility = View.GONE
                            } else {
                                binding.txtFromMessageTime.visibility = View.GONE
                            }
                            item.isTimeVisible = false
                        } else {
                            if (item.isFromMe) {
                                binding.txtToMessageTime.visibility = View.VISIBLE
                            } else {
                                binding.txtFromMessageTime.visibility = View.VISIBLE
                            }
                            item.isTimeVisible = true
                        }
                    }
                }
            }
        }

        fun updateSelectedView(item: ChatModel.MessageItem) {
            val messageID = if (item.mediaUri != null) {
                val lastNumber = item.mediaUri.lastPathSegment ?: ""
                "$lastNumber (Images)"
            } else item.smsId.toString()

            if (isLongClicked && messageID.isNotEmpty()) {
                if (selectedThreadIDList.contains(messageID)) {
                    selectedThreadIDList.remove(messageID)
                    if (item.isFromMe) {
                        binding.rvSelectedToView.background = null
                    } else {
                        binding.rvSelectedFromView.background = null
                    }
                } else {
                    selectedThreadIDList.add(messageID)
                    if (item.isFromMe) {
                        binding.rvSelectedToView.setBackgroundColor(context.getColorFromAttr(R.attr.itemSelectedColor))
                    } else {
                        binding.rvSelectedFromView.setBackgroundColor(context.getColorFromAttr(R.attr.itemSelectedColor))
                    }
                }

                SharedPreferencesHelper.saveArrayList(
                    context,
                    Const.SELECTED_MESSAGE_IDS,
                    selectedThreadIDList
                )
                (context as PersonalChatActivity).updateSelectedCount(selectedThreadIDList.size)
            }
        }

        fun clearSelectionView() {
            binding.rvSelectedToView.background = null
            binding.rvSelectedFromView.background = null
        }

        fun highlightOfSelectionView(item: ChatModel.MessageItem) {
            val targetView = if (item.isFromMe) {
                binding.rvSelectedToView
            } else {
                binding.rvSelectedFromView
            }
            blinkView(targetView)
        }

        fun updateTranslationState(item: ChatModel.MessageItem) {
            if (item.isFromMe) {
                binding.txtToMessage.text =
                    highlightPatterns(
                        context,
                        item.message,
                        searchQuery,
                        true,
                        anchorView = binding.txtToMessage
                    )
            } else {
                binding.txtFromMessage.text = highlightPatterns(
                    context,
                    item.message,
                    searchQuery,
                    anchorView = binding.txtFromMessage
                )
            }
        }

        fun updateChatBoxColor() {
            if (isMeChatBoxColor) {
                binding.ivFirstCorner.backgroundTintList =
                    ColorStateList.valueOf(context.getColorFromAttr(R.attr.chatBoxColor))
                binding.ivSecondCorner.backgroundTintList =
                    ColorStateList.valueOf(context.getColorFromAttr(R.attr.chatBoxColor))
                binding.rvFromMessageColor.backgroundTintList = null
                binding.ivRightCorner.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(context, chatBoxColor))
                binding.ivSecondRightCorner.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(context, chatBoxColor))
                binding.rvToMessageColor.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(context, chatBoxColor))
            } else {
                binding.ivRightCorner.backgroundTintList =
                    ColorStateList.valueOf(context.getColorFromAttr(R.attr.chatBoxColor))
                binding.ivSecondRightCorner.backgroundTintList =
                    ColorStateList.valueOf(context.getColorFromAttr(R.attr.chatBoxColor))
                binding.rvToMessageColor.backgroundTintList = null
                binding.ivFirstCorner.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(context, chatBoxColor))
                binding.ivSecondCorner.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(context, chatBoxColor))
                binding.rvFromMessageColor.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(context, chatBoxColor))
            }
        }

        fun updateProfileSection(item: ChatModel.MessageItem) {
            if (!item.isFromMe) {
                if (item.message.isNotEmpty() && !item.message.startsWith("[Image]")) {
                    binding.userContactAddress = contactAddress
                } else {
                    binding.rvToMessage.visibility = View.GONE
                    binding.rvFromMessage.visibility = View.GONE
                    binding.cvMessageUserProfile.visibility = View.GONE
                }
            } else {
                binding.cvMessageUserProfile.visibility = View.GONE
            }
        }
    }

    fun updateData(newList: List<ChatModel>, searchView: String) {
        val oldSize = chatList.size
        val newSize = newList.size

        val newItemsCount = newSize - oldSize

        searchQuery = searchView
        val diffCallback = ChatDiffCallback(chatList, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        chatList.clear()
        chatList.addAll(newList)

        if (newItemsCount > 0 && oldSize != 0) {
            notifyItemRangeInserted(oldSize, newItemsCount)
        } else {
            if (newItemsCount >= 0) {
                diffResult.dispatchUpdatesTo(this)
            } else {
                notifyDataSetChanged()
            }
        }
    }

    private fun extractOtpFromText(text: String): String? {
        val cleanedText = text.lowercase().replace("[^a-z0-9\\s]".toRegex(), " ")

        // 2. Blacklist transactional keywords
        val blacklistedKeywords =
            listOf("debit", "credited", "credit", "transaction", "rs", "inr", "amount")
        val blacklistRegex =
            Regex("\\b(${blacklistedKeywords.joinToString("|")})\\b", RegexOption.IGNORE_CASE)
        if (blacklistRegex.containsMatchIn(cleanedText)) {
            return null
        }

        // 3. Check for legitimate OTP keywords
        val keywordRegex = Regex(
            "\\b(otp|one time password|verification code|verification|code)\\b",
            RegexOption.IGNORE_CASE
        )
        if (!keywordRegex.containsMatchIn(cleanedText)) {
            return null
        }

        // 4. Extract 4- or 6-digit OTP
        val otpRegex = Regex("\\b\\d{4}\\b|\\b\\d{6}\\b")
        return otpRegex.find(cleanedText)?.value
    }

    private fun copyToClipboard(context: Context, code: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("OTP", code)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(
            context,
            context.getString(R.string.otp_copied_to_clipboard), Toast.LENGTH_SHORT
        ).show()
    }

    private fun highlightPatterns(
        context: Context,
        inputText: String,
        searchQuery: String,
        isFromMe: Boolean = false,
        anchorView: View
    ): SpannableString {

        if (inputText.isEmpty()) return SpannableString("")

        val spannable = SpannableString(inputText)

        try {
            val phoneRegex = "(\\+91[\\s-]?)?[7-9][0-9]{4}[\\s-]?[0-9]{5}".toRegex()
            val numberRegex = "\\b\\d+\\b".toRegex()
            val dateRegex =
                "\\b\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}\\b|\\b\\d{1,2}\\s+(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\\s+\\d{2,4}\\b"
                    .toRegex(RegexOption.IGNORE_CASE)
            val customDateRegex = "\\b\\d{2}-[A-Za-z]{3}-\\d{2}\\s\\d{2}:\\d{2}\\b".toRegex()
            val linkRegex = Patterns.WEB_URL.toRegex()
            val rsRegex = "(?i)(Rs\\.?\\s?\\d{1,10}|₹\\s?\\d{1,10})(?=\\b|\\s|$|[^\\w.])".toRegex()

            val allRegex =
                listOf(phoneRegex, rsRegex, dateRegex, customDateRegex, linkRegex, numberRegex)

            val matchedRanges = mutableListOf<IntRange>()

            for (regex in allRegex) {
                val matches = regex.findAll(inputText)

                for (match in matches) {
                    val start = match.range.first
                    val end = match.range.last + 1

                    // ✅ HARD SAFETY CHECK (prevents crash)
                    if (start < 0 || end > inputText.length || start >= end) continue

                    val currentRange = start until end

                    if (matchedRanges.none { it.first < end && start < it.last }) {
                        matchedRanges.add(currentRange)

                        val colorRes = if (isFromMe) R.color.black else R.color.app_theme_color
                        val color = ContextCompat.getColor(context, colorRes)
                        val matchedText = match.value

                        val clickableSpan = object : ClickableSpan() {
                            override fun onClick(widget: View) {
                                try {
                                    if (isFromMiniPopup) {
                                        onOpenFullChatInterface.onItemClick()
                                    } else {
                                        when {
                                            linkRegex.matches(matchedText) -> {
                                                showContactPopup(
                                                    anchorView,
                                                    isFromMe,
                                                    "LINK",
                                                    matchedText
                                                )
                                            }

                                            phoneRegex.matches(matchedText) ||
                                                    numberRegex.matches(matchedText) -> {
                                                showContactPopup(
                                                    anchorView,
                                                    isFromMe,
                                                    "NUMBER",
                                                    matchedText
                                                )
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.something_went_wrong),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }

                            override fun updateDrawState(ds: TextPaint) {
                                ds.isUnderlineText = true
                                ds.color = color
                            }
                        }

                        spannable.setSpan(
                            clickableSpan,
                            start,
                            end,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }
                }
            }

            // ✅ SEARCH HIGHLIGHT (safe)
            if (searchQuery.isNotBlank()) {
                val searchRegex = Regex(Regex.escape(searchQuery), RegexOption.IGNORE_CASE)

                for (match in searchRegex.findAll(inputText)) {
                    val start = match.range.first
                    val end = match.range.last + 1

                    if (start < 0 || end > inputText.length || start >= end) continue

                    spannable.setSpan(
                        BackgroundColorSpan(
                            ContextCompat.getColor(
                                context,
                                R.color.app_theme_color
                            )
                        ),
                        start,
                        end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )

                    spannable.setSpan(
                        ForegroundColorSpan(ContextCompat.getColor(context, R.color.white)),
                        start,
                        end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
            return SpannableString(inputText) // fallback safe
        }

        return spannable
    }

    private fun showContactPopup(
        anchor: View,
        isFromMe: Boolean,
        linkOrNumber: String,
        message: String
    ) {
        val activity = anchor.context as Activity
        val rootView = activity.window.decorView.findViewById<ViewGroup>(android.R.id.content)

        val topViewBinding =
            ItemMessageFeaturesTopViewPopupBinding.inflate(LayoutInflater.from(anchor.context))
        val bottomViewBinding =
            ItemMessageFeaturesBottomViewPopupBinding.inflate(LayoutInflater.from(anchor.context))

        var popupView = topViewBinding.root

        val popupWindow = PopupWindow(
            popupView,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        )

        val dimView = View(anchor.context).apply {
            setBackgroundColor("#73000000".toColorInt())
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val blurEffect = RenderEffect.createBlurEffect(50f, 50f, Shader.TileMode.CLAMP)
            rootView.setRenderEffect(blurEffect)
            rootView.addView(dimView)
        } else {
            popupWindow.setBackgroundDrawable("#73000000".toColorInt().toDrawable())
            popupWindow.isOutsideTouchable = true
            popupWindow.isFocusable = true
        }

        popupWindow.setOnDismissListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                rootView.setRenderEffect(null)
                rootView.removeView(dimView)
            }
        }

        popupView.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val popupHeight = popupView.measuredHeight
        val screenHeight = Resources.getSystem().displayMetrics.heightPixels

        val location = IntArray(2)
        anchor.getLocationOnScreen(location)
        val anchorY = location[1]
        val anchorHeight = anchor.height

        val spaceBelow = screenHeight - (anchorY + anchorHeight)

        popupView = if (spaceBelow >= popupHeight) {
            topViewBinding.root
        } else if (anchorY >= popupHeight) {
            bottomViewBinding.root
        } else {
            topViewBinding.root
        }

        popupWindow.contentView = popupView

        val yPos = when {
            spaceBelow >= popupHeight -> anchorY + anchorHeight
            anchorY >= popupHeight -> anchorY - popupHeight
            else -> screenHeight / 2 - popupHeight / 2
        }

        topViewBinding.userContactAddress = contactAddress ?: linkOrNumber
        bottomViewBinding.userContactAddress = contactAddress ?: linkOrNumber

        topViewBinding.rvViewInChat.visibility = View.GONE
        bottomViewBinding.rvViewInChat.visibility = View.GONE

        topViewBinding.rvCopyView.visibility = View.GONE
        bottomViewBinding.rvCopyView.visibility = View.GONE

        topViewBinding.txtUserName.setTextColor(
            ColorStateList.valueOf(
                ContextCompat.getColor(
                    context,
                    R.color.app_theme_color
                )
            )
        )
        topViewBinding.txtUserNumber.setTextColor(
            ColorStateList.valueOf(
                ContextCompat.getColor(
                    context,
                    R.color.app_theme_color
                )
            )
        )
        bottomViewBinding.txtUserName.setTextColor(
            ColorStateList.valueOf(
                ContextCompat.getColor(
                    context,
                    R.color.app_theme_color
                )
            )
        )
        bottomViewBinding.txtUserNumber.setTextColor(
            ColorStateList.valueOf(
                ContextCompat.getColor(
                    context,
                    R.color.app_theme_color
                )
            )
        )

        if (linkOrNumber.contains("LINK")) {
            topViewBinding.txtUserName.text = message
            topViewBinding.txtUserNumber.text = message
            bottomViewBinding.txtUserName.text = message
            bottomViewBinding.txtUserNumber.text = message

            topViewBinding.txtCopyTitle.text = context.getString(R.string.copy_link)
            bottomViewBinding.txtCopyTitle.text = context.getString(R.string.copy_link)

            topViewBinding.rvCallAction.visibility = View.GONE
            topViewBinding.rvCallActionView.visibility = View.GONE
            topViewBinding.rvSendMessage.visibility = View.GONE
            topViewBinding.rvSendMessageView.visibility = View.GONE
            topViewBinding.rvWhatsapp.visibility = View.GONE
            topViewBinding.rvWhatsappView.visibility = View.GONE

            bottomViewBinding.rvCallAction.visibility = View.GONE
            bottomViewBinding.rvCallActionView.visibility = View.GONE
            bottomViewBinding.rvSendMessage.visibility = View.GONE
            bottomViewBinding.rvSendMessageView.visibility = View.GONE
            bottomViewBinding.rvWhatsapp.visibility = View.GONE
            bottomViewBinding.rvWhatsappView.visibility = View.GONE

            topViewBinding.rvOpen.visibility = View.VISIBLE
            topViewBinding.rvOpenView.visibility = View.VISIBLE
            bottomViewBinding.rvOpen.visibility = View.VISIBLE
            bottomViewBinding.rvOpenView.visibility = View.VISIBLE
        } else {
            topViewBinding.txtUserName.text = message
            topViewBinding.txtUserNumber.text = message
            bottomViewBinding.txtUserName.text = message
            bottomViewBinding.txtUserNumber.text = message

            topViewBinding.txtCopyTitle.text = context.getString(R.string.copy)
            bottomViewBinding.txtCopyTitle.text = context.getString(R.string.copy)

            topViewBinding.rvOpen.visibility = View.GONE
            topViewBinding.rvOpenView.visibility = View.GONE
            bottomViewBinding.rvOpen.visibility = View.GONE
            bottomViewBinding.rvOpenView.visibility = View.GONE

            if (isPhoneNumber(message)) {
                topViewBinding.txtCall.text =
                    activity.getString(R.string.call_auto, message)
                topViewBinding.rvCallAction.visibility = View.VISIBLE
                topViewBinding.rvCallActionView.visibility = View.VISIBLE
                topViewBinding.rvSendMessage.visibility = View.VISIBLE
                topViewBinding.rvSendMessageView.visibility = View.VISIBLE
                topViewBinding.rvWhatsapp.visibility = View.VISIBLE
                topViewBinding.rvWhatsappView.visibility = View.VISIBLE
            } else {
                topViewBinding.rvCallAction.visibility = View.GONE
                topViewBinding.rvCallActionView.visibility = View.GONE
                topViewBinding.rvSendMessage.visibility = View.GONE
                topViewBinding.rvSendMessageView.visibility = View.GONE
                topViewBinding.rvWhatsapp.visibility = View.GONE
                topViewBinding.rvWhatsappView.visibility = View.GONE
            }

            if (isPhoneNumber(message)) {
                bottomViewBinding.txtCall.text =
                    activity.getString(R.string.call_auto, message)
                bottomViewBinding.rvCallAction.visibility = View.VISIBLE
                bottomViewBinding.rvCallActionView.visibility = View.VISIBLE
                bottomViewBinding.rvSendMessage.visibility = View.VISIBLE
                bottomViewBinding.rvSendMessageView.visibility = View.VISIBLE
                bottomViewBinding.rvWhatsapp.visibility = View.VISIBLE
                bottomViewBinding.rvWhatsappView.visibility = View.VISIBLE
            } else {
                bottomViewBinding.rvCallAction.visibility = View.GONE
                bottomViewBinding.rvCallActionView.visibility = View.GONE
                bottomViewBinding.rvSendMessage.visibility = View.GONE
                bottomViewBinding.rvSendMessageView.visibility = View.GONE
                bottomViewBinding.rvWhatsapp.visibility = View.GONE
                bottomViewBinding.rvWhatsappView.visibility = View.GONE
            }
        }

        topViewBinding.rvCallAction.setOnClickListener {
            onSelectedMessageFeatureClick.onSelectedMessageClick(message, "CALL")
            popupWindow.dismiss()
        }

        bottomViewBinding.rvCallAction.setOnClickListener {
            onSelectedMessageFeatureClick.onSelectedMessageClick(message, "CALL")
            popupWindow.dismiss()
        }

        topViewBinding.rvOpen.setOnClickListener {
            onSelectedMessageFeatureClick.onSelectedMessageClick(message, "OPEN")
            popupWindow.dismiss()
        }

        bottomViewBinding.rvOpen.setOnClickListener {
            onSelectedMessageFeatureClick.onSelectedMessageClick(message, "OPEN")
            popupWindow.dismiss()
        }

        topViewBinding.rvWhatsapp.setOnClickListener {
            onSelectedMessageFeatureClick.onSelectedMessageClick(message, "WHATSAPP_MESSAGE")
            popupWindow.dismiss()
        }

        bottomViewBinding.rvWhatsapp.setOnClickListener {
            onSelectedMessageFeatureClick.onSelectedMessageClick(message, "WHATSAPP_MESSAGE")
            popupWindow.dismiss()
        }

        topViewBinding.rvCopyMessage.setOnClickListener {
            onSelectedMessageFeatureClick.onSelectedMessageClick(message, "COPY_LINK")
            popupWindow.dismiss()
        }

        bottomViewBinding.rvCopyMessage.setOnClickListener {
            onSelectedMessageFeatureClick.onSelectedMessageClick(message, "COPY_LINK")
            popupWindow.dismiss()
        }

        topViewBinding.rvSendMessage.setOnClickListener {
            popupWindow.dismiss()
        }

        bottomViewBinding.rvSendMessage.setOnClickListener {
            popupWindow.dismiss()
        }

        if (isFromMe) {
            popupWindow.showAtLocation(rootView, Gravity.TOP or Gravity.END, 34, yPos)
        } else {
            popupWindow.showAtLocation(rootView, Gravity.TOP or Gravity.START, 34, yPos)
        }
    }

    private fun isPhoneNumber(address: String?): Boolean {
        if (address.isNullOrBlank()) return false
        return PhoneNumberUtils.isGlobalPhoneNumber(address)
    }

    private fun formatTimestamp(timeMillis: Long): String {
        val messageDate = Date(timeMillis)
        val now = Date()

        val messageCal = Calendar.getInstance().apply { time = messageDate }
        val nowCal = Calendar.getInstance().apply { time = now }

        val sameYear =
            messageCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR)
        val sameMonth =
            sameYear && messageCal.get(Calendar.MONTH) == nowCal.get(Calendar.MONTH)
        val sameDay =
            sameMonth && messageCal.get(Calendar.DAY_OF_MONTH) == nowCal.get(Calendar.DAY_OF_MONTH)

        return when {
            sameDay -> {
                SimpleDateFormat("hh:mm a", Locale.getDefault()).format(messageDate) // Today
            }

            DateUtils.isToday(timeMillis - DateUtils.DAY_IN_MILLIS) -> {
                "Yesterday"
            }

            sameYear && isThisWeek(timeMillis) -> {
                SimpleDateFormat(
                    "EEE hh:mm a",
                    Locale.getDefault()
                ).format(messageDate) // This week
            }

            sameYear -> {
                SimpleDateFormat(
                    "MMM dd, hh:mm a",
                    Locale.getDefault()
                ).format(messageDate) // Same year
            }

            else -> {
                SimpleDateFormat(
                    "MMM dd, yyyy, hh:mm a",
                    Locale.getDefault()
                ).format(messageDate) // Different year
            }
        }
    }

    private fun isThisWeek(timeMillis: Long): Boolean {
        val messageCal = Calendar.getInstance().apply {
            timeInMillis = timeMillis
        }
        val nowCal = Calendar.getInstance()

        val weekOfYearMsg = messageCal.get(Calendar.WEEK_OF_YEAR)
        val weekOfYearNow = nowCal.get(Calendar.WEEK_OF_YEAR)

        val yearMsg = messageCal.get(Calendar.YEAR)
        val yearNow = nowCal.get(Calendar.YEAR)

        return weekOfYearMsg == weekOfYearNow && yearMsg == yearNow
    }

    private fun isContainsLink(message: String): Boolean {
        val urlRegex =
            """((https?://|www\.)\S+|\b[a-zA-Z0-9-]+\.(com|net|org|in|ly|me|co|io|info|xyz)(/\S*)?)"""
                .toRegex(RegexOption.IGNORE_CASE)
        return urlRegex.containsMatchIn(message)
    }

    private fun areValidMessageIds(messageIds: String): Boolean {
        return !messageIds.trim().contains("images", ignoreCase = true)
    }

    fun updateSpecificItem(position: Int, statement: String) {
        translateMessage = statement
        notifyItemChanged(position, "update_statement")
    }

    fun highlightMessageOfView(messageId: String) {
        if (areValidMessageIds(messageId)) {
            for (i in chatList.indices) {
                if (chatList[i] is ChatModel.MessageItem) {
                    val item = chatList[i] as ChatModel.MessageItem
                    if (item.smsId.toString() == messageId) {
                        notifyItemChanged(i, "highlight_of_view")
                    }
                }
            }
        } else {
            val mediaUri = extractIdFromLabel(messageId)
            for (i in chatList.indices) {
                if (chatList[i] is ChatModel.MessageItem) {
                    val item = chatList[i] as ChatModel.MessageItem
                    val mediaId = item.mediaUri?.lastPathSegment
                    if (mediaUri == mediaId) {
                        notifyItemChanged(i, "highlight_of_view")
                    }
                }
            }
        }
    }

    private fun extractIdFromLabel(label: String): String {
        return if (label.contains(":")) {
            label.substringAfter(":").trim()
        } else {
            label
        }
    }

    fun updateStarredMessage(messageId: String) {
        if (areValidMessageIds(messageId)) {
            for (i in chatList.indices) {
                if (chatList[i] is ChatModel.MessageItem) {
                    val item = chatList[i] as ChatModel.MessageItem
                    if (item.smsId.toString() == messageId) {
                        notifyItemChanged(i)
                    }
                }
            }
        } else {
            val regex = Regex("^\\d+")
            val messageIdForImage = regex.find(messageId)?.value ?: ""

            for (i in chatList.indices) {
                if (chatList[i] is ChatModel.MessageItem) {
                    val item = chatList[i] as ChatModel.MessageItem
                    val mediaId = item.mediaUri?.lastPathSegment
                    if (messageIdForImage == mediaId) {
                        notifyItemChanged(i)
                    }
                }
            }
        }
    }

    fun updateChatBoxColorItem(chatBoxColorSelected: Int, isFromMeOrNot: Boolean) {
        isMeChatBoxColor = isFromMeOrNot
        chatBoxColor = chatBoxColorSelected
        for (i in chatList.indices) {
            notifyItemChanged(i, "update_chat_box_color")
        }
    }

    fun clearAndUpdateView() {
        selectedThreadIDList.clear()
        isLongClicked = false
        for (i in chatList.indices) {
            notifyItemChanged(i, "partialClear")
        }
    }

    fun updateProfileView(profileAddress: String) {
        contactAddress = profileAddress

        for (i in chatList.indices) {
            notifyItemChanged(i, "partialUpdateProfile")
        }
    }

    private fun formatTimeFromTimestamp(timestamp: Long): String {
        val date = Date(timestamp)
        val sdf = SimpleDateFormat("h:mm a", Locale.ENGLISH)
        return sdf.format(date)
    }

    private fun blinkView(view: View) {
        val highlightColor = context.getColorFromAttr(R.attr.itemSelectedColor)
        val normalColor = context.getColorFromAttr(R.attr.mainBackground)

        fun blink(count: Int) {
            if (count == 0) {
                view.setBackgroundColor(normalColor)
                return
            }
            view.setBackgroundColor(highlightColor)
            view.postDelayed({
                view.setBackgroundColor(normalColor)
                view.postDelayed({
                    blink(count - 1)
                }, 200L)
            }, 200L)
        }

        blink(3)
    }
}

class ChatDiffCallback(
    private val oldList: List<ChatModel>,
    private val newList: List<ChatModel>
) : DiffUtil.Callback() {

    override fun getOldListSize() = oldList.size
    override fun getNewListSize() = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]

        return when (oldItem) {
            is ChatModel.MessageItem if newItem is ChatModel.MessageItem -> {
                oldItem.timestamp == newItem.timestamp && oldItem.message == newItem.message
            }

            is ChatModel.Header if newItem is ChatModel.Header -> {
                oldItem.title == newItem.title
            }

            else -> false
        }
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}

