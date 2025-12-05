package lib.BookGenre;

import java.util.List;
import java.util.ArrayList;

/**
 * Abstract base builder class for book genre related operations.
 * 
 * @param <T> The type of the concrete builder extending this class.
 * 
 * @author dev-MichaelJohn
 */
public abstract class BookGenreBuilder<T extends BookGenreBuilder<T>> {
    private List<String> statements;
    private List<Object> values;
    public int id;
    public int bookID;
    public String genre;

    public BookGenreBuilder() {
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
        // assume the caller provided the full condition (e.g. "title LIKE ?").
        // In that case do not append " = ?" again (that caused "... = ? = ?").
        if (parameter != null && parameter.indexOf('?') >= 0) {
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
