# Liikennesimulaatio
 
Yksinkertainen ruutukaupunkisimulaatio, jossa suorakulmioautot liikkuvat
pääsuuntiin. Auton ohittaessa mittauspisteen syntyy tapahtuma, jonka
C#-palvelu tallentaa SQLite-tietokantaan.
 
## Rakenne
 
- `simulation/` – Java-simulaatio (Swing)
- `service/` – C#-palvelu (ASP.NET Core) ja SQLite-tietokanta
- `service.Tests/` – palvelun automaattiset testit (xUnit)
- `docs/` – suunnitelmat ja dokumentaatio
```
Java-simulaatio  --HTTP + JSON-->  C#-palvelu  --SQL-->  SQLite
```
 
## Tila
 
Simulaatio on valmis: autot ajavat kaksikaistaisessa ruudukossa liikennevaloissa, mittauspisteet tuottavat tapahtumia, ja palvelu tallentaa ne tietokantaan. Palvelun tallennus ja automaattiset testit ovat valmiit. Työn alla on tilastot tallennetusta datasta ja niiden katselu selaimessa. Tarkempi tila ja suunnitelma ovat tiedostossa docs/design.md.
 
Kaikki toimii paikallisesti.
 
## Vaatimukset
 
- Java 25
- .NET 10 SDK
 
## Ajaminen
 
Palvelu:
 
    cd service
    dotnet run
 
Simulaatio (toisessa terminaalissa):
 
    cd simulation
    javac -d out -sourcepath src src/traffic/Main.java
    java -cp out traffic.Main
 
Palvelu kuuntelee oletuksena osoitteessa `http://localhost:5256`, ja
simulaatio lähettää tapahtumat sinne.
 
Nollaa tietokanta ennen jokaista uutta simulaatioajoa, ettei eri ajojen data
sekoitu (ks. "Tietokannan nollaus" alempana).
 
Palvelun käsin testaus: `service/requests.http` sisältää valmiit pyynnöt.
Tiedoston voi ajaa esimerkiksi VS Coden REST Client -lisäosalla tai
JetBrains-ohjelmistojen ja Visual Studion omalla HTTP-tuella. GET-pyynnön
voi kokeilla myös selaimessa osoitteessa `http://localhost:5256/events`.
 
## Käyttö
 
- **Nopeus:** yläpalkin painikkeilla Slower ja Faster simulaation nopeus on
  0,5x–32x, ja Pause pysäyttää simulaation. Kello näyttää simulaatioajan
  (yksi tick on 5 sekuntia).
- **Mittauspisteet:** vasen klikkaus tieruutuun lisää mittauspisteen ja
  kysyy sen nimen. Vasen klikkaus olemassa olevaan pisteeseen muokkaa nimeä,
  ja oikea klikkaus poistaa pisteen.
- **Tallennus:** pisteet tallentuvat tiedostoon `measurement-points.txt`
  ohjelman käynnistyskansioon (`simulation/`) ja latautuvat käynnistyksessä.
  Tiedoston poistaminen palauttaa oletuspisteet.
 
## Debug-tulostus
 
Normaalissa ajossa konsoliin tulostuu vain käynnistysrivit (palvelun osoite
ja mittauspisteiden tiedosto) ja mahdolliset virheet. Käynnistämällä
simulaation asetuksella `-Dtraffic.debug=true` konsoliin tulostetaan lisäksi
jokaisen syntyvän auton reitti ja jokainen palvelulle lähetettävä tapahtuma.
PowerShellissä asetus kirjoitetaan lainausmerkkien kanssa:
 
    java "-Dtraffic.debug=true" -cp out traffic.Main
 
## Testit

Palvelun automaattiset testit ajetaan komennolla:

cd service.Tests
dotnet test

Pysäytä palvelu (dotnet run) ennen testien ajoa, koska käynnissä oleva palvelu lukitsee käännettävän tiedoston. Testit käynnistävät palvelun itse muistissa ja käyttävät väliaikaista tietokantaa, joten ne eivät koske service/traffic.db-tiedostoon.

## Portin vaihtaminen
 
Jos portti on varattu, käynnistä molemmat samalla uudella portilla.
Esimerkki portilla 5300:
 
    cd service
    dotnet run --urls http://localhost:5300
 
    cd simulation
    java "-Dtraffic.serviceUrl=http://localhost:5300" -cp out traffic.Main
 
Molempien osoitteiden pitää täsmätä. Jos simulaatio ei saa yhteyttä,
konsoliin tulostuu virhe, jossa näkyy osoite, johon se yritti yhdistää.
 
## Tietokannan nollaus
 
Simulaatio alkaa joka käynnistyksellä alusta: tick on 0, autojen
tunnisteet alkavat ykkösestä ja simulaatioaika alkaa maanantaista
2026-01-05 kello 06:00. Jos tietokantaa ei tyhjennetä ajojen välillä, eri
ajojen tapahtumat menevät päällekkäin samoille ajoille ja samoille
auto-tunnisteille, eikä palvelu huomaa sitä. Tilastot (jos niitä lasketaan
tallennetusta datasta) olisivat silloin virheellisiä. Siksi tietokanta
nollataan ennen jokaista uutta ajoa.
 
Pysäytä ensin palvelu (Ctrl+C sen terminaalissa) ja poista sitten
tietokantatiedostot `service/`-kansiossa:
 
    cd service
    Remove-Item traffic.db, traffic.db-wal, traffic.db-shm -ErrorAction SilentlyContinue
 
Komento on PowerShellille. Tietokanta koostuu kolmesta tiedostosta
(`traffic.db` ja SQLiten WAL-tilan apufiletiedostot `-wal` ja `-shm`), ja
kaikki kolme poistetaan. `-ErrorAction SilentlyContinue` estää virheilmoituksen,
jos jokin tiedostoista puuttuu. Käynnistä sitten palvelu uudelleen
(`dotnet run`): se luo uuden, tyhjän tietokannan.
 
Nollaus tarvitaan myös silloin, kun taulun rakenne muuttuu, koska palvelu ei
päivitä vanhaa taulua.