import io.github.cdimascio.dotenv.Dotenv
import io.github.cdimascio.dotenv.dotenv
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.html.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import kotlinx.html.*
import org.bson.types.ObjectId
import kotlin.math.roundToLong

val env = dotenv { ignoreIfMissing = true }

val httpClient = HttpClient(CIO) {
    install(ContentNegotiation) {
        json()
    }
}

class DefaultTemplate : Template<HTML> {
    val content = Placeholder<FlowContent>()
    override fun HTML.apply() {
        head {
            title("Первый новояпонский банк")
            linkToStylesheet()
        }
        body {
            insert(content)
        }
    }
}

fun main(args: Array<String>) {
    embeddedServer(Netty, port = 8080) {
        install(IgnoreTrailingSlash)
        install(Sessions) {
            cookie<DiscordSession>("user_session")
        }
        install(Authentication) {
            oauth("discord-auth") {
                // Configure oauth authentication
                urlProvider = { "${env["HOSTNAME"]}/login/callback" }
                providerLookup = {
                    OAuthServerSettings.OAuth2ServerSettings(
                        name = "discord",
                        authorizeUrl = "https://discord.com/oauth2/authorize",
                        accessTokenUrl = "https://discord.com/api/oauth2/token",
                        requestMethod = HttpMethod.Post,
                        clientId = env["DISCORD_CLIENT_ID"],
                        clientSecret = env["DISCORD_CLIENT_SECRET"],
                        defaultScopes = listOf("identify"),
                        extraAuthParameters = listOf("prompt" to "consent"),
                    )
                }
                client = httpClient
            }
            session<DiscordSession>("session-auth") {
                validate { session ->
                    val response = httpClient.get("https://discord.com/api/oauth2/@me") {
                        bearerAuth(session.token)
                    }
                    if (response.status.isSuccess()) session else {
                        null
                    }
                }
                challenge {
                    call.sessions.clear<DiscordSession>()
                    call.respondRedirect("/login")
                }
            }
        }
        routing {
            staticResources("/static", "assets")
            authenticate("discord-auth", "session-auth") {
                get("/login") {
                    call.respondRedirect("/")
                }
                get("/login/callback") {
                    val principal: OAuthAccessTokenResponse.OAuth2? = call.principal()
                    call.sessions.set(DiscordSession(principal!!.state!!, principal.accessToken))
//                    call.respond(principal.state!!)
                    call.respondRedirect("/")
                }
            }
            get("/logout") {
                call.sessions.clear<DiscordSession>()
                call.respondRedirect("/")
            }
            get("/style.css") {
                call.respondCss(stylesheet)
            }
            get("/") {
                val session = call.sessions.get<DiscordSession>()
                val user = session?.toUser()
                val loggedIn = session != null
                call.respondHtmlTemplate(DefaultTemplate()) {
                    content {
                        h1 { +"Добро пожаловать в Первый новояпонский банк" }
                        p { +"(дизайн сайта в прогрессе)" }
                        a(href = if (loggedIn) "/dashboard" else "/login", classes = "action") {
                            if (loggedIn) +"Перейти в панель управления..." else +"Войти с помощью дискорда"
                        }
                        if (loggedIn) a(href = "/logout", classes = "action") {
                            +"Выйти из аккаунта..."
                        }
                        if (loggedIn && user!!.isAdmin) {
                            a(href = "/admin", classes = "action") {
                                +"Admin"
                            }
                        }
                    }
                }
            }
            authenticate("discord-auth", "session-auth") {
                get("/admin") {
                    val session = call.sessions.get<DiscordSession>()!!
                    val user = session.toUser()
                    println("${user.id} - ${user.isAdmin}")
                    if (!user.isAdmin) {
                        call.respondHtmlTemplate(DefaultTemplate(), HttpStatusCode.Forbidden) {
                            content {
                                h1 { +"Первый новояпонский банк" }
                                img(alt = "SUS AMOGUS", src = "/static/amogus.png")
                                p { i { code { +"403 FORBIDDEN" } } }
                            }
                        }
                        return@get
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
                        }
                    }
                }
                get("/admin/deposit") {
                    val session = call.sessions.get<DiscordSession>()!!
                    val user = session.toUser()
                    println("${user.id} - ${user.isAdmin}")
                    if (!user.isAdmin) {
                        call.respondRedirect("/admin")
                        return@get
                    }
                    val address = ObjectId(call.request.queryParameters["address"]!!)
                    val amount = call.request.queryParameters["amount"]!!.toLong()
                    val account = Account.get(address)
                    if (account == null) {
                        call.respondHtmlTemplate(DefaultTemplate()) {
                            content {
                                h1 { +"Первый новояпонский банк" }
                                p { code { +"Account not found." } }
                                a(classes = "action", href = "/admin") { +"Go back" }
                            }
                        }
                        return@get
                    }
                    account.deposit(amount)
                    call.respondHtmlTemplate(DefaultTemplate()) {
                        content {
                            h1 { +"Первый новояпонский банк" }
                            p { code { +"Balance updated: +$amount (${account.balance})" } }
                        }
                    }
                }
                get("/admin/withdraw") {
                    val session = call.sessions.get<DiscordSession>()!!
                    val user = session.toUser()
                    println("${user.id} - ${user.isAdmin}")
                    if (!user.isAdmin) {
                        call.respondRedirect("/admin")
                        return@get
                    }
                    val address = ObjectId(call.request.queryParameters["address"]!!)
                    val amount = call.request.queryParameters["amount"]!!.toLong()
                    val account = Account.get(address)
                    if (account == null) {
                        call.respondHtmlTemplate(DefaultTemplate()) {
                            content {
                                h1 { +"Первый новояпонский банк" }
                                p { code { +"Account not found." } }
                                a(classes = "action", href = "/admin") { +"Go back" }
                            }
                        }
                        return@get
                    }
                    account.withdraw(amount)
                    call.respondHtmlTemplate(DefaultTemplate()) {
                        content {
                            h1 { +"Первый новояпонский банк" }
                            p { code { +"Balance updated: -$amount (${account.balance})" } }
                        }
                    }
                }
                get("/dashboard") {
                    val session = call.sessions.get<DiscordSession>()!!
                    val user = session.toUser()
                    call.respondHtmlTemplate(DefaultTemplate()) {
                        content {
                            h1 { +"Первый новояпонский банк" }
                            p { +user.name }
                            p { +"Баланс: ${"%.2f".format(user.balance / 32.0)} $okaneSymbol" }
                            a(classes = "action", href = "/dashboard/accounts") { +"Счета" }
                            a(classes = "action", href = "/dashboard/transfer") { +"Перевод" }
                            a(classes = "action", href = "/dashboard/transactions") { +"Транзакции" }
                        }
                    }
                }
                get("/dashboard/accounts") {
                    val session = call.sessions.get<DiscordSession>()!!
                    val user = session.toUser()
                    val accounts =
                        if (user.isAdmin && call.request.queryParameters["of"] != null) Account.byOwner(call.request.queryParameters["of"]!!.toLong()) else user.accounts
                    call.respondHtmlTemplate(DefaultTemplate()) {
                        content {
                            h1 { +"Первый новояпонский банк" }
                            h2 { +"Cчета" }
                            a(
                                classes = "action",
                                href = "/dashboard/accounts/new/checking${if (user.isAdmin && call.request.queryParameters["of"] != null) "?of=${call.request.queryParameters["of"]}" else ""}"
                            ) { +"Новый расчётный счёт" }
                            a(
                                classes = "action",
                                href = "/dashboard/accounts/new/savings${if (user.isAdmin && call.request.queryParameters["of"] != null) "?of=${call.request.queryParameters["of"]}" else ""}"
                            ) { +"Новый сберегательный счёт" }
                            if (accounts.toList().isEmpty()) {
                                p { +"У вас нет счетов." }
                            } else {
                                ul {
                                    accounts.forEach {
                                        li {
                                            +"${it.name} ("; code { +it._id.toHexString() }; +") - ${
                                            mapOf(
                                                "checking" to "расчётный",
                                                "savings" to "сберегательный"
                                            )[it.type.toString()]
                                        } - ${"%.2f".format(it.balance / 32.0)} $okaneSymbol "
                                            span {
                                                +"(";a(href = "/dashboard/accounts/delete/${it._id.toHexString()}") { +"Удалить" };/*+" ";a(
                                                href = "/dashboard/accounts/edit/${it._id.toHexString()}"
                                            ) { +"Переименовать" };*/+")"
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                get("/dashboard/accounts/new/{type}") {
                    val session = call.sessions.get<DiscordSession>()!!
                    val user = session.toUser()
                    if (!listOf("checking", "savings").contains(call.parameters["type"])) {
                        call.response.status(HttpStatusCode.NotFound)
                        call.respond("Not found.")
                        return@get
                    }
                    if (call.request.queryParameters["account-name"] != null) {
                        if (user.isAdmin && call.request.queryParameters["of"] != null)
                            Account.create(
                                call.request.queryParameters["of"]!!.toLong(), mapOf(
                                    "checking" to Account.AccountType.CHECKING,
                                    "savings" to Account.AccountType.SAVINGS
                                )[call.parameters["type"]]!!, call.request.queryParameters["account-name"]!!
                            )
                        else
                            user.createAccount(
                                mapOf(
                                    "checking" to Account.AccountType.CHECKING,
                                    "savings" to Account.AccountType.SAVINGS
                                )[call.parameters["type"]]!!, call.request.queryParameters["account-name"]!!
                            )
                        call.respondRedirect("/dashboard/accounts${if (call.request.queryParameters["of"] != null) "?of=${call.request.queryParameters["of"]}" else ""}")
                        return@get
                    }
                    call.respondHtmlTemplate(DefaultTemplate()) {
                        content {
                            h1 { +"Первый новояпонский банк" }
                            h2 {
                                +"Новый ${
                                    mapOf(
                                        "checking" to "расчётный",
                                        "savings" to "сберегательный"
                                    )[call.parameters["type"]]
                                } счёт"
                            }
                            form {
                                if (call.request.queryParameters["of"] != null) hiddenInput(name = "of") {
                                    value = call.request.queryParameters["of"]!!
                                }
                                p { +"Имя счёта: "; textInput(name = "account-name") }
                                submitInput { value = "Создать счёт..." }
                            }
                        }
                    }
                }
                get("/dashboard/accounts/delete/{id}") {
                    val session = call.sessions.get<DiscordSession>()!!
                    val user = session.toUser()
                    val account = Account.get(ObjectId(call.parameters["id"]!!))!!
                    if (user.id != account.ownerId && !user.isAdmin) {
                        call.respondHtmlTemplate(DefaultTemplate()) {
                            content {
                                h1 { +"Первый новояпонский банк" }
                                h2 {
                                    +"Удаление счета ${account.name} ("; code { +account._id.toHexString() };+")"
                                }
                                p { +"Ошибка: вы не владелец!" }
                            }
                        }
                        return@get
                    }
                    if (call.request.queryParameters.contains("confirm")) {
                        account.delete()
                        call.respondRedirect("/dashboard/accounts")
                        return@get
                    }
                    call.respondHtmlTemplate(DefaultTemplate()) {
                        content {
                            h1 { +"Первый новояпонский банк" }
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
                get("/dashboard/transfer") {
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
                                    h2 { +"Перевод" }
                                    p { +"Вы не владелец этого аккаунта!" }
                                    a(classes = "action", href = "/dashboard/transfer") { +"Вернуться..." }
                                }
                            }
                            return@get
                        }
                        val to = ObjectId(call.request.queryParameters["to"]!!)
                        val amount = (call.request.queryParameters["amount"]!!.toDouble() * 32.0).roundToLong()
                        from.transferTo(to, amount)
                        call.respondRedirect("/dashboard/accounts")
                        return@get
                    }
                    val availableAccounts = user.accounts.filter { it.balance > 0 }
                    call.respondHtmlTemplate(DefaultTemplate()) {
                        content {
                            h1 { +"Первый новояпонский банк" }
                            h2 { +"Перевод" }
                            form {
                                p {
                                    +"Счёт отправки: "
                                    if (user.isAdmin) textInput(name = "from") else select {
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
                                    textInput(name = "to")
                                }
                                p {
                                    +"Размер: "
                                    numberInput(name = "amount") {
                                        min = "0.03"
                                        step = ".01"
                                    }
                                    +" $okaneSymbol"
                                }
                                submitInput { value = "Отправить" }
                            }
                        }
                    }
                }
                get("/dashboard/transactions") {
                    val session = call.sessions.get<DiscordSession>()!!
                    val user = session.toUser()
                    val transactions =
                        user.accounts.map { it.transactions }.fold(listOf<Transaction>()) { acc, transactions -> acc + transactions }
                            .distinctBy { it._id.toHexString() }
                            .toList()
                    val myAddresses = user.accounts.map { it._id.toHexString() }
                    call.respondHtmlTemplate(DefaultTemplate()) {
                        content {
                            h1 { +"Первый новояпонский банк" }
                            h2 { +"Транзакции" }
                            if (transactions.isEmpty()) {
                                p { +"У вас нет транзакций." }
                            } else {
                                ul {
                                    transactions.forEach {
                                        li {
                                            if (it.from != null) {
                                                +"${Account.get(it.from)!!.name} ("
                                                code { +it.from.toHexString() }
                                                +") "
                                            }
                                            +"->"
                                            if (it.to != null) {
                                                +"${Account.get(it.to)!!.name} ("
                                                code { +it.to.toHexString() }
                                                +") "
                                            }
                                            +" - "
                                            span(
                                                classes = if(myAddresses.contains(it.from?.toHexString()) && myAddresses.contains(it.to?.toHexString())) "" else if (myAddresses.contains(it.from?.toHexString())) "negative-transaction" else if (myAddresses.contains(
                                                        it.to?.toHexString()
                                                    )
                                                ) "positive-transaction" else ""
                                            ) { +"${"%.2f".format(it.amount / 32.0)} $okaneSymbol" }
                                            if(it.message != null) +" - ${it.message}"
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }.start(wait = true)
}