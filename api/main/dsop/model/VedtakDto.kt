package dsop.model

import java.time.LocalDate

data class VedtakResponse(
    val periode: Periode?,
    val vedtaksliste: List<Vedtak>
)

data class Periode(
    val description: String?,
    val fomDato: LocalDate,
    val tomDato: LocalDate?
)

data class Vedtak(
    val vedtakId: String, // int32 i Arena, UUID i aap-vedtak
    val virkningsperiode: Periode,
    val vedtakstype: Vedtakstype,
    val vedtaksvariant: Vedtaksvariant?,
    val vedtaksstatus: Vedtaksstatus,
    val rettighetstype: Rettighetstype,
    val utfall: Utfall,
    val aktivitetsfase: Aktivitetsfase,
)

data class Vedtakstype(
    val description: String?,
    val kode: Kode,
    val termnavn: String? = kode.termnavn,
) {
    enum class Kode(val termnavn: String) {
        O("Ny rettighet"),
        E("Endring"),
        G("Gjenopptak"),
        S("Stans")
    }
}

data class Vedtaksvariant(
    val description: String?,
    val kode: Kode,
    val termnavn: String? = kode.termnavn
) {
    enum class Kode(val termnavn: String) {
        O_AVSLAG("Avslag på søknad"),
        O_INNV_NAV("Innvilgelse (generell)"),
        O_INNV_SOKNAD("Innvilgelse av søknad"),
        E_FORLENGE("Forlengelse"),
        E_VERDI("Endring"),
        G_AVSLAG("Avslag på søknad om gjenopptak"),
        G_INNV_NAV("Gjenopptak (av andre grunner enn etter søknad)"),
        G_INNV_SOKNAD("Innvilgelse av søknad om gjenopptak"),
        S_DOD("Stans ved dødsfall"),
        S_OPPHOR("Opphør (Endelig avslag)"),
        S_STANS("Stans")
    }
}

data class Vedtaksstatus(
    val description: String?,
    val kode: Vedtaksvariant.Kode,
    val termnavn: String? = kode.termnavn
) {
    enum class Kode(val termnavn: String) {
        AVSLU("Avsluttet"),
        IVERK("Iverksatt")
    }
}

data class Rettighetstype(
    val description: String?,
    val kode: Vedtaksvariant.Kode,
    val termnavn: String? = kode.termnavn
) {
    enum class Kode(val termnavn: String) {
        AAP("Arbeidsavklaringspenger"),
        AA115("§11-5 nedsatt arbeidsevne")
    }
}

data class Utfall(
    val description: String?,
    val kode: Vedtaksvariant.Kode,
    val termnavn: String? = kode.termnavn
) {
    enum class Kode(val termnavn: String) {
        JA("Ja"),
        NEI("Nei")
    }
}

data class Aktivitetsfase(
    val description: String?,
    val kode: Vedtaksvariant.Kode,
    val termnavn: String? = kode.termnavn
) {
    enum class Kode(val termnavn: String) {
        AU("	Arbeidsutprøving"),
        FA("	Ferdig avklart"),
        IKKE("	Ikke spesif. aktivitetsfase"),
        SPE("	Sykepengeerstatning"),
        UA("	Under arbeidsavklaring"),
        UVUP("	Vurdering for uføre")
    }
}
