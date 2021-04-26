package top.zibin.luban;

import java.io.IOException;

public class CompressFailException extends IOException {

    public CompressFailException() {
    }

    public CompressFailException(String message) {
        super(message);
    }

    public CompressFailException(String message, Throwable cause) {
        super(message, cause);
    }

    public CompressFailException(Throwable cause) {
        super(cause);
    }
}
