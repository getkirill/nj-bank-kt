package com.kraskaska.nj.bank

import com.kraskaska.nj.bank.clock.clock
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.util.pipeline.*
import kotlinx.css.CssBuilder
import kotlinx.html.*
import org.slf4j.LoggerFactory
import java.io.File
import java.util.UUID

fun HTML.insertCopyright() {
    comment("Made with love and Ktor")
    comment("Copyright 2023 Kraskaska. All rights reserved.")
    comment("Zero javascript rules!")
}

suspend inline fun ApplicationCall.respondCss(builder: CssBuilder.() -> Unit) {
    this.respondText(CssBuilder().apply(builder).toString(), ContentType.Text.CSS)
}

fun HEAD.linkToStylesheet() {
    link(rel = "stylesheet", href = "/style.css")
}

fun FlowContent.crumbBar(history: List<Pair<String, String>>) {
    div { history.forEach{ a(href = it.second) {+it.first}; +" / " } }
}

typealias RouteHandler = suspend PipelineContext<Unit, ApplicationCall>.(Unit) -> Unit

fun saveErrorForInspection(throwable: Throwable, session: DiscordSession? = null): String {
    val log = LoggerFactory.getLogger("Error Inspection Helper")
    log.info("Received error, saving...")
    val folder = File("./error-reports").apply {mkdir()}
    val uuid = UUID.randomUUID().toString()
    log.info("Saving under ${uuid}.report...")
    folder.toPath().resolve("${uuid}.report").toFile().writeText("""
        |Time: ${System.currentTimeMillis()} (clock: ${clock.millis()})
        |User: ${if(session?.toUser() != null) "${session.name} (uid: ${session.toUser().id})" else "not reported/invalid"}
        |Exception log:
        ${throwable.stackTraceToString().lines().map { "|$it" }.joinToString("\n")}
    """.trimMargin("|"))
    log.info("Writing complete!")
    return uuid
}