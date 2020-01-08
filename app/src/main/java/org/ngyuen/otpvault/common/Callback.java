package org.ngyuen.otpvault.common;

public interface Callback {

    public void success(Object obj);
    public void error(String errorMessage);
}
