# Turista · verzia 1.13

Android aplikácia pre mapy, turistiku a navigáciu. Táto aktualizácia prenáša zdrojový projekt Turista_v13 do existujúceho repozitára.

## Funkcie

- Online a offline mapy, navigácia, kompas a úpravy trasy.
- Krokomer s históriou a úspechmi, nahrávanie trás, uložené miesta a priečinky.
- Rozpoznávač prírody: fotografia, výber časti, návrhy určenia, miestny denník nálezov a online atlas GBIF.
- Voliteľné prihlásenie cez Supabase Auth. Účet zatiaľ nesynchronizuje miestne údaje.

## Zostavenie na pôvodnom telefóne

Otvor koreň projektu s `gradlew` a `app`, potom spusti Run alebo `./gradlew assembleDebug`.

Zachované nastavenia používateľovho Ubuntu prostredia:

- JDK 17: `/home/test/.jdks/jdk-17`
- AAPT2: `/usr/bin/aapt2`
- Gradle 8.13, Android SDK 35

`local.properties` sa neukladá do Gitu. Cestu k Android SDK nastav lokálne alebo ju nechaj vytvoriť editorom. Na inom počítači uprav miestne cesty JDK/AAPT2 podľa svojho prostredia; projekt zatiaľ nemá prenosnú CI konfiguráciu.

## Účty a služby

Postup je v [NASTAVENIE_UCTOV.txt](NASTAVENIE_UCTOV.txt). Supabase aktuálne vyžaduje vlastné SMTP aj na úpravu e-mailových šablón. Používateľ si SMTP už nastavil a prihlásenie v aplikácii potvrdil.

Supabase URL a verejný kľúč, osobné rozpoznávacie API kľúče a relácia zadané v telefóne sa do tohto repozitára neprenášajú. SMTP heslo patrí iba do nastavení Supabase. Súkromné kľúče a podpisové úložiská necommituj.

## Overenie a obmedzenia

Zdrojový projekt v13 prešiel samostatnou kompiláciou Kotlinu a Android resources, testami parsera rozpoznávača a kontrolou geometrie popisu kompasu. Úplný Gradle APK build, všetky živé API a kompletný test na zariadení neboli v prostredí asistenta vykonané. Pri aktualizácii ponechaj rovnaký podpis APK a aplikáciu neodinštaluj, ak chceš zachovať miestne dáta.

Podrobnejšie pokyny a história: [README_TURISTA.txt](README_TURISTA.txt).
