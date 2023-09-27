package com.kraskaska.nj.bank

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.server.auth.*
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.bson.Document
import org.bson.types.ObjectId

data class DiscordSession(val state: String, val token: String) : Principal {
    fun toUser() = DiscordUser(this)
}

class DiscordUser(val session: DiscordSession) {

    val userDocument get() = runBlocking { mongoClient.getCollection<Document>("users").find(Filters.eq("uid", id)).singleOrNull() }
    var defaultAccount: ObjectId?
        get() = userDocument!!.getObjectId("default_account") ?: accounts.firstOrNull()?._id
        set(value) = runBlocking {
            mongoClient.getCollection<Document>("users").findOneAndUpdate(Filters.eq("uid", id), Updates.set("default_account", value))
        }
    val oauthMe by lazy {
        runBlocking {
            httpClient.get("https://discord.com/api/oauth2/@me") { bearerAuth(session.token) }.body<JsonObject>()
        }
    }
    init {
        val user = userDocument
        if(user == null) {
            val userDoc = Document()
            userDoc["uid"] = id
            runBlocking { mongoClient.getCollection<Document>("users").insertOne(userDoc) }
        }
//        if(defaultAccount == null && accounts.toList().isNotEmpty()) {
//            defaultAccount = accounts.first()._id
//        }
    }


    val id: Long get() = oauthMe["user"]!!.jsonObject["id"]!!.jsonPrimitive.long
    val username: String get() = oauthMe["user"]!!.jsonObject["username"]!!.jsonPrimitive.content
    val displayName: String? get() = oauthMe["user"]!!.jsonObject["global_name"]?.jsonPrimitive?.content
    val tag: Int get() = oauthMe["user"]!!.jsonObject["discriminator"]!!.jsonPrimitive.int

    val name: String get() = if (displayName != null) displayName!! else if (tag == 0) username else "${username}#${tag}"

    val accounts: Iterable<Account>
        get() = runBlocking {
            mongoClient.getCollection<Account>("accounts")
                .find(Filters.eq("ownerId", id)).toList()
        }

    val balance: CurrencyPeni get() = accounts.map { it.balance }.sum()

    fun createAccount(type: Account.AccountType, name: String) = Account.create(id, type, name)

    val isAdmin
        get() = runBlocking {
            mongoClient.getCollection<Document>("users").find(Filters.eq("uid", id)).singleOrNull()?.getBoolean("admin")
                ?: false
        }

    val loansTaken get() = accounts.map { Loan.allLinkedTo(it._id) }.flatten()
}