package lib.BookGenre;

import config.DatabaseConnection;

/**
 * Builder class for updating book records in the database.
 * 
 * @author dev-MichaelJohn
 */
public class UpdateBookGenreBuilder extends BookGenreBuilder<UpdateBookGenreBuilder> {
    private DatabaseConnection dbConnection;

    public UpdateBookGenreBuilder() {
        super();
        this.dbConnection = DatabaseConnection.GetInstance();
    }

    @Override
    protected UpdateBookGenreBuilder self() {
        return this;
    }

    /**
     * Sets the ID condition for updating a book genre record.
     * 
     * @param id The ID of the book genre record.
     * @return The current UpdateBookGenreBuilder instance.
     */
    public UpdateBookGenreBuilder WhereID(int id) {
        if(id <= 0) throw new IllegalArgumentException("ID must be positive");
        if(this.id != 0) throw new IllegalStateException("ID has already been set");

        this.id = id;
        return this.SetField("id", id);
    }

    /**
     * Sets the new book ID value for the book genre record.
     * 
     * @param bookID The new book ID of the book genre record.
     * @return The current UpdateBookGenreBuilder instance.
     */
    public UpdateBookGenreBuilder SetBookID(int bookID) {
        if(bookID <= 0) throw new IllegalArgumentException("Book ID must be positive");
        if(this.bookID != 0) throw new IllegalStateException("Book ID has already been set");

        this.bookID = bookID;
        return this.SetField("book_id", bookID);
    }

    /**
     * Sets the new genre value for the book genre record.
     * 
     * @param genre The new genre of the book genre record.
     * @return The current UpdateBookGenreBuilder instance.
     */
    public UpdateBookGenreBuilder SetGenre(String genre) {
        if(genre == null || genre.trim().length() == 0) throw new IllegalArgumentException("Genre must be non-empty");
        if(this.genre != null) throw new IllegalStateException("Genre has already been set");

        this.genre = genre;
        return this.SetField("genre", genre);
    }

    /**
     * Executes the update operation to modify the book record in the database.
     * 
     * @return true if the update was successful, false otherwise.
     */
    public boolean Update() {
        if(this.id == 0) throw new IllegalStateException("ID must be set for update");
        if(this.GetStatements().isEmpty()) throw new IllegalStateException("At least one field must be set for update");

        StringBuilder queryBuilder = new StringBuilder("UPDATE book_genres SET ");
        queryBuilder.append(String.join(", ", this.GetStatements()));
        queryBuilder.append(" WHERE id = ?");

        String query = queryBuilder.toString();
        this.GetValues().add(this.bookID);
        int rowsAffected = this.dbConnection.ExecuteUpdate(query, this.GetValues().toArray());
        
        return rowsAffected > 0;
    }
}
