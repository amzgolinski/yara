package com.amzgolinski.yara.callbacks;

import net.dean.jraw.models.LoggedInAccount;

/**
 * Created by azgolinski on 10/1/16.
 */

public interface AccountRetrievedCallback {

  public void onAccountRetrieved(LoggedInAccount account, String message);

}
