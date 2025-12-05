package lib.Book;

import java.util.List;
import java.util.Map;

import config.DatabaseConnection;

/**
 * Builder class for reading book records from the database.
 * 
 * @author dev-MichaelJohn
 */
public class ReadBookBuilder extends BookBuilder<ReadBookBuilder> {
    private DatabaseConnection dbConnection;
    
    public ReadBookBuilder() {
        super();
        this.dbConnection = DatabaseConnection.GetInstance();
    }

    @Override
    protected ReadBookBuilder self() {
        return this;
    }
    
    /**
     * Sets the book ID condition for fetching book records.
     * 
     * @param bookID The ID of the book to filter by.
     * @return The current ReadBookBuilder instance.
     */
    public ReadBookBuilder WhereBookID(int bookID) {
        if(bookID <= 0) throw new IllegalArgumentException("Book ID must be greater than 0");
        if(this.bookID != 0) throw new IllegalStateException("Book ID has already been set");

        this.bookID = bookID;
        return this.SetField("id", bookID);
    }

    /**
     * Sets the title condition for fetching book records.
     * 
     * @param title The title of the book.
     * @return The current ReadBookBuilder instance.
     */
    public ReadBookBuilder WhereTitle(String title) {
        if(title == null || title.trim().isEmpty()) throw new IllegalArgumentException("Title cannot be null or empty");
        if(title.length() > 255) throw new IllegalArgumentException("Title cannot exceed 255 characters");
        if(this.title != null) throw new IllegalStateException("Title has already been set");

        this.title = title;
        // Use SQL placeholder and pass a wildcarded value so the builder
        // composes "title LIKE ?" and the value becomes e.g. "term%".
        return this.SetField("title LIKE ?", title + "%");
    }

    /**
     * Sets the author condition for fetching book records.
     * 
     * @param author The author of the book.
     * @return The current ReadBookBuilder instance.
     */
    public ReadBookBuilder WhereAuthor(String author) {
        if(author == null || author.trim().isEmpty()) throw new IllegalArgumentException("Author cannot be null or empty");
        if(author.length() > 255) throw new IllegalArgumentException("Author cannot exceed 255 characters");
        if(this.author != null) throw new IllegalStateException("Author has already been set");

        this.author = author;
        return this.SetField("author LIKE ?", author + "%");
    }

    /**
     * Sets the ISBN condition for fetching book records.
     * 
     * @param isbn The ISBN of the book.
     * @return The current ReadBookBuilder instance.
     */
    public ReadBookBuilder WhereIsbn(String isbn) {
        if(isbn == null || isbn.trim().isEmpty()) throw new IllegalArgumentException("ISBN cannot be null or empty");
        if(isbn.length() > 13) throw new IllegalArgumentException("ISBN cannot exceed 13 characters");
        if(this.isbn != null) throw new IllegalStateException("ISBN has already been set");

        this.isbn = isbn;
        return this.SetField("isbn LIKE ?", isbn + "%");
    }

    /**
     * Sets the year published condition for fetching book records.
     * 
     * @param yearPublished The year the book was published.
     * @return The current ReadBookBuilder instance.
     */
    public ReadBookBuilder WhereYearPublished(int yearPublished) {
        if(this.yearPublished != 0) throw new IllegalStateException("Year Published has already been set");

        this.yearPublished = yearPublished;
        return this.SetField("year_published = ?", yearPublished);
    }

    /**
     * Executes the read operation to fetch book records from the database.
     * 
     * @return A list of maps representing the fetched book records.
     */
    public List<Map<String, Object>> Read() {
        StringBuilder queryBuilder = new StringBuilder("SELECT * FROM books");
        if(!this.GetStatements().isEmpty()) {
            queryBuilder.append(" WHERE ");
            queryBuilder.append(String.join(" AND ", this.GetStatements()));
        }

        String query = queryBuilder.toString();
        List<Map<String, Object>> results;
        try {
            System.out.println("Calling Read...");
            results = this.dbConnection.ExecuteQuery(query, this.GetValues().toArray());
        } catch(Exception e) {
            System.out.println("Calling Failed...");
            throw e;
        }

        System.out.println("Calling success... " + results.toString());
        return results;
    }
}
