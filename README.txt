BUDA iOS 0.6.0

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
WhenUnlockedThisDeviceOnly. Haslo nie jest zapisywane. Dane nie sa
synchronizowane do iCloud ani wysylane na wlasny serwer Budy.
Wyloguj usuwa zapis. Sesja moze wygasnac po stronie szkoly.
Przy zmianie podpisu AltStore dostep do starego Keychain moze sie zmienic.
Nie przechowujemy trwale kopii ocen i pozostalych wpisow offline.

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
