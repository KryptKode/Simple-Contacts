package com.simplemobiletools.contacts.pro.helpers

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.BaseColumns
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.RawContacts
import android.util.Log
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.overloads.times
import com.simplemobiletools.contacts.pro.extensions.getEmptyContact
import com.simplemobiletools.contacts.pro.models.Contact
import com.simplemobiletools.contacts.pro.models.ContactSource
import com.simplemobiletools.contacts.pro.models.PhoneNumber
import com.simplemobiletools.contacts.pro.models.SimContact
import java.lang.StringBuilder
import java.util.ArrayList
import java.util.Collections

class SimContactsHelper(private val context: Context, private val contactsHelper: ContactsHelper) {
    private val ICC_URI = Uri.parse("content://icc/adn")
    private val SUB_ID = "subId"
    private val ID_COLUMN = BaseColumns._ID
    private val NAME_COLUMN_GET = "name"
    private val NAME_COLUMN_INSERT = "tag"
    private val NAME_COLUMN_UPDATE = "newTag"
    private val PHONE_NUMBER_COLUMN = "number"
    private val PHONE_NUMBER_COLUMN_UPDATE = "newNumber"
    private val TAG = "SimContactsHelper"

    fun getSimContacts(subscriptionId: Int): List<SimContact> {
        return getSimContacts(getIccUri(subscriptionId))
    }

    fun getSimContacts(): List<SimContact> {
        return getSimContacts(ICC_URI)
    }

    private fun getSimContacts(uri: Uri): List<SimContact> {
        val contacts = mutableListOf<SimContact>()
        val projection = arrayOf(ID_COLUMN, NAME_COLUMN_GET, PHONE_NUMBER_COLUMN)
        context.queryCursor(uri, projection) { cursor ->
            val id = cursor.getIntValue(ID_COLUMN)
            val name = cursor.getStringValue(NAME_COLUMN_GET)
            val number = cursor.getStringValue(PHONE_NUMBER_COLUMN)
            contacts.add(SimContact(id, name, number))
        }
        return contacts
    }

    fun insertSimContact(subscriptionId: Int, contact: Contact): Boolean {
        return insertSimContact(getIccUri(subscriptionId), contact)
    }

    fun insertSimContact(contact: Contact): Boolean {
        return insertSimContact(ICC_URI, contact)
    }

    private fun insertSimContact(uri: Uri, contact: Contact): Boolean {
        Log.d(TAG, "insertSimContact: $contact")
        val contentValues = ContentValues().apply {
            put(NAME_COLUMN_INSERT, contact.firstName)
            put(PHONE_NUMBER_COLUMN, contact.firstPhoneNumber)
        }
        val resultUri = context.contentResolver.insert(uri, contentValues)

        Log.i(TAG, "insertContact sim contact: $resultUri")
        return resultUri != null
    }

    fun updateSimContact(subscriptionId: Int, contact: Contact): Boolean {
        return updateSimContact(getIccUri(subscriptionId), contact)
    }

    fun updateSimContact(contact: Contact): Boolean {
        return updateSimContact(ICC_URI, contact)
    }

    private fun updateSimContact(uri: Uri, contact: Contact): Boolean {
        val oldContact = contactsHelper.getContactWithId(contact.id, false)!!
        Log.d(TAG, "updateing sim contact from $oldContact to $contact")
        val contentValues = ContentValues().apply {
            put(NAME_COLUMN_INSERT, oldContact.getNameToDisplay())
            put(PHONE_NUMBER_COLUMN, oldContact.firstPhoneNumber ?: "")
            put(NAME_COLUMN_UPDATE, contact.getNameToDisplay())
            put(PHONE_NUMBER_COLUMN_UPDATE, contact.firstPhoneNumber ?: "")
        }
        val rows = context.contentResolver.update(uri, contentValues, null, null)
        Log.d(TAG, "updateSimContact: $rows")
        return rows > 0
    }

    fun deleteSimContact(subscriptionId: Int, contact: Contact): Boolean {
        return deleteSimContact(getIccUri(subscriptionId), contact)
    }

    fun deleteSimContact(contact: Contact): Boolean {
        return deleteSimContact(ICC_URI, contact)
    }

    private fun deleteSimContact(uri: Uri, contact: Contact): Boolean {
        Log.d(TAG, "deleteSimContact: $contact")
        val name = contact.getNameToDisplay()
        val number = contact.firstPhoneNumber
        val selection = "tag='" + name + "' AND " +
            PHONE_NUMBER_COLUMN + "='" + number + "'"
        // IccProvider doesn't use the selection args.
        val rows = context.contentResolver.delete(uri, selection, null)
        Log.i(TAG, "deleteSimContact sim contact: $rows")
        return rows > 0
    }

    private fun getIccUri(subscriptionId: Int): Uri {
        return ICC_URI.buildUpon().appendPath(SUB_ID).appendPath(subscriptionId.toString()).build()
    }

    private fun fetchContactSourcesForSimContacts(list: List<SimContact>) {
        val map = mutableMapOf<ContactSource, Set<SimContact>>()


    }

    private fun queryRawContactsForSimContacts(contacts: List<SimContact>) {
        val selectionBuilder = StringBuilder()

        var phoneCount = 0
        var nameCount = 0
        for (contact in contacts) {
            if (contact.hasPhone) {
                phoneCount++
            } else if (contact.hasName) {
                nameCount++
            }
        }

        val selectionArgs = mutableListOf<String>()

        selectionBuilder.append('(')
        selectionBuilder.append(ContactsContract.Data.MIMETYPE).append("=? AND ")
        selectionArgs.add(Phone.CONTENT_ITEM_TYPE)

        selectionBuilder.append(Phone.NUMBER).append(" IN (")
            .append(("?," * phoneCount).trimEnd(','))
            .append(')')
        for (contact in contacts) {
            if (contact.hasPhone) {
                selectionArgs.add(contact.phoneNumber)
            }
        }
        selectionBuilder.append(')')

        if (nameCount > 0) {
            selectionBuilder.append(" OR (")
            selectionBuilder.append(ContactsContract.Data.MIMETYPE).append("=? AND ")
            selectionArgs.add(CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
            selectionBuilder.append(ContactsContract.Data.DISPLAY_NAME).append(" IN (")
                .append(("?," * nameCount).trimEnd(','))
                .append(')')
            for (contact in contacts) {
                if (!contact.hasPhone && contact.hasName) {
                    selectionArgs.add(contact.name)
                }
            }
            selectionBuilder.append(')')
        }

        val projection = arrayOf(RawContacts._ID, RawContacts.ACCOUNT_NAME, RawContacts.ACCOUNT_TYPE, RawContacts.DATA_SET)
        val uri = ContactsContract.Data.CONTENT_URI.buildUpon().appendQueryParameter(ContactsContract.Data.VISIBLE_CONTACTS_ONLY, "true").build()
        context.queryCursor(uri, projection, selectionBuilder.toString(), selectionArgs.toTypedArray()) { cursor ->
            val id = cursor.getIntValue(ContactsContract.Data.RAW_CONTACT_ID)
            val number = cursor.getIntValue(ContactsContract.Data.RAW_CONTACT_ID)
        }
    }
}
