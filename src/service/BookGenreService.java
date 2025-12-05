package service;

import lib.BookGenre.DeleteBookGenreBuilder;
import lib.BookGenre.InsertBookGenreBuilder;
import lib.BookGenre.ReadBookGenreBuilder;
import lib.BookGenre.UpdateBookGenreBuilder;

/**
 * Service class providing static methods to access book genre CRUD functionalities.
 * 
 * @author Darkuz69
 */
public class BookGenreService {
    /**
     * Method that returns a builder to insert a new book genre into the database.
     * 
     * @return InsertBookGenreBuilder instance
     */
    public static InsertBookGenreBuilder InsertBookGenre() {
        return new InsertBookGenreBuilder();
    }

    /**
     * Method that returns a builder to read book genres from the database.
     * 
     * @return ReadBookGenreBuilder instance
     */
    public static ReadBookGenreBuilder ReadBookGenre() {
        return new ReadBookGenreBuilder();
    }

    /**
     * Method that returns a builder to update existing book genres in the database.
     * 
     * @return UpdateBookGenreBuilder instance
     */
    public static UpdateBookGenreBuilder UpdateBookGenre() {
        return new UpdateBookGenreBuilder();
    }

    /**
     * Method that returns a builder to delete book genres from the database.
     * 
     * @return DeleteBookGenreBuilder instance
     */
    public static DeleteBookGenreBuilder DeleteBookGenre() {
        return new DeleteBookGenreBuilder();
    }
}
