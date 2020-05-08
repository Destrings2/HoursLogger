package io.destring.view

import io.destring.api.Client
import io.destring.api.TailorsoftPlatform
import javafx.animation.KeyFrame
import javafx.animation.Timeline
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.event.EventHandler
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.scene.control.Alert
import javafx.scene.text.FontWeight
import javafx.util.Duration
import kong.unirest.json.JSONObject
import tornadofx.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


private val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
private fun getCurrentTime() = LocalDateTime.now().format(formatter)

private var startTime : LocalDateTime? = null
private var currentTime = SimpleStringProperty(getCurrentTime())
private val timer = Timeline(KeyFrame(Duration.seconds(1.0), EventHandler {
    currentTime.set(getCurrentTime())
})).also {
    it.cycleCount = Timeline.INDEFINITE
    it.play()
}

private var currentIssue = SimpleStringProperty()
private var logMessage = SimpleStringProperty()

private var username = SimpleStringProperty()
private var password = SimpleStringProperty()

private val api = TailorsoftPlatform()

private val client = SimpleStringProperty()
private val project = SimpleStringProperty()

private val projectList = FXCollections.observableArrayList<String>()
private var projectMap: Map<String, Any>? = null

class LoginView : View("Login") {
    override val root = pane {
        form {
            fieldset {
                field("Username") {
                    textfield(username)
                }
                field("Password") {
                    passwordfield(password)
                }
                buttonbar{
                    button("Login") {
                        action {
                            if(username.value.isNullOrBlank() && password.value.isNullOrBlank())
                                return@action
                            if(api.login(username.value, password.value))
                                replaceWith<StartView>()
                            else
                                alert(Alert.AlertType.ERROR, "Error", "Wrong password")
                        }
                    }
                }
            }
        }
    }
}

class CommitMessageView : View("Message") {
    override val root = pane {
        form {
            fieldset("Log message") {
                field("Client") {
                    combobox(client, values = FXCollections.observableArrayList(Client.values().map{it.name})) {
                        valueProperty().addListener { _, _, new ->
                            val response = api.getProjects(Client.valueOf(new))
                            if(response.parsingError.isPresent || response.body.`object`.has("error"))
                                return@addListener
                            else {
                                projectMap = (response.body.`object`["success"] as JSONObject).toMap()
                                projectList.clear()
                                projectMap?.forEach { _, v ->
                                    projectList.add(v as String)
                                }
                            }
                        }
                    }
                }
                field("Project") {
                    combobox<String>(project) {
                        items = projectList
                    }
                }
                textarea(logMessage) {
                    prefWidth = 280.0
                    maxWidth = 280.0
                    maxHeight = 250.0
                    prefHeight = 250.0
                }
            }
            buttonbar {
                button("Submit") {
                    action {
                        val response = api.logHours(startTime!!, client.value, projectMap?.filterValues { it == project.value }?.keys?.first()!!, "${currentIssue.value} | ${logMessage.value}")
                        if (response.parsingError.isPresent || response.body.`object`.has("error")){
                            alert(Alert.AlertType.WARNING, "Error", "Could not log hours, please try again")
                            return@action
                        }
                        alert(Alert.AlertType.INFORMATION, "Success", "Hours logged correctly")
                        find<RunningView>().changeBack()
                        currentStage?.close()
                    }
                }
            }
        }
    }
}

class RunningView : View("Logging Time") {
    override val root = vbox {
        vbox {
            alignment = Pos.CENTER
            label(currentTime) {
                style {
                    fontSize = 18.pt
                    fontWeight = FontWeight.BOLD
                }
            }
        }
        textflow {
            label("Working on: ") {
                style {
                    fontWeight = FontWeight.BOLD
                }
            }
            label(currentIssue)
        }
        hbox {
            alignment = Pos.CENTER
            vboxConstraints {
                marginTop = 10.0
            }
            button("Stop and sync") {
                action {
                    api.refreshSession()
                    find(CommitMessageView::class).openWindow()
                }
            }
        }
    }

    fun changeBack() {
        replaceWith<StartView>()
    }
}

class StartView : View("Time Logger") {
    override val root = vbox {
        vbox {
            alignment = Pos.CENTER
            label(currentTime) {
                style {
                    fontSize = 18.pt
                    fontWeight = FontWeight.BOLD
                }
            }
        }
        form {
            fieldset {
                field("JIRA Issue", Orientation.VERTICAL) {
                    textfield(currentIssue)
                }
                buttonbar {
                    button("Start") {
                        action {
                            startTime = LocalDateTime.now()
                            replaceWith<RunningView>()
                        }
                    }
                }
            }
        }
    }
}

class MainView : View("Time Logger") {
    override val root = borderpane {
        center<LoginView>()
    }
}