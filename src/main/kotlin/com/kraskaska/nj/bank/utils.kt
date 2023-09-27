package com.kraskaska.nj.bank

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.util.pipeline.*
import kotlinx.css.CssBuilder
import kotlinx.html.*

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