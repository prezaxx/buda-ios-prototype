BUDA iOS 0.4.0

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
