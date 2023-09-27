package com.kraskaska.nj.bank

import kotlinx.css.*
import kotlinx.css.properties.TextDecoration
import kotlinx.css.properties.TextDecorationLine
import kotlinx.css.properties.TextDecorationStyle

val stylesheet: CssBuilder.() -> Unit = {
    rule("*") {
        padding = Padding(0.px)
        margin = Margin(0.px)
        boxSizing = BoxSizing.borderBox
    }
    body {
        fontFamily = "Arial, Helvetica, sans-serif"
        padding = Padding(2.rem)
    }
    rule("h1, h2, h3, h4, h5, h6") {
        marginBottom = 1.rem
    }
    rule("a.action") {
        display = Display.block
    }
    rule(".danger") {
        color = Color.red
        fontWeight = FontWeight.bold
        textDecoration = TextDecoration(setOf(TextDecorationLine.underline), TextDecorationStyle.solid, Color.red)
        textTransform = TextTransform.uppercase
    }
    rule(".negative-transaction, .positive-transaction") {
        fontWeight = FontWeight.bold
    }
    rule(".negative-transaction") {
        color = Color.red
    }
    rule(".positive-transaction") {
        color = Color.blue
    }
}