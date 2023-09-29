package com.kraskaska.nj.bank.routes.admin

import com.kraskaska.nj.bank.Account
import com.kraskaska.nj.bank.DiscordSession
import com.kraskaska.nj.bank.RouteHandler
import com.kraskaska.nj.bank.mongoClient
import com.mongodb.client.model.Filters
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking

val adminPayInterest: RouteHandler = handler@{
    val session = call.sessions.get<DiscordSession>()!!
    val user = session.toBankUser()
    if (!user.isAdmin) {
        call.respondRedirect("/admin")
        return@handler
    }
    val percent = call.request.queryParameters["percent"]?.toFloatOrNull()
    if (percent == null) {
        call.respond("Err no percent")
        return@handler
    }
    val allSavings = runBlocking {
        mongoClient.getCollection<Account>("accounts")
            .find(Filters.eq("type", Account.AccountType.SAVINGS)).toList()
    }
    allSavings.forEach { it.deposit((it.balance * percent).toLong()) { "выплата процентов" } }
    call.respondRedirect("/admin")
}