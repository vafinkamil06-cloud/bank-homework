package bank;

/** Выбрасывается, когда операция превышает дневной лимит на вывод. */
public class WithdrawalLimitExceededException extends RuntimeException {

    public WithdrawalLimitExceededException(String message) {
        super(message);
    }
}
