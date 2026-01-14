# TO_SR1500_koTOspring - Changelog

# Milestone I:

## Przegląd Projektu

koTOspring to platforma oparta na Spring Boot do uruchamiania i zarządzania eksperymentami optymalizacji wielokryterialnej z użyciem MOEAFramework. System umożliwia konfigurację, wykonywanie i analizę algorytmów optymalizacyjnych z różnymi problemami i wskaźnikami wydajności.

## Architektura

Projekt wykorzystuje **architekturę wielomodułową Maven**:

- **`client`** - CLI (Spring Shell)
- **`server`** - Backend REST API (Spring Boot)
- **`shared`** - Współdzielone kontrakty danych i modele między klientem a serwerem


## Główne Funkcjonalności

### 1. Zarządzanie Eksperymentami
- Tworzenie eksperymentów z wieloma konfiguracjami algorytmów
- Asynchroniczne wykonywanie algorytmów optymalizacyjnych
- Śledzenie statusu eksperymentu (QUEUED, IN_PROGRESS, SUCCESS, PARTIAL_SUCCESS, FAILED)
- Przechowywanie i pobieranie wyników eksperymentów

### 2. Wsparcie Algorytmów
- Wiele algorytmów optymalizacyjnych (NSGA-II, NSGA-III, eMOEA, itp.)
- Konfigurowalne parametry algorytmów
- Wsparcie dla różnych typów problemów (DTLZ, ZDT, itp.)


## Endpointy API

### Eksperymenty

#### Pobranie Listy Eksperymentów
```http
GET /experiments
```

#### Utworzenie Eksperymentu
```http
POST /experiments

```

#### Pobranie Pojedynczego Eksperymentu
```http
GET /experiments/{id}
```

#### Pobranie Statusu Eksperymentu
```http
GET /experiments/{id}/status

```

#### Pobranie Wyników Eksperymentu
```http
GET /experiments/{id}/result

```

#### Usunięcie Eksperymentu Z Bazy
```http
DELETE /experiments/{id}
```

#### Pobranie Statusu Części
```http
GET /experiments/{id}/parts/{partId}/status

```

#### Pobranie Wyników Części
```http
GET /experiments/{id}/parts/{partId}/result

```

### Pobranie Wszystkich Możliwych Problemów
```http
GET /problems
```

### Pobranie Wszystkich Możliwych Algorytmów
```http
GET /algorithms
```

### Pobranie Wszystkich Możliwych Metryk
```http
GET /indicators
```

## Architektura Warstwy Serwisowej

### Interfejsy Serwisów
- `ExperimentService`
- `AlgorithmRegistryService`
- `ProblemRegistryService`
- `IndicatorRegistryService`
- `ExperimentExecutionService` 

### Implementacje Serwisów
- `ExperimentServiceImpl` 
- `ExperimentExecutionService`
- `AlgorithmRegistryServiceImpl`
- `ProblemRegistryServiceImpl`
- `IndicatorRegistryServiceImpl`



## Uruchomienie programu:
- pobranie plików z repozytorium
- otworzenie za pomocą IDE (np. IntelliJ IDEA)
- uruchomienie aplikacji clienta i servera (sprawdzanie endpointów możliwe za pomocą Swaggera)

# Milestone II:

## Rozszerzenie modelu eksperymentu

Rozszerzenie modelu o klasę **ExperimentRun**
- klasa Experiment z milestone'a I została klasą ExperimentRun, która stanowi jedno z wywołań nowej klasy Experiment
- aktualna struktura: **Experiment** zawiera w sobie określoną przy tworzeniu listę pojedynczych wywołań eksperymentu (**ExperimentRun**), które z kolei zawierają swoje **ExperimentPart**, jak w milestonie I
  (Experiment -> ExperimentRun -> ExperimentPart)

## Dodanie filtrów

Aktualnie eksperymenty, ich runy oraz party runów można filtrować za pomocą parametrów: **status, nazwa algorytmu, nazwa problemu, nazwy indykatorów, budżet, daty rozpoczęcia i zakończenia**

## Agregacja wyników

Pobieranie otrzymanych wyników zgodnie z miarami statystycznymi, takimi jak **średnia, mediana, odchylenie standardowe**

## Usuwanie całych eksperymentów, a także ich konkretnych runów

## Refactor CLI

Używane w poprzednim milestonie CLI zostało usunięte i zastąpione nowym, przyjemniejszym w obsłudze CLI, napisanym za pomocą Spring Shella i TerminalUI

Instrukcja uruchomienia CLI:
- uruchom plik `client-external-console.bat` znajdujący się w projekcie
- poczekaj, aż okienko terminala będzie nasłuchiwać na porcie 5005
- za pomocą IntelliJ IDEA (lub innego IDE) ustaw konfigurację `Remote JVM Debug`, jako port ustawiając wartość 5005
- uruchom powyższą konfigurację w trybie Debug




