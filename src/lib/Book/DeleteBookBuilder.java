package lib.Book;

import config.DatabaseConnection;

/**
 * Builder class for deleting book records from the database.
 * 
 * @author dev-MichaelJohn
 */
public class DeleteBookBuilder extends BookBuilder<DeleteBookBuilder> {
    private DatabaseConnection dbConnection;    

    public DeleteBookBuilder() {
        super();
        this.dbConnection = DatabaseConnection.GetInstance();
    }

    @Override
    protected DeleteBookBuilder self() {
        return this;
    }

    /**
     * Sets the title condition for deleting book records.
     * 
     * @param title The title of the book.
     * @return The current DeleteBookBuilder instance.
     */
    public DeleteBookBuilder WhereTitle(String title) {
        if(title == null || title.trim().isEmpty()) throw new IllegalArgumentException("Title cannot be null or empty");
        if(title.length() > 255) throw new IllegalArgumentException("Title cannot exceed 255 characters");
        if(this.title != null) throw new IllegalStateException("Title has already been set");

        this.title = title;
        return this.SetField("title", title);
    }

    /**
     * Sets the author condition for deleting book records.
     * 
     * @param author The author of the book.
     * @return The current DeleteBookBuilder instance.
     */
    public DeleteBookBuilder WhereAuthor(String author) {
        if(author == null || author.trim().isEmpty()) throw new IllegalArgumentException("Author cannot be null or empty");
        if(author.length() > 255) throw new IllegalArgumentException("Author cannot exceed 255 characters");
        if(this.author != null) throw new IllegalStateException("Author has already been set");

        this.author = author;
        return this.SetField("author", author);
    }

    /**
     * Sets the ISBN condition for deleting book records.
     * 
     * @param isbn The ISBN of the book.
     * @return The current DeleteBookBuilder instance.
     */
    public DeleteBookBuilder WhereIsbn(String isbn) {
        if(isbn == null || isbn.trim().isEmpty()) throw new IllegalArgumentException("ISBN cannot be null or empty");
        if(isbn.length() > 13) throw new IllegalArgumentException("ISBN cannot exceed 13 characters");
        if(this.isbn != null) throw new IllegalStateException("ISBN has already been set");

        this.isbn = isbn;
        return this.SetField("isbn", isbn);
    }

    /**
     * Sets the year published condition for deleting book records.
     * 
     * @param yearPublished The year the book was published.
     * @return The current DeleteBookBuilder instance.
     */
    public DeleteBookBuilder WhereYearPublished(int yearPublished) {
        if(yearPublished < 0) throw new IllegalArgumentException("Year published cannot be negative");
        if(this.yearPublished != 0) throw new IllegalStateException("Year Published has already been set");

        this.yearPublished = yearPublished;
        return this.SetField("year_published", yearPublished);
    }

    /**
     * Sets the book ID condition for deleting a book record.
     * 
     * @param bookID The ID of the book.
     * @return The current DeleteBookBuilder instance.
     */
    public DeleteBookBuilder WhereBookID(int bookID) {
        if(bookID <= 0) throw new IllegalArgumentException("Book ID must be positive");
        if(this.bookID != 0) throw new IllegalStateException("Book ID has already been set");

        this.bookID = bookID;
        return this.SetField("id", bookID);
    }

    /**
     * Executes the delete operation to remove book records from the database.
     * 
     * @return true if the deletion was successful, false otherwise.
     */
    public boolean Delete() {
        if(this.GetStatements().isEmpty()) throw new IllegalStateException("At least one field must be set for deletion");

        StringBuilder queryBuilder = new StringBuilder("DELETE FROM books WHERE ");
        queryBuilder.append(String.join(" AND ", this.GetStatements()));

        String query = queryBuilder.toString();
        int rowsAffected;
        try {
            System.out.println("Calling Delete...");
            rowsAffected = this.dbConnection.ExecuteUpdate(query, this.GetValues().toArray());
        } catch (Exception e) {
            System.out.println("Calling failed!");
            throw e;
        }

        System.out.println("Calling sucess...");
        return rowsAffected > 0;
    }
}
