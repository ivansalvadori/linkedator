package br.ufsc.inf.lapesd.linkedator.links;

/**
 * Does not verify links, assume them all to be valid
 */
public class NullLinkVerifier implements LinkVerifier {
    @Override
    public boolean verify(String link) {
        return true;
    }
}
