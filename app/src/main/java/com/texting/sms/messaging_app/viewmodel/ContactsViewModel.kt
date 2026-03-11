package com.texting.sms.messaging_app.viewmodel

import android.content.Context
import android.provider.ContactsContract
import androidx.lifecycle.ViewModel
import com.texting.sms.messaging_app.utils.ContactNumberCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ContactsViewModel : ViewModel() {

    fun loadContactsNumbers(context: Context) {
        preloadContactNumbers(context.applicationContext)
    }

    private fun preloadContactNumbers(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                null,
                null,
                null
            )

            cursor?.use {
                val idIndex = it.getColumnIndex(
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID
                )
                val numberIndex = it.getColumnIndex(
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                )

                while (it.moveToNext()) {
                    ContactNumberCache.putNumber(
                        it.getString(idIndex),
                        it.getString(numberIndex)
                    )
                }
            }
        }
    }
}