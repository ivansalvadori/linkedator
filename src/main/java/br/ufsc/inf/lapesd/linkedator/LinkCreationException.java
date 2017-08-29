package br.ufsc.inf.lapesd.linkedator;

public class LinkCreationException extends RuntimeException {
    public LinkCreationException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public LinkCreationException(Throwable throwable) {
        super(throwable);
    }
}
