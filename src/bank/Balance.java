package bank;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Неизменяемый баланс: сумма в копейках (минимальных единицах валюты) и валюта.
 * Любая операция возвращает новый объект Balance, текущий не меняется.
 */
public final class Balance implements Comparable<Balance> {

    private final long amountInKopecks;
    private final Currency currency;

    public Balance(long amountInKopecks, Currency currency) {
        this.currency = Objects.requireNonNull(currency, "Валюта не должна быть null");
        if (amountInKopecks < 0) {
            throw new IllegalArgumentException(
                    "Сумма не может быть отрицательной: " + amountInKopecks);
        }
        this.amountInKopecks = amountInKopecks;
    }

    public static Balance zero(Currency currency) {
        return new Balance(0, currency);
    }

    public long getAmountInKopecks() {
        return amountInKopecks;
    }

    public Currency getCurrency() {
        return currency;
    }

    public boolean isZero() {
        return amountInKopecks == 0;
    }

    /** Прибавить сумму в копейках (в валюте этого баланса). */
    public Balance add(long kopecks) {
        if (kopecks < 0) {
            throw new IllegalArgumentException(
                    "Нельзя прибавлять отрицательную сумму: " + kopecks);
        }
        // addExact бросит ArithmeticException при переполнении long
        return new Balance(Math.addExact(amountInKopecks, kopecks), currency);
    }

    public Balance add(Balance other) {
        requireSameCurrency(other);
        return add(other.amountInKopecks);
    }

    /** Вычесть сумму в копейках (в валюте этого баланса). */
    public Balance subtract(long kopecks) {
        if (kopecks < 0) {
            throw new IllegalArgumentException(
                    "Нельзя вычитать отрицательную сумму: " + kopecks);
        }
        if (kopecks > amountInKopecks) {
            throw new InsufficientFundsException(
                    "Недостаточно средств: на балансе " + this
                            + ", требуется " + new Balance(kopecks, currency));
        }
        return new Balance(amountInKopecks - kopecks, currency);
    }

    public Balance subtract(Balance other) {
        requireSameCurrency(other);
        return subtract(other.amountInKopecks);
    }

    /**
     * Конвертация в другую валюту по курсу из enum Currency.
     * Результат округляется до копейки (HALF_UP).
     */
    public Balance convertTo(Currency target) {
        Objects.requireNonNull(target, "Целевая валюта не должна быть null");
        if (target == currency) {
            return this;
        }
        // Обе суммы в «копейках» своей валюты (1/100 основной единицы),
        // поэтому: новая сумма = сумма * цена_исходной / цена_целевой
        long converted = BigDecimal.valueOf(amountInKopecks)
                .multiply(BigDecimal.valueOf(currency.getPriceInRublesKopecks()))
                .divide(BigDecimal.valueOf(target.getPriceInRublesKopecks()),
                        0, RoundingMode.HALF_UP)
                .longValueExact();
        return new Balance(converted, target);
    }

    private void requireSameCurrency(Balance other) {
        Objects.requireNonNull(other, "Balance не должен быть null");
        if (other.currency != currency) {
            throw new IllegalArgumentException(
                    "Валюты не совпадают: " + currency + " и " + other.currency
                            + ". Сначала вызовите convertTo()");
        }
    }

    @Override
    public int compareTo(Balance other) {
        requireSameCurrency(other);
        return Long.compare(amountInKopecks, other.amountInKopecks);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Balance)) return false;
        Balance that = (Balance) o;
        return amountInKopecks == that.amountInKopecks && currency == that.currency;
    }

    @Override
    public int hashCode() {
        return Objects.hash(amountInKopecks, currency);
    }

    @Override
    public String toString() {
        return String.format("%d.%02d %s",
                amountInKopecks / 100, amountInKopecks % 100, currency.getSymbol());
    }
}
