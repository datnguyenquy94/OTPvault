package org.fedorahosted.freeotp.common;

import org.fedorahosted.freeotp.Token;

public interface Callback {

    public void success(Token token);
    public void error(String errorMessage);
}
