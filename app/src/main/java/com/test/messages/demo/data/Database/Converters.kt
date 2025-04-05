package com.test.messages.demo.data.Database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {

    class Converters {
        @TypeConverter
        fun fromMessageList(messages: List<String>): String {
            return Gson().toJson(messages)
        }

        @TypeConverter
        fun toMessageList(messagesJson: String): List<String> {
            return Gson().fromJson(messagesJson, object : TypeToken<List<String>>() {}.type)
        }
    }


    @TypeConverter
    fun fromDeletedMessagesList(value: List<String>): String {
        // Serialize the list to JSON
        val gson = Gson()
        return gson.toJson(value)
    }

    @TypeConverter
    fun toDeletedMessagesList(value: String): List<String> {
        // Deserialize the JSON back into a list
        val gson = Gson()
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType)
    }
}
