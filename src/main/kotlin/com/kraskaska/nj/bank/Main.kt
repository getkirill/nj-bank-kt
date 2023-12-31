package com.kraskaska.nj.bank

import com.kraskaska.nj.bank.clock.clock
import com.kraskaska.nj.bank.routes.admin.configureAdminRouting
import com.kraskaska.nj.bank.routes.dashboard.configureDashboardRouting
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
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import kotlinx.html.*
import org.slf4j.LoggerFactory
import java.time.Clock
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import kotlin.time.toJavaDuration

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

fun Routing.configureAuth() {
    authenticate("discord-auth") {
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
}

fun main(args: Array<String>) {
    val logger = LoggerFactory.getLogger("main")
    if ((env["FORCE_CLOCK"]?.toLongOrNull() ?: -1) >= 0) {
        clock = Clock.offset(
            Clock.systemDefaultZone(),
            (env["FORCE_CLOCK"]!!.toLong() - System.currentTimeMillis()).toDuration(DurationUnit.MILLISECONDS).toJavaDuration()
        )
        logger.info("Clock forced to ${env["FORCE_CLOCK"]!!.toLong()}")
    }
    embeddedServer(Netty, port = env["PORT"].toInt()) {
        install(IgnoreTrailingSlash)
        install(Sessions) {
            cookie<DiscordSession>("user_session")
        }
        install(StatusPages) {
            exception<Throwable> { call: ApplicationCall, cause: Throwable ->
                val uuid = saveErrorForInspection(cause, call.sessions.get())
                call.respond(
                    HttpStatusCode.InternalServerError,
                    "Ой ой!\n" +
                            "Произошла непредвиденная ошибка!\n" +
                            "Обратитесь к администратору сайта.\n" +
                            "Если вас спросят, код ошибки: $uuid"
                )
                cause.printStackTrace()
            }
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
            configureAuth()
            configureAdminRouting()
            configureDashboardRouting()
            get("/style.css") {
                call.respondCss(stylesheet)
            }
            get("/") {
                val session = call.sessions.get<DiscordSession>()
                val user = session?.toBankUser()
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
            get("/debug/exception") {
                throw Exception("debug!", Exception("'cause"))
            }
        }
    }.start(wait = true)
}