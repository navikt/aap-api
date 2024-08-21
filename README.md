# API for AAP-data
AAP-API tilbyr AAP-data til eksterne konsumenter som enten har hjemmel til å hente AAP-data eller et samtykke fra bruker. Denne versjonen av API’et vil kun eksponere et REST-endepunkt der konsumentene kan hente data. Hvert endepunkt vil være tilpasset det behovet den enkelte konsument har.

![img](diagram.png)

API’et vil hente data fra 2 kilder
- Arena – det eksisterende saksbehandlingsverktøyet NAV bruker til å behandle AAP-saker
- Kelvin – nytt saksbehandlingsverktøy som nå er under utvikling

Når Kelvin er ferdig vil data begynne å flyte derfra, men tanken er at dette ikke skal merkes av konsumentene.

Interndokumentasjon for datadeling fra aap kan finnes på [aap-sysdoc](https://aap-sysdoc.ansatt.nav.no/funksjonalitet/Datadeling/funksjonell).

## URL som benyttes
Base-URL for AAP-API er:
- For test: https://aap-api.ekstern.dev.nav.no/
- For prod: https://aap-api.nav.no/

## Beskrivelse av uttrekk
Beskrivelse av uttrekk kan finnes på vår [Swagger dokumentasjon](https://aap-api.ekstern.dev.nav.no/swagger).

## Tilgang
Tilgangen til AAP-API oppnås på to måter:
- Maskinporten
- Samtykke

### Tilgang med maskinporten
Konsumenter som har hjemmel til å hente AAP-data fra NAV trenger kun å benytte seg av et token utstedt av maskinporten.

For å bruke et Maskinporten-token må konsumenten ta kontakt med NAV og få registrert sitt organisasjonsnummer. Konsumenten får da tilgang til et scope.

Deretter må konsument registrere seg hos Digdir med det scope de har fått tilgang til. Detaljert beskrivelse om maskinporten finnes [på denne lenken](https://samarbeid.digdir.no/maskinporten/ta-i-bruk-maskinporten/97).

NAV har laget et [kodeeksempel](https://github.com/navikt/aap-test-token-provider/blob/main/app/main/tokenprovider/makinporten/MaskinportenTokenProvider.kt) for å utstede et test-token for Maskinporten.

### Tilgang med samtykke
Konsumenter som ikke har hjemmel til å hente AAP-data trenger samtykke fra personen man ønsker å hente data for. Dette tokenet må brukes sammen med Maskinporten-token. Samtykke-token må derfor legges inn i header på request som NAV-Samtykke-Token.

Samtykke er beskrevet [på Altinn sine sider](https://altinn.github.io/docs/utviklingsguider/samtykke/).




