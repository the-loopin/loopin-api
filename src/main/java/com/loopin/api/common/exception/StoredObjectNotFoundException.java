package com.loopin.api.common.exception;

public class StoredObjectNotFoundException
    extends InvalidMediaStateException {

    public StoredObjectNotFoundException() {
        super("Uploaded object was not found in storage");
    }

    public StoredObjectNotFoundException(String message) {
        super(message);
    }
}
