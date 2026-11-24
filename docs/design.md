# Suunnitteludokumentti
 
Tämä dokumentti kuvaa projektin rakenteen, tehdyt päätökset ja nykytilan. Päivitä sitä, kun jokin muuttuu. Ajo-ohjeet ovat tiedostossa `README.md`.
 
## Tavoite
 
Yksinkertainen ruutukaupunkisimulaatio, jossa suorakulmioautot liikkuvat vain neljään pääsuuntaan. Autot syntyvät kartan reunalla olevalle tielle, ajavat valitsemaansa reittiä toiselle reunatielle ja katoavat siellä. Tiet ovat kaksikaistaisia, ja risteyksissä on liikennevalot. Ulkoasun esikuvina ovat C64-peli Traffic ja Police Quest 1:n ajo-osuus. Kun auto ohittaa mittauspisteen, syntyy tapahtuma, jonka C#-palvelu tallentaa SQLite-tietokantaan.
 
Simulaatiolla on oma aika, jonka nopeutta voi säätää, ja nimettyjä mittauspisteitä voi lisätä, nimetä uudelleen ja poistaa ohjelman käydessä. Tallennetusta datasta lasketaan tilastoja, joita katsellaan palvelun tarjoamalta selainsivulta (vaihe 6).
 
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
| Tietokanta | `service/traffic.db` (sekä `-wal` ja `-shm`) | SQLite, ei versionhallinnassa |
| Palvelun testit | `service.Tests/` | C#, xUnit, WebApplicationFactory |

## Java-luokat
 
Kansiossa `simulation/src/traffic/`:
 
| Luokka | Tehtävä |
|---|---|
| `Main` | Käynnistää ikkunan (yläpalkki ja kartta), lataa mittauspisteet, kytkee tapahtumakuuntelijan ja pisteiden tallennuksen sekä käynnistää `SimulationRunner`in |
| `CityMap` | Kartta tietona (merkkirivit), tiesäännöt (`isInside`, `isRoad`, `roadAllows`) ja ruudun pituus metreinä |
| `CityPanel` | Piirtää kartan, mittauspisteet, valot, autot ja pisteiden nimet, ja välittää hiiren klikkaukset `PointEditor`ille |
| `Simulation` | Maailman tila: autot, valot, mittauspisteet ja syntymispisteet. `step()` on yksi tick: autojen liike ja uuden auton syntyminen. Hallinnoi mittauspisteitä (`addPoint`, `renamePoint`, `removePoint`, `pointAt`, `restorePoints`) ja ilmoittaa niiden muutoksista kuuntelijalle |
| `Car` | Auton sijainti, viimeisin ajosuunta, nopeus (`moveEveryTicks`, nimellisnopeus km/h) ja reitti (lista suuntia) |
| `Direction` | Neljä pääsuuntaa (enum) |
| `TrafficLight` | Risteyksen valo. Tila lasketaan tickistä |
| `SpawnPoint` | Syntymispiste: ruutu ja ajosuunta |
| `RoutePlanner` | Laskee lyhimmän reitin kahden ruudun välillä leveyshaulla ja palauttaa suuntalistan. Tasapelit ratkaistaan arpomalla |
| `MeasurementPoint` | Mittauspiste (tunniste, nimi ja ruutu). Nimi validoidaan konstruktorissa |
| `PassEvent` | Tapahtuma: tick, simTime, pointId, pointName, carId, direction, speedKmh |
| `EventSender` | Lähettää tapahtuman palvelulle HTTP POST -pyynnöllä (osoite annetaan konstruktorissa) |
| `Debug` | Debug-lokitus (`-Dtraffic.debug=true`) |
| `SimTime` | Laskee simulaatioajan tickistä (alkuaika ja tickin pituus) |
| `ControlBar` | Ikkunan yläpalkki: Pause, Slower, Faster, nopeuden näyttö ja kello |
| `SimulationRunner` | Omistaa ajastimen, nopeuden ja taukotilan (`setPaused`): 1x on tick 200 ms, nopeudet 0,5x–32x. Suurilla nopeuksilla ajaa useamman tickin per ajastinkutsu |
| `PointEditor` | Mittauspisteiden lisäys, nimeäminen ja poisto hiirellä. Pysäyttää simulaation dialogin ajaksi |
| `PointStore` | Lukee ja tallentaa mittauspisteet tiedostoon `measurement-points.txt` |
 
## Rajapinta
 
Palvelu kuuntelee osoitteessa `http://localhost:5256` (portti on tiedostossa `service/Properties/launchSettings.json`).
 
### POST /events
 
Tallentaa tapahtuman. Esimerkki:

json
{ "tick": 37, "simTime": "2026-01-05T06:03:05", "pointId": 1, "pointName": "Lehtotie", "carId": 2, "direction": "EAST", "speedKmh": 50 }

Kaikki kentät ovat pakollisia. Palvelu tarkistaa ne itse ja palauttaa 400 Bad Request, jos jokin ei kelpaa. Vastauksessa on kaikki virheet kerralla: { "errors": [ ... ] }.

tick on vähintään 0, pointId ja carId vähintään 1.
simTime on muodossa yyyy-MM-ddTHH:mm:ss ja oikea päivämäärä.
pointName on 1–30 merkkiä (kirjaimia, numeroita, välilyöntejä ja tavuviivoja), ei välilyöntiä alussa tai lopussa.
direction on NORTH, EAST, SOUTH tai WEST.

Onnistunut tallennus vastaa 201 Created.
### GET /events
 
Palauttaa 100 uusinta tapahtumaa, uusin ensin (200 OK).
 
Vastauksessa on kaikki tallennetut kentät.
 
### GET /stats/points

Palauttaa pistekohtaisen yhteenvedon: yksi rivi jokaista mittauspistettä (pointId) kohden, järjestettynä pointId:n mukaan (200 OK). Esimerkki:

json
[
  { "pointId": 1, "pointName": "Lehtotie", "passCount": 412, "averageSpeedKmh": 31.4,
    "directions": { "north": 12, "east": 200, "south": 8, "west": 192 } }
]

pointName on pisteen uusin nimi simulaatioajan mukaan. passCount on ohitusten määrä. averageSpeedKmh on ohitusten nimellisnopeuksien keskiarvo yhden desimaalin tarkkuudella. directions kertoo ohitusten määrän suunnittain (0, jos suuntaan ei ole ohituksia). Tyhjä tietokanta palauttaa tyhjän listan. Piste, jonka ohi ei ole ajanut yksikään auto, ei näy, koska palvelu tuntee pisteet vain tapahtumista.

### GET /stats/hourly

Palauttaa ohitusmäärät pisteittäin ja tunneittain: yksi rivi jokaista pisteen ja tasatunnin yhdistelmää kohden (200 OK), järjestettynä pointId:n ja hourStart:in mukaan. Esimerkki:

json
[
  { "pointId": 1, "pointName": "Lehtotie", "hourStart": "2026-01-05T06:00:00", "passCount": 37 }
]

hourStart on tunnin alku simulaatioaikana (ISO 8601, ilman aikavyöhykettä). Eri päivien samat kellonajat ovat eri rivejä. Tunnit, joissa ei ollut ohituksia, eivät ole mukana. pointName on sama uusin nimi kuin pistekohtaisessa yhteenvedossa.

## Tietokanta
 
Taulu `events`:
 
| Sarake | Tyyppi | Kuvaus |
|---|---|---|
| `id` | INTEGER, pääavain | juokseva numero |
| `tick` | INTEGER | simulaation aika tapahtumahetkellä |
| `sim_time` | TEXT | simulaatioaika, ISO 8601 (esim. 2026-01-05T06:03:05) |
| `point_id` | INTEGER | mittauspisteen tunniste |
| `point_name` | TEXT | pisteen nimi tapahtumahetkellä |
| `car_id` | INTEGER | auton tunniste |
| `direction` | TEXT | ajosuunta pisteen ylitettäessä |
| `speed_kmh` | TEXT | INTEGER | auton nimellisnopeus |
| `received_at` | TEXT | vastaanottohetki (UTC), tietokanta täyttää itse |

Indeksi idx_events_point_time on sarakkeille (point_id, sim_time). Se palvelee suunniteltuja tilastokyselyjä. Sen riittävyys on mitattu vaiheessa 6C (ks. Tehdyt päätökset → Tilastot), ja lisäindeksejä ei tarvita.
 
Taulu luodaan palvelun käynnistyessä, jos sitä ei ole. Taulun rakenteen muuttuessa vanha tietokanta poistetaan (kaikki kolme tiedostoa: `traffic.db`, `traffic.db-wal` ja `traffic.db-shm`, kun palvelu ei ole käynnissä), ja palvelu luo uuden.
 
## Tehdyt päätökset
 
### Yleiset
 
- **Pieni ensimmäinen versio.** Uusia ominaisuuksia ei lisätä ennen kuin niiden tarve on perusteltu ja päätetty.
- **Ei Mavenia vielä.** Javassa käytetään pelkkää `javac`ia, kunnes tarvitaan ulkopuolisia kirjastoja.
### Tallennus ja rajapinta
 
- **Raakaa SQL:ää, ei Entity Frameworkia.** Pidetään tallennus näkyvänä ja yksinkertaisena. SQL-lauseet käyttävät parametreja (ei merkkijonojen liittämistä) SQL-injektion estämiseksi.
- **Tick ja simulaatioaika kulkevat tapahtumassa mukana ja tallentuvat.** Simulaatio lähettää sekä tickin että simulaatioajan (simTime), ja palvelu tallentaa molemmat (tick, sim_time). sim_time on tekstinä ISO 8601 -muodossa, jolloin aakkosjärjestys on aikajärjestys ja SQLiten aikafunktiot (strftime) ymmärtävät sen.
- **Suunnalle ei ole oletusarvoa.** Puuttuva suunta on virhe, jonka halutaan näkyä heti.
- **Aikaleima on ISO 8601 -muodossa, sekunnin tarkkuudella ja ilman aikavyöhykettä** (esimerkiksi `2026-01-05T08:15:30`). Se on simuloidun maailman paikallinen aika. JSON-aikaleima muotoillaan `DateTimeFormatter.ISO_LOCAL_DATE_TIME`illa, jotta sekunnit ovat aina mukana (`LocalDateTime.toString()` jättää ne pois, kun ne ovat nolla).
- **SQLite käyttää WAL-tilaa ja `synchronous = NORMAL`-asetusta.** Yksi tapahtuma per pyyntö ja SQLiten oletustila (rollback journal) rajoittivat kirjoitusnopeutta niin, että simulaation nopeudella 16x pyynnöt ylittivät aikakatkaisun. WAL-tila (`PRAGMA journal_mode = WAL`) asetetaan tietokantaan kerran käynnistyksessä, ja `synchronous = NORMAL` jokaiselle yhteydelle (`OpenConnection`). Hinta: äkillinen sähkökatko voi hävittää viimeisimmät kirjoitukset, mutta tietokanta ei rikkoudu.
- **Palvelu validoi kaiken itse.** Palvelu ei luota lähettäjään vaan tarkistaa jokaisen kentän (rajat ylhäällä kohdassa Rajapinta), vaikka simulaatio validoi pisteen nimen jo itse. Puuttuvat kentät ovat virhe, myös vanhan muotoisissa pyynnöissä, koska siitä halutaan näkyvän heti. Kentät on määritelty nullable-tyyppisinä (int?, string?), jotta puuttuva kenttä erottuu arvosta 0.
- **Kaikki virheet palautetaan kerralla.** ({ "errors": [ ... ] }), koska se nopeuttaa kokeilua ja vika löytyy yhdellä pyynnöllä.
- **Pisteen nimi tallennetaan jokaiseen tapahtumaan.** Nimeä voi muuttaa kesken ajon, joten tapahtuma kertoo nimen sellaisena kuin se oli tapahtumahetkellä. Tilastoissa pisteen avain on pointId, ei nimi.
- **Ajot erotellaan nollaamalla tietokanta käsin.** Simulaatio alkaa joka käynnistyksellä alusta (tick 0, carId 1, sama alkuaika), joten usean ajon data sekoittuisi. Tietokanta poistetaan ajojen välillä (palvelu pysäytettynä, kolme tiedostoa). Ajotunniste (runId) tapahtumaan jätettiin pois, koska se vaatisi muutoksen Javaan ja tapahtuman muotoon. Sen voi lisätä myöhemmin omana vaiheenaan.
### Simulaation rakenne
 
- **Kartta ja piirto erillään.** `CityMap` kuvaa kartan tietona, `Simulation` pitää kirjaa maailman tilasta ja `CityPanel` piirtää sen.
- **Auto liikkuu yhden ruudun per tick.** Liike on hyppäys ruudulta toiselle (ei sulavaa liikettä), joten mittauspisteen ylitys on helppo havaita. Tickin pituus on 1x-nopeudella 200 ms (`BASE_TICK_MILLIS` tiedostossa `SimulationRunner.java`).
- **Nopeus on kokonaisluku tickeinä.** Auto liikkuu joka N:s tick (`moveEveryTicks`). Arvo arvotaan auton syntyessä: auto liikkuu joka, joka toinen tai joka kolmas tick.
- **Simulaatioaika lasketaan tickistä.** `SimTime.at(tick)` palauttaa ajan, joka on `tick × SECONDS_PER_TICK` sekuntia alkuajan jälkeen. Yksi tick on 5 sekuntia, ja alkuaika on maanantai 2026-01-05 06:00. Erillistä kelloa ei ole, joten nopeudensäätö ei vaikuta dataan: sama ajo tuottaa samat aikaleimat millä nopeudella tahansa. Kello näkyy yläpalkissa (`ControlBar`) muodossa `Mon 2026-01-05 06:00:00`.
- **Nopeudensäätö.** Yläpalkin painikkeet Slower, Faster ja Pause. Nopeudet ovat 0,5x–32x. Ajastin ei käy alle 25 ms:n välein: kun nopeus vaatisi tiheämpää, ajetaan useampi tick yhdellä ajastinkutsulla (`stepsPerFrame`) ja piirretään vasta sen jälkeen. `SimulationRunner` omistaa ajastimen ja nopeuden, ja `ControlBar` vain kutsuu sen metodeja.
### Liikenne
 
- **Autot syntyvät kartan reunoilla ja poistuvat määränpäässään.** Syntymispisteet löytyvät kartasta automaattisesti: reunalla oleva tieruutu, jonka suunta on reunasta sisäänpäin (nykyisellä kartalla kahdeksan). Syntymisväli on satunnainen (5–20 tickiä), piste arvotaan, ja jos kaista on varattu, syntyminen odottaa seuraavaan tickiin. Auto poistuu, kun se on ajanut reittinsä loppuun. Jokainen auto saa juoksevan tunnisteen.
- **Auto saa reitin syntyessään.** Määränpää on jokin muu syntymispiste, ja reitti lasketaan kerran `RoutePlanner`illa. Reittiä ei muuteta kesken matkan, eikä auto väistä ruuhkaa. Käännökset tulevat reitistä. Auton `direction` on viimeisin ajosuunta, ja käännös näkyy kaistan vaihtumisena risteyksen jälkeen.
- **Reitti haetaan leveyshaulla (BFS) ja se on lista suuntia.** Naapureiden järjestys arvotaan joka haulla, joten samanpituisista reiteistä valitaan satunnainen. Reitittömyys palautetaan arvona `null`.
- **Liikenne on oikeanpuoleista.** Auto ajaa ajosuuntansa oikealla puolella ja piirretään sinne (siirtymä `LANE_OFFSET`). Kaistojen välissä on katkoviiva.
- **Varaus tehdään ruudun ja suunnan parina.** Tavallisella tieruudulla auto estää vain samaan suuntaan ajavan auton, joten vastakkaiset suunnat eivät häiritse toisiaan. Risteysruudussa (`+`) on kerrallaan vain yksi auto.
- **Auto liikkuu vain, jos seuraava ruutu on vapaa.** Jono ja pysähtyminen syntyvät tästä yhdestä säännöstä. Autot käsitellään listan järjestyksessä, mikä voi jättää jonoon pieniä rakoja, ja korjaus tehdään tarvittaessa myöhemmin.
- **Älä tuki risteystä.** Auto ajaa risteykseen vain, jos ruutu, johon se risteyksestä jatkaa (reitin mukaan), on vapaa.
- **Valoilla ei ole omaa tilaa.** Vaihe lasketaan tickistä (`tick % 48`): vaaka vihreä 20 tickiä, kaikki punaisella 4, pysty vihreä 20, kaikki punaisella 4. Kaikki valot ovat samassa tahdissa, ja ne luodaan kartan `+`-ruuduista. Auto odottaa ruudussa ennen risteystä, jos sen suunnan valo on punainen. Piirto ja liikkumissääntö käyttävät samaa `allows`-kutsua.
### Piirto
 
- **Valot piirretään risteyksen kaikkiin neljään reunaan.** Vasen ja oikea reuna näyttävät vaakasuunnan värin ja ylä- ja alareuna pystysuunnan värin. `TrafficLight` erottelee vain vaaka- ja pystysuunnan.
- **Mittauspiste piirretään viivana tien poikki.** Viiva on ruudun keskellä, ja se kattaa molemmat kaistat. Pisteen nimi näkyy tekstinä ruudun kulmassa tummalla taustalla. Nimet piirretään omassa vaiheessaan autojen ja valojen päälle, jotta ne pysyvät luettavina. Risteykseen tai muulle ruudulle asetettu piste piirretään kehyksenä. Piirto lukee pisteet simulaatiolta joka kerta, joten lisättävät ja poistettavat pisteet näkyvät automaattisesti.
### Mittauspisteiden muokkaus ja tallennus
 
- **Hiirellä muokkaus.** Vasen klikkaus tieruutuun (`-`, `|` tai `+`) lisää pisteen ja kysyy nimen dialogilla. Vasen klikkaus olemassa olevan pisteen ruutuun avaa nimen muokkauksen, ja oikea klikkaus poistaa pisteen (ei vahvistusta). Nurmelle ei voi asettaa pistettä, ja ruudussa on enintään yksi piste. Kartan työkaluvihje kertoo käytön.
- **Simulaatio pysähtyy dialogin ajaksi.** `PointEditor` pysäyttää `SimulationRunner`in dialogin ajaksi ja palauttaa sen aiempaan tilaan, joten jo pysäytetty simulaatio pysyy pysäytettynä.
- **Pisteen tunnistetta ei käytetä uudelleen.** `Simulation` jakaa tunnisteet juoksevalla laskurilla (`nextPointId`), joka ei pienene poiston jälkeen. Nimen vaihtaminen säilyttää tunnisteen (`renamePoint` korvaa muuttumattoman `MeasurementPoint`-recordin uudella).
- **Muokkaus tapahtuu Swingin tapahtumasäikeessä**, jossa myös simulaatio ajetaan ja piirto tehdään, joten pisteitä ei tarvitse suojata lukituksilla. `Simulation.points()` palauttaa vain luettavan näkymän, ja muutokset tehdään vain metodeilla.
- **Pisteet tallennetaan tiedostoon.** `PointStore` kirjoittaa pisteet tiedostoon `measurement-points.txt` (ohjelman käynnistyskansio, eli käytännössä `simulation/`) jokaisen muutoksen jälkeen ja lataa ne käynnistyksessä. Tiedosto on `.gitignore`ssa. Rivin muoto on `id;x;y;nimi`, ja ensimmäisellä rivillä on seuraava vapaa tunniste (`nextId=…`), jotta poistettu tunniste ei tule uudelleen käyttöön käynnistyksen jälkeen.
- **Tallennus on turvallinen, ja lataus sietää virheet.** Tiedosto kirjoitetaan väliaikaiseen tiedostoon ja siirretään paikalleen yhdellä toimenpiteellä, joten kaatuminen kesken kirjoituksen ei riko vanhaa tiedostoa. Jos tiedostoa ei ole, käytetään oletuspisteitä (Lehtotie, Koivutie ja Kuusitie) ja tiedosto luodaan. Jos tiedosto on rikki, tulostetaan virhe ja käytetään oletuspisteitä (seuraava muutos korvaa rikkinäisen tiedoston). Kelvottomat pisteet (nurmella, kartan ulkopuolella tai samassa ruudussa toisen pisteen kanssa) ohitetaan varoituksella.
- **Simulaatio ei tiedä tiedostoista.** Se ilmoittaa pisteiden muutoksista kuuntelijalle (`setPointsChangedListener`), ja `Main` kytkee kuuntelijaksi tallennuksen. Näin kaikki muutokset tallentuvat riippumatta siitä, mistä ne tulevat.
### Tapahtumat ja yhteys palveluun
 
- **Mittauspiste on yksi ruutu, ja se havaitsee molempien suuntien autot.** Suunta kulkee mukana tapahtumassa (`direction`). Tapahtuma syntyy, kun auto saapuu pisteen ruutuun.
- **Simulaatio ei tiedä tapahtumien käsittelystä.** Se kutsuu konstruktorissa annettua `Consumer<PassEvent>`-kuuntelijaa.
- **Lähetys on asynkronista (`sendAsync`).** Hidas tai poissa oleva palvelu ei pysäytä simulaatiota. Yksi pyyntö per tapahtuma, JSON kirjoitetaan käsin. Epäonnistunut lähetys kirjataan `System.err`iin, eikä sitä yritetä uudelleen.
- **Palvelun osoite on `Main.java`ssa oletusarvona (`DEFAULT_SERVICE_URL`), ja sen voi ohittaa asetuksella `-Dtraffic.serviceUrl=...`.** Simulaatio ei lue palvelun `launchSettings.json`ia, koska se on `dotnet run` -kehityskäytön tiedosto eikä osa rajapintaa.
- **Konsoli on tavallisessa ajossa hiljainen, ja debug-tila avaa lisätulostuksen.** Tavallisessa ajossa tulostuvat vain virheet (`System.err`) ja käynnistysrivit (palvelun osoite ja mittauspisteiden tiedosto). Asetuksella `-Dtraffic.debug=true` tulostetaan lisäksi autojen reitit ja palvelulle lähetettävät tapahtumat. Tulostus kulkee `Debug.log`in kautta, ja `EventSender` lokittaa lähettämänsä JSON:n.
- **Mittauspisteellä on nimi.** Tunniste (`id`) on avain, eikä sitä käytetä uudelleen. Nimi on 1–30 merkkiä (kirjaimia, numeroita, välilyöntejä ja tavuviivoja) ilman välilyöntejä alussa tai lopussa. Nimi validoidaan `MeasurementPoint`in konstruktorissa, joten virheellistä nimeä ei voi syntyä. Siksi JSON-kenttään ei tarvita pakotusta.
- **Tapahtuman muoto.** Tapahtuma sisältää kentät `tick`, `simTime`, `pointId`, `pointName`, `carId`, `direction` ja `speedKmh`. Auton lähtö- ja määränpäätä ei lähetetä, koska oikeassa mittauksessa niitä ei tunnettaisi. Palvelu tallentaa kaikki kentät.
- **Nopeus tapahtumassa on auton nimellisnopeus (km/h).** Se lasketaan `moveEveryTicks`ista, tickin pituudesta ja ruudun pituudesta (70 m, `CityMap.TILE_LENGTH_METERS`). Arvot ovat 50, 25 ja 17 km/h. Se ei kuvaa jonossa seisomista.

### Tilastot

- **Tilastot lasketaan palvelussa SQL-kyselyillä.** Laskenta tehdään `GROUP BY`-ryhmittelyllä eikä katseluliittymässä. Kyselyt ovat tiedostossa `Statistics.cs`, ja luokka saa valmiin tietokantayhteyden parametrina. Tilastot käyttävät simulaatioaikaa (`sim_time`), eivät vastaanottoaikaa (`received_at`), koska simulaation nopeus vaihtelee.
- **Pisteen nimi on uusin nimi simulaatioajan mukaan.** Nimi valitaan järjestyksellä `ORDER BY sim_time DESC, id DESC`, ei saapumisjärjestyksen mukaan, koska tapahtumat lähetetään asynkronisesti ja ne voivat saapua väärässä järjestyksessä. Samalla ajalla myöhemmin tallennettu nimi voittaa. Nimen hakuun käytetään yhteistä kyselyn osaa, joten molemmissa tilastoissa nimi valitaan samalla säännöllä. Koska palvelu tuntee nimet vain tapahtumista, uusi nimi näkyy tilastoissa vasta, kun pisteen ohi ajaa auto nimenvaihdon jälkeen.
- **Tunnit jaotellaan tasatunnin ja päivämäärän mukaan.** Rivin avain on `hourStart` (esim. `2026-01-05T06:00:00`). Kellonajan mukaan yhdistäminen (kaikki päivät yhteen) onnistuu jälkikäteen datasta, mutta päinvastoin ei, joten tarkempi jaottelu valittiin. Tunteja, joissa ei ollut ohituksia, ei palauteta. Vaiheessa 6D katseluliittymä täyttää ne nollilla.
- **Keskinopeus on ohitusten nimellisnopeuksien keskiarvo.** Se pyöristetään yhteen desimaaliin SQL:ssä.
- **Indeksin riittävyys mitattiin.** Kyselyt ajettiin väliaikaisella testillä 200 000 tekaistulla tapahtumalla (3 pistettä ja noin 28 tuntia simulaatioaikaa, eli 84 pisteen ja tunnin yhdistelmää), ja testi poistettiin mittauksen jälkeen. Indeksin `(point_id, sim_time)` kanssa pistekohtainen yhteenveto kesti noin 90–100 ms ja tuntikohtainen noin 160–190 ms. Ilman indeksiä ajat olivat noin 140–150 ms ja noin 2,1–2,3 s. Ero on suurin tuntikyselyssä ja tulee pisteen nimen alikyselystä: indeksin kanssa se hakee pisteen rivit suoraan (`SEARCH ... point_id=?`), ilman indeksiä se lukee koko taulun jokaiselle ryhmälle erikseen (tuntikyselyssä 84 kertaa). **Päätös: nykyinen indeksi riittää, eikä lisäindeksejä tehdä.** Jokainen lisäindeksi hidastaisi jokaista kirjoitusta (palvelu vastaanottaa kymmeniä tapahtumia sekunnissa nopeudella 32x), eikä tarve ole osoitettu. Mittaus on tehty tekaistulla datalla yhdellä koneella ilman rinnakkaisia pyyntöjä.

### Testit

- **Testit ajetaan HTTP:n kautta.** Testi käynnistää palvelun muistissa (`WebApplicationFactory<Program>`, ei oikeaa porttia), lähettää pyyntöjä ja tarkistaa vastaukset. Näin testataan koko ketju (validointi, SQLite, vastaus) samalla tavalla kuin simulaatio sitä käyttää. Validointi ei ole erillisessä luokassa, koska erillinen yksikkötestaus ei tuonut tarpeeseen nähden lisäarvoa.
- **Jokainen testi saa oman väliaikaisen tietokantatiedoston.** Tietokannan sijainti tulee asetuksesta `ConnectionStrings:Traffic`, ja oletuksena on `Data Source=traffic.db`. Testit antavat asetukselle väliaikaisen tiedoston, joka poistetaan testin jälkeen (myös `-wal` ja `-shm`). Asetus luetaan vasta `Build()`-kutsun jälkeen, jotta testien antama arvo ehtii mukaan. Näin testit eivät koske oikeaan tietokantaan eivätkä toisiinsa.
- **`Program`-luokka on julkinen.** Tiedoston lopussa oleva rivi `public partial class Program { }` tekee luokan näkyväksi testiprojektille.
- **Testiprojekti on `service.Tests/`, `service/`-kansion vieressä.** Se ei voi olla `service/`:n sisällä, koska palveluprojekti kääntäisi silloin testitiedostot mukaansa. Solution-tiedostoa (`.sln`) ei käytetä.
- **`Microsoft.AspNetCore.Mvc.Testing` on versiossa `10.*`.** NuGetin uusin vakaa versio (11.0.0) tukee vain .NET 11:tä eikä sovi tähän projektiin.
- **Testit ajetaan komennolla `dotnet test` kansiossa `service.Tests/`.** Palvelu pitää pysäyttää ensin, koska muuten käynnissä oleva palvelu lukitsee käännettävän tiedoston.
- **Testit tarkistavat virheilmoitusten tarkan tekstin.** Siksi ilmoitusta muutettaessa testi pitää päivittää.

## Suunnitellut ratkaisut (vaihe 6D–6E, päätetty, ei vielä toteutettu))
 
- **Katseluliittymä on yksi HTML-sivu**, jota palvelu tarjoaa osoitteessa `/`. Se käyttää pelkkää JavaScriptiä ja SVG:tä, ei ulkoisia kirjastoja eikä verkkopalveluja.
- **Tilastot olettavat, että tietokanta sisältää yhden ajon.** Tietokanta nollataan ajojen välillä (ks. Tehdyt päätökset).
- **Vuorokausirytmi tehdään viimeisenä.** Nyt syntymisväli on tasainen, joten tuntikohtainen kaavio on aluksi tasainen. Ruuhka-ajat lisätään simulaatioon vasta, kun tilastot on nähty (vaihe 6E).
## Tunnetut rajoitukset
 
- **Ajojen sekoittuminen.** Jos tietokantaa ei nollata ajojen välillä, eri ajojen tapahtumat sekoittuvat samoille simulaatioajoille ja carId:t toistuvat, eikä palvelu huomaa sitä.
- **Tilastokyselyt lukevat koko taulun,** joten niiden kesto kasvaa suunnilleen rivimäärän mukana (200 000 rivillä alle 0,2 s). Tuloksia ei välimuistiteta. Jos data kasvaa moninkertaiseksi, ratkaisuksi tulee silloin harkita esimerkiksi esilaskettuja yhteenvetoja.
- **Jumiutumisriski.** Kun autoja on paljon, ne voivat jonottaa ristiin niin, ettei kukaan pääse liikkeelle. Valot, rajoitettu syntymisväli ja "älä tuki risteystä" -sääntö vähentävät riskiä. Jos jumiutumista esiintyy, se ratkaistaan silloin.
- **Nopeus tapahtumassa on nimellisnopeus, ei hetkellisnopeus.** Arvoja on vain kolme (50, 25 ja 17 km/h), joten keskinopeudet ovat sekoitus näistä.
## Väliaikaiset ratkaisut
 
Ei väliaikaisia ratkaisuja.
 
## Ajaminen
 
Palvelun ja simulaation ajo-ohjeet, portin vaihtaminen ja debug-asetus ovat tiedostossa `README.md`.
 
## Tila
 
Valmista:
 
- C#-palvelu: `POST /events`, `GET /events`, SQLite-tallennus (WAL-tila).
- C#-palvelu: tapahtuman kaikki kentät (sim_time, point_name ja speed_kmh) tallentuvat, palvelu validoi ne ja tauluun on indeksi (point_id, sim_time).
- C#-palvelun automaattiset testit (xUnit, 87 testiä): vastaanotto, haku, kaikki tarkistukset ja tilastot.
- C#-palvelu: tilastot GET /stats/points (ohitukset, keskinopeus, suuntajakauma) ja GET /stats/hourly (ohitukset tunneittain), laskettu SQL:llä simulaatioajan mukaan.
- Java: ikkuna, joka piirtää kaupungin ruudukon (12 × 9 ruutua, kartta tekstirivinä).
- Java: kaksikaistaiset tiet, oikeanpuoleinen liikenne ja kaistaviiva.
- Java: autot syntyvät satunnaisesti kartan reunoilla, saavat satunnaisen määränpään ja lyhimmän reitin (`RoutePlanner`, leveyshaku), kääntyvät risteyksissä reitin mukaan ja poistuvat määränpäässään.
- Java: autoilla on eri nopeus, auto liikkuu vain jos edessä oleva ruutu on vapaa, ja risteykseen ajetaan vain jos sen jälkeinen ruutu on vapaa.
- Java: liikennevalot kaikissa `+`-risteyksissä, piirretty risteysruudun reunoihin.
- Java: simulaatiolla on oma aika (yksi tick on 5 sekuntia), joka näkyy ikkunan yläpalkissa, ja nopeutta voi säätää (Pause, Slower ja Faster, 0,5x–32x).
- Java: mittauspisteillä on nimet, ja ne piirretään kartalle. Pisteitä voi lisätä, nimetä uudelleen ja poistaa hiirellä ohjelman käydessä, ja ne tallentuvat tiedostoon ja latautuvat käynnistyksessä.
- Java: `EventSender` lähettää tapahtumat (mukana `simTime`, `pointName` ja `speedKmh`) palvelulle, ja ne tallentuvat tietokantaan. Koko ketju Java → HTTP → C# → SQLite toimii.

Simulaatio, palvelun tallennus, tilastot ja palvelun testit ovat valmiita. Jäljellä on katseluliittymä.
 
## Seuraavat vaiheet
 
Vaiheet 1–5 ja 6A-6C on tehty.
 
6. **Tietojen käsittely ja esitys.** Kukin osa kokeillaan ja commitoidaan erikseen:

   - 6C. Tilastot: pistekohtainen yhteenveto ja tuntikohtaiset ohitusmäärät SQL-kyselyinä, testeineen.
   - 6D. Katseluliittymä: yksi HTML-sivu osoitteessa `/` (yhteenvetotaulukko, tuntikaavio ja viimeisimmät tapahtumat).
   - 6E. Vuorokausirytmi simulaatioon (ruuhka-ajat), kun tilastot on nähty.
## Ei tehdä
 
Jalankulkijat, ohitukset, kolarit ja ruuhkaa väistävä reitinvalinta jätetään pois.
 
## Jätetty myöhemmäksi
 
- **Tapahtumien erälähetys.** Poistettu suunnitelmasta, koska WAL-tila ratkaisi suorituskyvyn (palvelu kestää 32x-nopeuden). Otetaan käyttöön vain, jos tarve ilmenee.
- **Java-testit ja Maven.** JUnit vaatii ulkopuolisen kirjaston, joten Mavenin (tai Gradlen) käyttöönotosta päätetään erikseen. Alustava kanta on, ettei näitä tehdä.
 