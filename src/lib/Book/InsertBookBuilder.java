package lib.Book;

import config.DatabaseConnection;

/**
 * Builder class for inserting new book records into the database.
 * 
 * @author dev-MichaelJohn
 */
public class InsertBookBuilder extends BookBuilder<InsertBookBuilder> {
    private DatabaseConnection dbConnection;

    public InsertBookBuilder() {
        super();
        this.dbConnection = DatabaseConnection.GetInstance();
    }

    @Override
    protected InsertBookBuilder self() {
        return this;
    }

    /**
     * Sets the title of the book to be inserted.
     * 
     * @param title The title of the book.
     * @return The current InsertBookBuilder instance.
     */
    public InsertBookBuilder SetTitle(String title) {
        if(title == null || title.trim().isEmpty()) throw new IllegalArgumentException("Title cannot be null or empty");
        if(title.length() > 255) throw new IllegalArgumentException("Title cannot exceed 255 characters");
        if(this.title != null) throw new IllegalStateException("Title has already been set");

        this.title = title;
        return this.SetField("title", title);
    }

    /**
     * Sets the author of the book to be inserted.
     * 
     * @param author The author of the book.
     * @return The current InsertBookBuilder instance.
     */
    public InsertBookBuilder SetAuthor(String author) {
        if(author == null || author.trim().isEmpty()) throw new IllegalArgumentException("Author cannot be null or empty");
        if(author.length() > 255) throw new IllegalArgumentException("Author cannot exceed 255 characters");
        if(this.author != null) throw new IllegalStateException("Author has already been set");

        this.author = author;
        return this.SetField("author", author);
    }

    /**
     * Sets the ISBN of the book to be inserted.
     * 
     * @param isbn The ISBN of the book.
     * @return The current InsertBookBuilder instance.
     */
    public InsertBookBuilder SetIsbn(String isbn) {
        if(isbn == null || isbn.trim().isEmpty()) throw new IllegalArgumentException("ISBN cannot be null or empty");
        if(isbn.length() > 13) throw new IllegalArgumentException("ISBN cannot exceed 13 characters");
        if(this.isbn != null) throw new IllegalStateException("ISBN has already been set");

        this.isbn = isbn;
        return this.SetField("isbn", isbn);
    }

    /**
     * Sets the year published of the book to be inserted.
     * 
     * @param yearPublished The year the book was published.
     * @return The current InsertBookBuilder instance.
     */
    public InsertBookBuilder SetYearPublished(int yearPublished) {
        if(yearPublished < 0) throw new IllegalArgumentException("Year published cannot be negative");
        if(this.yearPublished != 0) throw new IllegalStateException("Year published has already been set");

        this.yearPublished = yearPublished;
        return this.SetField("year_published", yearPublished);
    }

    /**
     * Executes the insert operation to add the new book record to the database.
     * 
     * @return true if the insert was successful, false otherwise.
     */
    public boolean Insert() {
        if(this.title == null) throw new IllegalStateException("Title must be set before inserting");
        if(this.author == null) throw new IllegalStateException("Author must be set before inserting");
        if(this.isbn == null) throw new IllegalStateException("ISBN must be set before inserting");
        if(this.yearPublished == 0) throw new IllegalStateException("Year published must be set before inserting");

        String baseStatement = "INSERT INTO books (title, author, isbn, year_published) VALUES (?, ?, ?, ?)";
        int rowsAffected;
        try {
            rowsAffected = dbConnection.ExecuteUpdate(baseStatement, this.GetValues().toArray());
        } catch(Exception e) {
            throw e;
        }

        return rowsAffected > 0;
    }
}
