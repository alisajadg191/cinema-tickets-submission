# Cinema Tickets

Java 21 solution for the DWP cinema tickets coding exercise.

## Requirements

- Java 21
- Maven 3.6.3 or above

## Running the tests

From the project root:

```bash
mvn clean test
```

Alternatively, open the project in IntelliJ IDEA and run
Maven → Lifecycle → test.

## Ticket rules

| Ticket type | Price | Seat required |
|-------------|-------|---------------|
| Adult       | £25   | Yes           |
| Child       | £15   | Yes           |
| Infant      | £0    | No            |

A purchase is limited to 25 tickets, including infants.
Child and infant tickets require an adult ticket in the same purchase.

## Testing

Tests use JUnit 5 and Mockito.