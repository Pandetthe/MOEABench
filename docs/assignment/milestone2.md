# Milestone 2

**Deadline PR:** 7.01

## Wymagania

- **Runy** — rozszerzenie modelu eksperymentu o runy (powtórzenia pary algorytm+problem). Przykład: `algorithms=[NSGAII, SPEA2], problems=[UF1, UF2, UF3], runsNo=10` → 6 par × 10 powtórzeń = 60 runów.

- **Usuwanie runów** — możliwość usunięcia wybranych runów (w szczególności całego eksperymentu).

- **Wyniki pojedynczych runów** — podgląd w tym samym formacie co wyniki eksperymentu z M1.

- **Zagregowane wyniki** — średnia, mediana, odchylenie standardowe. Miary dobierane podobnie jak metryki.

- **Filtrowanie wyników** (zagregowanych lub nie) po:
  - algorytmach
  - problemach
  - metrykach
  - identyfikatorach eksperymentów
  - numerach runów
  - datach (zakres)
