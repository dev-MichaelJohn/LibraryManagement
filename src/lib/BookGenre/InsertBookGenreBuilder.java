package lib.BookGenre;

import config.DatabaseConnection;

/**
 * Builder class for inserting new book records into the database.
 * 
 * @author dev-MichaelJohn
 */
public class InsertBookGenreBuilder extends BookGenreBuilder<InsertBookGenreBuilder> {
    private DatabaseConnection dbConnection;

    public InsertBookGenreBuilder() {
        super();
        this.dbConnection = DatabaseConnection.GetInstance();
    }

    @Override
    protected InsertBookGenreBuilder self() {
        return this;
    }

    /**
     * Sets the book ID for the new book genre record.
     * 
     * @param bookID The book ID of the book genre record.
     * @return The current InsertBookGenreBuilder instance.
     */
    public InsertBookGenreBuilder SetBookID(int bookID) {
        if(bookID <= 0) throw new IllegalArgumentException("Book ID must be positive");
        if(this.bookID != 0) throw new IllegalStateException("Book ID has already been set");

        this.bookID = bookID;
        return this.SetField("book_id", bookID);
    }

    /**
     * Sets the genre name for the new book genre record.
     * 
     * @param genre The genre of the book genre record.
     * @return The current InsertBookGenreBuilder instance.
     */
    public InsertBookGenreBuilder SetGenre(String genre) {
        if(genre == null || genre.trim().length() == 0) throw new IllegalArgumentException("Genre must be non-empty");
        if(this.genre != null) throw new IllegalStateException("Genre has already been set");

        this.genre = genre;
        return this.SetField("genre", genre);
    }

    /**
     * Executes the insert operation to add the new book genre record to the database.
     * 
     * @return true if the insert was successful, false otherwise.
     */
    public boolean Insert() {
        if(this.bookID == 0) throw new IllegalStateException("Book ID must be set before inserting");
        if(this.genre == null) throw new IllegalStateException("Genre must be set before inserting");

        String baseStatement = "INSERT INTO book_genres (book_id, genre) VALUES (?, ?)";
        int rowsAffected;
        try {
            rowsAffected = dbConnection.ExecuteUpdate(baseStatement, this.GetValues().toArray());
        } catch(Exception e) {
            throw e;
        }

        return rowsAffected > 0;
    }
}
