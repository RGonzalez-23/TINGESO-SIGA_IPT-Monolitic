package cl.usach.tingeso.exception;

/**
 * BusinessRuleException is thrown when an operation violates domain business rules.
 */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
