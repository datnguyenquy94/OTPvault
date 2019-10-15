package org.fedorahosted.freeotp.common;

import org.fedorahosted.freeotp.Token;

public interface Callback {

    public void success(Object obj);
    public void error(String errorMessage);
}
