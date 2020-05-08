package io.destring.api

import kong.unirest.HttpResponse
import kong.unirest.JsonNode
import kong.unirest.Unirest
import java.time.LocalDateTime

enum class Client(val num: Int) {
    BOL(37),
    Tailorsoft(9)
}

class TailorsoftPlatform {
    private var sessionId : String? = null
    private var goodName: String? = null
    private var goodPass: String? = null

    fun login(name: String, password: String): Boolean {
        val response: HttpResponse<JsonNode> = Unirest.post("https://hours.tailorsoft.co/ajax.php?p=login")
                .multiPartContent().field("u", name)
                .field("p", password)
                .asJson()

        if(!response.parsingError.isPresent || response.body.`object`.has("error")){
            goodName = name
            goodPass = password
            if(response.cookies.getNamed("PHPSESSID") != null)
                sessionId = response.cookies.getNamed("PHPSESSID").value
        }

        return !response.parsingError.isPresent || response.body.`object`.has("error")
    }

    fun getProjects(client: Client): HttpResponse<JsonNode> {
        return Unirest.get("https://hours.tailorsoft.co/ajax.php?p=getprojects&client_id=${client.num}")
                .header("Cookie", "PHPSESSID=$sessionId")
                .asJson()
    }

    fun refreshSession() {
        login(goodName!!, goodPass!!)
    }

    fun logHours(startTime: LocalDateTime, client_id: String, project_id: String, comment: String): HttpResponse<JsonNode> {
        val start = startTime.hour*3600+startTime.minute*60+startTime.second
        val current = LocalDateTime.now()
        val end = current.hour*3600+current.minute*60+current.second
        val response: HttpResponse<JsonNode> = Unirest.post("https://hours.tailorsoft.co/ajax/loghours.php")
                .header("Cookie", "PHPSESSID=$sessionId; client_id=$client_id; project_id=$project_id")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .field("y", startTime.year.toString())
                .field("m", startTime.monthValue.toString().padStart(2,'0'))
                .field("d", startTime.dayOfMonth.toString().padStart(2,'0'))
                .field("client_id", client_id)
                .field("project_id", project_id)
                .field("comment", comment)
                .field("start", start.toString())
                .field("end", end.toString())
                .asJson()

        return response
    }
}