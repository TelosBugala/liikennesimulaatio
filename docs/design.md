# Suunnitteludokumentti
 
Tämä dokumentti kuvaa projektin rakenteen, tehdyt päätökset ja nykytilan. Päivitä sitä, kun jokin muuttuu. Ajo-ohjeet ovat tiedostossa `README.md`.
 
## Tavoite
 
Yksinkertainen ruutukaupunkisimulaatio, jossa suorakulmioautot liikkuvat vain neljään pääsuuntaan. Autot syntyvät kartan reunalla olevalle tielle, ajavat valitsemaansa reittiä toiselle reunatielle ja katoavat siellä. Tiet ovat kaksikaistaisia, ja risteyksissä on liikennevalot. Ulkoasun esikuvina ovat C64-peli Traffic ja Police Quest 1:n ajo-osuus. Kun auto ohittaa mittauspisteen, syntyy tapahtuma, jonka C#-palvelu tallentaa SQLite-tietokantaan.
 
Tavoitteena on myös, että simulaatiolla on oma aika, jonka nopeutta voi säätää, ja että nimettyjä mittauspisteitä voi lisätä ja poistaa ohjelman käydessä (vaihe 5).
 
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
| `Main` | Käynnistää ikkunan (yläpalkki ja kartta) ja ajastimen (tick 200 ms), kytkee tapahtumakuuntelijan |
| `CityMap` | Kartta tietona (merkkirivit) ja tiesäännöt (`isInside`, `roadAllows`) |
| `CityPanel` | Piirtää kartan, mittauspisteet, valot ja autot |
| `Simulation` | Maailman tila: autot, valot, mittauspisteet ja syntymispisteet. `step()` on yksi tick: autojen liike ja uuden auton syntyminen |
| `Car` | Auton sijainti, viimeisin ajosuunta, nopeus ja reitti (lista suuntia) |
| `Direction` | Neljä pääsuuntaa (enum) |
| `TrafficLight` | Risteyksen valo. Tila lasketaan tickistä |
| `SpawnPoint` | Syntymispiste: ruutu ja ajosuunta |
| `RoutePlanner` | Laskee lyhimmän reitin kahden ruudun välillä leveyshaulla ja palauttaa suuntalistan. Tasapelit ratkaistaan arpomalla |
| `MeasurementPoint` | Mittauspiste (tunniste ja ruutu) |
| `PassEvent` | Tapahtuma: tick, simTime, pointId, carId, direction |
| `EventSender` | Lähettää tapahtuman palvelulle HTTP POST -pyynnöllä (osoite annetaan konstruktorissa) |
| `Debug` | Debug-lokitus (-Dtraffic.debug=true) |
| `SimTime` | Laskee simulaatioajan tickistä (alkuaika ja tickin pituus) |
| `ControlBar` | Ikkunan yläpalkki, jossa kello |
  
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
 
Simulaatio lähettää tapahtumassa jo lisäkentän simTime (ISO 8601, esimerkiksi 2026-01-05T08:15:30), ja vaiheessa 5 tulee lisää kenttiä (ks. 'Suunnitellut ratkaisut'). Palvelu ohittaa ne, kunnes se päivitetään vaiheessa 6.
 
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

### Yleiset
 
- **Pieni ensimmäinen versio.** Uusia ominaisuuksia ei lisätä ennen kuin niiden tarve on perusteltu ja päätetty.
- **Ei Mavenia vielä.** Javassa käytetään pelkkää `javac`ia, kunnes tarvitaan ulkopuolisia kirjastoja.
### Tallennus ja rajapinta
 
- **Raakaa SQL:ää, ei Entity Frameworkia.** Pidetään tallennus näkyvänä ja yksinkertaisena. SQL-lauseet käyttävät parametreja (ei merkkijonojen liittämistä) SQL-injektion estämiseksi.
- **Tick ja simulaatioaika kulkevat tapahtumassa mukana.** Simulaatio lähettää sekä tickin että simulaatioajan (`simTime`). Palvelu tallentaa simulaatioajan (`sim_time`) vaiheessa 6. Tietokanta on kertakäyttöinen, joten sarakkeen lisääminen on helppoa tarpeen tullen.
- **Suunnalle ei ole oletusarvoa.** Puuttuva suunta on virhe, jonka halutaan näkyä heti.
- **Aikaleima on ISO 8601 -muodossa, sekunnin tarkkuudella ja ilman aikavyöhykettä** (esimerkiksi `2026-01-05T08:15:30`). Se on simuloidun maailman paikallinen aika. JSON-aikaleima muotoillaan `DateTimeFormatter.ISO_LOCAL_DATE_TIME`illa, jotta sekunnit ovat aina mukana (`LocalDateTime.toString()` jättää ne pois, kun ne ovat nolla).
### Simulaation rakenne
 
- **Kartta ja piirto erillään.** `CityMap` kuvaa kartan tietona, `Simulation` pitää kirjaa maailman tilasta ja `CityPanel` piirtää sen.
- **Auto liikkuu yhden ruudun per tick.** Liike on hyppäys ruudulta toiselle (ei sulavaa liikettä), joten mittauspisteen ylitys on helppo havaita. Tickin pituus on 200 ms (`TICK_MILLIS` tiedostossa `Main.java`).
- **Nopeus on kokonaisluku tickeinä.** Auto liikkuu joka N:s tick (`moveEveryTicks`). Arvo arvotaan auton syntyessä: auto liikkuu joka, joka toinen tai joka kolmas tick.
- **Simulaatioaika lasketaan tickistä.** `SimTime.at(tick)` palauttaa ajan, joka on `tick × SECONDS_PER_TICK` sekuntia alkuajan jälkeen. Yksi tick on 5 sekuntia, ja alkuaika on maanantai 2026-01-05 06:00. Erillistä kelloa ei ole, joten nopeudensäätö ei vaikuta dataan: sama ajo tuottaa samat aikaleimat millä nopeudella tahansa. Kello näkyy yläpalkissa (`ControlBar`) muodossa `Mon 2026-01-05 06:00:00`.
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
- **Mittauspiste piirretään viivana tien poikki.** Viiva on ruudun keskellä, ja se kattaa molemmat kaistat. Pisteen tunniste näkyy numerona ruudun kulmassa. Risteykseen tai muulle ruudulle asetettu piste piirretään kehyksenä. Piirto lukee pisteet simulaatiolta joka kerta, joten myöhemmin lisättävät ja poistettavat pisteet näkyvät automaattisesti.
### Tapahtumat ja yhteys palveluun
 
- **Mittauspiste on yksi ruutu, ja se havaitsee molempien suuntien autot.** Suunta kulkee mukana tapahtumassa (`direction`). Tapahtuma syntyy, kun auto saapuu pisteen ruutuun.
- **Simulaatio ei tiedä tapahtumien käsittelystä.** Se kutsuu konstruktorissa annettua `Consumer<PassEvent>`-kuuntelijaa.
- **Lähetys on asynkronista (`sendAsync`).** Hidas tai poissa oleva palvelu ei pysäytä simulaatiota. Yksi pyyntö per tapahtuma, JSON kirjoitetaan käsin. Epäonnistunut lähetys kirjataan `System.err`iin, eikä sitä yritetä uudelleen.
- **Palvelun osoite on `Main.java`ssa oletusarvona (`DEFAULT_SERVICE_URL`), ja sen voi ohittaa asetuksella `-Dtraffic.serviceUrl=...`.** Simulaatio ei lue palvelun `launchSettings.json`ia, koska se on `dotnet run` -kehityskäytön tiedosto eikä osa rajapintaa.
- **Konsoli on tavallisessa ajossa hiljainen, ja debug-tila avaa lisätulostuksen.** Tavallisessa ajossa tulostuvat vain virheet (`System.err`) ja yksi käynnistysrivi (palvelun osoite). Asetuksella `-Dtraffic.debug=true` tulostetaan lisäksi autojen reitit ja palvelulle lähetettävät tapahtumat. Tulostus kulkee `Debug.log`in kautta, ja `EventSender` lokittaa lähettämänsä JSON:n.
## Suunnitellut ratkaisut (vaihe 5, päätetty, ei vielä toteutettu)
 
- **Nopeudensäätö.** Yläpalkissa painikkeet Slower, Faster ja Pause sekä nykyisen nopeuden näyttö. Nopeudet ovat 0,5x–32x. Alle 25 ms:n ajastinvälejä ei käytetä, vaan suuremmilla nopeuksilla ajetaan useampi tick yhdellä ajastinkutsulla.
- **Mittauspisteellä on nimi.** Tunniste (`id`) on avain, eikä sitä käytetä uudelleen. Nimi on 1–30 merkkiä (kirjaimia, numeroita, välilyöntejä ja tavuviivoja), ja se näkyy kartalla numeron tilalla.
- **Hiirellä muokkaus.** Vasen klikkaus tieruutuun lisää pisteen tai muokkaa olemassa olevan pisteen nimeä, oikea klikkaus poistaa pisteen. Simulaatio pysähtyy dialogin ajaksi.
- **Pisteiden tallennus.** Pisteet tallennetaan tiedostoon `simulation/measurement-points.txt` (rivi per piste: `id;x;y;nimi`) jokaisen muutoksen jälkeen ja ladataan käynnistyessä. Tiedosto on `.gitignore`ssa. Jos sitä ei ole, aloitetaan kolmella oletuspisteellä (Lehtotie, Koivutie, Mäntytie).
- **Tapahtuman muoto.** Tapahtuma sisältää kentät `tick`, `simTime`, `pointId`, `pointName`, `carId`, `direction` ja `speedKmh`. Palvelu ei muutu ennen vaihetta 6: se ohittaa tuntemattomat kentät, joten uudet kentät eivät tallennu ennen sitä.
- **Nopeus tapahtumassa on auton nimellisnopeus (km/h).** Se lasketaan `moveEveryTicks`ista, tickin pituudesta ja ruudun pituudesta (70 m). Se ei kuvaa jonossa seisomista.
## Tunnetut rajoitukset
 
- **Jumiutumisriski.** Kun autoja on paljon, ne voivat jonottaa ristiin niin, ettei kukaan pääse liikkeelle. Valot, rajoitettu syntymisväli ja "älä tuki risteystä" -sääntö vähentävät riskiä. Jos jumiutumista esiintyy, se ratkaistaan silloin.
## Väliaikaiset ratkaisut
 
Nämä ovat käytössä nyt ja korvataan seuraavien vaiheiden aikana:
 
- **Mittauspisteet on kovakoodattu `Simulation.createPoints()`-metodiin**, eikä niitä voi muokata ohjelman käydessä. Korvautuu lisäyksellä, poistolla ja tallennuksella (vaiheet 5D ja 5E).
## Ajaminen
 
Palvelun ja simulaation ajo-ohjeet, portin vaihtaminen ja debug-asetus ovat tiedostossa `README.md`.
 
## Tila
 
Valmista:
 
- C#-palvelu: `POST /events`, `GET /events`, SQLite-tallennus.
- Java: ikkuna, joka piirtää kaupungin ruudukon (12 × 9 ruutua, kartta tekstirivinä).
- Java: kaksikaistaiset tiet, oikeanpuoleinen liikenne ja kaistaviiva.
- Java: autot syntyvät satunnaisesti kartan reunoilla, saavat satunnaisen määränpään ja lyhimmän reitin (`RoutePlanner`, leveyshaku), kääntyvät risteyksissä reitin mukaan ja poistuvat määränpäässään.
- Java: autoilla on eri nopeus, auto liikkuu vain jos edessä oleva ruutu on vapaa, ja risteykseen ajetaan vain jos sen jälkeinen ruutu on vapaa.
- Java: liikennevalot kaikissa `+`-risteyksissä, piirretty risteysruudun reunoihin.
- Java: kolme mittauspistettä, jotka on piirretty kartalle ja jotka tuottavat `PassEvent`-tapahtuman, kun auto saapuu pisteen ruutuun.
- Java: `EventSender` lähettää tapahtumat palvelulle, ja ne tallentuvat tietokantaan. Koko ketju Java → HTTP → C# → SQLite toimii.
- Java: simulaatiolla on oma aika (yksi tick on 5 sekuntia), joka näkyy ikkunan yläpalkissa, ja tapahtuma kuljettaa aikaleiman (simTime).

## Seuraavat vaiheet
 
Vaiheet 1–4 ja 5A on tehty
 
5. **Simulaation lisäominaisuudet.** Kukin osa kokeillaan ja commitoidaan erikseen:

   - 5B. Nopeudensäätö.
   - 5C. Mittauspisteiden nimet sekä tapahtuman `pointName`- ja `speedKmh`-kentät.
   - 5D. Mittauspisteiden lisäys, nimeäminen ja poisto hiirellä.
   - 5E. Pisteiden tallennus ja lataus.
6. **Palvelun muutokset (uudet kentät), tilastot ja yhteenvedot, tapahtumien erälähetys, datan katseluliittymä ja automaattiset testit**, vasta kun simulaatio on valmis. Näiden keskinäistä järjestystä ei ole vielä päätetty.
## Ei tehdä
 
Jalankulkijat, ohitukset, kolarit, ruuhkaa väistävä reitinvalinta.
 