package kafka

import no.nav.aap.dto.kafka.IverksettVedtakKafkaDto
import no.nav.aap.kafka.serde.json.JsonSerde
import no.nav.aap.kafka.streams.Topic

object Topics {
    val vedtak = Topic("aap.vedtak.v1", JsonSerde.jackson<IverksettVedtakKafkaDto>())
}
