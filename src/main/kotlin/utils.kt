import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import kotlinx.css.CssBuilder
import kotlinx.html.HEAD
import kotlinx.html.HTML
import kotlinx.html.link

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