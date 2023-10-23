package api.util

import io.prometheus.client.Counter

val fellesordningenCallCounter: Counter = Counter.build(
    "fellesordningen_http_call",
    "Teller HTTP-kall for fellesordningen"
).register()

val fellesordningenCallFailedCounter: Counter = Counter.build(
    "fellesordningen_http_call_failed",
    "Teller feilede HTTP-kall for fellesordningen"
).register()