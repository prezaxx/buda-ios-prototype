BUDA iOS 0.1 - PROTOTYP DO TESTU API

Zakres: e-mail/haslo eduVULCAN, rejestracja urzadzenia, uczniowie,
wybor semestru i oceny. Zadania bez wlasnego serwera. Brak push.
Nie jest to przekonwertowane etna.apk. Wykorzystuje osobny kod API MIT.
Nie uruchamiano na iOS ani nie potwierdzono logowania na prawdziwym koncie.

Mac: zainstaluj Xcode obslugujacy iOS 27 oraz JDK 17 lub 21.
Rozpakuj ZIP. W Terminalu w katalogu projektu wykonaj:
  chmod +x gradlew
  ./gradlew :composeApp:jvmTest
Otworz iosApp/iosApp.xcodeproj, wybierz scheme iosApp i symulator iPhone.
Run zbuduje Kotlin framework przez Gradle (pierwszy raz pobiera zaleznosci).
Na fizycznym iPhone wybierz w Signing swoj Team i unikalny bundle identifier.
Nie dolaczono podpisow ani konta Apple. Windows nie buduje aplikacji iOS.

Test: zaloguj sie, wybierz ucznia, porownaj oceny ze strona szkoly,
zmien ucznia i semestr, sprawdz puste konto oraz bledne haslo.
Zapisz nazwe bledu z ekranu; nie przesylaj hasla/tokenow/odpowiedzi API.
Sesja i klucz urzadzenia sa tylko w pamieci. Kolejne logowanie rejestruje
nowe urzadzenie; unikaj wielokrotnych prob. Wygasla sesja wymaga logowania.
Wymagane zgody nalezy obsluzyc samodzielnie na eduvulcan.pl.
Brak gwarancji zgodnosci API ze wszystkimi szkolami i planami konta.
Kod sieciowy pochodzi z referencji; to nie potwierdzona integracja producenta.
