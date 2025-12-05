package lib.BookGenre;

import config.DatabaseConnection;

/**
 * Builder class for deleting book genre records from the database.
 * 
 * @author dev-MichaelJohn
 */
public class DeleteBookGenreBuilder extends BookGenreBuilder<DeleteBookGenreBuilder> {
    private DatabaseConnection dbConnection;    

    public DeleteBookGenreBuilder() {
        super();
        this.dbConnection = DatabaseConnection.GetInstance();
    }

    @Override
    protected DeleteBookGenreBuilder self() {
        return this;
    }

    /**
     * Sets the ID condition for deleting a book genre record.
     * 
     * @param id The ID of the book genre record.
     * @return The current DeleteBookGenreBuilder instance.
     */
    public DeleteBookGenreBuilder WhereID(int id) {
        if(id <= 0) throw new IllegalArgumentException("Book ID must be positive");
        if(this.id != 0) throw new IllegalStateException("Book ID has already been set");

        this.id = id;
        return this.SetField("id", id);
    }

    /**
     * Sets the book ID condition for deleting a book genre record.
     * 
     * @param bookID The book ID of the book genre record.
     * @return The current DeleteBookGenreBuilder instance.
     */
    public DeleteBookGenreBuilder WhereBookID(int bookID) {
        if(bookID <= 0) throw new IllegalArgumentException("Book ID must be positive");
        if(this.bookID != 0) throw new IllegalStateException("Book ID has already been set");

        this.bookID = bookID;
        return this.SetField("book_id", bookID);
    }

    /**
     * Sets the genre name condition for deleting a book genre record.
     * 
     * @param genre The genre of the book genre record.
     * @return The current DeleteBookGenreBuilder instance.
     */
    public DeleteBookGenreBuilder WhereGenre(String genre) {
        if(genre == null || genre.trim().length() == 0) throw new IllegalArgumentException("Genre must not be empty");
        if(this.genre != null) throw new IllegalStateException("Genre has already been set");

        this.genre = genre;
        return this.SetField("genre", genre);
    }

    /**
     * Executes the delete operation to remove book genre records from the database.
     * 
     * @return true if the deletion was successful, false otherwise.
     */
    public boolean Delete() {
        if(this.GetStatements().isEmpty()) throw new IllegalStateException("At least one field must be set for deletion");

        StringBuilder queryBuilder = new StringBuilder("DELETE FROM book_genres WHERE ");
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
