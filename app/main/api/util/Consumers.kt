package api.util

import org.slf4j.LoggerFactory

private val secureLog = LoggerFactory.getLogger("secureLog")

object Consumers {
    private const val NAV_ORGNR = "889640782"
    private const val AFP_FELLERORDNINGEN_ORGNR = "987414502"

    private val consumerTags = mapOf(
        NAV_ORGNR to "NAV",
        AFP_FELLERORDNINGEN_ORGNR to "fellesordningen"
    )

    fun getConsumerTag(orgnr: String) = consumerTags[orgnr] ?: orgnr.also {
        secureLog.warn("Prøver å lage prometheus tag for $orgnr, men den er ikke mappet opp.")
    }
}