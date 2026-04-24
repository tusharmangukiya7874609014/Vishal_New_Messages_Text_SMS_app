package com.texting.sms.messaging_app.adapter

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.RenderEffect
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import android.provider.BlockedNumberContract
import android.text.format.DateUtils
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.texting.sms.messaging_app.R
import com.texting.sms.messaging_app.activity.PersonalChatActivity
import com.texting.sms.messaging_app.activity.PrivateChatListActivity
import com.texting.sms.messaging_app.database.Const
import com.texting.sms.messaging_app.database.SharedPreferencesHelper
import com.texting.sms.messaging_app.databinding.ItemDirectSingleReadMessagesBinding
import com.texting.sms.messaging_app.databinding.ItemMessageHistoryBinding
import com.texting.sms.messaging_app.listener.OnChatUserInterface
import com.texting.sms.messaging_app.listener.OnClickMessagesFeature
import com.texting.sms.messaging_app.listener.OnClickPreviewImageInterface
import com.texting.sms.messaging_app.listener.OnOpenFullChatInterface
import com.texting.sms.messaging_app.listener.OnSelectedMessageFeatureClick
import com.texting.sms.messaging_app.model.ChatModel
import com.texting.sms.messaging_app.model.ChatUser
import com.texting.sms.messaging_app.model.MessagesResult
import com.texting.sms.messaging_app.utils.getDrawableFromAttr
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class PrivateChatsAdapter(
    private var messageFilterList: MutableList<ChatUser>,
    private var selectedThreadIDList: ArrayList<String>,
    private var onChartUserInterface: OnChatUserInterface,
    private var onClickMessagesFeature: OnClickMessagesFeature,
    private var activeSIM: Int = 0,
    private var context: Context
) : RecyclerView.Adapter<PrivateChatsAdapter.ViewHolder>() {

    private var isLongClicked = false
    private var chatBoxColorList = mutableListOf<Int>()
    private var lastIsMeChatBoxColor = true
    private var storeThreadIDList = ArrayList<String>()
    private var isMultiSelectionEnableFromPage = false

    class ViewHolder(val binding: ItemMessageHistoryBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemMessageHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder) {
            binding.apply {
                isMultiSelectionOptionsEnable = isMultiSelectionEnableFromPage
                activateSimDetails = activeSIM

                executePendingBindings()
            }

            itemView.setOnLongClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION || absoluteAdapterPosition >= messageFilterList.size) return@setOnLongClickListener true

                if (!isMultiSelectionEnableFromPage) {
                    messageFilterList[absoluteAdapterPosition].apply {
                        showContactPopup(
                            itemView,
                            threadId, contactName.toString(), address
                        )
                    }
                }
                true
            }

            val threadId = messageFilterList[absoluteAdapterPosition].threadId.toString()
            messageFilterList[absoluteAdapterPosition].isMessageSelected =
                selectedThreadIDList.contains(threadId)

            binding.item = messageFilterList[absoluteAdapterPosition]

            itemView.setOnClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION || absoluteAdapterPosition >= messageFilterList.size) return@setOnClickListener

                val threadId = messageFilterList[absoluteAdapterPosition].threadId.toString()

                if (isMultiSelectionEnableFromPage) {
                    if (selectedThreadIDList.contains(threadId)) {
                        selectedThreadIDList.remove(threadId)
                        messageFilterList[absoluteAdapterPosition].isMessageSelected = false
                    } else {
                        selectedThreadIDList.add(threadId)
                        messageFilterList[absoluteAdapterPosition].isMessageSelected = true
                    }

                    binding.item = messageFilterList[absoluteAdapterPosition]
                    SharedPreferencesHelper.saveArrayList(
                        context, Const.SELECTED_MESSAGE_IDS, selectedThreadIDList
                    )
                    (context as PrivateChatListActivity).updateSelectedCount(selectedThreadIDList.size)
                } else {
                    onChartUserInterface.chatUserClick(messageFilterList[absoluteAdapterPosition])
                }
            }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty()) {
            val payload = payloads[0]
            if (position >= messageFilterList.size) return
            with(holder) {
                when (payload) {
                    "partialClear" -> {
                        binding.isMultiSelectionOptionsEnable = false
                        messageFilterList[absoluteAdapterPosition].isMessageSelected = false
                        binding.item = messageFilterList[absoluteAdapterPosition]
                    }

                    "partialBlocked" -> {
                        if (isAddressBlocked(context, messageFilterList[absoluteAdapterPosition].address)) {
                            SharedPreferencesHelper.addToBlockList(
                                context, messageFilterList[absoluteAdapterPosition].address
                            )
                        } else {
                            SharedPreferencesHelper.removeFromBlockList(
                                context, messageFilterList[absoluteAdapterPosition].address
                            )
                        }
                        messageFilterList[absoluteAdapterPosition].isMessageSelected = false
                        binding.item = messageFilterList[absoluteAdapterPosition]
                    }

                    "partialMarkAsRead" -> {
                        if (isAddressBlocked(context, messageFilterList[absoluteAdapterPosition].address)) {
                            SharedPreferencesHelper.addToBlockList(
                                context, messageFilterList[absoluteAdapterPosition].address
                            )
                        } else {
                            SharedPreferencesHelper.removeFromBlockList(
                                context, messageFilterList[absoluteAdapterPosition].address
                            )
                        }
                        messageFilterList[absoluteAdapterPosition].isMessageSelected = false
                        binding.item = messageFilterList[absoluteAdapterPosition]
                    }

                    "partialUpdateOfAllItem" -> {
                        binding.isMultiSelectionOptionsEnable = isMultiSelectionEnableFromPage
                    }
                }
            }
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    private fun showContactPopup(
        anchor: View, threadId: Long, contactsName: String, address: String
    ) {
        val activity = anchor.context as? Activity ?: return
        val rootView =
            activity.window.decorView.findViewById<ViewGroup>(android.R.id.content) ?: return

        val dialogViewBinding =
            ItemDirectSingleReadMessagesBinding.inflate(LayoutInflater.from(anchor.context))
        val popupView = dialogViewBinding.root

        val maxWidth = (context.resources.displayMetrics.widthPixels * 0.9).toInt()

        val popupWindow = PopupWindow(
            popupView, maxWidth, LinearLayout.LayoutParams.WRAP_CONTENT, true
        )

        val isPopupDismissed = false

        fun safeDismiss() {
            if (!isPopupDismissed && popupWindow.isShowing) {
                popupWindow.dismiss()
            }
        }

        val dimView = View(activity).apply {
            setBackgroundColor("#73000000".toColorInt())
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            rootView.setRenderEffect(
                RenderEffect.createBlurEffect(40f, 40f, Shader.TileMode.CLAMP)
            )
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

        chatBoxColorList.clear()
        val isDark = isDarkTheme()
        chatBoxColorList.add(
            if (isDark) R.color.dark_chat_default_color
            else R.color.light_chat_default_color
        )

        val arrayRes = if (isDark) {
            R.array.dark_chat_box_colors
        } else {
            R.array.light_chat_box_colors
        }

        val ta = context.resources.obtainTypedArray(arrayRes)
        try {
            repeat(ta.length()) {
                chatBoxColorList.add(ta.getResourceId(it, 0))
            }
        } finally {
            ta.recycle()
        }

        dialogViewBinding.rvSenderChatView.layoutManager = LinearLayoutManager(activity)

        dialogViewBinding.txtAddToPrivateChats.text =
            context.getString(R.string.remove_from_private_chats)
        dialogViewBinding.ivPrivateChats.setImageDrawable(context.getDrawableFromAttr(R.attr.removeToPrivateChatPopup))

        if (SharedPreferencesHelper.isPinned(context, threadId = threadId)) {
            dialogViewBinding.ivPinUnpin.setImageDrawable(
                context.getDrawableFromAttr(R.attr.unPinPopup)
            )
            dialogViewBinding.txtPin.text = context.getString(R.string.unpin)
        } else {
            dialogViewBinding.ivPinUnpin.setImageDrawable(
                context.getDrawableFromAttr(R.attr.pinPopup)
            )
            dialogViewBinding.txtPin.text = context.getString(R.string.pin)
        }

        dialogViewBinding.txtBlocked.text =
            if (isAddressBlocked(activity, address)) activity.getString(R.string.unblock)
            else activity.getString(R.string.block)

        var chatBoxColor =
            if (isDarkTheme()) R.color.dark_chat_default_color else R.color.light_chat_default_color

        val latsChatBoxColorPosition =
            SharedPreferencesHelper.getInt(context, Const.CHAT_BOX_COLOR_POSITION, -1)

        if (latsChatBoxColorPosition in chatBoxColorList.indices) {
            chatBoxColor = chatBoxColorList[latsChatBoxColorPosition]
            lastIsMeChatBoxColor =
                SharedPreferencesHelper.getBoolean(activity, Const.IS_ME_CHAT_BOX_COLOR, true)
        } else {
            lastIsMeChatBoxColor = true
        }

        fun navigateToChatPage() {
            SharedPreferencesHelper.saveLong(context, "CURRENT_THREAD_ID", threadId)
            val intent = Intent(context, PersonalChatActivity::class.java)
            intent.putExtra(Const.THREAD_ID, threadId)
            intent.putExtra(Const.SENDER_ID, address)
            context.startActivity(intent)
        }

        val rvPersonalChatListAdapter = PersonalChatAdapter(
            activity,
            threadId,
            mutableListOf(),
            chatBoxColor,
            lastIsMeChatBoxColor,
            storeThreadIDList,
            onClickPreviewImageInterface = object : OnClickPreviewImageInterface {
                override fun onItemImagePreviewClick(uri: Uri) {
                    safeDismiss()
                    navigateToChatPage()
                }

                override fun onItemTranslateClick(
                    item: ChatModel.MessageItem, position: Int
                ) {
                    safeDismiss()
                    navigateToChatPage()
                }
            },
            onSelectedMessageFeatureClick = object : OnSelectedMessageFeatureClick {
                override fun onSelectedMessageClick(
                    linkOrNumber: String, performAction: String
                ) {
                    safeDismiss()
                    navigateToChatPage()
                }
            },
            onOpenFullChatInterface = object : OnOpenFullChatInterface {
                override fun onItemClick() {
                    safeDismiss()
                    navigateToChatPage()
                }
            },
            isFromMiniPopup = true
        )

        dialogViewBinding.rvSenderChatView.adapter = rvPersonalChatListAdapter
        (dialogViewBinding.rvSenderChatView.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations =
            false

        rvPersonalChatListAdapter.updateProfileView(address)

        CoroutineScope(Dispatchers.IO).launch {
            val originalMessages = getMessagesForThread(context, threadId)

            val finalChatList = groupMessagesWithHeaders(originalMessages.messages)

            withContext(Dispatchers.Main) {
                if (finalChatList.isNotEmpty()) {
                    rvPersonalChatListAdapter.updateData(finalChatList, "")
                    dialogViewBinding.rvSenderChatView.scrollToPosition(rvPersonalChatListAdapter.itemCount - 1)
                    dialogViewBinding.paginationProgress.visibility = View.GONE
                    dialogViewBinding.viewOfProfile.visibility = View.VISIBLE
                }
            }
        }

        dialogViewBinding.cvTransparentView.setOnClickListener {
            safeDismiss()
        }

        dialogViewBinding.ivCloseDialog.setOnClickListener {
            safeDismiss()
        }

        dialogViewBinding.ivSeeFullChat.setOnClickListener {
            safeDismiss()
            navigateToChatPage()
        }

        dialogViewBinding.txtUserName.text =
            if (contactsName.isNotEmpty() && contactsName != "null") contactsName else address

        dialogViewBinding.txtUserNumber.text = address

        dialogViewBinding.rvPin.setOnClickListener {
            safeDismiss()
            onClickMessagesFeature.onClickOfMessageFeature(threadId, "PIN")
        }

        dialogViewBinding.rvMarkAsRead.setOnClickListener {
            safeDismiss()
            onClickMessagesFeature.onClickOfMessageFeature(threadId, "MARK_AS_READ")
        }

        dialogViewBinding.rvAddToPrivateChat.setOnClickListener {
            safeDismiss()
            onClickMessagesFeature.onClickOfMessageFeature(threadId, "REMOVE_FROM_PRIVATE")
        }

        dialogViewBinding.rvBlockedMessage.setOnClickListener {
            safeDismiss()
            onClickMessagesFeature.onClickOfMessageFeature(threadId, "BLOCKED")
        }

        dialogViewBinding.rvDeleteChat.setOnClickListener {
            safeDismiss()
            onClickMessagesFeature.onClickOfMessageFeature(threadId, "DELETE")
        }

        popupView.measure(
            View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED
        )

        popupWindow.showAtLocation(rootView, Gravity.CENTER, 0, 0)
    }

    private fun isDarkTheme(): Boolean {
        val currentNightMode =
            context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES
    }

    private fun getMessagesForThread(
        context: Context,
        threadId: Long
    ): MessagesResult {
        val allMessages = mutableListOf<ChatModel.MessageItem>()

        val smsUri = "content://sms".toUri()
        val smsProjection = arrayOf("_id", "date", "body", "type", "read")
        val smsSelection = "thread_id = ?"
        val smsArgs = arrayOf(threadId.toString())

        context.contentResolver.query(
            smsUri,
            smsProjection,
            smsSelection,
            smsArgs,
            "date DESC LIMIT 10"
        )
            ?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow("_id")
                val dateIndex = cursor.getColumnIndexOrThrow("date")
                val bodyIndex = cursor.getColumnIndexOrThrow("body")
                val typeIndex = cursor.getColumnIndexOrThrow("type")
                val readIndex = cursor.getColumnIndexOrThrow("read")

                var lastMessage: ChatModel.MessageItem? = null

                while (cursor.moveToNext()) {
                    val smsId = cursor.getLong(idIndex)
                    val timestamp = cursor.getLong(dateIndex)
                    val body = cursor.getString(bodyIndex) ?: ""
                    val type = cursor.getInt(typeIndex)
                    val isFromMe = (type != 1)
                    val isRead = cursor.getInt(readIndex) == 1

                    if (lastMessage != null &&
                        lastMessage.isFromMe == isFromMe &&
                        abs(lastMessage.timestamp - timestamp) < 100
                    ) {
                        lastMessage = lastMessage.copy(
                            message = lastMessage.message + body,
                            timestamp = timestamp
                        )
                        allMessages[allMessages.lastIndex] = lastMessage
                    } else {
                        val newMessage = ChatModel.MessageItem(
                            smsId = smsId,
                            message = body,
                            timestamp = timestamp,
                            isFromMe = isFromMe,
                            isRead = isRead,
                            mediaUri = null
                        )
                        allMessages.add(newMessage)
                        lastMessage = newMessage
                    }
                }
            }

        val partsMap = mutableMapOf<Long, MutableList<Pair<String, Uri?>>>()
        context.contentResolver.query(
            "content://mms/part".toUri(),
            arrayOf("_id", "ct", "text", "mid"),
            null, null, null
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndex("_id")
            val ctIndex = cursor.getColumnIndex("ct")
            val textIndex = cursor.getColumnIndex("text")
            val midIndex = cursor.getColumnIndex("mid")

            if (idIndex == -1 || ctIndex == -1 || midIndex == -1) return@use

            while (cursor.moveToNext()) {
                val partId = cursor.getString(idIndex) ?: continue
                val contentType = cursor.getString(ctIndex) ?: continue
                val mid = cursor.getLong(midIndex)

                if (contentType.equals("application/smil", true) ||
                    contentType.equals("text/xml", true)
                ) continue

                val mediaUri = if (contentType.startsWith("image/") ||
                    contentType.startsWith("video/") ||
                    contentType.startsWith("audio/")
                ) {
                    "content://mms/part/$partId".toUri()
                } else null

                val text = if (mediaUri == null && textIndex != -1) {
                    cursor.getString(textIndex) ?: ""
                } else ""

                partsMap.getOrPut(mid) { mutableListOf() }
                    .add(Pair(text, mediaUri))
            }
        }

        val mmsUri = "content://mms".toUri()
        val mmsProjection = arrayOf("_id", "date", "msg_box", "read")
        val mmsSelection = "thread_id = ?"
        val mmsArgs = arrayOf(threadId.toString())

        context.contentResolver.query(
            mmsUri,
            mmsProjection,
            mmsSelection,
            mmsArgs,
            "date DESC LIMIT 10"
        )
            ?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow("_id")
                val dateIndex = cursor.getColumnIndexOrThrow("date")
                val boxIndex = cursor.getColumnIndexOrThrow("msg_box")
                val readIndex = cursor.getColumnIndexOrThrow("read")

                while (cursor.moveToNext()) {
                    val mmsId = cursor.getLong(idIndex)
                    val timestamp = cursor.getLong(dateIndex) * 1000L
                    val isFromMe = cursor.getInt(boxIndex) != 1
                    val isRead = cursor.getInt(readIndex) == 1

                    val parts = partsMap[mmsId] ?: continue

                    for ((text, mediaUri) in parts) {
                        if (text.isBlank() && mediaUri == null) continue

                        allMessages.add(
                            ChatModel.MessageItem(
                                smsId = mmsId,
                                message = text,
                                timestamp = timestamp,
                                isFromMe = isFromMe,
                                isRead = isRead,
                                mediaUri = mediaUri
                            )
                        )
                    }
                }
            }

        val sorted = allMessages.sortedBy { it.timestamp }

        val hasMore = sorted.size > 10
        val last10 = if (sorted.size > 10) sorted.takeLast(10) else sorted

        return MessagesResult(messages = last10, hasMore = hasMore)
    }

    private fun groupMessagesWithHeaders(messages: List<ChatModel.MessageItem>): MutableList<ChatModel> {
        if (messages.isEmpty()) return mutableListOf()

        val groupedList = mutableListOf<ChatModel>()
        var lastDateHeader: String? = null
        var lastTimeHeader: String? = null

        for (message in messages) {
            val isToday = DateUtils.isToday(message.timestamp)

            if (isToday) {
                val currentTime = formatTime(message.timestamp)
                if (currentTime != lastTimeHeader) {
                    groupedList.add(ChatModel.Header(title = message.timestamp))
                    lastTimeHeader = currentTime
                }
            } else {
                val currentDateKey = getHeaderKey(message.timestamp)
                if (currentDateKey != lastDateHeader) {
                    groupedList.add(ChatModel.Header(title = message.timestamp))
                    lastDateHeader = currentDateKey
                }
            }

            groupedList.add(message)
        }

        return groupedList
    }

    private fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("hh:mm a", Locale.ENGLISH)
        return sdf.format(Date(timestamp))
    }

    private fun getHeaderKey(timeMillis: Long): String {
        val messageDate = Date(timeMillis)
        val now = Date()

        val messageCal = Calendar.getInstance().apply { time = messageDate }
        val nowCal = Calendar.getInstance().apply { time = now }

        return when {
            DateUtils.isToday(timeMillis) -> "TODAY"
            DateUtils.isToday(timeMillis - DateUtils.DAY_IN_MILLIS) -> "YESTERDAY"
            isThisWeek(timeMillis) -> {
                SimpleDateFormat("EEE", Locale.ENGLISH).format(messageDate)
            }

            messageCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) -> {
                SimpleDateFormat("MMM dd", Locale.ENGLISH).format(messageDate)
            }

            else -> {
                SimpleDateFormat(
                    "MMM dd, yyyy", Locale.ENGLISH
                ).format(messageDate)
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

    private fun isAddressBlocked(context: Context?, number: String): Boolean {
        return try {
            BlockedNumberContract.isBlocked(context, number)
        } catch (_: Exception) {
            false
        }
    }

    fun updateItemByThreadId(threadId: Long, chatUser: ChatUser?) {
        val position = messageFilterList.indexOfFirst { it.threadId == threadId }
        if (position != -1) {
            if (chatUser != null) {
                messageFilterList[position] = chatUser
            }
            notifyItemChanged(position)
        }
    }

    fun removeByThreadId(threadID: Long) {
        val index = messageFilterList.indexOfFirst { it.threadId == threadID }
        if (index != -1) {
            messageFilterList.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    fun updateBlockContacts() {
        selectedThreadIDList.clear()
        isLongClicked = false
        val blockedList = SharedPreferencesHelper.getAllBlocked(context)
        blockedList.forEach { address ->
            val index = messageFilterList.indexOfFirst { it.address == address }
            if (index != -1) {
                notifyItemChanged(index, "partialBlocked")
            }
        }
    }

    fun updateMarkAsRead(threadID: Long) {
        selectedThreadIDList.clear()
        isLongClicked = false
        val index = messageFilterList.indexOfFirst { it.threadId == threadID }
        notifyItemChanged(index, "partialMarkAsRead")
    }

    fun clearAndUpdateView() {
        isMultiSelectionEnableFromPage = false
        selectedThreadIDList.clear()
        isLongClicked = false
        notifyItemRangeChanged(
            0,
            messageFilterList.size,
            "partialClear"
        )
    }

    fun updateSelectionView(multiSelection: Boolean) {
        isMultiSelectionEnableFromPage = multiSelection
        notifyItemRangeChanged(
            0,
            messageFilterList.size,
            "partialUpdateOfAllItem"
        )
    }

    fun updateData(newList: List<ChatUser>) {
        val diffCallback = PrivateChatDiffCallback(messageFilterList, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        messageFilterList.clear()
        messageFilterList.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }

    override fun getItemCount(): Int {
        return messageFilterList.size
    }
}

class PrivateChatDiffCallback(
    private val oldList: List<ChatUser>, private val newList: List<ChatUser>
) : DiffUtil.Callback() {
    override fun getOldListSize(): Int = oldList.size
    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].threadId == newList[newItemPosition].threadId
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]
        return oldItem == newItem
    }
}