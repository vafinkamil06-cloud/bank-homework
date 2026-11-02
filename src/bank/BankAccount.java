package bank;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Банковский счёт. Баланс неизменяемый, поэтому при каждой операции
 * в поле balance записывается новый объект.
 * Дополнительно: дневной лимит на вывод, заморозка счёта и история операций.
 */
public class BankAccount {

    private final String accountNumber;
    private final String ownerName;
    private final List<String> history = new ArrayList<>();

    private Balance balance;
    private long dailyWithdrawalLimitInKopecks;
    private long withdrawnTodayInKopecks;
    private boolean frozen;

    public BankAccount(String accountNumber, String ownerName,
                       Balance initialBalance, long dailyWithdrawalLimitInKopecks) {
        this.accountNumber = requireNotBlank(accountNumber, "Номер счёта");
        this.ownerName = requireNotBlank(ownerName, "Имя владельца");
        this.balance = Objects.requireNonNull(initialBalance, "Начальный баланс не должен быть null");
        validateLimit(dailyWithdrawalLimitInKopecks);
        this.dailyWithdrawalLimitInKopecks = dailyWithdrawalLimitInKopecks;
        record("Счёт открыт, баланс " + initialBalance);
    }

    public void deposit(long kopecks) {
        ensureNotFrozen();
        requirePositive(kopecks);
        balance = balance.add(kopecks);
        record("Пополнение +" + new Balance(kopecks, balance.getCurrency()));
    }

    public void withdraw(long kopecks) {
        ensureNotFrozen();
        requirePositive(kopecks);
        Balance newBalance = balance.subtract(kopecks);
        checkLimit(kopecks);
        balance = newBalance;
        withdrawnTodayInKopecks += kopecks;
        record("Снятие -" + new Balance(kopecks, balance.getCurrency()));
    }

    /**
     * Перевод на другой счёт. Сумма указывается в валюте ЭТОГО счёта,
     * получатель получает её в своей валюте по курсу. Лимит на вывод учитывается.
     * Сначала всё проверяется и считается, и только потом меняется состояние,
     * поэтому при ошибке ни один из счетов не изменится.
     */
    public void transfer(BankAccount target, long kopecks) {
        Objects.requireNonNull(target, "Счёт получателя не должен быть null");
        if (target == this) {
            throw new IllegalArgumentException("Нельзя переводить деньги самому себе");
        }
        ensureNotFrozen();
        target.ensureNotFrozen();
        requirePositive(kopecks);

        Balance newSourceBalance = balance.subtract(kopecks);
        checkLimit(kopecks);
        Balance received = new Balance(kopecks, balance.getCurrency())
                .convertTo(target.balance.getCurrency());
        if (received.isZero()) {
            throw new IllegalArgumentException(
                    "Сумма слишком мала: после конвертации получится 0");
        }
        Balance newTargetBalance = target.balance.add(received);

        balance = newSourceBalance;
        withdrawnTodayInKopecks += kopecks;
        target.balance = newTargetBalance;
        record("Перевод -" + new Balance(kopecks, balance.getCurrency())
                + " на счёт " + target.accountNumber);
        target.record("Перевод +" + received + " со счёта " + accountNumber);
    }

    public void freeze() {
        frozen = true;
        record("Счёт заморожен");
    }

    public void unfreeze() {
        frozen = false;
        record("Счёт разморожен");
    }

    public void setDailyWithdrawalLimit(long kopecks) {
        validateLimit(kopecks);
        dailyWithdrawalLimitInKopecks = kopecks;
        record("Новый дневной лимит: " + new Balance(kopecks, balance.getCurrency()));
    }

    /** Сбросить счётчик снятого за день (вызывать в начале нового дня). */
    public void resetDailyLimit() {
        withdrawnTodayInKopecks = 0;
        record("Дневной лимит обнулён");
    }

    public Balance getRemainingDailyLimit() {
        return new Balance(dailyWithdrawalLimitInKopecks - withdrawnTodayInKopecks,
                balance.getCurrency());
    }

    public Balance getBalance() {
        return balance; // Balance неизменяемый, отдавать наружу безопасно
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public boolean isFrozen() {
        return frozen;
    }

    public List<String> getHistory() {
        return Collections.unmodifiableList(history);
    }

    private void checkLimit(long kopecks) {
        if (withdrawnTodayInKopecks + kopecks > dailyWithdrawalLimitInKopecks) {
            throw new WithdrawalLimitExceededException(
                    "Превышен дневной лимит. Осталось: " + getRemainingDailyLimit()
                            + ", запрошено: " + new Balance(kopecks, balance.getCurrency()));
        }
    }

    private void ensureNotFrozen() {
        if (frozen) {
            throw new IllegalStateException("Счёт " + accountNumber + " заморожен");
        }
    }

    private static void requirePositive(long kopecks) {
        if (kopecks <= 0) {
            throw new IllegalArgumentException("Сумма должна быть больше нуля: " + kopecks);
        }
    }

    private static void validateLimit(long kopecks) {
        if (kopecks < 0) {
            throw new IllegalArgumentException("Лимит не может быть отрицательным: " + kopecks);
        }
    }

    private static String requireNotBlank(String value, String fieldName) {
        Objects.requireNonNull(value, "Поле «" + fieldName + "» не должно быть null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Поле «" + fieldName + "» не должно быть пустым");
        }
        return value;
    }

    private void record(String text) {
        history.add(text);
    }

    @Override
    public String toString() {
        return "BankAccount{№" + accountNumber + ", владелец=" + ownerName
                + ", баланс=" + balance
                + ", осталось по лимиту=" + getRemainingDailyLimit()
                + (frozen ? ", ЗАМОРОЖЕН" : "") + "}";
    }
}
