package lib.Book;

import config.DatabaseConnection;

/**
 * Builder class for updating book records in the database.
 * 
 * @author Dev-MichaelJohn
 */
public class UpdateBookBuilder extends BookBuilder<UpdateBookBuilder> {
    private DatabaseConnection dbConnection;

    public UpdateBookBuilder() {
        super();
        this.dbConnection = DatabaseConnection.GetInstance();
    }

    @Override
    protected UpdateBookBuilder self() {
        return this;
    }

    /**
     * Sets the title of the book to be updated.
     * 
     * @param title The title of the book.
     * @return The current UpdateBookBuilder instance.
     */
    public UpdateBookBuilder SetTitle(String title) {
        if(title == null || title.trim().isEmpty()) throw new IllegalArgumentException("Title cannot be null or empty");
        if(title.length() > 255) throw new IllegalArgumentException("Title cannot exceed 255 characters");
        if(this.title != null) throw new IllegalStateException("Title has already been set");

        this.title = title;
        return this.SetField("title", title);
    }

    /**
     * Sets the author of the book to be updated.
     * 
     * @param author The author of the book.
     * @return The current UpdateBookBuilder instance.
     */
    public UpdateBookBuilder SetAuthor(String author) {
        if(author == null || author.trim().isEmpty()) throw new IllegalArgumentException("Author cannot be null or empty");
        if(author.length() > 255) throw new IllegalArgumentException("Author cannot exceed 255 characters");
        if(this.author != null) throw new IllegalStateException("Author has already been set");

        this.author = author;
        return this.SetField("author", author);
    }

    /**
     * Sets the ISBN of the book to be updated.
     * 
     * @param isbn The ISBN of the book.
     * @return The current UpdateBookBuilder instance.
     */
    public UpdateBookBuilder SetIsbn(String isbn) {
        if(isbn == null || isbn.trim().isEmpty()) throw new IllegalArgumentException("ISBN cannot be null or empty");
        if(isbn.length() > 13) throw new IllegalArgumentException("ISBN cannot exceed 13 characters");
        if(this.isbn != null) throw new IllegalStateException("ISBN has already been set");

        this.isbn = isbn;
        return this.SetField("isbn", isbn);
    }

    /**
     * Sets the year published of the book to be updated.
     * 
     * @param yearPublished The year the book was published.
     * @return The current UpdateBookBuilder instance.
     */
    public UpdateBookBuilder SetYearPublished(int yearPublished) {
        if(yearPublished < 0) throw new IllegalArgumentException("Year published cannot be negative");
        if(this.yearPublished != 0) throw new IllegalStateException("Year published has already been set");

        this.yearPublished = yearPublished;
        return this.SetField("year_published", yearPublished);
    }

    /**
     * Sets the book ID of the book to be updated.
     * 
     * @param bookID The ID of the book.
     * @return The current UpdateBookBuilder instance.
     */
    public UpdateBookBuilder WhereBookID(int bookID) {
        if(bookID <= 0) throw new IllegalArgumentException("Book ID must be positive");
        if(this.bookID != 0) throw new IllegalStateException("Book ID has already been set");

        this.bookID = bookID;
        return this;
    }

    /**
     * Sets the availability flag for the book.
     *
     * @param available true if the book is available, false otherwise
     * @return this builder
     */
    public UpdateBookBuilder SetIsAvailable(boolean available) {
        if(this.GetStatements().stream().anyMatch(s -> s.startsWith("is_available"))) throw new IllegalStateException("is_available has already been set");
        return this.SetField("is_available", available ? 1 : 0);
    }

    /**
     * Executes the update operation to modify the book record in the database.
     * 
     * @return true if the update was successful, false otherwise.
     */
    public boolean Update() {
        if(this.bookID == 0) throw new IllegalStateException("Book ID must be set for update");
        if(this.GetStatements().isEmpty()) throw new IllegalStateException("At least one field must be set for update");

        StringBuilder queryBuilder = new StringBuilder("UPDATE books SET ");
        queryBuilder.append(String.join(", ", this.GetStatements()));
        queryBuilder.append(" WHERE id = ?");

        String query = queryBuilder.toString();
        this.GetValues().add(this.bookID);
        int rowsAffected = this.dbConnection.ExecuteUpdate(query, this.GetValues().toArray());
        
        return rowsAffected > 0;
    }
}
