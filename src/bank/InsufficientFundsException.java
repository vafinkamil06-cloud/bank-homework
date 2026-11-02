package bank;

/** Выбрасывается, когда на балансе недостаточно средств для операции. */
public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException(String message) {
        super(message);
    }
}
