package com.texting.sms.messaging_app.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.texting.sms.messaging_app.model.ScheduledSms

class ScheduledSMSDatabaseHelper(context: Context) : SQLiteOpenHelper(
    context, DATABASE_NAME, null, DATABASE_VERSION
) {

    companion object {
        private const val DATABASE_NAME = "scheduled_sms.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_NAME = "scheduled_sms"

        private const val COLUMN_ID = "id"
        private const val COLUMN_NAME = "name"
        private const val COLUMN_NUMBER = "number"
        private const val COLUMN_MESSAGE = "message"
        private const val COLUMN_IMAGE_URIS = "imageURIs"
        private const val COLUMN_TIME = "time"
        private const val COLUMN_PHOTO_URI = "photoURI"
        private const val COLUMN_THREAD_ID = "threadID"
        private const val COLUMN_SCHEDULED_MILLIS = "scheduledMillis"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE $TABLE_NAME (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_NAME TEXT,
                $COLUMN_NUMBER TEXT,
                $COLUMN_MESSAGE TEXT,
                $COLUMN_IMAGE_URIS TEXT,
                $COLUMN_TIME TEXT,
                $COLUMN_PHOTO_URI TEXT,
                $COLUMN_THREAD_ID TEXT,
                $COLUMN_SCHEDULED_MILLIS INTEGER
            )
        """.trimIndent()
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun insertSms(sms: ScheduledSms): ScheduledSms {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_NAME, sms.name)
            put(COLUMN_NUMBER, sms.number)
            put(COLUMN_MESSAGE, sms.message)
            put(COLUMN_IMAGE_URIS, sms.imageURIs)
            put(COLUMN_TIME, sms.time)
            put(COLUMN_PHOTO_URI, sms.contactUserPhotoUri)
            put(COLUMN_THREAD_ID, sms.threadID)
            put(COLUMN_SCHEDULED_MILLIS, sms.scheduledMillis)
        }
        val newId = db.insert(TABLE_NAME, null, values)
        return sms.copy(id = newId.toInt())
    }

    fun deleteSms(id: Int): Int {
        val db = writableDatabase
        return db.delete(TABLE_NAME, "$COLUMN_ID = ?", arrayOf(id.toString()))
    }

    fun getAllSms(): List<ScheduledSms> {
        val smsList = mutableListOf<ScheduledSms>()
        val db = readableDatabase
        val cursor =
            db.query(TABLE_NAME, null, null, null, null, null, "$COLUMN_SCHEDULED_MILLIS ASC")

        cursor.use {
            if (it.moveToFirst()) {
                do {
                    val sms = ScheduledSms(
                        id = it.getInt(it.getColumnIndexOrThrow(COLUMN_ID)),
                        name = it.getString(it.getColumnIndexOrThrow(COLUMN_NAME)),
                        number = it.getString(it.getColumnIndexOrThrow(COLUMN_NUMBER)),
                        message = it.getString(it.getColumnIndexOrThrow(COLUMN_MESSAGE)),
                        imageURIs = it.getString(it.getColumnIndexOrThrow(COLUMN_IMAGE_URIS)),
                        time = it.getString(it.getColumnIndexOrThrow(COLUMN_TIME)),
                        contactUserPhotoUri = it.getString(
                            it.getColumnIndexOrThrow(
                                COLUMN_PHOTO_URI
                            )
                        ),
                        threadID = it.getString(it.getColumnIndexOrThrow(COLUMN_THREAD_ID)),
                        scheduledMillis = it.getLong(
                            it.getColumnIndexOrThrow(
                                COLUMN_SCHEDULED_MILLIS
                            )
                        )
                    )
                    smsList.add(sms)
                } while (it.moveToNext())
            }
        }

        return smsList
    }
}