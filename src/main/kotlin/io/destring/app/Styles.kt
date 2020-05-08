package io.destring.app

import javafx.scene.text.FontWeight
import tornadofx.*

class Styles : Stylesheet() {
    init {
        button {
            backgroundColor += c("#445fef")
            textFill = c("white")
        }
    }
}