package vedtak

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import no.nav.aap.kafka.streams.Store

fun Routing.vedtak(vedtakStore: Store<IverksettVedtakKafkaDto>) {

    get("/vedtak/{personident}") {
        val personident: String = call.parameters.getOrFail("personident")

        when (val vedtak = vedtakStore[personident]) {
            null -> call.respond(HttpStatusCode.NotFound)
            else -> call.respond(HttpStatusCode.OK, vedtak)
        }
    }
}
