# Liikennesimulaatio

Yksinkertainen ruutukaupunkisimulaatio, jossa suorakulmioautot liikkuvat
pääsuuntiin. Auton ohittaessa mittauspisteen syntyy tapahtuma, jonka
C#-palvelu tallentaa SQLite-tietokantaan.

## Rakenne

- `simulation/` – Java-simulaatio (Swing)
- `service/` – C#-palvelu (ASP.NET Core) ja SQLite-tietokanta
- `docs/` – suunnitelmat ja dokumentaatio

```
Java-simulaatio  --HTTP + JSON-->  C#-palvelu  --SQL-->  SQLite
```

## Tila

Työn alla. Koko ketju toimii: autot ajavat ruudukossa liikennevaloissa,
mittauspisteet tuottavat tapahtumia, ja palvelu tallentaa ne tietokantaan.
Simulaatiota täydennetään parhaillaan. Tarkempi tila ja suunnitelma ovat
tiedostossa `docs/design.md`.

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

## Portin vaihtaminen

Jos portti on varattu, käynnistä molemmat samalla uudella portilla.
Esimerkki portilla 5300:

    cd service
    dotnet run --urls http://localhost:5300

    cd simulation
    java "-Dtraffic.serviceUrl=http://localhost:5300" -cp out traffic.Main

Molempien osoitteiden pitää täsmätä. Jos simulaatio ei saa yhteyttä,
konsoliin tulostuu virhe, jossa näkyy osoite, johon se yritti yhdistää.