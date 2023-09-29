package com.kraskaska.nj.bank.routes.dashboard.accounts

import com.kraskaska.nj.bank.*
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import kotlinx.html.*
import org.bson.types.ObjectId

val deleteAccount: RouteHandler = handler@{
    val session = call.sessions.get<DiscordSession>()!!
    val user = session.toBankUser()
    val account = Account.get(ObjectId(call.parameters["id"]!!))!!
    if (user.id != account.ownerId && !user.isAdmin) {
        call.respondHtmlTemplate(DefaultTemplate()) {
            content {
                h1 { +"Первый новояпонский банк" }
                crumbBar(
                    listOf(
                        "Панель управления" to "/dashboard",
                        "Счета" to "/dashboard/accounts",
                        "Удалить счет" to "#",
                        "Ошибка" to "#"
                    )
                )
                h2 {
                    +"Удаление счета ${account.name} ("; code { +account._id.toHexString() };+")"
                }
                p { +"Ошибка: вы не владелец!" }
            }
        }
        return@handler
    }
    if (call.request.queryParameters.contains("confirm")) {
        account.delete()
        call.respondRedirect("/dashboard/accounts")
        return@handler
    }
    call.respondHtmlTemplate(DefaultTemplate()) {
        content {
            h1 { +"Первый новояпонский банк" }
            crumbBar(
                listOf(
                    "Панель управления" to "/dashboard",
                    "Счета" to "/dashboard/accounts",
                    "Удалить счет" to "#"
                )
            )
            h2 {
                +"Удаление счета ${account.name} ("; code { +account._id.toHexString() };+")"
            }
            p { +"Вы уверены, что хотите удалить счёт?" }
            p(classes = "danger") { +"Все средства, оставшиеся на счету, будут утеряны!" }
            (List(9) { "Нет" } + listOf("Да")).shuffled().map {
                a(
                    classes = "action",
                    href = mapOf(
                        "Нет" to "/dashboard/accounts",
                        "Да" to "/dashboard/accounts/delete/${call.parameters["id"]!!}?confirm"
                    )[it]
                ) { +it }
            }
        }
    }
//                    call.respond(call.request.queryParameters.entries().map{it.toPair()}.joinToString("\n") { "${it.first}: ${it.second}" })
}