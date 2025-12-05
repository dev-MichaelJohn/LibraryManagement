package service;

import lib.Book.DeleteBookBuilder;
import lib.Book.InsertBookBuilder;
import lib.Book.ReadBookBuilder;
import lib.Book.UpdateBookBuilder;

/**
 * Service class providing static methods to access various book CRUD functionalities.
 * 
 * @author Darkuz69
 */
public class BookService {
    /**
     * Method that returns a builder to insert a new book into the database.
     * 
     * @return InsertBookBuilder instance
     */
    public static InsertBookBuilder InsertBook() {
        return new InsertBookBuilder();
    }

    /**
     * Method that returns a builder to read books from the database.
     * 
     * @return ReadBookBuilder instance
     */
    public static ReadBookBuilder ReadBook() {
        return new ReadBookBuilder();
    }

    /**
     * Method that returns a builder to update existing books in the database.
     * 
     * @return UpdateBookBuilder instance
     */
    public static UpdateBookBuilder UpdateBook() {
        return new UpdateBookBuilder();
    }

    /**
     * Method that returns a builder to delete books from the database.
     * 
     * @return DeleteBookBuilder instance
     */
    public static DeleteBookBuilder DeleteBook() {
        return new DeleteBookBuilder();
    }
}
