# Milestone 3

**Deadline PR:** 21.01

## Wymagania

- **Grupy runów**
  - Tworzenie grupy z wybranych runów (po id), z podaną nazwą
  - Grupa może obejmować runy z jednego lub wielu eksperymentów — runy muszą być kompatybilne (ta sama para algorytm-problem)
  - Modyfikowanie grupy (dodawanie/usuwanie runa) oraz usuwanie grupy
  - Agregacja wyników (średnia/mediana itp.) po nazwie grupy zamiast eksperymentu

- **Prezentacja wyników jako plik pobierany z serwera**
  - Tekstowa: plik CSV
  - Graficzna: wykres metryka od iteracji (generowany przez MOEA Framework)

- **Stronicowanie runów** podczas listowania

- **Testy integracyjne** (wymaganie niefunkcjonalne) — przynajmniej dla warstwy logika+baza. Zalecana biblioteka: Testcontainers (automatyczne rozstawianie bazy w Dockerze).
