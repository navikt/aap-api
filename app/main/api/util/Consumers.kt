package api.util

import org.slf4j.LoggerFactory

private val secureLog = LoggerFactory.getLogger("secureLog")

object Consumers {
    private const val NAV_ORGNR = "889640782"
    private const val AFP_FELLERORDNINGEN_ORGNR = "987414502"
    private const val SPK_ORGNR = "982583462"

    private val consumerTags = mapOf(
        NAV_ORGNR to "NAV",
        AFP_FELLERORDNINGEN_ORGNR to "fellesordningen",
        SPK_ORGNR to "SPK"
    )

    private val behandlingsgrunnlag = mapOf(
        AFP_FELLERORDNINGEN_ORGNR to "GDPR Art. 6(1)e. AFP-tilskottsloven §17 første ledd, §29 andre ledd, første punktum. GDPR Art. 9(2)b",
        SPK_ORGNR to "GDPR Art. 6(1)e. AFP-tilskottsloven §17 første ledd, §29 andre ledd, første punktum. GDPR Art. 9(2)b"
    )

    fun getConsumerTag(orgnr: String) = consumerTags[orgnr] ?: orgnr.also {
        secureLog.warn("Prøver å lage prometheus tag for $orgnr, men den er ikke mappet opp.")
    }

    fun getBehandlingsgrunnlag(orgnr: String) = behandlingsgrunnlag[orgnr]
        ?: throw Exception("Behandlingsgrunnlag ikke funnet for $orgnr")
}
