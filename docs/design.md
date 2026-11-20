# Suunnitteludokumentti

Tämä dokumentti kuvaa projektin rakenteen, tehdyt päätökset ja nykytilan. Päivitä sitä, kun jokin muuttuu.

## Tavoite

Yksinkertainen ruutukaupunkisimulaatio, jossa suorakulmioautot liikkuvat vain neljään pääsuuntaan. Ulkoasun esikuvina ovat C64-peli Traffic ja Police Quest 1:n ajo-osuus. Kun auto ohittaa mittauspisteen, syntyy tapahtuma, jonka C#-palvelu tallentaa SQLite-tietokantaan.

Kaikki toimii paikallisesti. Pilvipalveluja tai maksullisia palveluja ei käytetä.

## Kokonaisrakenne

```
Java-simulaatio  --HTTP + JSON-->  C#-palvelu  --SQL-->  SQLite (traffic.db)
```

Tiedonkulku on yksisuuntainen: simulaatio lähettää tapahtumia, palvelu tallentaa. Simulaatio ei tiedä tietokannasta mitään. Jos palvelu ei vastaa, simulaation pitää jatkaa toimintaa ja kirjata virhe lokiin.

## Osat

| Osa | Kansio | Tekniikka |
|---|---|---|
| Simulaatio | `simulation/` | Java 25, Swing, ei Mavenia (käännös `javac`illa) |
| Palvelu | `service/` | C#, ASP.NET Core minimal API (.NET 10) |
| Tietokanta | `service/traffic.db` | SQLite, ei versionhallinnassa |

## Rajapinta

Palvelu kuuntelee osoitteessa `http://localhost:5256` (portti on tiedostossa `service/Properties/launchSettings.json`).

### POST /events

Tallentaa tapahtuman. Esimerkki:

```json
{ "tick": 1042, "pointId": 3, "carId": 17, "direction": "NORTH" }
```

- `direction` on yksi arvoista `NORTH`, `EAST`, `SOUTH`, `WEST`. Muu arvo tuottaa vastauksen 400 Bad Request.
- Onnistunut tallennus vastaa 201 Created.

### GET /events

Palauttaa 100 uusinta tapahtumaa, uusin ensin (200 OK).

## Tietokanta

Taulu `events`:

| Sarake | Tyyppi | Kuvaus |
|---|---|---|
| `id` | INTEGER, pääavain | juokseva numero |
| `tick` | INTEGER | simulaation aika tapahtumahetkellä |
| `point_id` | INTEGER | mittauspisteen tunniste |
| `car_id` | INTEGER | auton tunniste |
| `direction` | TEXT | ajosuunta pisteen ylitettäessä |
| `received_at` | TEXT | vastaanottohetki (UTC), tietokanta täyttää itse |

Taulu luodaan palvelun käynnistyessä, jos sitä ei ole. Taulun rakenteen muuttuessa vanha `traffic.db` poistetaan, ja palvelu luo uuden.

## Tehdyt päätökset

- **Pieni ensimmäinen versio.** Uusia ominaisuuksia ei lisätä ennen kuin niiden tarve on perusteltu ja päätetty.
- **Ei Mavenia vielä.** Javassa käytetään pelkkää `javac`ia, kunnes tarvitaan ulkopuolisia kirjastoja.
- **Raakaa SQL:ää, ei Entity Frameworkia.** Pidetään tallennus näkyvänä ja yksinkertaisena. SQL-lauseet käyttävät parametreja (ei merkkijonojen liittämistä) SQL-injektion estämiseksi.
- **Tick tallennetaan, simuloitu kellonaika (`sim_time`) lisätään myöhemmin.** Tietokanta on kertakäyttöinen, joten sarakkeen lisääminen on helppoa tarpeen tullen.
- **Suunnalle ei ole oletusarvoa.** Puuttuva suunta on virhe, jonka halutaan näkyä heti.
- **Kartta ja piirto erillään.** `CityMap` kuvaa kartan tietona, `CityPanel` piirtää sen.
- **Repositorio on yksityinen.** Nimi `liikennesimulaatio` on tarkoituksella suomeksi.
- **Auto liikkuu yhden ruudun per tick (ei sulavaa liikettä).** Kartan reunalla auto kiertää toiselle puolelle, väliaikainen ratkaisu.
- **Nopeus on kokonaisluku tickeinä (liikkuu joka N:s tick).** Autot käsitellään listan järjestyksessä, mikä voi jättää jonoon pieniä rakoja, ja korjaus tehdään tarvittaessa myöhemmin.”
## Ajaminen

Palvelu:

```
cd service
dotnet run
```

Simulaatio:

```
cd simulation
javac -d out -sourcepath src src/traffic/Main.java
java -cp out traffic.Main
```

Palvelun käsin testaus: `service/requests.http` (VS Coden REST Client -lisäosa).

## Tila

Valmista:

- C#-palvelu: `POST /events`, `GET /events`, SQLite-tallennus
- Java: ikkuna, joka piirtää kaupungin ruudukon (12 × 9 ruutua, kartta tekstirivinä)
- Java: kolme autoa samalla tiellä, autoilla eri nopeus (moveEveryTicks). Auto liikkuu vain, jos edessä oleva ruutu on vapaa, mistä syntyy jono.

## Seuraavat vaiheet

1. Risteykset ja liikennevalot
2. Mittauspiste tulostaa tapahtumia konsoliin
3. Tapahtumat lähetetään palvelulle ja tallentuvat tietokantaan

## Jätetty myöhemmäksi

Tilastot ja yhteenvedot, ohitukset ja kaistat, kolarit, jalankulkijat, älykkäämpi reitinvalinta, tapahtumien erälähetys, datan katseluliittymä, automaattiset testit, `sim_time`.