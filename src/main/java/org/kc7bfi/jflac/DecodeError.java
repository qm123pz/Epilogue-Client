package org.kc7bfi.jflac;

import java.io.IOException;

public class DecodeError extends IOException {

    public DecodeError(String message) {
        super(message);
    }

}
