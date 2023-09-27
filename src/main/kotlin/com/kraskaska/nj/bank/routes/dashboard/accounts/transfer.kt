package com.kraskaska.nj.bank.routes.dashboard.accounts

import com.kraskaska.nj.bank.*
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import kotlinx.html.*
import org.bson.types.ObjectId
import kotlin.math.roundToLong

val accountTransfer: RouteHandler = handler@{
    val session = call.sessions.get<DiscordSession>()!!
    val user = session.toUser()
    if (call.request.queryParameters["from"] != null && call.request.queryParameters["from"] != "no-accounts" && call.request.queryParameters["to"] != null && call.request.queryParameters["amount"] != null) {
        val from = Account.get(ObjectId(call.request.queryParameters["from"]!!))!!
        if (
            from.ownerId != user.id && !user.isAdmin
        ) {
            call.respondHtmlTemplate(DefaultTemplate()) {
                content {
                    h1 { +"Первый новояпонский банк" }
                    crumbBar(
                        listOf(
                            "Панель управления" to "/dashboard",
                            "Перевод" to "/dashboard/transfer",
                            "Ошибка" to "#"
                        )
                    )
                    h2 { +"Перевод" }
                    p { +"Вы не владелец этого аккаунта!" }
                    a(classes = "action", href = "/dashboard/transfer") { +"Вернуться..." }
                }
            }
            return@handler
        }
        val to = ObjectId(call.request.queryParameters["to"]!!)
        val amount = (call.request.queryParameters["amount"]!!.toDouble() * 32.0).roundToLong()
        val message =
            if (!call.request.queryParameters["message"].isNullOrBlank()) call.request.queryParameters["message"] else null
        from.transferTo(to, amount, message)
        call.respondRedirect("/dashboard/accounts")
        return@handler
    }
    val availableAccounts = user.accounts.filter { it.balance > 0 }
    call.respondHtmlTemplate(DefaultTemplate()) {
        content {
            h1 { +"Первый новояпонский банк" }
            crumbBar(listOf("Панель управления" to "/dashboard", "Перевод" to "#"))
            h2 { +"Перевод" }
            form {
                p {
                    +"Счёт отправки: "
                    if (user.isAdmin) textInput(name = "from") {required = true} else select {
                        name = "from"
                        if (availableAccounts.size < 1) {
                            disabled = true
                            option {
                                value = "no-accounts"
                                +"Нет доступных счетов со средствами!"
                            }
                        }
                        availableAccounts.forEach {
                            option {
                                value = it._id.toHexString()
                                +"${it.name} - ${it._id.toHexString()} - ${"%.2f".format(it.balance / 32.0)} $okaneSymbol"
                            }
                        }
                    }
                }
                p {
                    +"Адрес доставки: "
                    textInput(name = "to") {required = true}
                }
                p {
                    +"Размер: "
                    numberInput(name = "amount") {
                        min = "0.03"
                        step = ".01"
                        required = true
                    }
                    +" $okaneSymbol"
                }
                p {
                    +"Сообщение (необязательно):"
                    br()
                    textArea { name = "message" }
                }
                submitInput { value = "Отправить" }
            }
        }
    }
}