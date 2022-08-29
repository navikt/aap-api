package kafka

import no.nav.aap.kafka.streams.Table

object Tables {
    val vedtak = Table("vedtak", Topics.vedtak, false)
}
