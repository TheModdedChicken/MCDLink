package org.loganshaw.mcdlink.util.errors;

public class TooManySimilarPlayerLinks extends Exception {
    public TooManySimilarPlayerLinks() {
        super("Found too many player links with the same identifier");
    }
}
