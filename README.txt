BUDA iOS 0.13.0 (build 16) - Moj dzien i nowa os zmian

- Pulpit Moj dzien: gradient, delikatne tlo, data i wybrany uczen.
- Karty zastepcze podczas pierwszego ladowania dnia, bez zapetlonej animacji.
- Czytelniejsze karty ocen, wieksze kafelki i szczegoly wysuwane od dolu.
  Kolory ocen nauczyciela oraz tekstowe wartosci pozostaja zachowane.
- Centrum zmian z grupami Dzisiaj, Wczoraj i datami w lokalnej strefie czasowej.
- Os czasu z porownaniem przed/po, opisem i oznaczeniem nieprzeczytanych wpisow.
- Delikatne animacje filtrow historii respektuja systemowe Ogranicz ruch.
- Dotychczasowe funkcje dziennika i konfiguracja Web Push pozostaja dostepne.
- To aktualizacja wygladu; nie zmienia godzinnego harmonogramu dodatku push.
Weryfikacja wygladu i gestow na fizycznym iPhonie pozostaje do potwierdzenia.
IPA: Buda-iOS-0.13.0-unsigned.ipa, do instalacji przez AltStore.
Wyniki kompilacji i testow: VALIDATION.txt.

Archiwum opisow wydan:
BUDA iOS 0.12.2 (build 15) - zapis zaszyfrowanej sesji

Kompilacja i testy: PASS
https://github.com/prezaxx/buda-ios-prototype/actions/runs/43235799595
IPA: Buda-iOS-0.12.2-unsigned.ipa; szczegoly: VALIDATION.txt.

- Po wybraniu klucza pojawia sie sekcja Plik sesji jest gotowy.
- Nacisnij osobny przycisk Zapisz zaszyfrowana sesje i zapisz plik .buda.
  Okno zapisu nie jest juz otwierane w callbacku zamykanego wyboru pliku.
- Anulowanie lub blad zapisu zachowuje zaszyfrowany dokument do ponowienia.
- Stan i bledy eksportu sa widoczne na gorze ekranu.
- Bledny klucz i brak zapisanej sesji maja oddzielne komunikaty.
- Weryfikacja na fizycznym iPhonie pozostaje do potwierdzenia.
Instalacja: IPA z workflow Build IPA for AltStore, podpis przez AltStore.
Instrukcje polaczenia i zakres powiadomien opisano ponizej.

Archiwum opisow wydan:
BUDA iOS 0.12.1 (build 14) - poprawka nawigacji i konfiguracja Web Push

Aktualny kod aplikacji: 02fe5477119da046b968694b294cf14e8215f656
Kompilacja i testy:
https://github.com/prezaxx/buda-ios-prototype/actions/runs/43211495778
IPA: Buda-iOS-0.12.1-unsigned.ipa, do podpisania i instalacji przez AltStore.
Wyniki weryfikacji i suma SHA256: VALIDATION.txt.

Zmiany w 0.12.1:
- Centrum zmian, Powiadomienia, O aplikacji i przypomnienia otwierane przez
  jeden router SwiftUI nalezacy do glownego widoku aplikacji.
- Usunieto prezentowanie tych ekranow przez wyszukiwanie aktywnego okna UIKit,
  ktore moglo wskazywac zamykane okno menu Compose.
- Kolejkowanie przejsc podczas zamykania ekranu; obsluga dotkniecia banera.
- Bezposrednie wejscie do konfiguracji Web Push w ustawieniach powiadomien.
- Testy wszystkich ekranow, powtarzanych klikniec i przejsc przy zamykaniu.

Dodane w 0.12.0:
- Opcjonalny prywatny dodatek PWA Buda Powiadomienia:
  https://buda-powiadomienia.amberline-0653.chatgpt.site
- Eksport sesji z aplikacji: RSA-OAEP SHA256 + AES-GCM, bez hasla i cookies
  wiadomosci. Serwer otrzymuje sesje dopiero po wczytaniu zaszyfrowanego pliku.
- Dodatek przechowuje zaszyfrowane dane, historie do 100 zmian i kolejke
  ponawiania wysylki. Dotkniecie Web Push otwiera historie dodatku.
- Obslugiwane: oceny, frekwencja, sprawdziany, zadania, uwagi i ogloszenia.
  Powiadomienia o wiadomosciach nie sa jeszcze obslugiwane.

Aktualny stan powiadomien (stan konfiguracji z 2026-11-12):
- Harmonogram dodatku jest wlaczony: raz na godzine, Europe/Warsaw.
  Platforma odrzucila czestotliwosc 15 minut; minimum to jedna godzina.
- Wymagane sa polaczona sesja, wlaczone sprawdzanie, zapisane urzadzenie
  i zgoda na Web Push. Sam przelacznik lokalnych powiadomien Budy nie wystarcza.
- Przy ostatniej weryfikacji serwer nie mial jeszcze sesji dziennika uzytkownika.
- Import sesji uruchamia pierwsze pobranie od razu. Pierwszy odczyt ustala
  cichy stan poczatkowy; nie wysyla starych ocen jako nowych.
- To nie jest natywny APNs Budy ani wykorzystanie uslugi push eduVULCAN.
  Lokalne sprawdzanie w aplikacji nadal zalezy od decyzji iOS.
- Odbior prawdziwego Web Push oraz poprawione ekrany wymagaja potwierdzenia
  na fizycznym iPhonie. Udana kompilacja nie potwierdza dostarczenia alertu.

Jak polaczyc dodatek:
1. Zainstaluj IPA przez AltStore.
2. Otworz dodatek w Safari, dodaj do ekranu poczatkowego, uruchom z ikony
   i zaloguj sie kontem ChatGPT wlasciciela prywatnej strony.
3. Pobierz klucz polaczenia JSON z dodatku.
4. W Budzie wybierz Powiadomienia -> Polacz sesje z dodatkiem powiadomien;
   zatwierdz eksport, wybierz klucz i zapisz zaszyfrowany plik .buda.
5. Wczytaj plik w dodatku, wlacz powiadomienia i wybierz Wyslij test.
6. Sprawdz odbior na zablokowanym iPhonie.
Usuniecie danych w dodatku usuwa jego kopie sesji. Wylogowanie z samej
aplikacji Buda nie usuwa sesji zapisanej na serwerze dodatku.

ARCHIWUM WYDAN
Ponizsze opisy dotycza historycznych wersji. W szczegolnosci dawne stwierdzenia
o braku zewnetrznego serwera dotycza wersji sprzed opcjonalnego dodatku 0.12.

BUDA iOS 0.11.0 - pulpit i informacje o aplikacji

- Nowy naglowek, menu, zielono-niebieska paleta i zaokraglona nawigacja.
- Numer wersji w naglowku pochodzi z metadanych zainstalowanej aplikacji.
- O aplikacji: wersja, build, nowosci, prywatnosc, diagnostyka do skopiowania.
- Skroty na pulpicie do centrum zmian ocen i planowania przypomnien.
- Karta trwajacej lub najblizszej lekcji: czas do konca/startu i postep.
  Dane wynikaja z pobranego planu. Karta nie potwierdza realnego trwania lekcji.
  Nie pokazuje zajec odwolanych, starych danych, blednych godzin ani danych
  z ostrzezeniem o niepelnym pobraniu planu.
- Kolory ocen nauczyciela pozostaja bez zmian.
- Powiadomienia i historia 0.10 zachowane; nadal brak APNs push.

Historyczne opisy poprzednich wersji:
BUDA iOS 0.10.0 - centrum zmian ocen

Nowe:
- Historia: filtr ucznia, wyszukiwanie, tylko nieprzeczytane, oznaczanie widocznych.
- Szczegoly zmiany: zapis wczesniej/po zmianie, opis, komentarz i lokalny czas wykrycia.
- Dotkniecie nowego banera filtruje historie do partii zmian z tego powiadomienia.
  Starsze banery bez identyfikatora partii otwieraja cala historie.
- Opcjonalny podglad ucznia, przedmiotu i oceny na banerze; domyslnie wylaczony.
- Oznaczenia przeczytania sa lokalne w Keychain, nie zmieniaja dziennika szkolnego.
- Nowe wpisy rozrozniaja konta nawet przy identycznych nazwach uczniow.
  Starsze wpisy 0.9 bez klucza konta sa filtrowane wedlug zapisanej nazwy.
- Historia nadal ma limit 100 zmian; wylogowanie usuwa historie i oznaczenia.
- Lokalne sprawdzanie, nie APNs push. iOS nadal decyduje o uruchomieniach w tle.

Biezace funkcje: logowanie eduVULCAN, uczniowie, oceny, plan, wiadomosci
z tworzeniem/szkicami, dane dziennika, lokalne przypomnienia i ograniczony
cache offline. Szczegoly ograniczen sa opisane w sekcjach kolejnych wersji.
Ponizej zachowano historyczne opisy - stwierdzenia o brakach dotycza danej wersji.

BUDA iOS 0.9.0

Zakres: logowanie eduVULCAN, wybor ucznia i semestru, oceny,
plan z zastepstwami i dodatkowymi zajeciami, frekwencja, sprawdziany,
zadania domowe, uwagi, ogloszenia, srednie szkoly oraz oceny przewidywane
(Entry_1) i koncowe (Entry_2). Inny wpis klasyfikacji jest opisany osobno.
Zalaczniki HTTPS otwieraja sie w przegladarce. Ta wersja tylko odczytuje dane:
nie wysyla rozwiazan zadan, usprawiedliwien ani wiadomosci.
Brak push. To jeszcze nie pelny odpowiednik wszystkich funkcji Etny.

Plan domyslnie obejmuje biezacy tydzien (strzalki zmieniaja tydzien).
Frekwencja: ostatnie 30 dni. Sprawdziany/zadania: 30 dni wstecz i 60 do przodu.
Dotknij zakresu dat, aby wybrac inne daty (maksymalnie 366 dni).
Wpisy sa pobierane stronicami. Blad formatu lub uprawnien jest wyswietlany,
a nie zastepowany informacja o pustej liscie. Czesc planu/podsumowania moze
byc dostepna mimo bledu drugiej czesci; komunikat opisuje ten przypadek.

Zapamietaj sesje: klucz urzadzenia, konta uczniow i sesja wiadomosci w iOS Keychain,
Domyslnie WhenUnlockedThisDeviceOnly; opcjonalne sprawdzanie ocen w tle
zmienia to na AfterFirstUnlockThisDeviceOnly. Haslo nie jest zapisywane. Dane nie sa
synchronizowane do iCloud ani wysylane na wlasny serwer Budy.
Wyloguj usuwa zapis. Sesja moze wygasnac po stronie szkoly.
Przy zmianie podpisu AltStore dostep do starego Keychain moze sie zmienic.
Nie ma pelnego trybu offline. Po wlaczeniu powiadomien lokalnych zapisujemy
w Keychain historie ocen potrzebna do porownywania zmian.

IPA: GitHub Actions, workflow Build IPA for AltStore.
Niepodpisany plik wymaga podpisania i instalacji przez AltStore.
Mac: Xcode + JDK 21; chmod +x gradlew; otworz iosApp/iosApp.xcodeproj.
Testy czesci wspolnej: ./gradlew :composeApp:jvmTest.

Kod API i host Xcode na bazie referencji MIT, zobacz NOTICE.txt i LICENSE-UPSTREAM.
To osobny klient, nie przekonwertowane etna.apk ani oficjalny klient VULCAN.
Uzytkownik potwierdzil dzialanie wersji 0.2 na swoim koncie i urzadzeniu.
Nie jest to niezalezny test kazdej funkcji. Nie gwarantujemy zgodnosci
z kazda szkola ani dostepnosci wszystkich danych w danym planie konta.
Nowe w 0.3: skrzynki wiadomosci, odebrane (po 50), pelna tresc i zalaczniki.
Wybierz skrzynke jawnie; nazwa ucznia nie sluzy do zgadywania skrzynki.
Otwarcie wiadomosci nie oznacza jej jako przeczytanej na serwerze.
Brak wysylania odpowiedzi. HTML wyswietlany jako tekst, bez skryptow/obrazow.
Zalaczniki otwierane tylko przez HTTPS. Brak linku oznacza koniecznosc
otwarcia zalacznika w portalu; nie zgadujemy adresu na podstawie ID.
Przegladarka moze wymagac osobnego logowania do portalu.
Po aktualizacji z 0.2 wyloguj sie i zaloguj ponownie, aby uzyskac sesje
wiadomosci. Haslo nadal nie jest zapisywane. Sesja wiadomosci moze wygasnac
niezaleznie od ocen. Blad wiadomosci nie usuwa konta ucznia.
Odczyt wiadomosci wymaga uprawnien przyznanych kontu przez eduVULCAN.
Wersja 0.3 wymaga jeszcze sprawdzenia wiadomosci na rzeczywistym koncie.

Nowe w 0.4 (build 5):
- Poprawka logowania do wiadomosci: jawne POST -> 302/303 -> GET,
  zachowanie formularza dla 307/308, wzgledne adresy i limit przekierowan.
- Przekierowania/formularze tylko do jawnie dozwolonych hostow HTTPS.
- Kody diagnostyczne bez URL, cookies, hasel i tresci odpowiedzi.
- Plan lekcji wedlug dni, godziny w kolejnosci, wyroznienie DZISIAJ.
  Brak lekcji jest pokazywany tylko bez bledu pobierania.
- Nowa zielona paleta, zaokraglone karty, systemowy tryb jasny/ciemny.
- Wyszukiwanie w pobranych ocenach/wpisach oraz temacie i nadawcy wiadomosci.
- Filtr nieprzeczytanych wiadomosci; dotyczy tylko juz pobranych stron.
Ponowne logowanie nie jest wymagane przez sama aktualizacje 0.3 -> 0.4.
Sesja serwera moze jednak wygasnac. Odczyt na zywym koncie wymaga potwierdzenia.

Nowe w 0.5 (build 6):
- Automatyczna skrzynka ucznia: zapamietany wybor, identyfikator MessageBox
  z konta, a nastepnie jednoznaczna pelna nazwa skrzynki z metadanych konta.
  Brak zgadywania po fragmencie nazwiska lub pierwszej pozycji listy.
- Jesli API nie daje jednoznacznego powiazania, wybierz skrzynke raz.
  Wybor jest zapisany oddzielnie dla ucznia razem z sesja w Keychain
  (gdy Zapamietaj sesje jest wlaczone), a nie wspolnie dla rodzenstwa.
- Zmien ucznia: szybkie menu utrzymuje aktualna zakladke.
- Odczyt folderu Wyslane, stronicowanie i wyszukiwanie; bez wysylania.
- Nowe karty wiadomosci, naglowek szczegolow, kopiowanie tresci przez
  zaznaczenie tekstu, przewijane filtry i ponawianie pobierania przy bledzie.
Uzytkownik potwierdzil pobieranie wiadomosci w 0.4. Nowe zachowanie 0.5
wymaga jeszcze potwierdzenia na jego koncie. Wyslane zalezne od API/uprawnien.

Nowe w 0.6 (build 7):
- Ekran Start po wybraniu ucznia: dzisiejszy plan oraz zadania/sprawdziany
  z terminem w najblizszych 7 dniach. Skroty do glownych funkcji.
  Sekcje pobieraja sie niezaleznie; blad jednej nie ukrywa pozostalych.
- Plan: zwijane grupy dni, wybor dnia lub Dzisiaj, biezacy tydzien,
  godziny po lewej, wyszukiwanie, Rozwin/Zwin dni. Oznaczenie Teraz wynika
  z planu i zegara telefonu; nie potwierdza faktycznego odbywania lekcji.
  Odwolane zajecia i bledne godziny nie sa oznaczane jako trwajace.
- Oceny: grupy przedmiotow lub chronologia; dotkniecie oceny otwiera
  opis i komentarz z mozliwoscia zaznaczenia/kopiowania tekstu.
  Nie wyliczamy srednich z ocen tekstowych ani nie zgadujemy wag.
- Nowy pulpit i karty, systemowy tryb jasny/ciemny; stan wyboru dnia,
  wyszukiwania i zwijania dotyczy biezacego widoku.
Start pokazuje do 8 najblizszych wpisow na sekcje; pelna lista przez Wszystkie.
Nie jest to tryb offline ani implementacja powiadomien push.
Uzytkownik potwierdzil dzialanie 0.5. Nowe widoki wymagaja sprawdzenia na iPhonie.

Nowe w 0.7 (build 8):
- Kolor oceny z Column.Color (kolor nauczyciela) ma pierwszenstwo.
  Bez poprawnego koloru: pomocnicza skala 6 zielona -> 1 czerwona;
  np/zw/bz pozostaja neutralne. Kontrast tekstu dobierany do tla.
- Czytelniejsze kafelki ocen i legenda; kolor zachowany w szczegolach.
- Lokalne powiadomienia o nowych/zmienionych ocenach wszystkich zapisanych
  uczniow. Pierwszy odczyt kazdego konta/semestru zapisuje cicha baze.
  Kolejne odczyty porownuja dane; chwilowo pusta odpowiedz nie zeruje bazy.
- Powiadomienia -> Wlacz -> Sprawdz teraz / Test banera i dzwieku.
  Potrzebne Zapamietaj sesje i zgoda iOS. Baner nie ujawnia nazwiska ani ocen.
- Sprawdzanie po otwarciu i co 5 minut podczas aktywnego uzywania.
  BGAppRefreshTask z earliestBeginDate za 30 minut: to prosba, nie harmonogram.
  iOS decyduje, czy/kiedy uruchomi sprawdzenie; wymuszone zamkniecie,
  oszczedzanie energii i ustawienia systemu moga je ograniczac.
- Po wlaczeniu sesja/historia w Keychain ThisDeviceOnly sa dostepne w tle
  po pierwszym odblokowaniu od restartu. Wylaczenie przywraca WhenUnlocked.
  Haslo nadal nie jest zapisywane; brak zewnetrznego serwera przechowujacego sesje.
- Wylogowanie/wylaczenie zapamietywania sesji wylacza sprawdzanie.
  Wylogowanie usuwa takze historie porownan i dostarczone powiadomienia Budy.
To NIE jest wdrozenie APNs push. Brak nadawcy APNs i podpisu z odpowiednim
uprawnieniem. Test banera sprawdza lokalne wyswietlanie, nie prace w tle.
Wersja nie dodaje jeszcze powiadomien o wiadomosciach i pozostalych wpisach.
Porownanie Etny z Buda0.6: ETNA-AUDIT.txt. Nie mamy pelnej zgodnosci funkcji.

Nowe w 0.8 (build 9):
- Dolna nawigacja Start / Plan / Oceny / Skrzynka / Wiecej, autorskie ikony,
  nowa paleta indygo i jasne/ciemne karty. Kolory nauczycieli bez zmian.
- Pulpit z licznikami lekcji dzis (bez odwolanych) i terminow na 7 dni;
  blad sekcji pokazuje kreske zamiast mylacego zera.
- Dzwonek: niezalezny test lokalnego banera teraz lub za 15 sekund.
  Do testu nie trzeba zapisanej sesji ani wlaczonych sprawdzen ocen.
- Diagnostyka zgody, alertow, dzwieku, odswiezania w tle, oszczedzania
  energii i dostepnosci sesji; zapamietany ostatni wynik i uruchomienie tla.
- Moje przypomnienia: wlasny tytul, data/godzina, lista przyszlych i usuwanie.
  Tytul na blokadzie domyslnie ukryty. Dane sa w lokalnym systemie powiadomien.
  Wylogowanie/wylaczenie zapamietywania usuwa przypomnienia. Wylaczenie
  samych sprawdzen ocen zachowuje wlasne przypomnienia.
- Przejscie aplikacji w stan inactive (np. dialog systemowy) nie przerywa
  automatycznie sprawdzania; przerwanie po przejsciu w tlo jest opisane.
Test pod blokada potwierdza prezentacje, nie wykrycie nowej oceny w tle.
Nie podlaczono serwera eduVULCAN ani APNs. iOS steruje czasem synchronizacji.
Przypomnienia sa lokalne i jednorazowe; zalezne od zgod i trybu skupienia.

Nowe w 0.9 (build 10):
- Historia ostatnich 100 zmian ocen: uczen, semestr, przedmiot, wartosc przed/po,
  opis i komentarz. Dotkniecie banera ocen otwiera historie. Dostep tez w Dzwonku.
  Szczegoly sa zapisywane przed zleceniem banera. Gdy iOS nie przyjmie banera,
  historia pozostaje dostepna; nie ponawiamy go automatycznie.
  Zmiany samych spacji/nowych linii nie wywoluja powiadomienia.
  Stare banery z 0.7/0.8 nie zawieraly historii i nie mozna ich odtworzyc.
- Skrzynka: nowa wiadomosc, lista adresatow z API, wybor wielu odbiorcow,
  potwierdzenie nadawcy/odbiorcow/tematu przed POST. Brak testowych wysylek.
  Odpowiedz to nowa wiadomosc do zweryfikowanego nadawcy, bez watkowania,
  reply-all i zalacznikow. Dostepnosc zalezy od uprawnien/sesji po stronie API.
- Jeden lokalny szkic na ucznia/skrzynke. Zapis po 500 ms od edycji i przy
  zamknieciu. Gdy wynik POST nieznany, szkic pozostaje zablokowany: sprawdz
  Wyslane przed usunieciem szkicu i ewentualna nowa wysylka. Bez auto-retry.
  Poprawne HTTP z pustym/null body lub rozpoznanym ACK oznacza przyjecie
  zadania przez serwer, nie potwierdzenie odczytu przez nauczyciela.
- Offline: ostatnie poprawne widoki ocen, planu, zadan, sprawdzianow i pozostalych
  sekcji dziennika (poza wiadomosciami). Klucz obejmuje ucznia, profil, semestr,
  sekcje i zakres dat; zapis do 20 widokow, calosc limitowana do 750 kB.
  Kopia pokazuje czas UTC, rowniez podczas odswiezania. Czastkowe bledy nie
  nadpisuja ostatniego pelnego zapisu. Inny zakres dat moze nie miec kopii.
- Kopie i szkice w osobnym Keychain ThisDeviceOnly/WhenUnlocked; wymagaja
  Zapamietaj sesje do trwalego zapisu. Bez tego tylko pamiec bieżącej sesji.
  Wylogowanie/wylaczenie zapamietywania usuwa dane lokalne.
- Przypomnij mi przy zadaniu/sprawdzianie (takze na pulpicie): formularz z
  tytulem i proponowana godzina 18:00 dzien przed terminem, jesli przyszla.
  Inaczej propozycja za godzine. Uzytkownik wybiera i zatwierdza termin.
Wysylanie na prawdziwym koncie i nawigacja po dotknieciu banera wymagaja
sprawdzenia na iPhonie. Testy nie wysylaly wiadomosci do szkoly.
Powiadomienia pozostaja lokalne; nie uruchomiono APNs ani integracji push VULCAN.
