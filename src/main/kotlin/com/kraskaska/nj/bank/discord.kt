package com.kraskaska.nj.bank

import com.mongodb.client.model.Filters
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.server.auth.*
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.bson.Document

data class DiscordSession(val state: String, val token: String) : Principal {
    fun toUser() = DiscordUser(this)
}

class DiscordUser(val session: DiscordSession) {
    val oauthMe by lazy {
        runBlocking {
            httpClient.get("https://discord.com/api/oauth2/@me") { bearerAuth(session.token) }.body<JsonObject>()
        }
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
}