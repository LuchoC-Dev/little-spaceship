package dev.luchoc.littlespaceship.core.architecture;

/**
 * Searching Java sources for a token, as a whole word.
 *
 * <p>The forbidden API checks are plain text searches, deliberately: they have to agree with the
 * grep a reviewer runs by hand. Whole words are the one refinement, so that a field named
 * threadCount is not read as a use of the Thread class.
 */
final class JavaSource {

    private JavaSource() {
    }

    /**
     * Finds a token as a whole word, so a type whose name merely contains the token does not count.
     *
     * @param content the text to search
     * @param token the exact token to look for
     * @return true when the token appears on its own
     */
    static boolean containsToken(String content, String token) {
        int from = 0;
        while (true) {
            int at = content.indexOf(token, from);
            if (at < 0) {
                return false;
            }
            int end = at + token.length();
            boolean startsClean = at == 0
                || !Character.isJavaIdentifierPart(content.charAt(at - 1));
            boolean endsClean = end >= content.length()
                || !Character.isJavaIdentifierPart(content.charAt(end));
            if (startsClean && endsClean) {
                return true;
            }
            from = at + 1;
        }
    }
}
