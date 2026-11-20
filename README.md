# Liikennesimulaatio

Yksinkertainen ruutukaupunkisimulaatio, jossa suorakulmioautot liikkuvat
pääsuuntiin. Auton ohittaessa mittauspisteen syntyy tapahtuma, jonka
C#-palvelu tallentaa SQLite-tietokantaan.

## Rakenne

- `simulation/` – Java-simulaatio (tulossa)
- `service/` – C#-palvelu (tulossa)
- `docs/` – suunnitelmat ja dokumentaatio (tulossa)

## Tila

Suunnitteluvaihe. Kaikki toimii paikallisesti.

## Palvelun ajaminen

    cd service
    dotnet run

Palvelu kuuntelee osoitteessa `http://localhost:5256`. Jos portti on
varattu, vaihda se tiedostossa `service/Properties/launchSettings.json`.