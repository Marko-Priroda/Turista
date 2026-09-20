TURISTA 1.13 – CELÝ ZDROJOVÝ PROJEKT

AKO SPUSTIŤ
1. Rozbaľ Turista_v13.zip.
2. V editore otvor priečinok Turista_v13, v ktorom vidíš gradlew a app.
3. Stlač Run. Použi rovnaký podpis ako pri existujúcej aplikácii.
   Pôvodnú aplikáciu neodinštaluj, aby sa zachovali mapy a uložené údaje.
JDK 17: /home/test/.jdks/jdk-17. Gradle nastavenia zostali zachované.
ZIP obsahuje projekt, nie hotové APK.

NOVÉ V TEJTO VERZII
• Kompas: názov svetovej strany má vyhradenú plochu a neorezáva sa.
• Krokomer: veľký denný počet, priebeh cieľa a väčší sedemdňový graf.
  Spodné karty Dnes / História / Úspechy. História siaha po prvý uložený
  záznam; úspechy majú ukazovatele postupu. Dáta zostávajú zachované.
  Počet vychádza zo snímača, neuvádzame vymyslené kalórie ani čas chôdze.
  Meranie má rovnakú službu ako v12; zmenený vzhľad neodstraňuje obmedzenia
  snímača a uspávania telefónom. Zapni krokomer a potrebné povolenia.
• Prvá položka menu: Prihlásiť sa / účet. Registrácia, overovací kód,
  obnova hesla, odhlásenie. Najprv treba nastaviť Supabase Auth – presný
  postup je v NASTAVENIE_UCTOV.txt. Serverový projekt nie je súčasťou ZIP.
• Rozpoznávač v zeleno-krémovom štýle s kartami Nálezy / Identifikovať / Druhy.

ROZPOZNÁVAČ – POSTUP
1. Identifikovať -> kategória -> Odfotiť alebo Vybrať z galérie.
2. Potvrď Použiť fotografiu.
3. Vyber časť (list, kvet, klobúk, hlava, kryštály a ďalšie podľa kategórie).
4. Potvrď odoslanie službe. Možno pridať až 5 fotiek toho istého nálezu.
5. Porovnaj návrhy -> Vybrať a uložiť tento nález -> Uložiť nález.
6. V Nálezoch zostane tvoja fotografia, dátum a vybrané určenie aj offline.
   Ťuknutím otvoríš detail alebo môžeš nález odstrániť.
Pri rastlinách sú porovnávacie obrázky zobrazené iba ak ich Pl@ntNet dodá
s podporovanou licenciou a autorom; pri ostatných kategóriách sa nevymýšľajú.
Výsledok zostáva označený ako odhad vybraný používateľom, nie odborné overenie.

SLUŽBY
Rastliny: vlastný Pl@ntNet API kľúč v ozubenom koliesku.
Zvieratá, huby, kamene a kategória Neviem: vlastný Gemini API kľúč.
Pôvodné uložené kľúče zostávajú zachované. Limity určujú poskytovatelia.
Kategóriu nálezu vyber pred pridaním fotografie; pri zmene začni nový nález.
Atlas Druhy používa GBIF, vyhľadávanie a postupné načítavanie rastlinných
 druhov. Potrebuje internet, nepotrebuje tvoj API kľúč. Záznamy a názvy
závisia od katalógu GBIF, nejde o záruku zoznamu každej existujúcej rastliny.
Fotografie sa neposielajú bez potvrdenia. Neurčuj jedlosť podľa aplikácie.
Turista nie je oficiálna aplikácia Pl@ntNet.

OVERENIE
Kompilácia Kotlin zdrojov a Android grafických zdrojov; test parsera
výsledkov, licencií obrázkov a mapovania častí; kontrola geometrie textu
kompasu. Úplné zostavenie APK, meranie na telefóne a živé prihlásenie alebo
rozpoznávanie s API kľúčmi neboli v tomto prostredí overené.

PREDCHÁDZAJÚCE FUNKCIE A POKYNY (ARCHÍV v12)
--------------------------------------------------
TURISTA 1.12 – CELÝ PROJEKT (Turista_v12)

AKO SPUSTIŤ
1. Rozbaľ Turista_v12.zip.
2. V editore otvor priečinok Turista_v12, v ktorom sú gradlew a app.
3. Stlač Run a nainštaluj aktualizáciu cez existujúcu aplikáciu.
   Použi rovnaké podpisovanie aplikácie. Pôvodnú aplikáciu neodinštaluj,
   aby zostali zachované stiahnuté mapy a dáta.
JDK 17 zostal nastavený na /home/test/.jdks/jdk-17. Gradle konfigurácia
ani výber Javy sa oproti v9 nemenili. ZIP obsahuje zdrojový projekt, nie APK.

NOVINKY VO VERZII 1.12
NAVIGÁCIA
V paneli miesta vyber Pešo, Bicykel alebo Auto. Zvolený režim má tmavozelené
pozadie a značku začiarknutia. Tlačidlá teraz iba vyberajú režim; výpočet
spustí až Navigovať sem. Dodatočný dialóg Vyber spôsob navigácie bol odstránený.
Predvolená je pešia navigácia. Pri zmene voľby počas prebiehajúcej navigácie
sa existujúca trasa nezmení, kým nestlačíš Navigovať sem.

ULOŽENÉ A PRIEČINKY
Menu -> Uložené -> Nový priečinok -> názov.
Priečinok môže obsahovať miesta aj nahrané trasy. Prepínač Miesta/Trasy
mení zobrazený typ. Ťukni na položku -> Presunúť do priečinka.
Voľby Všetky uložené a Bez priečinka umožňujú prehliadať pôvodné položky.
Priečinok otvor -> Upraviť priečinok -> Premenovať alebo Odstrániť.
Odstránením priečinka sa jeho obsah presunie do Bez priečinka, nevymaže sa.
Nové miesta a trasy sa najprv uložia do Bez priečinka. Priečinky sú v jednej
úrovni; vnorené priečinky nie sú súčasťou tejto verzie.

ROZPOZNÁVAČ
Nový postup v zeleno-krémovom štýle Turistu:
1. Vyber kategóriu.
2. Pridaj 1 až 5 fotografií toho istého nálezu. Ťuknutím na miniatúru
   ju zväčšíš, Odobrať odstráni vybraný pohľad. Pri rastline odfoť napríklad
   celú rastlinu, list a kvet. Nemiešaj viaceré druhy v jednej požiadavke.
3. Rozpoznať online -> potvrď odoslanie uvedenej službe.
Rastliny · Pl@ntNet používa skutočné Pl@ntNet API. Výsledky sú karty možných
rastlinných druhov, názvy, čeľaď a skóre dodané službou. Skóre nie je záruka
správnosti ani jedlosti; bežné názvy môžu byť v inom jazyku.
Na túto voľbu potrebuješ vlastný Pl@ntNet API kľúč:
https://my.plantnet.org/settings/api-key
V kategórii Rastliny otvor Nastavenie online služby a ulož Pl@ntNet kľúč.
Zvieratá, huby, kamene a Všetko používajú pôvodný Gemini kľúč, ktorý zostáva
zachovaný. Rastliny možno cez Gemini skúsiť vo voľbe Všetko vrátane rastlín.
Gemini kľúč nefunguje v Pl@ntNete a naopak. Limity a prípadné poplatky
spravuje každá služba samostatne. Kľúče nevkladaj do zdrojového kódu ani chatu.
Fotografie odosielame až po potvrdení, bez pôvodných EXIF metadát. Obsah
fotografie vidí zvolený poskytovateľ. Sú to AI odhady; podľa výsledkov
nekonzumuj huby ani rastliny. Turista nie je oficiálnou aplikáciou Pl@ntNet.
API dokumentácia: https://my.plantnet.org/doc/api/identify

OVERENIE 1.12
Kotlin a Android zdroje sa skompilovali. Databázové testy overili migráciu
z oboch starších schém, presun do priečinka a odstránenie priečinka bez straty
miest, trás, GPS bodov a krokov. Nebol vykonaný úplný Gradle APK build,
test na telefóne ani živé volanie Pl@ntNet/Gemini bez používateľských kľúčov.
JDK 17 zostáva nezmenené. Aktualizuj s rovnakým podpisom bez odinštalovania.

NOVINKY VO VERZII 1.11 – KOMPLETNÝ BALÍK
Obsahuje všetky novinky 1.10 nižšie: obnovenie krokomera, históriu,
úrovne a úspechy, online rozpoznávač s fotoaparátom a výberom fotky.
Navyše nový vzhľad Offline máp: krémové pozadie, tmavozelené nadpisy,
vektorové mapové ikony, zaoblené karty kontinentov a krajín, vyhľadávanie
v zozname krajín a jednotný detail sťahovania. Detail je posúvateľný,
aby sa ovládanie dalo dosiahnuť aj na malej obrazovke.
Opravené otvorenie konkrétnej krajiny z výsledkov hlavného vyhľadávania.

Inštalácia: rozbaľ ZIP a v editore otvor Turista_v12 s gradlew a app.
Použi rovnaké podpisovanie a aktualizuj bez odinštalovania. JDK 17 sa nemení.
Kotlin celej verzie 1.11 a Android zdroje prešli kompiláciou.
Testy krokomera z 1.10 zostávajú platné, jeho logika sa v 1.11 nemenila.
Celý APK build a overenie na telefóne ešte treba vykonať cez Run.
Rozpoznávač potrebuje vlastný Gemini API kľúč. Živé API, fotoaparát a senzory
neboli v tomto prostredí overené. Vlastný kľúč nikdy neposielaj do chatu.

NOVINKY VO VERZII 1.10
KROKOMER
- Zelený a krémový prehľad, kruh denného cieľa a týždenný graf.
- História od prvého uloženého merania, prehľad po mesiacoch až po dnes.
  Dni bez údajov sú označené Bez záznamu; staršie kroky sa nedajú spätne získať.
- Úspechy: 9 úrovní, 8 denných míľnikov, 6 sérií dní, 7 míľnikov aktívnych dní
  a 7 míľnikov celkových krokov. Vychádzajú výhradne z uložených meraní.
- Kumulatívna hodnota snímača a započítané kroky sa ukladajú v jednej transakcii.
  Po reštarte procesu sa nepoužije celá hodnota snímača ako nové kroky.
  Zapnutá služba žiada systém o obnovenie; otvorenie krokomera obnoví meranie,
  ak bolo predtým zapnuté a povolenie zostalo udelené.
- Prednostne sa používa prebúdzací snímač, ak ho telefón poskytuje.
  Snímač môže údaje posielať oneskorene. Pri TYPE_STEP_DETECTOR nemožno
  spätne dopočítať udalosti stratené počas prerušenia služby.
- Samsung: Nastavenia -> Aplikácie -> Turista -> Batéria -> Neobmedzené;
  podľa verzie systému aj Nikdy neuspávané aplikácie. Povoľ Fyzickú aktivitu.
  Po reštarte telefónu otvor krokomer. Vynútené zastavenie počítanie preruší.
- Rozdiel počítadla cez hranicu dňa sa nezapočíta naslepo do nového dňa:
  presné rozdelenie bez priebežných meraní nie je dostupné. Súčet preto môže
  chýbajúci interval podhodnotiť, nikdy však zámerne nevymýšľa kroky.

ROZPOZNÁVAČ – PRVÁ ONLINE VERZIA
Menu -> Rozpoznávač -> Odfotiť nález alebo Vybrať fotku.
Fotografia sa zobrazí lokálne; odosiela sa až po potvrdení Rozpoznať online.
Podporované zameranie: rastliny, zvieratá, huby, horniny a minerály.
Ide o všeobecný AI odhad; nie o overenú taxonomickú databázu alebo znalecký
posudok. Fotografia nemusí stačiť na druhové určenie. Huby ani rastliny
podľa výsledku nekonzumuj ani nepoužívaj na liečenie.

POTREBNÉ NASTAVENIE
1. V Rozpoznávači otvor Získať vlastný API kľúč (Google AI Studio).
2. Over si dostupnosť, podmienky, spracovanie dát a limity svojho účtu:
   https://ai.google.dev/gemini-api/terms
   https://ai.google.dev/gemini-api/docs/pricing
3. Kľúč vlož iba do Nastavenie online služby priamo v telefóne.
   Neposielaj ho do chatu. Je uložený šifrovane cez Android Keystore.
4. Názov obrazového modelu sa dá zmeniť podľa ponuky tvojho účtu. Predvolený
   gemini-3.8-flash zodpovedá aktuálnym príkladom dokumentácie; dostupnosť
   konkrétneho modelu pre tvoj účet musí overiť živá požiadavka.
5. Rozpoznať online -> potvrď odoslanie Google Gemini.
Nie je pribalený spoločný API kľúč ani garantovaná bezplatná služba.
Pri limite, neplatnom kľúči či výpadku sa zobrazí hlásenie, nie vymyslený výsledok.
Ide o prototyp pre osobné používanie s vlastným kľúčom. Pred verejným vydaním
priprav samostatný server s autentifikáciou a obmedzením spotreby; spoločný
platený kľúč nikdy nevkladaj do distribuovaného APK.
Fotka sa zmenší, znovu uloží ako JPEG a pôvodné EXIF údaje sa neodosielajú.
Obsah obrázka je však stále viditeľný poskytovateľovi. Neukladajú sa dejiny
rozpoznávania. Dočasné obrázky sú v súkromnej cache; po zavretí obrazovky sa
aktuálna fotka vymaže. Systém môže cache vymazať aj skôr.

OVERENIE 1.10
Kotlin a Android zdroje boli skompilované. Testy overili rozdiely počítadla,
obnovu baseline, prechod dňa/reštart, súčty, série dní a zachovanie dát pri
migrácii databázy. Kompletný Gradle APK build, senzory, fotoaparát a živá
požiadavka Gemini na telefóne tu neboli overené. API kľúč nebol poskytnutý.
JDK 17 a konfigurácia jeho spúšťania zostávajú nezmenené.

NOVINKY VO VERZII 1.9
- Aplikácia sa otvorí rovno na mape bez úvodnej stránky.
- Pri opakovanom odchýlení od cestnej trasy sa spustí automatický prepočet.
  Vyžaduje aspoň tri primerane presné GPS merania počas ôsmich sekúnd.
  Pri neúspechu zostáva pôvodná trasa; ďalší pokus má časový odstup.
  Offline prepočet potrebuje stiahnuté routovacie dáta.
- Pri vlastnej kreslenej trase aplikácia upozorní na odchýlenie, ale nenahradí
  tvoju trasu automaticky cestnou trasou.
- Editor: vyber cieľ -> Upraviť trasu. Ťuknutím pridávaj medzibody.
  Pre cestnú trasu sa body spoja po cestách po stlačení Použiť.
  Pre voľné kreslenie vyber vlastnú pešiu trasu a zapni Kresliť prstom.
  V tomto režime ťah kreslí; pre posúvanie mapy kreslenie vypni.
  Body umožňujú zmenu poradia a odstránenie; Späť vráti poslednú úpravu.
  Voľná čiara nie je overený chodník a nemá cestné pokyny na každom ohybe.
- Nastavenia -> hlas: výber slovenského hlasu, rýchlosť a skúška.
  Kvalita závisí od hlasového systému telefónu. Nový syntetizátor ani platený
  hlas nie sú pribalené. Offline potrebuješ stiahnutý slovenský hlas.
  Po zmene systémového hlasového systému aplikáciu reštartuj.
- Krokomer: kruh denného cieľa, nastaviteľný cieľ, sedemdňový graf a súčet.
  Graf používa reálne uložené merania; údaje a stiahnuté mapy sa zachovávajú
  pri aktualizácii s rovnakým podpisom bez odinštalovania.

OVERENIE VERZIE 1.9
Zdrojový Kotlin sa skompiloval; Android zdroje prešli AAPT2 kompiláciou.
Automatické testy overujú GPS projekciu a spúšťanie opakovaného prepočtu.
Kompletný APK build ani skúška GPS, hlasu a senzorov na telefóne tu neprebehli.

ZACHOVANÉ FUNKCIE VERZIE 1.8
- Výsledky vyhľadávania sa zobrazujú pri písaní od tretieho znaku,
  po krátkej pauze. Ťuknutím vyberieš konkrétny výsledok.
- Výsledky sú priebežné, staršia odpoveď neprepíše novšie hľadanie.
  Online vyhľadáva Photon; offline sa hľadá v tvojich uložených miestach.
- Zoznamy krajín, obrazovka sťahovania a detail krajiny rešpektujú
  spodnú lištu telefónu. Posledná krajina sa dá dosunúť a stlačiť.
- Menu obsahuje Krokomer, Nahrávanie trasy, Uložené a Dobrovoľný príspevok.
  Rozšírené menu sa dá posúvať aj na menšej obrazovke.

KROKOMER
Menu -> Krokomer -> Zapnúť krokomer.
Povoľ Fyzickú aktivitu a ideálne aj oznámenia. Krokomer používa hardvérový
snímač; ak ho telefón neposkytuje, obrazovka to oznámi.
Zobrazuje dnešné kroky a posledných sedem dní. Počíta iba od zapnutia,
nie spätne kroky z času, keď bol vypnutý. Po reštarte telefónu alebo nútenom
ukončení aplikácie ho zapni znovu. Pri zmene dňa sa prvá hodnota snímača
použije ako nová základná hodnota, aby sa staršie kroky nepriradili novému dňu.
Krokomer môže bežať aj bez nahrávania GPS trasy.

NAHRÁVANIE TRASY
Menu -> Nahrávanie trasy -> Spustiť nahrávanie.
Povoľ presnú polohu, zapni GPS a ideálne povoľ oznámenia.
Záznam prebieha v službe na popredí s oznámením a môže pokračovať aj so
zhasnutou obrazovkou. Zobrazuje vzdialenosť, čas prijatého GPS záznamu a počet
bodov. Čas neobsahuje pauzy ani dlhé výpadky GPS.
Pozastaviť nahrávanie preruší; Pokračovať vytvorí nový úsek.
Ukončiť a uložiť umožní pomenovať trasu a uloží ju do Uložené -> Trasy.
Aj pred ukončením sa body priebežne zapisujú do databázy v telefóne.
Po páde alebo nútenom ukončení zostáva rozpracovaná trasa dostupná;
po otvorení aplikácie môžeš pokračovať alebo ju ukončiť a uložiť.
Pri obmedzení aplikácie systémom/batériou nemusí záznam pokračovať.
Aplikáciu počas nahrávania ručne nevypínaj cez Vynútiť zastavenie.
Veľmi nepresné, staré a zjavne chybné GPS body sa nezapisujú. Medzera pri
pauze, vypnutí GPS alebo výpadku nad dve minúty sa nespája priamkou.

ULOŽENÉ MIESTA A TRASY
Miesto: ťukni na mapu alebo vyber výsledok vyhľadávania -> Uložiť miesto -> názov.
Menu -> Uložené: prepínaj medzi miestami a trasami.
Položky môžeš zobraziť na mape, premenovať alebo vymazať po potvrdení.
Rozpracovanú trasu najprv ukonči a ulož, až potom ju možno vymazať.
Uložené miesto sa otvorí ako cieľ a môžeš sa k nemu navigovať.
Nahraná trasa sa zobrazí fialovou čiarou; pauzy zostanú oddelené.
Zobrazenie záznamu je prehliadanie, nie spustenie navigácie po zázname.
Tlačidlo Skryť zobrazenú trasu na mape odstráni iba zobrazenie, nie uložený záznam.

DOBROVOĽNÝ PRÍSPEVOK
Položka menu a obrazovka sú pripravené. Platba zatiaľ nie je aktivovaná,
pretože vlastník aplikácie neposkytol platobný odkaz ani IBAN a meno príjemcu.
Na dokončenie pošli tieto údaje v rozhovore. Netreba posielať číslo karty,
heslo, prihlasovacie údaje ani tajný API kľúč k platobnej bráne.
V kóde je pripravená konfigurácia DonationConfig v TripActivity.kt:
HTTPS odkaz sa otvorí v prehliadači; pri IBAN-e je tlačidlo na skopírovanie.
Príspevok neodomyká ani nezamyká funkcie aplikácie.

ZACHOVANÉ Z VERZIE 1.7
Mapy a stiahnuté krajiny, offline navigácia BRouter, kompas, nastavenia,
satelitná/terénna mapa, dopravná vrstva s vlastným kľúčom TomTom,
hlasové pokyny, kreslenie vlastnej pešej trasy a peší záver automobilovej trasy.
Dopravná vrstva zatiaľ neovplyvňuje výpočet trás podľa zápch.
Vlastné úseky mimo chodníkov nemajú overenú priechodnosť.

OVERENIE PRED ODOVZDANÍM
- Kompilácia všetkých Kotlin zdrojov proti závislostiam projektu.
- Kompilácia XML/drawable zdrojov cez AAPT2 a kontrola odkazov na zdroje.
- Testy GPS filtrov, medzier a pokračovania po pauze.
- Testy krokového počítadla: prvá hodnota, opakovanie, zmena dňa, reštart,
  a opätovné zapnutie bez započítania vypnutého intervalu.
- Overenie SQLite schémy, uchovania oddelených úsekov a vymazania záznamu.
- Živá požiadavka Photon pre vyhľadávanie vrátila výsledky.
Celý APK build, obrazovky a beh služby na konkrétnom telefóne tu neboli
odskúšané. Over hlavne povolenia a krátku skúšobnú trasu so zhasnutým displejom.

PRE ĎALŠÍ VÝVOJ
Vyhľadávanie využíva verejný Photon server s obmedzenou frekvenciou požiadaviek
(minimálne 3 znaky, oneskorenie, medzipamäť, jedna pracovná sieťová požiadavka).
Pri širšom publikovaní aplikácie použi vlastnú inštanciu alebo dohodnutú službu;
verejný server negarantuje dostupnosť. Verejný Nominatim sa na priebežné
vyhľadávanie nepoužíva.
Dokumentácia: https://github.com/komoot/photon
Android služby: https://developer.android.com/develop/background-work/services/fgs/service-types
