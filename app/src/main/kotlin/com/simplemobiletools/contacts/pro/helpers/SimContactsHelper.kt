package com.simplemobiletools.contacts.pro.helpers

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import com.simplemobiletools.commons.extensions.getStringValue
import com.simplemobiletools.commons.extensions.normalizePhoneNumber
import com.simplemobiletools.commons.extensions.queryCursor
import com.simplemobiletools.contacts.pro.extensions.getEmptyContact
import com.simplemobiletools.contacts.pro.models.Contact
import com.simplemobiletools.contacts.pro.models.PhoneNumber

class SimContactsHelper(private val context: Context, private val contactsHelper : ContactsHelper) {
    private val ICC_URI = Uri.parse("content://icc/adn")
    private val NAME_COLUMN_GET = "name"
    private val NAME_COLUMN_INSERT = "tag"
    private val NAME_COLUMN_UPDATE = "newTag"
    private val PHONE_NUMBER_COLUMN = "number"
    private val PHONE_NUMBER_COLUMN_UPDATE = "newNumber"
    private val TAG = "SimContactsHelper"

    fun getSimContacts(): List<Contact> {
        val contacts = mutableListOf<Contact>()
        val projection = arrayOf(NAME_COLUMN_GET, PHONE_NUMBER_COLUMN)
        context.queryCursor(ICC_URI, projection) { cursor ->
            val name = cursor.getStringValue(NAME_COLUMN_GET)
            val number = cursor.getStringValue(PHONE_NUMBER_COLUMN)
            val sources = contactsHelper.getDeviceContactSources()
            contacts.add(
                context.getEmptyContact().copy(
                    firstName = name,
                    phoneNumbers = arrayListOf(
                        PhoneNumber(
                            value = number,
                            type = ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE,
                            label = "",
                            normalizedNumber = number.normalizePhoneNumber(),
                        )
                    ),
                )
            )
        }
        return contacts
    }

    fun updateSimContact(contact: Contact): Boolean {
        val newName = contact.getNameToDisplay()
        val newPhoneNumber = contact.firstPhoneNumber ?: ""
        val oldContact = contactsHelper.getContactWithId(contact.id, false)!!
        Log.d(TAG, "updateing sim contact from $oldContact to $contact")
        val contentValues = ContentValues().apply {
            put(NAME_COLUMN_UPDATE, newName)
            put(PHONE_NUMBER_COLUMN_UPDATE, newPhoneNumber)
        }
        val selection = "$NAME_COLUMN_GET = ? AND $PHONE_NUMBER_COLUMN = ?"
        val selectionArgs = arrayOf(oldContact.getNameToDisplay(), oldContact.firstPhoneNumber)
        val rows = context.contentResolver.update(ICC_URI, contentValues, selection, selectionArgs)
        Log.d(TAG, "updateSimContact: $rows")
        return rows > 0
    }

    fun insertSimContact(contact: Contact): Boolean {
        Log.d(TAG, "insertSimContact: $contact")
        val contentValues = ContentValues().apply {
            put(NAME_COLUMN_INSERT, contact.firstName)
            put(PHONE_NUMBER_COLUMN, contact.firstPhoneNumber)
        }
        val resultUri = context.contentResolver.insert(ICC_URI, contentValues)

        Log.i(TAG, "insertContact sim contact: $resultUri")
        return resultUri != null
    }
}
