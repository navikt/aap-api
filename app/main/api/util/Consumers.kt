package api.util

import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(Consumers::class.java)

object Consumers {
    private const val NAV_ORGNR = "889640782"
    private const val AFP_FELLERORDNINGEN_ORGNR = "987414502"
    private const val SPK_ORGNR = "982583462"
    private const val GABLER = "916833520"
    private const val KLP = "938708606"
    private const val STOREBRAND = "931936492"
    private const val OSLO_PENSJONSFORSIKRING = "982759412"
    private const val ARENDAL = "940380014"
    private const val DRAMMEN = "980650383"
    private const val GFF = "974652382"

    private val consumerTags = mapOf(
        NAV_ORGNR to "NAV",
        AFP_FELLERORDNINGEN_ORGNR to "fellesordningen",
        SPK_ORGNR to "SPK",
        GABLER to "Gabler",
        KLP to "KLP",
        STOREBRAND to "Storebrand",
        OSLO_PENSJONSFORSIKRING to "Oslo Pensjonsforsikring",
        ARENDAL to "Arendal Kommunale Pensjonskasse",
        DRAMMEN to "Drammen Kommunale Pensjonskasse",
        GFF to "GARANTIKASSEN FOR FISKERE"
    )

    private val behandlingsgrunnlag = mapOf(
        AFP_FELLERORDNINGEN_ORGNR to "GDPR Art. 6(1)e. AFP-tilskottsloven §17 første ledd, §29 andre ledd, første punktum. GDPR Art. 9(2)b",
        SPK_ORGNR to "GDPR Art. 6(1)e. AFP-tilskottsloven §17 første ledd, §29 andre ledd, første punktum. GDPR Art. 9(2)b",
        GABLER to "GDPR Art. 6(1)e. AFP-tilskottsloven §17 første ledd, §29 andre ledd, første punktum. GDPR Art. 9(2)b",
        KLP to "GDPR Art. 6(1)e. AFP-tilskottsloven §17 første ledd, §29 andre ledd, første punktum. GDPR Art. 9(2)b",
        STOREBRAND to "GDPR Art. 6(1)e. AFP-tilskottsloven §17 første ledd, §29 andre ledd, første punktum. GDPR Art. 9(2)b",
        OSLO_PENSJONSFORSIKRING to "GDPR Art. 6(1)e. AFP-tilskottsloven §17 første ledd, §29 andre ledd, første punktum. GDPR Art. 9(2)b",
        DRAMMEN to "GDPR Art. 6(1)e. AFP-tilskottsloven §17 første ledd, §29 andre ledd, første punktum. GDPR Art. 9(2)b",
        ARENDAL to "GDPR Art. 6(1)e. AFP-tilskottsloven §17 første ledd, §29 andre ledd, første punktum. GDPR Art. 9(2)b",
        GFF to "GDPR Art. 6(1)e. AFP-tilskottsloven §17 første ledd, §29 andre ledd, første punktum. GDPR Art. 9(2)b",

    )

    fun getConsumerTag(orgnr: String) = consumerTags[orgnr] ?: orgnr.also {
        log.warn("Prøver å lage prometheus tag for $orgnr, men den er ikke mappet opp.")
    }

    fun getBehandlingsgrunnlag(orgnr: String) = behandlingsgrunnlag[orgnr]
        ?: throw Exception("Behandlingsgrunnlag ikke funnet for $orgnr")
}
