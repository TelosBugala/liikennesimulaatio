# Suunnitteludokumentti

Tämä dokumentti kuvaa projektin rakenteen, tehdyt päätökset ja nykytilan. Päivitä sitä, kun jokin muuttuu.

## Tavoite

Yksinkertainen ruutukaupunkisimulaatio, jossa suorakulmioautot liikkuvat vain neljään pääsuuntaan. Autot syntyvät kartan reunalla olevalle tielle, ajavat valitsemaansa reittiä toiselle reunatielle ja katoavat siellä. Tiet ovat kaksikaistaisia, ja risteyksissä on liikennevalot. Ulkoasun esikuvina ovat C64-peli Traffic ja Police Quest 1:n ajo-osuus. Kun auto ohittaa mittauspisteen, syntyy tapahtuma, jonka C#-palvelu tallentaa SQLite-tietokantaan.

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

## Java-luokat

Kansiossa `simulation/src/traffic/`:

| Luokka | Tehtävä |
|---|---|
| `Main` | Käynnistää ikkunan ja ajastimen (tick 200 ms), kytkee tapahtumakuuntelijan |
| `CityMap` | Kartta tietona (merkkirivit) |
| `CityPanel` | Piirtää kartan, valot ja autot |
| `Simulation` | Maailman tila: autot, valot, mittauspisteet ja tick. `step()` on yksi tick |
| `Car` | Auton sijainti, suunta ja nopeus |
| `Direction` | Neljä pääsuuntaa (enum) |
| `TrafficLight` | Risteyksen valo. Tila lasketaan tickistä |
| `MeasurementPoint` | Mittauspiste (tunniste ja ruutu) |
| `PassEvent` | Tapahtuma: tick, pointId, carId, direction |
| `EventSender` | Lähettää tapahtuman palvelulle HTTP POST -pyynnöllä (osoite annetaan konstruktorissa) |


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

Toteutettu ja voimassa:

- **Pieni ensimmäinen versio.** Uusia ominaisuuksia ei lisätä ennen kuin niiden tarve on perusteltu ja päätetty.
- **Ei Mavenia vielä.** Javassa käytetään pelkkää `javac`ia, kunnes tarvitaan ulkopuolisia kirjastoja.
- **Raakaa SQL:ää, ei Entity Frameworkia.** Pidetään tallennus näkyvänä ja yksinkertaisena. SQL-lauseet käyttävät parametreja (ei merkkijonojen liittämistä) SQL-injektion estämiseksi.
- **Tick tallennetaan, simuloitu kellonaika (`sim_time`) lisätään myöhemmin.** Tietokanta on kertakäyttöinen, joten sarakkeen lisääminen on helppoa tarpeen tullen.
- **Suunnalle ei ole oletusarvoa.** Puuttuva suunta on virhe, jonka halutaan näkyä heti.
- **Kartta ja piirto erillään.** `CityMap` kuvaa kartan tietona, `Simulation` pitää kirjaa maailman tilasta ja `CityPanel` piirtää sen.
- **Auto liikkuu yhden ruudun per tick.** Liike on hyppäys ruudulta toiselle (ei sulavaa liikettä), joten mittauspisteen ylitys on helppo havaita. Tickin pituus on 200 ms (`TICK_MILLIS` tiedostossa `Main.java`).
- **Nopeus on kokonaisluku tickeinä.** Auto liikkuu joka N:s tick (`moveEveryTicks`).
- **Auto liikkuu vain, jos seuraava ruutu on vapaa.** Jono ja pysähtyminen syntyvät tästä yhdestä säännöstä. Autot käsitellään listan järjestyksessä, mikä voi jättää jonoon pieniä rakoja, ja korjaus tehdään tarvittaessa myöhemmin.
- **Valoilla ei ole omaa tilaa.** Vaihe lasketaan tickistä (`tick % 48`): vaaka vihreä 20 tickiä, kaikki punaisella 4, pysty vihreä 20, kaikki punaisella 4. Kaikki valot ovat samassa tahdissa, ja ne luodaan kartan `+`-ruuduista. Auto odottaa ruudussa ennen risteystä, jos sen suunnan valo on punainen. Piirto ja liikkumissääntö käyttävät samaa `allows`-kutsua.
- **Varaus tehdään ruudun ja suunnan parina.** Tavallisella tieruudulla auto estää vain samaan suuntaan ajavan auton, joten vastakkaiset suunnat eivät häiritse toisiaan. Risteysruudussa (`+`) on kerrallaan vain yksi auto
- **Liikenne on oikeanpuoleista.** Auto piirretään ajosuuntansa oikealle puolelle (siirtymä `LANE_OFFSET`), ja kaistojen välissä on katkoviiva.
- **Valot piirretään risteyksen kaikkiin neljään reunaan.** Vasen ja oikea reuna näyttävät vaakasuunnan värin ja ylä- ja alareuna pystysuunnan värin. `TrafficLight` erottelee vain vaaka- ja pystysuunnan.
- **Simulaatio ei tiedä tapahtumien käsittelystä.** Se kutsuu konstruktorissa annettua `Consumer<PassEvent>`-kuuntelijaa. Tapahtuma syntyy, kun auto saapuu mittauspisteen ruutuun.
- **Lähetys on asynkronista (`sendAsync`).** Hidas tai poissa oleva palvelu ei pysäytä simulaatiota. Yksi pyyntö per tapahtuma, JSON kirjoitetaan käsin. Epäonnistunut lähetys kirjataan `System.err`iin, eikä sitä yritetä uudelleen.
- **Palvelun osoite on Main.javassa oletusarvona (DEFAULT_SERVICE_URL), ja sen voi ohittaa asetuksella -Dtraffic.serviceUrl=.... .**Simulaatio ei lue palvelun launchSettings.jsonia, koska se on dotnet run -kehityskäytön tiedosto eikä osa rajapintaa.”

## Suunnitellut ratkaisut (päätetty, ei vielä toteutettu)

- **Reitinhaku.** Auto saa reitin (ruutulistan) kerran syntyessään ja seuraa sitä. Reitti haetaan leveyshaulla (BFS), joka löytää lyhimmän reitin. Käännökset määräytyvät reitistä, joten satunnaisia käännöksiä ei tehdä. Auto ei väistä ruuhkaa eikä vaihda reittiä kesken matkan.
- **Syntyminen ja katoaminen.** Kartan tiet jatkuvat reunalle kahdeksassa kohdassa (vaakateiden ja pystyteiden päät). Autoja syntyy satunnaisin väliajoin (aluksi noin 5–20 tickiä) satunnaiselle reunatielle A, ja loppupiste B valitaan satunnaisesti toiseksi reunatieksi. Jos syntymisruutu on varattu, auto odottaa seuraavaan tickiin. Auto katoaa, kun se poistuu kartalta pisteessä B.
- **Mittauspisteet kaksikaistaisella tiellä.** Piste on yksi ruutu, ja se havaitsee molempien suuntien autot. Suunta kulkee mukana tapahtumassa (`direction`).
- **Jumiutumisriski.** Kun autoja on paljon, ne voivat jonottaa ristiin niin, ettei kukaan pääse liikkeelle. Valot ja rajoitettu syntymisväli vähentävät riskiä. Jos jumiutumista esiintyy, se ratkaistaan silloin.

## Väliaikaiset ratkaisut

Nämä ovat käytössä nyt ja korvataan seuraavien vaiheiden aikana:

- **Autot ajavat vain suoraan risteyksissä.** Korvautuu reitinhaulla (vaihe 2).
- **Kartan reunalla auto kiertää toiselle puolelle.** Korvautuu syntymisellä ja katoamisella (vaihe 3).
- **Autot on kovakoodattu `Simulation`in konstruktoriin.** Korvautuu satunnaisella syntymisellä (vaihe 3).
- **Mittauspisteet on kovakoodattu `Simulation.createPoints()`-metodiin eikä niitä piirretä.** Piirto tulee vaiheessa 4, ja lisäys ja poisto hiirellä tulee myöhemmin.

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

### Portin vaihtaminen

Palvelun oletusportti on 5256 (tiedosto `service/Properties/launchSettings.json`), ja simulaatio käyttää samaa oletusta (`DEFAULT_SERVICE_URL` tiedostossa `Main.java`). Jos portti on jo käytössä, käynnistä molemmat samalla uudella portilla. Esimerkki portilla 5300:

Palvelu:

```
cd service
dotnet run --urls http://localhost:5300
```

Simulaatio:

```
cd simulation
java "-Dtraffic.serviceUrl=http://localhost:5300" -cp out traffic.Main
```

Molempien osoitteiden pitää täsmätä. Jos simulaatio ei saa yhteyttä, konsoliin tulostuu virhe, jossa näkyy osoite, johon se yritti yhdistää.

## Tila

Valmista:

- C#-palvelu: `POST /events`, `GET /events`, SQLite-tallennus
- Java: ikkuna, joka piirtää kaupungin ruudukon (12 × 9 ruutua, kartta tekstirivinä)
- Java: useita autoja, joilla on eri nopeus. Auto liikkuu vain, jos edessä oleva ruutu on vapaa, mistä syntyy jono
- Java: autoja sekä vaakatiellä (itään) että pystytiellä (etelään), ja risteyksessä vain yksi auto kerrallaan
- Java: liikennevalot kaikissa `+`-risteyksissä, piirretty risteysruudun reunaan (vaakasuunnan valo vasempaan, pystysuunnan yläreunaan)
- Java: kolme mittauspistettä, jotka tuottavat `PassEvent`-tapahtuman, kun auto saapuu pisteen ruutuun
- Java: `EventSender` lähettää tapahtumat palvelulle, ja ne tallentuvat tietokantaan. Koko ketju Java → HTTP → C# → SQLite toimii
- Java: kaksikaistaiset tiet. Autoja ajaa kaikkiin neljään suuntaan, kaistat on piirretty katkoviivalla.

## Seuraavat vaiheet

Simulaation täydentäminen valmiiksi (kukin vaihe kokeillaan ja commitoidaan erikseen):

2. **Reitinhaku ja käännökset.** Auto saa reitin ja seuraa sitä myös risteyksissä kääntyen.
3. **Syntyminen ja katoaminen reunoilla.** Kiertäminen reunasta ympäri poistuu.
4. **Mittauspisteiden piirto.** Pisteet näkyvät kartalla.

Sen jälkeen:

5. **Simulaation lisäominaisuudet.** Oma simulaatioaika (`sim_time`, nopeutus ja hidastus) sekä mittauspisteiden lisäys ja poisto hiirellä.
6. **Tilastot ja yhteenvedot, tapahtumien erälähetys, datan katseluliittymä ja automaattiset testit**, vasta kun simulaatio on valmis. Näiden keskinäistä järjestystä ei ole vielä päätetty.

## Ei tehdä

Jalankulkijat, ohitukset ja kolarit jätetään kokonaan pois.