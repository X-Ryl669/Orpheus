package org.opensilk.music.library.kutr.client;

/**
 * Created by cyril on 05/12/2016.
 */

public class KutrAuthException extends Exception {

    public KutrAuthException(String message) {
        super(message);
    }
    public KutrAuthException(String message, Throwable throwable) {
        super(message, throwable);
    }
    public KutrAuthException(Throwable throwable) {
        super(throwable);
    }
}
