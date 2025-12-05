package lib.BookGenre;

import java.util.List;
import java.util.Map;

import config.DatabaseConnection;

/**
 * Builder class for reading book records from the database.
 * 
 * @author dev-MichaelJohn
 */
public class ReadBookGenreBuilder extends BookGenreBuilder<ReadBookGenreBuilder> {
    private DatabaseConnection dbConnection;
    
    public ReadBookGenreBuilder() {
        super();
        this.dbConnection = DatabaseConnection.GetInstance();
    }

    @Override
    protected ReadBookGenreBuilder self() {
        return this;
    }
    
    /**
     * Sets the book ID condition for filtering book genre records.
     * 
     * @param bookID The book ID of the book genre record.
     * @return The current ReadBookGenreBuilder instance.
     */
    public ReadBookGenreBuilder WhereBookID(int bookID) {
        if(bookID <= 0) throw new IllegalArgumentException("Book ID must be positive");
        if(this.bookID != 0) throw new IllegalStateException("Book ID has already been set");

        this.bookID = bookID;
        return this.SetField("book_id", bookID);
    }

    /**
     * Sets the genre name condition for filtering book genre records.
     * Uses LIKE matching with the provided prefix.
     * 
     * @param genre The genre prefix to search for.
     * @return The current ReadBookGenreBuilder instance.
     */
    public ReadBookGenreBuilder WhereGenre(String genre) {
        if(genre == null || genre.trim().length() == 0) throw new IllegalArgumentException("Genre must be non-empty");
        if(this.genre != null) throw new IllegalStateException("Genre has already been set");

        this.genre = genre;
        return this.SetField("genre LIKE ?", genre + "%");
    }


    /**
     * Executes the read operation to fetch book records from the database.
     * 
     * @return A list of maps representing the fetched book records.
     */
    public List<Map<String, Object>> Read() {
        StringBuilder queryBuilder = new StringBuilder("SELECT * FROM book_genres");
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
