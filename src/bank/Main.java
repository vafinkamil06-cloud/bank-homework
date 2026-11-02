package bank;

public class Main {

    public static void main(String[] args) {
        BankAccount rub = new BankAccount("RU-001", "Иван Иванов",
                new Balance(100_000_00, Currency.RUB), 50_000_00);
        BankAccount usd = new BankAccount("US-002", "Мария Петрова",
                new Balance(500_00, Currency.USD), 1_000_00);

        System.out.println("=== Начальное состояние ===");
        System.out.println(rub);
        System.out.println(usd);

        System.out.println("\n=== Неизменяемость Balance ===");
        Balance b = new Balance(10_000, Currency.RUB);
        Balance b2 = b.add(5_000);
        System.out.println("Исходный: " + b + ", после add: " + b2);

        System.out.println("\n=== Конвертация ===");
        Balance hundredUsd = new Balance(100_00, Currency.USD);
        System.out.println(hundredUsd + " = " + hundredUsd.convertTo(Currency.RUB));
        System.out.println(hundredUsd + " = " + hundredUsd.convertTo(Currency.EUR));
        System.out.println(hundredUsd + " = " + hundredUsd.convertTo(Currency.CNY));

        System.out.println("\n=== Операции со счётом ===");
        rub.deposit(2_500_50);
        rub.withdraw(10_000_00);
        System.out.println(rub);

        System.out.println("\n=== Перевод RUB -> USD (получатель получит в долларах) ===");
        rub.transfer(usd, 9_600_00);
        System.out.println(rub);
        System.out.println(usd);

        System.out.println("\n=== Корнер кейсы (все должны выбросить исключение) ===");
        attempt("Прибавить отрицательную сумму", () -> new Balance(100, Currency.RUB).add(-1));
        attempt("Создать баланс с отрицательной суммой", () -> new Balance(-5, Currency.RUB));
        attempt("Баланс с валютой null", () -> new Balance(100, null));
        attempt("Сложить рубли и доллары без конвертации",
                () -> new Balance(100, Currency.RUB).add(new Balance(100, Currency.USD)));
        attempt("Снять больше, чем есть на балансе", () -> usd.withdraw(10_000_00));
        attempt("Превысить дневной лимит", () -> rub.withdraw(45_000_00));
        attempt("Перевод самому себе", () -> rub.transfer(rub, 100));
        attempt("Перевод на null", () -> rub.transfer(null, 100));
        attempt("Нулевая сумма пополнения", () -> rub.deposit(0));
        attempt("Пустое имя владельца",
                () -> new BankAccount("X-1", "  ", Balance.zero(Currency.RUB), 0));

        System.out.println("\n=== Заморозка счёта ===");
        usd.freeze();
        attempt("Перевод на замороженный счёт", () -> rub.transfer(usd, 100_00));
        usd.unfreeze();

        System.out.println("\n=== Лимит: сброс в новый день ===");
        System.out.println("Осталось до сброса: " + rub.getRemainingDailyLimit());
        rub.resetDailyLimit();
        System.out.println("Осталось после сброса: " + rub.getRemainingDailyLimit());

        System.out.println("\n=== История операций " + usd.getAccountNumber() + " ===");
        usd.getHistory().forEach(line -> System.out.println(" - " + line));
        attempt("Изменить историю снаружи", () -> usd.getHistory().add("взлом"));
    }

    private static void attempt(String description, Runnable action) {
        try {
            action.run();
            System.out.println("[!] " + description + ": исключения НЕ было");
        } catch (RuntimeException e) {
            System.out.println("[OK] " + description + " -> "
                    + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }
}
