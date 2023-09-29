package com.kraskaska.nj.bank.routes.admin

import com.kraskaska.nj.bank.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.html.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import kotlinx.html.*

fun Routing.configureAdminRouting() {
    authenticate("session-auth") {
        get("/admin", adminHome)
        get("/admin/deposit", adminDeposit)
        get("/admin/withdraw", adminWithdraw)
        get("/admin/pay-interest", adminPayInterest)
        get("/admin/inspection", adminInspection)
        get("/admin/inspection/set-user-props", inspectionSetUserProps)
        get("/admin/inspection/new-account", inspectionNewAccount)
        get("/admin/inspection/delete-account", inspectionDeleteAccount)
        get("/admin/inspection/transfer", inspectionAccountTransfer)
        get("/admin/inspection/change-funds", inspectionChangeFunds)
        get("/admin/inspection/modify-account", inspectionModifyAccount)
        get("/admin/inspection/new-loan", inspectionNewLoan)
    }
}

val adminHome: RouteHandler = handler@{
    val session = call.sessions.get<DiscordSession>()!!
    val user = session.toBankUser()
    if (!user.isAdmin) {
        call.respondHtmlTemplate(DefaultTemplate(), HttpStatusCode.Forbidden) {
            content {
                h1 { +"Первый новояпонский банк" }
                img(alt = "SUS AMOGUS", src = "/static/amogus.png")
                p { i { code { +"403 FORBIDDEN" } } }
            }
        }
        return@handler
    }
    call.respondHtmlTemplate(DefaultTemplate()) {
        content {
            h1 { +"Первый новояпонский банк" }
            h2 { +"Панель управления администрации" }
            p { +"Добавить средства на аккаунт:" }
            form(action = "/admin/deposit") {
                p { +"Адрес: "; textInput(name = "address") }
                p { +"Количество (Peni, 1/32 okane): "; numberInput(name = "amount") { min = "0" } }
                submitInput { value = "Добавить" }
            }
            p { +"Снять средства с аккаунта:" }
            form(action = "/admin/deposit") {
                p { +"Адрес: "; textInput(name = "address") }
                p { +"Количество (Peni, 1/32 okane): "; numberInput(name = "amount") { min = "0" } }
                submitInput { value = "Снять" }
            }
            p { +"Выплатить процент всем сберегательным аккаунтам" }
            form(action = "/admin/pay-interest") {
                p { +"Процент (1%=0.01)"; numberInput(name = "percent") { min = "0"; step = "any" } }
                submitInput { value = "Выплатить" }
            }
            h2 { +"Inspection" }
            p { +"you give uid, i give you all the shit i know" }
            form(action = "/admin/inspection") {
                p { +"uid "; textInput(name = "uid") }
                submitInput()
            }
        }
    }
}