package api.fellesordningen

import api.openapi.OpenApiTag
import api.arena.ArenaoppslagRestClient
import api.fellesordningenCallCounter
import api.fellesordningenCallFailedCounter
import api.sporingslogg.SporingsloggKafkaClient
import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.papsign.ktor.openapigen.route.tag
import com.papsign.ktor.openapigen.route.throws
import io.ktor.http.*
import org.slf4j.LoggerFactory
import java.lang.Exception

private val logger = LoggerFactory.getLogger("FellesordningenRoutes")

private val fellesordningen_tag = OpenApiTag("Fellesordningen", "Uttrekk for fellesordningen")

fun NormalOpenAPIRoute.fellesordningen(arenaoppslagRestClient: ArenaoppslagRestClient, sporingsloggKafkaClient: SporingsloggKafkaClient) {
    route("/fellesordning/vedtak").tag(fellesordningen_tag) {
        throws(HttpStatusCode.InternalServerError, Exception::class) {
            post<VedtakParams, VedtakResponse, VedtakRequest>(
                info(summary = "Fellesordningen - vedtak", description = "Hent ut AAP-vedtak")
            ) { params, body ->
                fellesordningenCallCounter.inc()
                runCatching {
                    arenaoppslagRestClient.hentVedtak(params.`x-callid`, body)
                }.onFailure { ex ->
                    fellesordningenCallFailedCounter.inc()
                    logger.error("Feil i kall mot hentVedtak", ex)
                    throw ex
                }.onSuccess { res ->
                    respond(res)
                }
            }
        }
    }
}
