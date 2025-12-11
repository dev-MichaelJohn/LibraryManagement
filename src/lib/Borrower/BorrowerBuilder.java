package lib.Borrower;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base builder class for borrower-related operations.
 * 
 * @param <T> The type of the concrete builder extending this class.
 * 
 * @author dev-MichaelJohn
 */
public abstract class BorrowerBuilder<T extends BorrowerBuilder<T>> {
    private List<String> statements;
    private List<Object> values;

    public int id;
    public String firstName;
    public String middleName;
    public String lastName;
    public String contactNum;

    public BorrowerBuilder() {
        this.statements = new ArrayList<>();
        this.values = new ArrayList<>();
    }

    protected abstract T self();

    /**
     * Adds a field and its corresponding value to the builder.
     * 
     * @param parameter The field name.
     * @param value The value to be set for the field.
     * @return The current builder instance.
     */
    protected T SetField(String parameter, Object value) {
        // If the parameter string already contains a SQL placeholder ('?'),
        // assume the caller provided the full condition (e.g. "firstName LIKE ?").
        // In that case do not append " = ?" again (that caused "... = ? = ?").
        if(parameter != null && parameter.indexOf('?') >= 0) {
            statements.add(parameter);
        } else {
            statements.add(parameter + " = ?");
        }

        values.add(value);
        return self();
    }

    protected List<String> GetStatements() { return this.statements; }
    protected List<Object> GetValues() { return this.values; }
}
