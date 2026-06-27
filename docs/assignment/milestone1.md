# Milestone 1

**Deadline PR:** 17.12

Tworzymy aplikację do sterowania wielokryterialnymi algorytmami ewolucyjnymi (MOEA). Architektura klient-serwer: klientem jest konsolowa aplikacja sterująca uruchamianiem i zbieraniem rezultatów z eksperymentów na serwerze.

## Wymagania

- **Uruchamianie eksperymentu** przy użyciu biblioteki MOEA Framework. Parametry eksperymentu:
  - nazwy testowanych algorytmów
  - nazwy problemów testowych
  - nazwy metryk
  - budżet (liczba wywołań funkcji celu lub liczba iteracji)
  
  Wszystkie narzędzia dostarcza MOEA Framework — nie piszemy własnych problemów.

- **Asynchroniczne wykonanie** — eksperymenty odpalane asynchronicznie na serwerze, może trwać ich wiele jednocześnie, z uwzględnieniem ograniczeń maszyny.

- **Listowanie eksperymentów** — zarówno trwających, jak i zakończonych.

- **Status eksperymentu** — czy trwa, zakończył się, lub zakończył się błędem.

- **Rezultaty eksperymentu** — wypisywane na konsoli klienta. Format: tabelka dla każdej pary (algorytm, problem), wiersze = kolejne iteracje algorytmu, kolumny = wartości metryk (np. IGD, GD, Spacing maleją z czasem gdy algorytm działa poprawnie).

- **CLI** — obsługa API z poziomu klienta z obsługą błędów.

## Materiały

- [Dokumentacja MOEA Framework](https://github.com/MOEAFramework/MOEAFramework/tree/master/docs)
- [Przykład użycia MOEA Framework](https://github.com/Soamid/kemo)
