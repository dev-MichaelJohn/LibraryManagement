package lib.BookLoan;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Abstract base builder class for book loan related operations.
 * 
 * @param <T> The type of the concrete builder extending this class.
 * 
 * @author dev-MichaelJohn
 */
public abstract class BookLoanBuilder<T extends BookLoanBuilder<T>>  {
    private List<String> statements;
    private List<Object> values;

    public int id;
    public int bookID;
    public Date dueDate;
    public Date returnedAt;
    public Date borrowedAt;

    public BookLoanBuilder() {
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
    protected T SetField(String statement, Object value) {
        // If the parameter string already contains a SQL placeholder ('?'),
        // assume the caller provided the full condition (e.g. "title LIKE ?").
        // In that case do not append " = ?" again (that caused "... = ? = ?").
        if(statement != null && statement.indexOf('?') >= 0) statements.add(statement);
        else statements.add(statement + " = ?");

        values.add(value);
        return self();
    }

    protected List<String> GetStatements() { return statements; }
    protected List<Object> GetValues() { return values; }

}
