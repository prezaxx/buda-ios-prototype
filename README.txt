BUDA iOS 0.2.0

Zakres: logowanie eduVULCAN, wybor ucznia i semestru, oceny,
plan z zastepstwami i dodatkowymi zajeciami, frekwencja, sprawdziany,
zadania domowe, uwagi, ogloszenia, srednie szkoly oraz oceny przewidywane
(Entry_1) i koncowe (Entry_2). Inny wpis klasyfikacji jest opisany osobno.
Zalaczniki HTTPS otwieraja sie w przegladarce. Ta wersja tylko odczytuje dane:
nie wysyla rozwiazan zadan, usprawiedliwien ani wiadomosci.
Brak wiadomosci i push. To jeszcze nie pelny odpowiednik wszystkich funkcji Etny.

Plan domyslnie obejmuje biezacy tydzien (strzalki zmieniaja tydzien).
Frekwencja: ostatnie 30 dni. Sprawdziany/zadania: 30 dni wstecz i 60 do przodu.
Dotknij zakresu dat, aby wybrac inne daty (maksymalnie 366 dni).
Wpisy sa pobierane stronicami. Blad formatu lub uprawnien jest wyswietlany,
a nie zastepowany informacja o pustej liscie. Czesc planu/podsumowania moze
byc dostepna mimo bledu drugiej czesci; komunikat opisuje ten przypadek.

Zapamietaj sesje: klucz urzadzenia i konta uczniow w iOS Keychain,
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
Nowe funkcje wymagaja sprawdzenia na koncie; nie gwarantujemy zgodnosci
z kazda szkola ani dostepnosci wszystkich danych w danym planie konta.