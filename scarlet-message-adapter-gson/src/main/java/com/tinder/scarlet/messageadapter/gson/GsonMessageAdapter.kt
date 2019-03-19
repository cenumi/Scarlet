/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.messageadapter.gson

import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.reflect.TypeToken
import com.tinder.scarlet.Message
import com.tinder.scarlet.MessageAdapter
import com.tinder.scarlet.ws.Receive
import okio.Buffer
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.io.StringReader
import java.lang.reflect.Type
import java.nio.charset.StandardCharsets.UTF_8

/**
 * A [message adapter][MessageAdapter] that uses Gson.
 */
class GsonMessageAdapter<T> private constructor(
    private val gson: Gson,
    private val typeAdapter: TypeAdapter<T>,
    private val annotations: Array<Annotation>,
    private val key: String
) : MessageAdapter<T> {

    override fun fromMessage(message: Message): T {
        val stringValue = when (message) {
            is Message.Text -> message.value
            is Message.Bytes -> String(message.value)
        }
        val annotation = annotations.first { isReceiveAnnotation(it) } as Receive

        return if (annotation.type == ""){
            deserializeJson(stringValue)
        }else{
            deserializeJsonGivenReceiveType(stringValue,key,annotation.type)
        }
    }

    override fun toMessage(data: T): Message {
        val buffer = Buffer()
        val writer = OutputStreamWriter(buffer.outputStream(), UTF_8)
        val jsonWriter = gson.newJsonWriter(writer)
        typeAdapter.write(jsonWriter, data)
        jsonWriter.close()
        val stringValue = buffer.readByteString().utf8()
        return Message.Text(stringValue)
    }

    private fun isReceiveAnnotation(annotation: Annotation): Boolean {
        return when (annotation) {
            is Receive -> true
            else -> false
        }
    }

    private fun deserializeJson(jsonString : String) : T {
        val jsonReader = gson.newJsonReader(StringReader(jsonString))
        return typeAdapter.read(jsonReader)!!
    }

    private fun deserializeJsonGivenReceiveType(jsonString : String, key: String,type : String) : T {
        val jsonObject = JSONObject(jsonString)
        val typeFromJson = jsonObject.getString(key)

        if (typeFromJson != type) throw Exception()

        return deserializeJson(jsonString)
    }


    class Factory(
        private val gson: Gson = DEFAULT_GSON,
        private val key:String = "type"
    ) : MessageAdapter.Factory {

        override fun create(type: Type, annotations: Array<Annotation>): MessageAdapter<*> {
            val typeAdapter = gson.getAdapter(TypeToken.get(type))
            return GsonMessageAdapter(gson, typeAdapter,annotations,key)
        }

        companion object {
            private val DEFAULT_GSON = Gson()
        }
    }
}
